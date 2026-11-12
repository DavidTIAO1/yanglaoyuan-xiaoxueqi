<template>
  <div class="family-page">
    <!-- My family ID card -->
    <div class="my-id-card">
      <div class="id-info">
        <el-icon :size="20"><Key /></el-icon>
        <div>
          <span class="id-label">我的家庭ID</span>
          <span class="id-value">{{ auth.user?.familyId }}</span>
        </div>
      </div>
      <el-button round size="small" @click="copyFamilyId">
        <el-icon><CopyDocument /></el-icon> 复制
      </el-button>
    </div>

    <div class="page-grid">
      <!-- Search & bind section -->
      <div class="bind-section">
        <h3 class="section-title">
          <el-icon><Search /></el-icon>
          添加家人
        </h3>
        <p class="section-desc">输入对方的10位家庭ID，发送绑定请求</p>

        <div class="search-row">
          <el-input
            v-model="searchId"
            placeholder="输入10位家庭ID"
            maxlength="10"
            clearable
            @input="searchId = searchId.replace(/\D/g, '')"
          />
          <el-button
            type="primary"
            @click="handleSearch"
            :loading="searching"
            :disabled="searchId.length !== 10"
          >
            搜索
          </el-button>
        </div>

        <!-- Search result -->
        <div v-if="searchResult" class="search-result">
          <div class="result-card">
            <el-avatar :size="44" class="result-avatar">
              {{ searchResult.realName?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="result-info">
              <span class="result-name">{{ searchResult.realName }}</span>
              <span class="result-username">@{{ searchResult.username }}</span>
              <span class="result-id">ID: {{ searchResult.familyId }}</span>
            </div>
            <el-button
              type="primary"
              round
              size="small"
              @click="showBindDialog = true"
            >
              发送请求
            </el-button>
          </div>
        </div>

        <div v-if="searchError" class="search-error">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ searchError }}</span>
        </div>
      </div>

      <!-- Family members section -->
      <div class="members-section">
        <h3 class="section-title">
          <el-icon><User /></el-icon>
          家庭成员
        </h3>

        <div v-if="membersLoading" class="loading-state">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        </div>

        <div v-else-if="members.length === 0" class="empty-members">
          <el-icon :size="40" color="#D1D5DB"><UserFilled /></el-icon>
          <p>还没有绑定的家人</p>
          <p class="hint">通过上方搜索添加，或将您的家庭ID分享给家人</p>
        </div>

        <div v-else class="members-list">
          <div v-for="member in members" :key="member.userId" class="member-card">
            <el-avatar :size="40" class="member-avatar">
              {{ member.realName?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="member-info">
              <div class="member-top">
                <span class="member-name">{{ member.realName }}</span>
                <el-tag size="small" type="info" effect="plain" round>
                  {{ member.relationship || '家人' }}
                </el-tag>
              </div>
              <span class="member-phone">{{ member.phone }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bind dialog -->
    <el-dialog v-model="showBindDialog" title="发送绑定请求" width="400px" :close-on-click-modal="false">
      <p style="margin-bottom: 16px; color: #6B7280; font-size: 0.9rem;">
        向 <strong>{{ searchResult?.realName }}</strong> 发送家人绑定请求
      </p>
      <el-form label-position="top">
        <el-form-item label="关系描述" required>
          <el-select v-model="relationship" placeholder="选择关系" style="width: 100%">
            <el-option label="父母" value="父母" />
            <el-option label="子女" value="子女" />
            <el-option label="配偶" value="配偶" />
            <el-option label="兄弟姐妹" value="兄弟姐妹" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBindDialog = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="handleBind">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  Key, CopyDocument, Search, User, UserFilled,
  WarningFilled, Loading
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { familyApi } from '../../api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()

const searchId = ref('')
const searching = ref(false)
const searchResult = ref(null)
const searchError = ref('')
const showBindDialog = ref(false)
const relationship = ref('')
const binding = ref(false)
const members = ref([])
const membersLoading = ref(false)

onMounted(() => loadMembers())

async function handleSearch() {
  if (searchId.value.length !== 10) return
  searching.value = true
  searchResult.value = null
  searchError.value = ''
  try {
    const res = await familyApi.search(searchId.value)
    if (res.data) {
      if (res.data.familyId === auth.user?.familyId) {
        searchError.value = '不能绑定自己'
      } else {
        searchResult.value = res.data
      }
    } else {
      searchError.value = '未找到该家庭ID对应的用户'
    }
  } catch {
    searchError.value = '搜索失败，请检查ID是否正确'
  } finally {
    searching.value = false
  }
}

async function handleBind() {
  if (!relationship.value) {
    ElMessage.warning('请选择关系')
    return
  }
  binding.value = true
  try {
    await familyApi.bind({
      targetFamilyId: searchResult.value.familyId,
      relationship: relationship.value
    })
    ElMessage.success('绑定请求已发送，等待对方确认')
    showBindDialog.value = false
    searchResult.value = null
    searchId.value = ''
    relationship.value = ''
    await loadMembers()
  } catch {
    // handled by interceptor
  } finally {
    binding.value = false
  }
}

async function loadMembers() {
  membersLoading.value = true
  try {
    const res = await familyApi.members()
    members.value = res.data || []
  } catch {
    // silent
  } finally {
    membersLoading.value = false
  }
}

function copyFamilyId() {
  if (!auth.user?.familyId) return
  navigator.clipboard.writeText(auth.user.familyId).then(() => {
    ElMessage.success('家庭ID已复制')
  }).catch(() => {
    ElMessage.info('家庭ID: ' + auth.user.familyId)
  })
}
</script>

<style scoped>
.family-page {
  max-width: 860px;
}

/* ── My ID card ── */
.my-id-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #EEF2FF, #C7D2FE);
  border-radius: 14px;
  padding: 18px 22px;
  margin-bottom: 24px;
}

.id-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #4F46E5;
}

.id-label {
  display: block;
  font-size: 0.78rem;
  color: #6366F1;
  margin-bottom: 2px;
}

.id-value {
  font-size: 1.2rem;
  font-weight: 700;
  font-family: monospace;
  color: #3730A3;
  letter-spacing: 1px;
}

/* ── Page grid ── */
.page-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 768px) {
  .page-grid { grid-template-columns: 1fr; }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 1rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.section-desc {
  font-size: 0.82rem;
  color: #9CA3AF;
  margin-bottom: 16px;
}

/* ── Bind section ── */
.bind-section,
.members-section {
  background: white;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.search-row {
  display: flex;
  gap: 10px;
}

.search-row .el-input {
  flex: 1;
}

.search-result {
  margin-top: 16px;
}

.result-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #F9FAFB;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
}

.result-avatar {
  background: linear-gradient(135deg, #4F46E5, #818CF8);
  color: white;
  font-weight: 600;
}

.result-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.result-name {
  font-weight: 600;
  color: #1F2937;
  font-size: 0.95rem;
}

.result-username {
  font-size: 0.8rem;
  color: #9CA3AF;
}

.result-id {
  font-size: 0.75rem;
  color: #9CA3AF;
  font-family: monospace;
}

.search-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  color: #DC2626;
  font-size: 0.85rem;
}

/* ── Members ── */
.loading-state {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.empty-members {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 0;
  color: #9CA3AF;
  text-align: center;
}

.empty-members p {
  margin-top: 10px;
  font-size: 0.9rem;
}

.empty-members .hint {
  font-size: 0.8rem;
  color: #D1D5DB;
  margin-top: 4px;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #F9FAFB;
  border-radius: 12px;
  border: 1px solid #F3F4F6;
  transition: border-color 0.2s;
}

.member-card:hover {
  border-color: #C7D2FE;
}

.member-avatar {
  background: linear-gradient(135deg, #4F46E5, #818CF8);
  color: white;
  font-weight: 600;
}

.member-info {
  flex: 1;
}

.member-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.member-name {
  font-weight: 600;
  color: #1F2937;
  font-size: 0.92rem;
}

.member-phone {
  font-size: 0.8rem;
  color: #9CA3AF;
}
</style>
