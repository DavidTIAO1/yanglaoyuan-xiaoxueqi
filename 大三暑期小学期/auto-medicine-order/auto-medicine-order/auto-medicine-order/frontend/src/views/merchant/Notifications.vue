<template>
  <div class="notifications-page">
    <!-- Header row -->
    <div class="page-top" v-if="unreadCount > 0">
      <el-button text style="color: #059669;" @click="handleMarkAllRead">
        全部已读
      </el-button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <!-- Empty -->
    <div v-else-if="notifications.length === 0" class="empty-state">
      <el-icon :size="56" color="#A7F3D0"><Bell /></el-icon>
      <p>暂无通知</p>
    </div>

    <!-- Notification list -->
    <div v-else class="notification-list">
      <div
        v-for="n in notifications"
        :key="n.id"
        class="notification-item"
        :class="{ unread: !n.isRead, clickable: !!n.relatedOrderId }"
        @click="handleClick(n)"
      >
        <div class="noti-icon" :class="'type-' + n.type">
          <el-icon :size="18"><component :is="typeIcon(n.type)" /></el-icon>
        </div>
        <div class="noti-body">
          <div class="noti-header">
            <span class="noti-title">{{ n.title }}</span>
            <span v-if="!n.isRead" class="unread-dot"></span>
          </div>
          <p class="noti-content">{{ n.content }}</p>
          <div class="noti-footer">
            <span class="noti-time">{{ formatTime(n.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell, Loading, ShoppingCart, GoodsFilled,
  CreditCard, UserFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { notificationApi } from '../../api'

const router = useRouter()
const loading = ref(false)
const notifications = ref([])
const unreadCount = ref(0)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      notificationApi.list(),
      notificationApi.unreadCount()
    ])
    notifications.value = (listRes.data || []).sort((a, b) =>
      new Date(b.createdAt) - new Date(a.createdAt)
    )
    unreadCount.value = countRes.data || 0
  } catch {
    // silent
  } finally {
    loading.value = false
  }
}

function typeIcon(type) {
  const map = {
    NEW_ORDER: 'ShoppingCart',
    ORDER_STATUS: 'GoodsFilled',
    PAYMENT: 'CreditCard',
    FAMILY: 'UserFilled'
  }
  return map[type] || 'Bell'
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function handleClick(n) {
  if (!n.isRead) {
    try {
      await notificationApi.markRead(n.id)
      n.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {}
  }
  if (n.relatedOrderId) {
    router.push('/merchant/orders')
  }
}

async function handleMarkAllRead() {
  try {
    const promises = notifications.value
      .filter(n => !n.isRead)
      .map(n => notificationApi.markRead(n.id))
    await Promise.all(promises)
    notifications.value.forEach(n => (n.isRead = true))
    unreadCount.value = 0
    ElMessage.success('已全部标为已读')
  } catch {}
}
</script>

<style scoped>
.notifications-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
}

.page-top {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

/* ── States ── */
.loading-state,
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9CA3AF;
}

.empty-state p {
  margin-top: 12px;
  font-size: 0.95rem;
}

/* ── Notification list ── */
.notification-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  background: white;
  border-radius: 14px;
  border: 1px solid #F3F4F6;
  transition: all 0.2s;
}

.notification-item.unread {
  background: #F0FDF9;
  border-color: #A7F3D0;
}

.notification-item.clickable {
  cursor: pointer;
}

.notification-item.clickable:hover {
  box-shadow: 0 2px 10px rgba(5, 150, 105, 0.1);
  transform: translateY(-1px);
}

.noti-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type-NEW_ORDER     { background: #D1FAE5; color: #059669; }
.type-ORDER_STATUS  { background: #DBEAFE; color: #2563EB; }
.type-PAYMENT       { background: #FEF3C7; color: #D97706; }
.type-FAMILY        { background: #FEE2E2; color: #DC2626; }

.noti-body {
  flex: 1;
  min-width: 0;
}

.noti-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.noti-title {
  font-weight: 600;
  font-size: 0.92rem;
  color: #1F2937;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #059669;
  flex-shrink: 0;
}

.noti-content {
  font-size: 0.85rem;
  color: #6B7280;
  line-height: 1.5;
  margin-bottom: 6px;
}

.noti-footer {
  display: flex;
  align-items: center;
  gap: 12px;
}

.noti-time {
  font-size: 0.75rem;
  color: #9CA3AF;
}
</style>
