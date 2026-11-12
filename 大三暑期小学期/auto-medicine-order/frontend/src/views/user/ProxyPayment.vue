<template>
  <div class="proxy-payment-page">
    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <!-- Empty state -->
    <div v-else-if="orders.length === 0" class="empty-state">
      <el-icon :size="56" color="#C7D2FE"><Wallet /></el-icon>
      <p>暂无需要代付的订单</p>
      <span class="empty-hint">当您的亲属下单后，待支付订单会出现在这里</span>
    </div>

    <!-- Order cards -->
    <div v-else class="order-cards">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card"
      >
        <!-- Card header: who placed the order -->
        <div class="card-header">
          <div class="user-info">
            <el-icon :size="18" color="#4F46E5"><UserFilled /></el-icon>
            <span class="user-name">{{ order.userRealName || '亲属' }}</span>
            <el-tag type="warning" effect="light" size="small" round>待代付</el-tag>
          </div>
          <span class="order-no">{{ order.orderNo }}</span>
        </div>

        <!-- Items preview -->
        <div class="card-items">
          <div v-for="(item, i) in order.items" :key="i" class="item-row">
            <span class="item-name">{{ item.medicineName }}</span>
            <span class="item-qty">x{{ item.quantity }}</span>
            <span class="item-subtotal">¥{{ item.subtotal }}</span>
          </div>
        </div>

        <!-- Delivery address -->
        <div class="card-address" v-if="order.deliveryAddress">
          <el-icon :size="14"><Location /></el-icon>
          <span>{{ order.deliveryAddress }}</span>
        </div>

        <!-- Card footer -->
        <div class="card-footer">
          <span class="order-time">{{ formatTime(order.createdAt) }}</span>
          <div class="footer-right">
            <span class="total-amount">¥{{ order.totalAmount }}</span>
            <el-button
              type="primary"
              size="small"
              round
              :loading="payingId === order.id"
              @click="handlePay(order)"
            >
              代付
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading, Wallet, UserFilled, Location } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../../api'

const loading = ref(false)
const orders = ref([])
const payingId = ref(null)

onMounted(() => loadOrders())

async function loadOrders() {
  loading.value = true
  try {
    const res = await orderApi.getProxyPayable()
    orders.value = (res.data || []).sort((a, b) =>
      new Date(b.createdAt) - new Date(a.createdAt)
    )
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

async function handlePay(order) {
  try {
    await ElMessageBox.confirm(
      `确认为 ${order.userRealName || '亲属'} 代付 ¥${order.totalAmount} ？`,
      '代付确认',
      {
        confirmButtonText: '确认代付',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    payingId.value = order.id
    await orderApi.pay(order.id)
    ElMessage.success('代付成功')
    // Remove from list
    orders.value = orders.value.filter(o => o.id !== order.id)
  } catch {
    // cancelled or error
  } finally {
    payingId.value = null
  }
}
</script>

<style scoped>
.proxy-payment-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
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
  color: #6B7280;
}

.empty-hint {
  font-size: 0.82rem;
  color: #9CA3AF;
  margin-top: 4px;
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
  border: 1px solid #F3F4F6;
  transition: box-shadow 0.2s;
}

.order-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1F2937;
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
  margin-bottom: 10px;
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

/* Address */
.card-address {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.82rem;
  color: #9CA3AF;
  margin-bottom: 10px;
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
</style>
