<template>
  <div class="dashboard-page">
    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: #ECFDF5;">
          <el-icon :size="24" color="#059669"><Calendar /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ todayOrders }}</span>
          <span class="stat-label">今日订单</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #D1FAE5;">
          <el-icon :size="24" color="#047857"><Money /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ totalRevenue.toFixed(2) }}</span>
          <span class="stat-label">总收入</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #A7F3D0;">
          <el-icon :size="24" color="#065F46"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ acceptedCount }}</span>
          <span class="stat-label">已接单</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #6EE7B7;">
          <el-icon :size="24" color="#064E3B"><Van /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ deliveringCount }}</span>
          <span class="stat-label">配送中</span>
        </div>
      </div>
    </div>

    <!-- Recent Orders -->
    <div class="recent-orders">
      <h3 class="section-title">最近订单</h3>

      <!-- Loading -->
      <div v-if="loading" class="loading-wrap">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- Empty state -->
      <div v-else-if="orders.length === 0" class="empty-state">
        <el-icon :size="56" color="#D1D5DB"><Goods /></el-icon>
        <p class="empty-text">暂无订单数据</p>
      </div>

      <!-- Orders list -->
      <div v-else class="orders-list">
        <div
          v-for="order in recentOrders"
          :key="order.id"
          class="order-card"
        >
          <div class="order-header">
            <span class="order-no">{{ order.orderNo }}</span>
            <el-tag :type="statusTagType(order.status)" size="small" effect="light">
              {{ statusLabel(order.status) }}
            </el-tag>
          </div>
          <div class="order-body">
            <span class="order-items">{{ itemsPreview(order.items) }}</span>
            <span class="order-amount">&yen;{{ order.totalAmount?.toFixed(2) }}</span>
          </div>
          <div class="order-footer">
            <span class="order-time">{{ relativeTime(order.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Calendar, Money, CircleCheck, Van, Goods } from '@element-plus/icons-vue'
import { orderApi } from '../../api'

const orders = ref([])
const loading = ref(true)

// ── Status helpers ──
const STATUS_LABELS = {
  PENDING: '待处理',
  WAITING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '已接单',
  DELIVERING: '配送中',
  DELIVERED: '已送达',
  COMPLETED: '已完成'
}

const STATUS_TAG_TYPES = {
  PENDING: 'info',
  WAITING_PAYMENT: 'warning',
  PAID: '',
  ACCEPTED: '',
  DELIVERING: '',
  DELIVERED: 'success',
  COMPLETED: 'success'
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status
}

function statusTagType(status) {
  return STATUS_TAG_TYPES[status] ?? 'info'
}

// ── Computed stats ──
function isToday(dateStr) {
  if (!dateStr) return false
  const d = new Date(dateStr)
  const now = new Date()
  return d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
}

const todayOrders = computed(() =>
  orders.value.filter(o => isToday(o.createdAt)).length
)

const totalRevenue = computed(() =>
  orders.value
    .filter(o => o.status === 'COMPLETED')
    .reduce((sum, o) => sum + (o.totalAmount || 0), 0)
)

const acceptedCount = computed(() =>
  orders.value.filter(o => o.status === 'ACCEPTED').length
)

const deliveringCount = computed(() =>
  orders.value.filter(o => o.status === 'DELIVERING').length
)

const recentOrders = computed(() =>
  [...orders.value]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 10)
)

// ── Helpers ──
function itemsPreview(items) {
  if (!items || items.length === 0) return ''
  const names = items.map(i => i.medicineName)
  return names.length > 3
    ? names.slice(0, 3).join('、') + ` 等${names.length}件`
    : names.join('、')
}

function relativeTime(dateStr) {
  if (!dateStr) return ''
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diff = now - then

  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`

  const d = new Date(dateStr)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

// ── Load data ──
onMounted(async () => {
  try {
    const res = await orderApi.getMyOrders()
    orders.value = res.data || []
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
  gap: 20px;
}

/* ── Stats Row ── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  flex-shrink: 0;
}

.stat-card {
  background: white;
  border-radius: 14px;
  padding: 20px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 1.6rem;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.stat-label {
  font-size: 0.85rem;
  color: #6B7280;
  margin-top: 2px;
}

/* ── Recent Orders ── */
.recent-orders {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.section-title {
  font-size: 1.05rem;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 14px 0;
  flex-shrink: 0;
}

.loading-wrap {
  background: white;
  border-radius: 14px;
  padding: 24px;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.empty-text {
  color: #9CA3AF;
  font-size: 0.95rem;
}

.orders-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.order-card {
  background: white;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: box-shadow 0.2s;
  flex-shrink: 0;
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.order-no {
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.85rem;
  color: #9CA3AF;
}

.order-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.order-items {
  font-size: 0.9rem;
  color: #374151;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 16px;
}

.order-amount {
  font-weight: 700;
  color: #DC2626;
  font-size: 1rem;
  flex-shrink: 0;
}

.order-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.order-time {
  font-size: 0.8rem;
  color: #9CA3AF;
}
</style>
