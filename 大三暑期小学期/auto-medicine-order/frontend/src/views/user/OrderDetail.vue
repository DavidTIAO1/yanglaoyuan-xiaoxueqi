<template>
  <div class="order-detail-page">
    <!-- Back button -->
    <div class="page-back" @click="router.push('/user/orders')">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回订单列表</span>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <template v-else-if="order">
      <!-- Status header -->
      <div class="status-header" :class="statusTheme">
        <div class="status-icon">
          <el-icon :size="28"><component :is="statusIcon" /></el-icon>
        </div>
        <div class="status-text">
          <h2>{{ statusLabel }}</h2>
          <p>{{ statusHint }}</p>
        </div>
      </div>

      <!-- Status timeline -->
      <div class="timeline-section">
        <h3 class="section-title">订单进度</h3>
        <div class="timeline">
          <div
            v-for="step in timelineSteps"
            :key="step.status"
            class="timeline-step"
            :class="{ active: step.reached, current: step.status === order.status }"
          >
            <div class="step-dot"></div>
            <div class="step-info">
              <span class="step-label">{{ step.label }}</span>
              <span class="step-time" v-if="step.time">{{ formatTime(step.time) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Order info -->
      <div class="info-section">
        <h3 class="section-title">订单信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">订单编号</span>
            <span class="info-value mono">{{ order.orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatTime(order.createdAt) }}</span>
          </div>
          <div class="info-item full">
            <span class="info-label">配送地址</span>
            <span class="info-value">{{ order.deliveryAddress }}</span>
          </div>
          <div class="info-item full" v-if="order.remark">
            <span class="info-label">备注</span>
            <span class="info-value">{{ order.remark }}</span>
          </div>
        </div>
      </div>

      <!-- Items table -->
      <div class="items-section">
        <h3 class="section-title">药品清单</h3>
        <div class="items-list">
          <div v-for="(item, i) in order.items" :key="i" class="item-row">
            <span class="item-name">{{ item.medicineName }}</span>
            <span class="item-qty">x{{ item.quantity }}</span>
            <span class="item-unit">¥{{ item.unitPrice }}/件</span>
            <span class="item-subtotal">¥{{ item.subtotal }}</span>
          </div>
        </div>
        <div class="total-row">
          <span>共 {{ totalQty }} 件商品</span>
          <span class="total-price">合计 ¥{{ order.totalAmount }}</span>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="action-section" v-if="showActions">
        <el-button
          v-if="order.status === 'PENDING' || order.status === 'WAITING_PAYMENT'"
          type="primary"
          size="large"
          round
          @click="handlePay"
        >
          立即支付 ¥{{ order.totalAmount }}
        </el-button>
        <el-button
          v-if="order.status === 'DELIVERED'"
          type="success"
          size="large"
          round
          @click="handleConfirm"
        >
          确认收货
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, Loading, Clock, CircleCheck, Van,
  GoodsFilled, Promotion, Finished
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../../api'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const order = ref(null)

const statusLabels = {
  PENDING: '待处理',
  WAITING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '已接单',
  DELIVERING: '配送中',
  DELIVERED: '已送达',
  COMPLETED: '已完成'
}

const statusHints = {
  PENDING: '请尽快完成支付，等待商家接单',
  WAITING_PAYMENT: '等待您完成支付',
  PAID: '已支付，等待商家接单',
  ACCEPTED: '商家已接单，正在准备药品',
  DELIVERING: '药品正在配送途中',
  DELIVERED: '药品已送达，请确认收货',
  COMPLETED: '订单已完成，感谢您的使用'
}

const statusThemes = {
  PENDING: 'theme-info',
  WAITING_PAYMENT: 'theme-warning',
  PAID: 'theme-primary',
  ACCEPTED: 'theme-primary',
  DELIVERING: 'theme-primary',
  DELIVERED: 'theme-success',
  COMPLETED: 'theme-success'
}

const statusIcons = {
  PENDING: 'Clock',
  WAITING_PAYMENT: 'Promotion',
  PAID: 'CircleCheck',
  ACCEPTED: 'GoodsFilled',
  DELIVERING: 'Van',
  DELIVERED: 'Finished',
  COMPLETED: 'CircleCheck'
}

const statusLabel = computed(() => statusLabels[order.value?.status] || '')
const statusHint = computed(() => statusHints[order.value?.status] || '')
const statusTheme = computed(() => statusThemes[order.value?.status] || 'theme-info')
const statusIcon = computed(() => statusIcons[order.value?.status] || 'Clock')
const showActions = computed(() =>
  ['PENDING', 'WAITING_PAYMENT', 'DELIVERED'].includes(order.value?.status)
)

const totalQty = computed(() =>
  (order.value?.items || []).reduce((s, i) => s + i.quantity, 0)
)

const statusOrder = ['PENDING', 'WAITING_PAYMENT', 'PAID', 'ACCEPTED', 'DELIVERING', 'DELIVERED', 'COMPLETED']

const timelineSteps = computed(() => {
  const currentIdx = statusOrder.indexOf(order.value?.status)
  const timeMap = {
    PENDING: order.value?.createdAt,
    WAITING_PAYMENT: order.value?.createdAt,
    PAID: order.value?.paidAt,
    ACCEPTED: order.value?.acceptedAt,
    DELIVERING: order.value?.acceptedAt,
    DELIVERED: order.value?.deliveredAt,
    COMPLETED: order.value?.deliveredAt
  }
  return statusOrder.map((s, i) => ({
    status: s,
    label: statusLabels[s],
    time: timeMap[s],
    reached: i <= currentIdx
  }))
})

onMounted(async () => {
  try {
    const res = await orderApi.getOrder(route.params.id)
    order.value = res.data
  } catch {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
})

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function handlePay() {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${order.value.totalAmount} ？`, '支付确认', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消'
    })
    await orderApi.pay(order.value.id)
    ElMessage.success('支付成功')
    order.value.status = 'PAID'
  } catch {}
}

async function handleConfirm() {
  try {
    await ElMessageBox.confirm('确认已收到药品？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消'
    })
    await orderApi.updateStatus(order.value.id, 'COMPLETED')
    ElMessage.success('已确认收货')
    order.value.status = 'COMPLETED'
  } catch {}
}
</script>

<style scoped>
.order-detail-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
  overflow-y: auto;
}

.page-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.9rem;
  color: #6B7280;
  cursor: pointer;
  margin-bottom: 16px;
  transition: color 0.2s;
}

.page-back:hover { color: #4F46E5; }

.loading-state {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

/* ── Status header ── */
.status-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: 14px;
  margin-bottom: 20px;
}

.theme-info    { background: linear-gradient(135deg, #EEF2FF, #E0E7FF); }
.theme-warning { background: linear-gradient(135deg, #FEF3C7, #FDE68A); }
.theme-primary { background: linear-gradient(135deg, #EEF2FF, #C7D2FE); }
.theme-success { background: linear-gradient(135deg, #D1FAE5, #A7F3D0); }

.status-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4F46E5;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.theme-warning .status-icon { color: #D97706; }
.theme-success .status-icon { color: #059669; }

.status-text h2 {
  font-size: 1.3rem;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 2px;
}

.status-text p {
  font-size: 0.88rem;
  color: #6B7280;
}

/* ── Section shared ── */
.section-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 14px;
}

/* ── Timeline ── */
.timeline-section {
  background: white;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  position: relative;
  padding-left: 10px;
}

.timeline-step {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  position: relative;
}

.timeline-step::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 28px;
  bottom: -10px;
  width: 2px;
  background: #E5E7EB;
}

.timeline-step:last-child::before { display: none; }

.step-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #E5E7EB;
  flex-shrink: 0;
  z-index: 1;
}

.timeline-step.active .step-dot {
  background: #4F46E5;
}

.timeline-step.current .step-dot {
  background: #4F46E5;
  box-shadow: 0 0 0 4px rgba(79,70,229,0.2);
}

.step-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.step-label {
  font-size: 0.88rem;
  color: #9CA3AF;
}

.timeline-step.active .step-label {
  color: #374151;
  font-weight: 500;
}

.timeline-step.current .step-label {
  color: #4F46E5;
  font-weight: 600;
}

.step-time {
  font-size: 0.78rem;
  color: #9CA3AF;
}

/* ── Info section ── */
.info-section {
  background: white;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.info-item.full {
  grid-column: 1 / -1;
}

.info-label {
  display: block;
  font-size: 0.78rem;
  color: #9CA3AF;
  margin-bottom: 4px;
}

.info-value {
  font-size: 0.9rem;
  color: #1F2937;
}

.info-value.mono {
  font-family: monospace;
  font-size: 0.85rem;
}

/* ── Items ── */
.items-section {
  background: white;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.items-list {
  border-top: 1px solid #F3F4F6;
  border-bottom: 1px solid #F3F4F6;
}

.item-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #F9FAFB;
  font-size: 0.9rem;
}

.item-row:last-child { border-bottom: none; }

.item-name { flex: 1; color: #374151; }
.item-qty { color: #6B7280; margin-right: 16px; }
.item-unit { color: #9CA3AF; margin-right: 16px; font-size: 0.82rem; }
.item-subtotal { font-weight: 500; color: #374151; min-width: 60px; text-align: right; }

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  font-size: 0.9rem;
  color: #6B7280;
}

.total-price {
  font-size: 1.15rem;
  font-weight: 700;
  color: #DC2626;
}

/* ── Actions ── */
.action-section {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 20px;
}

.action-section .el-button {
  min-width: 180px;
}
</style>
