<template>
  <div class="family-members-page">
    <div class="members-card">
      <h3 class="section-title">
        <el-icon><User /></el-icon>
        家庭成员
        <el-tag v-if="members.length > 0" size="small" round effect="plain" class="count-tag">
          {{ members.length }} 人
        </el-tag>
      </h3>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
      </div>

      <div v-else-if="members.length === 0" class="empty-members">
        <el-icon :size="56" color="#D1D5DB"><UserFilled /></el-icon>
        <h4>还没有绑定的家人</h4>
        <p>前往「添加家人」页面搜索并发送绑定请求</p>
        <el-button type="primary" round @click="$router.push('/user/family/add')">
          去添加家人
        </el-button>
      </div>

      <div v-else class="members-list">
        <div v-for="member in members" :key="member.userId" class="member-card">
          <el-avatar :size="48" class="member-avatar">
            {{ member.realName?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="member-info">
            <div class="member-top">
              <span class="member-name">{{ member.realName }}</span>
              <el-tag size="small" type="info" effect="plain" round>
                {{ member.relationship || '家人' }}
              </el-tag>
            </div>
            <div class="member-meta">
              <span class="member-phone">
                <el-icon :size="12"><Phone /></el-icon>
                {{ member.phone }}
              </span>
              <span class="member-username">@{{ member.username }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, UserFilled, Phone, Loading } from '@element-plus/icons-vue'
import { familyApi } from '../../api'

const members = ref([])
const loading = ref(false)

onMounted(() => loadMembers())

async function loadMembers() {
  loading.value = true
  try {
    const res = await familyApi.members()
    members.value = res.data || []
  } catch {
    // silent
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.family-members-page {
  height: calc(100vh - 64px - 48px);
  display: flex;
  flex-direction: column;
}

.members-card {
  background: white;
  border-radius: 14px;
  padding: 28px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 1.1rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 20px;
}

.count-tag {
  margin-left: 4px;
}

/* ── States ── */
.loading-state {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.empty-members {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9CA3AF;
  text-align: center;
}

.empty-members h4 {
  margin-top: 14px;
  color: #6B7280;
  font-size: 1rem;
}

.empty-members p {
  margin-top: 6px;
  font-size: 0.85rem;
  margin-bottom: 20px;
}

/* ── Members list ── */
.members-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: #F9FAFB;
  border-radius: 14px;
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
  font-size: 1.1rem;
}

.member-info {
  flex: 1;
}

.member-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.member-name {
  font-weight: 600;
  color: #1F2937;
  font-size: 1rem;
}

.member-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.member-phone,
.member-username {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.82rem;
  color: #9CA3AF;
}
</style>
