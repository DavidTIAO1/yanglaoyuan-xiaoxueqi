<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="sidebar-header">
        <div class="logo" v-show="!isCollapse">
          <span class="logo-text">商家后台</span>
        </div>
        <div class="logo-mini" v-show="isCollapse">
          <span>商</span>
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
        <el-menu-item index="/merchant/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/merchant/orders">
          <el-icon><List /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/merchant/medicines">
          <el-icon><Goods /></el-icon>
          <template #title>药品管理</template>
        </el-menu-item>
        <el-menu-item index="/merchant/shop">
          <el-icon><Setting /></el-icon>
          <template #title>店铺设置</template>
        </el-menu-item>
        <el-menu-item index="/merchant/notifications">
          <el-icon>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0"
                       :is-dot="isCollapse" :max="99"
                       color="#EF4444" class="noti-badge">
              <Bell />
            </el-badge>
          </el-icon>
          <template #title>消息通知</template>
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
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ auth.user?.realName?.charAt(0) || 'M' }}
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
  DataLine, List, Goods, Setting, Bell,
  Expand, Fold, ArrowDown, SwitchButton
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { notificationApi } from '../api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isCollapse = ref(false)

const currentRoute = computed(() => route.path)

const pageNames = {
  '/merchant/dashboard': '工作台',
  '/merchant/orders': '订单管理',
  '/merchant/medicines': '药品管理',
  '/merchant/shop': '店铺设置',
  '/merchant/notifications': '消息通知'
}

const currentPageName = computed(() => {
  for (const [path, name] of Object.entries(pageNames)) {
    if (route.path.startsWith(path)) return name
  }
  return '工作台'
})

function handleCommand(cmd) {
  if (cmd === 'logout') auth.logout()
}

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

// Refresh whenever the route changes
watch(() => route.path, () => refreshUnreadCount())

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
  background: linear-gradient(180deg, #059669 0%, #065F46 100%);
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
  background: var(--merchant-primary);
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

/* Badge on dark sidebar */
:deep(.noti-badge .el-badge__content) {
  background-color: #EF4444 !important;
  border: 2px solid #065F46;
}
</style>
