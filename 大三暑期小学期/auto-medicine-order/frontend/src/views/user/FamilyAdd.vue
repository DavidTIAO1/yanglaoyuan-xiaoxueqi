<template>
  <div class="family-add-page">
    <div class="add-card">
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
          <el-avatar :size="48" class="result-avatar">
            {{ searchResult.realName?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="result-info">
            <span class="result-name">{{ searchResult.realName }}</span>
            <span class="result-username">@{{ searchResult.username }}</span>
            <span class="result-id">ID: {{ searchResult.familyId }}</span>
          </div>
          <el-button type="primary" round @click="showBindDialog = true">
            发送请求
          </el-button>
        </div>
      </div>

      <div v-if="searchError" class="search-error">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ searchError }}</span>
      </div>

      <!-- Recent bind tips -->
      <div class="tips-section">
        <h4>使用说明</h4>
        <ul>
          <li>家庭ID为10位数字，可在上方导航栏查看并复制自己的ID</li>
          <li>发送绑定请求后，对方需要确认才能成为家人</li>
          <li>绑定成功后，家人可以为您代付药品订单</li>
        </ul>
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
import { ref } from 'vue'
import { Search, WarningFilled } from '@element-plus/icons-vue'
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
  } catch {
    // handled by interceptor
  } finally {
    binding.value = false
  }
}
</script>

<style scoped>
.family-add-page {
  height: calc(100vh - 64px - 48px);
  display: flex;
  flex-direction: column;
}

.add-card {
  background: white;
  border-radius: 14px;
  padding: 28px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  flex: 1;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 1.1rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.section-desc {
  font-size: 0.85rem;
  color: #9CA3AF;
  margin-bottom: 20px;
}

.search-row {
  display: flex;
  gap: 10px;
}

.search-row .el-input {
  flex: 1;
}

.search-result {
  margin-top: 20px;
}

.result-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #F9FAFB;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
}

.result-avatar {
  background: linear-gradient(135deg, #4F46E5, #818CF8);
  color: white;
  font-weight: 600;
  font-size: 1.1rem;
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
  font-size: 1rem;
}

.result-username {
  font-size: 0.82rem;
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
  margin-top: 16px;
  padding: 12px 16px;
  background: #FEF2F2;
  border-radius: 10px;
  color: #DC2626;
  font-size: 0.88rem;
}

/* Tips */
.tips-section {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #F3F4F6;
}

.tips-section h4 {
  font-size: 0.9rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.tips-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-section li {
  position: relative;
  padding-left: 18px;
  font-size: 0.85rem;
  color: #6B7280;
  line-height: 1.8;
}

.tips-section li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #C7D2FE;
}
</style>
