<template>
  <div class="chat-page">
    <!-- Chat header toolbar -->
    <div class="chat-toolbar">
      <span class="toolbar-hint">
        <el-icon :size="16"><ChatLineSquare /></el-icon>
        描述您的症状或需要的药品，AI 将为您智能匹配
      </span>
      <el-button text type="danger" :icon="Delete" @click="handleClearHistory" :disabled="messages.length === 0">
        清空对话
      </el-button>
    </div>

    <!-- Message list -->
    <div class="chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="empty-state">
        <el-icon :size="56" color="#C7D2FE"><ChatDotRound /></el-icon>
        <h3>文字聊天</h3>
        <p>请输入你需要的药品</p>
      </div>

      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="message-row"
        :class="msg.role"
      >
        <!-- AI avatar (left side) -->
        <div class="avatar ai-avatar" v-if="msg.role === 'ai'">
          <el-icon :size="20"><Monitor /></el-icon>
        </div>

        <div class="bubble-wrapper">
          <div class="bubble" v-if="msg.text">
            <span v-html="formatText(msg.text)"></span>
          </div>

          <!-- Medicine cards from AI response -->
          <div class="medicine-cards" v-if="msg.medicines && msg.medicines.length > 0">
            <div class="cards-header">
              <el-icon :size="14"><FirstAidKit /></el-icon>
              <span>为您匹配的药品</span>
            </div>
            <div
              v-for="med in msg.medicines"
              :key="med.medicineId"
              class="med-card"
              :class="{ disabled: !med.inStock, selected: isMedSelected(med.medicineId) }"
              @click="med.inStock && toggleMedSelection(med)"
            >
              <div class="med-info">
                <span class="med-name">{{ med.name }}</span>
                <span class="med-price">¥{{ med.price }}</span>
              </div>
              <div class="med-meta">
                <span v-if="med.inStock" class="in-stock">有货 · x{{ med.quantity }}</span>
                <span v-else class="out-stock">缺货</span>
                <el-icon v-if="isMedSelected(med.medicineId)" class="check-icon"><CircleCheck /></el-icon>
              </div>
            </div>
            <el-button
              v-if="msg.medicines.some(m => m.inStock)"
              type="primary"
              round
              size="small"
              class="order-btn"
              :disabled="orderedMessages.has(idx)"
              @click="showOrderDialog(msg.medicines.filter(m => m.inStock), idx)"
            >
              <el-icon v-if="!orderedMessages.has(idx)"><ShoppingCart /></el-icon>
              <el-icon v-else><CircleCheck /></el-icon>
              {{ orderedMessages.has(idx) ? '已下单' : '确认下单' }}
            </el-button>
          </div>
        </div>

        <!-- User avatar (right side) -->
        <div class="avatar user-avatar" v-if="msg.role === 'user'">
          {{ auth.user?.realName?.charAt(0) || 'U' }}
        </div>
      </div>

      <!-- Typing indicator -->
      <div v-if="sending" class="message-row ai">
        <div class="avatar ai-avatar"><el-icon :size="20"><Monitor /></el-icon></div>
        <div class="bubble-wrapper">
          <div class="bubble typing">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </div>
    </div>

    <!-- Input area -->
    <div class="chat-input-area">
      <div class="input-wrapper">
        <el-input
          v-model="inputText"
          placeholder="描述您的症状或药品需求..."
          :autosize="{ minRows: 1, maxRows: 4 }"
          type="textarea"
          resize="none"
          @keydown.enter.exact.prevent="handleSend"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          :loading="sending"
          :disabled="!inputText.trim()"
          circle
          class="send-btn"
          @click="handleSend"
        />
      </div>
    </div>

    <!-- Order dialog -->
    <el-dialog v-model="orderDialogVisible" title="确认订单" width="460px" :close-on-click-modal="false">
      <div class="order-items-preview">
        <div v-for="item in selectedMedicines" :key="item.medicineId" class="order-item-row">
          <span>{{ item.name }}</span>
          <span>x{{ item.quantity }}</span>
          <span class="item-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
        </div>
        <div class="order-total">
          <span>合计</span>
          <span class="total-price">¥{{ orderTotal }}</span>
        </div>
      </div>
      <el-form :model="orderForm" label-position="top" style="margin-top: 16px;">
        <el-form-item label="配送地址" required>
          <el-input v-model="orderForm.deliveryAddress" placeholder="请输入详细配送地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="2" placeholder="如有特殊要求请备注（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingOrder" @click="handleCreateOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatLineSquare, ChatDotRound, Delete, Promotion,
  Monitor, FirstAidKit, ShoppingCart, CircleCheck
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi, orderApi } from '../../api'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const messagesRef = ref(null)
const inputText = ref('')
const sending = ref(false)
const messages = ref([])
const selectedMedicines = ref([])
const orderDialogVisible = ref(false)
const creatingOrder = ref(false)
const orderForm = ref({ deliveryAddress: '', remark: '' })
const orderSourceIdx = ref(-1)
const orderedMessages = ref(new Set())

// Initialize: fill delivery address from user profile
onMounted(async () => {
  if (auth.user?.address) {
    orderForm.value.deliveryAddress = auth.user.address
  }
  await loadHistory()
})

const orderTotal = ref('0.00')

// ── Load chat history ──
async function loadHistory() {
  try {
    const res = await chatApi.getHistory()
    if (res.data && res.data.length > 0) {
      const loaded = []
      for (const record of res.data) {
        const role = record.role === 'assistant' ? 'ai' : record.role
        const text = record.content || record.message || ''
        if (text) {
          loaded.push({ role, text })
        }
      }
      messages.value = loaded
      scrollToBottom()
    }
  } catch {
    // silent fail on history load
  }
}

// ── Send message ──
async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  await sendMessage(text)
}

async function sendMessage(text) {
  // Add user message
  messages.value.push({ role: 'user', text })
  inputText.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const res = await chatApi.send({ message: text })
    const data = res.data

    // Build AI message
    const aiMsg = { role: 'ai', text: data.reply || '' }
    if (data.matchedMedicines && data.matchedMedicines.length > 0) {
      aiMsg.medicines = data.matchedMedicines
    }
    messages.value.push(aiMsg)
  } catch (e) {
    messages.value.push({ role: 'ai', text: '抱歉，网络出现了问题，请稍后重试。' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

// ── Medicine selection ──
function isMedSelected(medicineId) {
  return selectedMedicines.value.some(m => m.medicineId === medicineId)
}

function toggleMedSelection(med) {
  const idx = selectedMedicines.value.findIndex(m => m.medicineId === med.medicineId)
  if (idx >= 0) {
    selectedMedicines.value.splice(idx, 1)
  } else {
    selectedMedicines.value.push({ ...med })
  }
}

// ── Order creation ──
function showOrderDialog(medicines, idx) {
  selectedMedicines.value = medicines
  orderSourceIdx.value = idx
  const total = medicines.reduce((sum, m) => sum + m.price * m.quantity, 0)
  orderTotal.value = total.toFixed(2)
  orderDialogVisible.value = true
}

async function handleCreateOrder() {
  if (!orderForm.value.deliveryAddress.trim()) {
    ElMessage.warning('请输入配送地址')
    return
  }
  creatingOrder.value = true
  try {
    const items = selectedMedicines.value.map(m => ({
      medicineId: m.medicineId,
      quantity: m.quantity
    }))
    const res = await orderApi.create({
      items,
      deliveryAddress: orderForm.value.deliveryAddress,
      remark: orderForm.value.remark || undefined
    })
    ElMessage.success('订单创建成功')
    orderDialogVisible.value = false
    // Mark this message as ordered — replace entire Set to guarantee reactivity
    if (orderSourceIdx.value >= 0) {
      orderedMessages.value = new Set([...orderedMessages.value, orderSourceIdx.value])
    }
    selectedMedicines.value = []
    const orderId = res.data?.id
    messages.value.push({
      role: 'ai',
      text: '订单已创建成功！正在为您跳转到支付页面...'
    })
    scrollToBottom()
    // Navigate to order detail page for payment
    if (orderId) {
      router.push(`/user/orders/${orderId}`)
    }
  } catch {
    // handled by interceptor
  } finally {
    creatingOrder.value = false
  }
}

// ── Clear history ──
async function handleClearHistory() {
  try {
    await ElMessageBox.confirm('确定清空所有对话记录？', '提示', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await chatApi.clearHistory()
    messages.value = []
    selectedMedicines.value = []
    ElMessage.success('对话已清空')
  } catch {
    // cancelled
  }
}

// ── Helpers ──
function formatText(text) {
  return text.replace(/\n/g, '<br>')
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px); /* viewport - header - main padding */
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

/* ── Toolbar ── */
.chat-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  border-bottom: 1px solid #F3F4F6;
  background: #FAFBFC;
}

.toolbar-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: #6B7280;
}

/* ── Messages area ── */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  scroll-behavior: smooth;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6B7280;
  text-align: center;
}

.empty-state h3 {
  margin: 16px 0 6px;
  color: #374151;
  font-size: 1.15rem;
}

.empty-state p {
  font-size: 0.9rem;
}

/* Message rows */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}

.message-row.user {
  /* user messages pushed to right via bubble margin */
}

.message-row.user .bubble-wrapper {
  margin-left: auto;
  align-items: flex-end;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  color: white;
  font-size: 0.85rem;
  font-weight: 600;
}

.avatar.ai-avatar {
  background: linear-gradient(135deg, #4F46E5, #6366F1);
}

.avatar.user-avatar {
  background: linear-gradient(135deg, #4F46E5, #818CF8);
}

.bubble-wrapper {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bubble {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 0.92rem;
  line-height: 1.6;
  word-break: break-word;
}

.message-row.ai .bubble {
  background: #F3F4F6;
  color: #1F2937;
  border-top-left-radius: 4px;
}

.message-row.user .bubble {
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  color: white;
  border-top-right-radius: 4px;
}

/* Typing indicator */
.typing {
  display: flex;
  gap: 4px;
  padding: 14px 18px;
}

.dot {
  width: 8px;
  height: 8px;
  background: #9CA3AF;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ── Medicine cards ── */
.medicine-cards {
  background: #F9FAFB;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 14px;
}

.cards-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  color: #6B7280;
  margin-bottom: 10px;
}

.med-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  margin-bottom: 8px;
  background: white;
  border: 1.5px solid #E5E7EB;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.med-card:hover {
  border-color: #A5B4FC;
}

.med-card.selected {
  border-color: #4F46E5;
  background: #EEF2FF;
}

.med-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.med-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.med-name {
  font-weight: 500;
  color: #1F2937;
  font-size: 0.9rem;
}

.med-price {
  color: #4F46E5;
  font-weight: 600;
  font-size: 0.9rem;
}

.med-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
}

.in-stock { color: #059669; }
.out-stock { color: #DC2626; }

.check-icon {
  color: #4F46E5;
  font-size: 18px;
}

.order-btn {
  margin-top: 4px;
  width: 100%;
}

.order-btn:disabled {
  background: #D1D5DB !important;
  border-color: #D1D5DB !important;
  color: #9CA3AF !important;
  cursor: not-allowed;
}

/* ── Input area ── */
.chat-input-area {
  padding: 14px 20px;
  border-top: 1px solid #F3F4F6;
  background: #FAFBFC;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.input-wrapper :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 0.92rem;
  resize: none;
  box-shadow: none;
}

.send-btn {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  background: var(--user-primary);
  border-color: var(--user-primary);
}

.send-btn:disabled {
  background: #C7D2FE;
  border-color: #C7D2FE;
}

/* ── Order dialog ── */
.order-items-preview {
  background: #F9FAFB;
  border-radius: 10px;
  padding: 14px;
}

.order-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 0.9rem;
  color: #374151;
}

.order-item-row span:first-child {
  flex: 1;
}

.item-price {
  font-weight: 600;
  color: #4F46E5;
  min-width: 70px;
  text-align: right;
}

.order-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  margin-top: 8px;
  border-top: 1px solid #E5E7EB;
  font-weight: 600;
}

.total-price {
  font-size: 1.15rem;
  color: #DC2626;
}
</style>
