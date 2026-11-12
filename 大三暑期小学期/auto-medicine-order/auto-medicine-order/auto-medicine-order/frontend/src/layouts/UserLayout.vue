<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="sidebar-header">
        <div class="logo" v-show="!isCollapse">
          <span class="logo-text">药品配送</span>
        </div>
        <div class="logo-mini" v-show="isCollapse">
          <span>药</span>
        </div>
      </div>

      <el-menu
        :default-active="currentRoute"
        router
        :collapse="isCollapse"
        class="sidebar-menu"
        background-color="transparent"
        text-color="rgba(255,255,255,0.75)"
        active-text-color="#FFFFFF"
      >
        <el-menu-item index="/user/voice-chat">
          <el-icon><Microphone /></el-icon>
          <template #title>语音聊天</template>
        </el-menu-item>
        <el-menu-item index="/user/chat">
          <el-icon><ChatLineSquare /></el-icon>
          <template #title>文字聊天</template>
        </el-menu-item>
        <el-menu-item index="/user/orders">
          <el-icon><Document /></el-icon>
          <template #title>我的订单</template>
        </el-menu-item>
        <el-menu-item index="/user/proxy-payment">
          <el-icon><Wallet /></el-icon>
          <template #title>代付订单</template>
        </el-menu-item>
        <el-menu-item index="/user/family/add">
          <el-icon><Plus /></el-icon>
          <template #title>添加家人</template>
        </el-menu-item>
        <el-menu-item index="/user/family/members">
          <el-icon><UserFilled /></el-icon>
          <template #title>家庭成员</template>
        </el-menu-item>
        <el-menu-item index="/user/notifications">
          <el-icon>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0"
                       :is-dot="isCollapse" :max="99"
                       color="#EF4444" class="noti-badge">
              <Bell />
            </el-badge>
          </el-icon>
          <template #title>消息通知</template>
        </el-menu-item>
        <el-menu-item index="/user/profile">
          <el-icon><Setting /></el-icon>
          <template #title>个人信息</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon><component :is="isCollapse ? 'Expand' : 'Fold'" /></el-icon>
        </div>
      </div>
    </el-aside>

    <!-- Main area -->
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-name">{{ currentPageName }}</span>
        </div>
        <div class="header-right">
          <div class="family-id" v-if="auth.user?.familyId" @click="copyFamilyId">
            <el-icon :size="14"><Key /></el-icon>
            <span>{{ auth.user.familyId }}</span>
            <el-icon :size="12"><CopyDocument /></el-icon>
          </div>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ auth.user?.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="user-name">{{ auth.user?.realName || auth.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatLineSquare, Microphone, Document, UserFilled, Bell, Plus, Wallet,
  Expand, Fold, Key, CopyDocument, ArrowDown, SwitchButton, Setting
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { notificationApi } from '../api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isCollapse = ref(false)

const currentRoute = computed(() => route.path)

const pageNames = {
  '/user/voice-chat': '语音聊天',
  '/user/chat': '文字聊天',
  '/user/orders': '我的订单',
  '/user/proxy-payment': '代付订单',
  '/user/family/add': '添加家人',
  '/user/family/members': '家庭成员',
  '/user/notifications': '消息通知',
  '/user/profile': '个人信息'
}

const currentPageName = computed(() => {
  // Check order detail first (more specific path)
  if (/^\/user\/orders\/\d+/.test(route.path)) return '订单详情'
  for (const [path, name] of Object.entries(pageNames)) {
    if (route.path.startsWith(path)) return name
  }
  return '语音聊天'
})

// ── Unread notification count ──
const unreadCount = ref(0)
let pollTimer = null

async function refreshUnreadCount() {
  try {
    const res = await notificationApi.unreadCount()
    unreadCount.value = res.data ?? 0
  } catch {
    // silent — don't interrupt the user experience
  }
}

// Refresh whenever the route changes (user navigates, marks read, etc.)
watch(() => route.path, () => refreshUnreadCount())

function copyFamilyId() {
  navigator.clipboard.writeText(auth.user.familyId).then(() => {
    ElMessage.success('家庭ID已复制')
  }).catch(() => {
    ElMessage.info('家庭ID: ' + auth.user.familyId)
  })
}

function handleCommand(cmd) {
  if (cmd === 'logout') auth.logout()
}

onMounted(() => {
  refreshUnreadCount()
  pollTimer = setInterval(refreshUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* Sidebar */
.sidebar {
  background: linear-gradient(180deg, #4F46E5 0%, #3730A3 100%);
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: white;
  font-size: 1.2rem;
  font-weight: 700;
  letter-spacing: 1px;
}

.logo-mini span {
  color: white;
  font-size: 1.3rem;
  font-weight: 700;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  padding: 8px 0;
}

.sidebar-menu .el-menu-item {
  margin: 4px 8px;
  border-radius: 10px;
  height: 48px;
  line-height: 48px;
}

.sidebar-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.12) !important;
}

.sidebar-menu .el-menu-item.is-active {
  background: rgba(255, 255, 255, 0.2) !important;
  font-weight: 600;
}

.sidebar-footer {
  padding: 12px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: white;
}

/* Badge on dark sidebar — push the red dot/number slightly outward */
:deep(.noti-badge .el-badge__content) {
  background-color: #EF4444 !important;
  border: 2px solid #3730A3;
}

/* Header */
.header {
  height: 64px;
  background: white;
  border-bottom: 1px solid #E5E7EB;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.page-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1F2937;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.family-id {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #F3F4F6;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #6B7280;
  cursor: pointer;
  transition: background 0.2s;
}

.family-id:hover {
  background: #E5E7EB;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #F3F4F6;
}

.user-avatar {
  background: var(--user-primary);
  color: white;
  font-size: 0.85rem;
}

.user-name {
  font-size: 0.9rem;
  color: #374151;
  font-weight: 500;
}

/* Main content */
.main-content {
  background: #F9FAFB;
  padding: 24px;
  overflow-y: auto;
}
</style>
