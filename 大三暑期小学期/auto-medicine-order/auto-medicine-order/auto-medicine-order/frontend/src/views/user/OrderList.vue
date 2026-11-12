<template>
  <div class="order-list-page">
    <!-- Status filter tabs -->
    <div class="filter-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="currentTab = tab.value"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <!-- Empty state -->
    <div v-else-if="filteredOrders.length === 0" class="empty-state">
      <el-icon :size="56" color="#C7D2FE"><Document /></el-icon>
      <p>暂无订单</p>
    </div>

    <!-- Order cards -->
    <div v-else class="order-cards">
      <div
        v-for="order in filteredOrders"
        :key="order.id"
        class="order-card"
        @click="goDetail(order.id)"
      >
        <!-- Card header -->
        <div class="card-header">
          <span class="order-no">{{ order.orderNo }}</span>
          <el-tag :type="statusTagType(order.status)" effect="plain" round size="small">
            {{ statusLabel(order.status) }}
          </el-tag>
        </div>

        <!-- Items preview -->
        <div class="card-items">
          <div v-for="(item, i) in order.items" :key="i" class="item-row">
            <span class="item-name">{{ item.medicineName }}</span>
            <span class="item-qty">x{{ item.quantity }}</span>
            <span class="item-subtotal">¥{{ item.subtotal }}</span>
          </div>
        </div>

        <!-- Card footer -->
        <div class="card-footer">
          <span class="order-time">{{ formatTime(order.createdAt) }}</span>
          <div class="footer-right">
            <span class="total-amount">¥{{ order.totalAmount }}</span>
            <div class="card-actions" @click.stop>
              <el-button
                v-if="order.status === 'PENDING' || order.status === 'WAITING_PAYMENT'"
                type="primary"
                size="small"
                round
                @click="handlePay(order)"
              >
                立即支付
              </el-button>
              <el-button
                v-if="order.status === 'DELIVERED'"
                type="success"
                size="small"
                round
                @click="handleConfirm(order)"
              >
                确认收货
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../../api'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const currentTab = ref('ALL')

const tabs = [
  { label: '全部', value: 'ALL' },
  { label: '待处理', value: 'PENDING' },
  { label: '待支付', value: 'WAITING_PAYMENT' },
  { label: '已支付', value: 'PAID' },
  { label: '已接单', value: 'ACCEPTED' },
  { label: '配送中', value: 'DELIVERING' },
  { label: '已送达', value: 'DELIVERED' },
  { label: '已完成', value: 'COMPLETED' }
]

const statusMap = {
  PENDING: '待处理',
  WAITING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '已接单',
  DELIVERING: '配送中',
  DELIVERED: '已送达',
  COMPLETED: '已完成'
}

const statusTypeMap = {
  PENDING: 'info',
  WAITING_PAYMENT: 'warning',
  PAID: '',
  ACCEPTED: '',
  DELIVERING: '',
  DELIVERED: 'success',
  COMPLETED: 'success'
}

const filteredOrders = computed(() => {
  if (currentTab.value === 'ALL') return orders.value
  return orders.value.filter(o => o.status === currentTab.value)
})

onMounted(() => loadOrders())

async function loadOrders() {
  loading.value = true
  try {
    const res = await orderApi.getMyOrders()
    orders.value = (res.data || []).sort((a, b) =>
      new Date(b.createdAt) - new Date(a.createdAt)
    )
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function statusLabel(status) {
  return statusMap[status] || status
}

function statusTagType(status) {
  return statusTypeMap[status] || 'info'
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function goDetail(id) {
  router.push(`/user/orders/${id}`)
}

async function handlePay(order) {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${order.totalAmount} ？`, '支付确认', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'info'
    })
    await orderApi.pay(order.id)
    ElMessage.success('支付成功')
    order.status = 'PAID'
  } catch {
    // cancelled or error
  }
}

async function handleConfirm(order) {
  try {
    await ElMessageBox.confirm('确认已收到药品？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'info'
    })
    await orderApi.updateStatus(order.id, 'COMPLETED')
    ElMessage.success('已确认收货')
    order.status = 'COMPLETED'
  } catch {
    // cancelled or error
  }
}
</script>

<style scoped>
.order-list-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
}

/* ── Filter tabs ── */
.filter-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.tab-item {
  padding: 7px 16px;
  border-radius: 20px;
  font-size: 0.85rem;
  color: #6B7280;
  background: white;
  border: 1px solid #E5E7EB;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.tab-item:hover {
  border-color: #A5B4FC;
  color: #4F46E5;
}

.tab-item.active {
  background: #4F46E5;
  color: white;
  border-color: #4F46E5;
}

/* ── Loading / Empty ── */
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

/* ── Order cards ── */
.order-cards {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}

.order-card {
  background: white;
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;
  border: 1px solid #F3F4F6;
}

.order-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-no {
  font-size: 0.82rem;
  color: #9CA3AF;
  font-family: monospace;
}

/* Items */
.card-items {
  border-top: 1px solid #F3F4F6;
  border-bottom: 1px solid #F3F4F6;
  padding: 10px 0;
  margin-bottom: 12px;
}

.item-row {
  display: flex;
  align-items: center;
  padding: 4px 0;
  font-size: 0.9rem;
}

.item-name {
  flex: 1;
  color: #374151;
}

.item-qty {
  color: #9CA3AF;
  margin-right: 16px;
  font-size: 0.85rem;
}

.item-subtotal {
  font-weight: 500;
  color: #374151;
  min-width: 60px;
  text-align: right;
}

/* Footer */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-time {
  font-size: 0.8rem;
  color: #9CA3AF;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-amount {
  font-size: 1.1rem;
  font-weight: 700;
  color: #DC2626;
}

.card-actions {
  display: flex;
  gap: 8px;
}
</style>
