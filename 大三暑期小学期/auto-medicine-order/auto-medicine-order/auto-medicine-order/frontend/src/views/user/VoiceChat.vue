<template>
  <div class="voice-chat-page">
    <!-- Header toolbar -->
    <div class="chat-toolbar">
      <span class="toolbar-hint">
        <el-icon :size="16"><Microphone /></el-icon>
        输入文字或按住麦克风说话，AI 回复会自动语音播报
      </span>
      <div class="toolbar-right">
        <el-switch v-model="ttsEnabled" active-text="语音播报" inactive-text="静音"
                   inline-prompt style="margin-right:12px" />
        <el-button text type="danger" :icon="Delete" @click="handleClearHistory" :disabled="messages.length === 0">
          清空对话
        </el-button>
      </div>
    </div>

    <!-- Message list -->
    <div class="chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="empty-state">
        <el-icon :size="56" color="#C7D2FE"><Microphone /></el-icon>
        <h3>语音聊天</h3>
        <p>按住按钮说话，告诉我你想买的药品</p>
      </div>

      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="message-row"
        :class="msg.role"
      >
        <!-- AI avatar -->
        <div class="avatar ai-avatar" v-if="msg.role === 'ai'">
          <el-icon :size="20"><Monitor /></el-icon>
        </div>

        <div class="bubble-wrapper">
          <div class="bubble" v-if="msg.text">
            <span v-html="formatText(msg.text)"></span>
            <!-- TTS replay button on AI messages -->
            <button
              v-if="msg.role === 'ai' && msg.text && ttsEnabled"
              class="tts-replay-btn"
              :class="{ speaking: speakingIdx === idx }"
              @click="speakText(msg.text, idx)"
              :title="speakingIdx === idx ? '正在播报...' : '点击重新播报'"
            >
              <el-icon :size="16"><component :is="speakingIdx === idx ? 'Loading' : 'Headset'" /></el-icon>
            </button>
          </div>

          <!-- Medicine cards -->
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

        <!-- User avatar -->
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

    <!-- Input area: mic only -->
    <div class="input-area">
      <!-- Recording overlay -->
      <div v-if="isRecording" class="recording-overlay">
        <div class="recording-bar">
          <span class="rec-dot"></span>
          <span class="rec-label">录音中</span>
          <span class="rec-text" v-if="interimText">{{ interimText }}</span>
          <span class="rec-text placeholder" v-else>请说话...</span>
        </div>
      </div>

      <div class="mic-section">
        <button
          class="mic-btn"
          :class="{ active: isRecording, disabled: !speechSupported }"
          @click="toggleRecording"
          :disabled="!speechSupported"
        >
          <el-icon :size="40"><Microphone /></el-icon>
          <span v-if="isRecording" class="ripple-ring ring-1"></span>
          <span v-if="isRecording" class="ripple-ring ring-2"></span>
          <span v-if="isRecording" class="ripple-ring ring-3"></span>
        </button>
        <span class="mic-hint">
          <template v-if="!speechSupported">浏览器不支持语音</template>
          <template v-else-if="isRecording">再次点击结束</template>
          <template v-else>点击开始说话</template>
        </span>
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  Microphone, Delete, Monitor, FirstAidKit,
  ShoppingCart, CircleCheck, Headset, Loading
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi, orderApi } from '../../api'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const messagesRef = ref(null)

// ── Message state (same as Chat.vue) ──
const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const selectedMedicines = ref([])
const orderDialogVisible = ref(false)
const creatingOrder = ref(false)
const orderForm = ref({ deliveryAddress: '', remark: '' })
const orderSourceIdx = ref(-1)
const orderedMessages = ref(new Set())
const orderTotal = ref('0.00')

// ── Voice order state ──
const pendingOrder = ref(null)  // { medicines, address, total, sourceIdx }

// ── TTS state ──
const ttsEnabled = ref(true)
const speakingIdx = ref(-1)
let synth = window.speechSynthesis || null

// ── Speech Recognition state ──
const speechSupported = ref(false)
const isRecording = ref(false)
const interimText = ref('')
let recognition = null

// ── Lifecycle ──
onMounted(async () => {
  if (auth.user?.address) {
    orderForm.value.deliveryAddress = auth.user.address
  }
  initSpeechRecognition()
  await loadHistory()
})

onUnmounted(() => {
  stopSpeaking()
  clearTimeout(onendTimer)
  waitingOnend = false
  if (recognition) {
    try { recognition.abort() } catch {}
  }
})

// ═══════════════════════════════════════════
//  Speech Recognition (STT) — Web Speech API
// ═══════════════════════════════════════════
let collectedFinal = ''   // accumulates final results across segments
let latestInterim = ''    // tracks the most recent interim text
let onendTimer = null     // timeout for onend callback
let waitingOnend = false  // true between stop() and onend firing

function initSpeechRecognition() {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SR) {
    speechSupported.value = false
    return
  }
  speechSupported.value = true
}

function createRecognition() {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SR) return null

  const rec = new SR()
  rec.lang = 'zh-CN'
  rec.continuous = true        // allow longer recordings
  rec.interimResults = true
  rec.maxAlternatives = 1

  rec.onresult = (event) => {
    console.log('[STT] onresult fired, results length:', event.results.length, 'resultIndex:', event.resultIndex)
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript
      console.log('[STT] result[' + i + ']:', transcript, 'isFinal:', event.results[i].isFinal)
      if (event.results[i].isFinal) {
        collectedFinal += transcript
      } else {
        interim += transcript
      }
    }
    latestInterim = interim
    interimText.value = collectedFinal + interim
  }

  rec.onerror = (event) => {
    console.warn('[STT] error:', event.error)
    if (event.error === 'not-allowed') {
      ElMessage.warning('请在浏览器地址栏左侧点击锁图标，允许麦克风权限')
      isRecording.value = false
      waitingOnend = false
    } else if (event.error === 'network') {
      ElMessage.warning('语音识别需要联网，请检查网络连接后重试')
      isRecording.value = false
      waitingOnend = false
    } else if (event.error === 'no-speech') {
      // onend will fire next
    } else if (event.error === 'aborted') {
      // Abort requested — reset state but don't show error
      isRecording.value = false
      waitingOnend = false
    } else {
      ElMessage.error('语音识别出错: ' + event.error)
      isRecording.value = false
      waitingOnend = false
    }
  }

  rec.onend = () => {
    clearTimeout(onendTimer)
    waitingOnend = false
    const finalText = collectedFinal.trim()
    const text = finalText || latestInterim.trim()
    console.log('[STT] onend — final:', JSON.stringify(finalText), '| latestInterim:', JSON.stringify(latestInterim), '| sending:', JSON.stringify(text))
    interimText.value = ''
    isRecording.value = false
    collectedFinal = ''
    latestInterim = ''
    if (text) {
      ElMessage.success('已识别: ' + text)
      sendMessage(text)
    } else {
      ElMessage.info('未识别到语音内容，请重试')
    }
  }

  return rec
}

function toggleRecording() {
  console.log('[STT] toggle clicked, isRecording:', isRecording.value, 'sending:', sending.value)
  if (sending.value) return

  if (isRecording.value) {
    // Stop recording — let onend handle cleanup
    isRecording.value = false
    waitingOnend = true
    try { recognition?.stop() } catch {}
    console.log('[STT] stop() called, waiting for onend')
    // Safety timeout: if onend never fires, force reset after 8 seconds
    clearTimeout(onendTimer)
    onendTimer = setTimeout(() => {
      if (waitingOnend) {
        console.warn('[STT] onend did not fire within 8s, force resetting')
        waitingOnend = false
        interimText.value = ''
        collectedFinal = ''
        latestInterim = ''
        try { recognition?.abort() } catch {}
        ElMessage.warning('语音识别服务未响应，请在下方输入框中输入文字')
      }
    }, 8000)
  } else {
    if (!speechSupported.value) {
      ElMessage.warning('当前浏览器不支持语音识别，请使用 Chrome')
      return
    }
    // DO NOT abort old instance here — let its onend fire naturally
    collectedFinal = ''
    latestInterim = ''
    interimText.value = ''

    // Pre-check: request microphone permission explicitly first
    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      navigator.mediaDevices.getUserMedia({ audio: true })
        .then(stream => {
          stream.getTracks().forEach(t => t.stop())
          console.log('[STT] mic permission granted, starting recognition')
          doStartRecognition()
        })
        .catch(err => {
          console.warn('[STT] mic permission denied:', err)
          ElMessage.warning('无法获取麦克风权限。请点击地址栏左侧的锁图标，允许麦克风访问。')
        })
    } else {
      doStartRecognition()
    }
  }
}

function doStartRecognition() {
  recognition = createRecognition()
  if (!recognition) {
    ElMessage.warning('语音识别不可用')
    return
  }
  try {
    recognition.start()
    isRecording.value = true
    console.log('[STT] recognition started successfully')
  } catch (e) {
    console.warn('[STT] start failed:', e)
    isRecording.value = false
    ElMessage.warning('语音识别启动失败: ' + (e.message || '未知错误'))
  }
}

// ═══════════════════════════════════════════
//  Text-to-Speech (TTS) — Web Speech API
// ═══════════════════════════════════════════
function stopSpeaking() {
  try {
    if (synth) synth.cancel()
  } catch (e) {
    console.warn('[TTS] stopSpeaking error:', e)
  }
  speakingIdx.value = -1
}

function speakText(text, idx) {
  if (!synth || !ttsEnabled.value) return
  // Stop any current speech
  synth.cancel()
  speakingIdx.value = idx

  // Strip HTML-like tags for clean speech
  const cleanText = text.replace(/<[^>]*>/g, '')

  const utterance = new SpeechSynthesisUtterance(cleanText)
  utterance.lang = 'zh-CN'
  utterance.rate = 0.85   // slightly slower for elderly
  utterance.pitch = 1.0
  utterance.volume = 1.0

  // Try to pick a Chinese voice
  const voices = synth.getVoices()
  const zhVoice = voices.find(v => v.lang.startsWith('zh'))
  if (zhVoice) utterance.voice = zhVoice

  utterance.onend = () => {
    speakingIdx.value = -1
  }
  utterance.onerror = () => {
    speakingIdx.value = -1
  }

  synth.speak(utterance)
}

// ═══════════════════════════════════════════
//  Chat history
// ═══════════════════════════════════════════
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
    // silent
  }
}

// ═══════════════════════════════════════════
//  Send messages
// ═══════════════════════════════════════════
function handleTextSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  sendMessage(text)
}

// Confirmation keywords for voice order
const CONFIRM_WORDS = ['好的', '确认', '可以', '没问题', '对', '是的', '行', '下单', '确定', '好', '嗯', '对对']
const CANCEL_WORDS = ['不要', '取消', '算了', '不用', '不需要']

async function sendMessage(text) {
  console.log('[Chat] sendMessage called with:', text)
  stopSpeaking()

  // ── Check if user is confirming or cancelling a pending order ──
  if (pendingOrder.value) {
    const lowerText = text.trim()
    const isConfirm = CONFIRM_WORDS.some(w => lowerText.includes(w))
    const isCancel = CANCEL_WORDS.some(w => lowerText.includes(w))

    if (isConfirm) {
      messages.value.push({ role: 'user', text })
      sending.value = true
      scrollToBottom()
      await autoCreateOrder()
      return
    }
    if (isCancel) {
      messages.value.push({ role: 'user', text })
      pendingOrder.value = null
      const cancelReply = '好的，已取消本次订单。您还需要其他药品吗？'
      messages.value.push({ role: 'ai', text: cancelReply })
      scrollToBottom()
      if (ttsEnabled.value) {
        await nextTick()
        speakText(cancelReply, messages.value.length - 1)
      }
      return
    }
    // Not a confirm/cancel — fall through to normal AI chat
    pendingOrder.value = null
  }

  messages.value.push({ role: 'user', text })
  inputText.value = ''
  sending.value = true
  scrollToBottom()

  try {
    console.log('[Chat] sending API request...')
    const res = await chatApi.send({ message: text })
    const data = res.data
    console.log('[Chat] response data:', data)

    const aiMsg = { role: 'ai', text: data.reply || '' }
    const hasMedicines = data.matchedMedicines && data.matchedMedicines.length > 0

    if (hasMedicines) {
      aiMsg.medicines = data.matchedMedicines
    }
    messages.value.push(aiMsg)

    // ── If AI found medicines, auto-build pending order ──
    if (hasMedicines) {
      const inStockMeds = data.matchedMedicines.filter(m => m.inStock)
      if (inStockMeds.length > 0) {
        const address = auth.user?.address || orderForm.value.deliveryAddress || ''
        const total = inStockMeds.reduce((sum, m) => sum + m.price * m.quantity, 0)
        const msgIdx = messages.value.length - 1

        pendingOrder.value = {
          medicines: inStockMeds,
          address,
          total: total.toFixed(2),
          sourceIdx: msgIdx
        }

        // Build confirmation prompt text
        const medList = inStockMeds.map(m => `${m.name}，${m.quantity}件，${m.price}元`).join('；')
        const confirmText = `为您找到以下药品：${medList}，合计${total.toFixed(2)}元。` +
          (address ? `配送地址是${address}。` : '请问您的配送地址是哪里？') +
          '确认下单请说"好的"，取消请说"不要"。'

        messages.value.push({ role: 'ai', text: confirmText })
        scrollToBottom()

        // TTS the order confirmation
        if (ttsEnabled.value) {
          await nextTick()
          speakText(confirmText, messages.value.length - 1)
        }
      }
    } else {
      // Normal AI reply — just TTS
      if (ttsEnabled.value && aiMsg.text) {
        await nextTick()
        speakText(aiMsg.text, messages.value.length - 1)
      }
    }
  } catch (err) {
    console.error('[Chat] sendMessage error:', err)
    messages.value.push({ role: 'ai', text: '抱歉，网络出现了问题，请稍后重试。' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

// ═══════════════════════════════════════════
//  Auto-create order (voice confirmation flow)
// ═══════════════════════════════════════════
async function autoCreateOrder() {
  const order = pendingOrder.value
  if (!order) return

  const address = order.address || auth.user?.address || ''
  if (!address.trim()) {
    const askAddr = '还没有您的配送地址，请先告诉我您的详细地址，我再帮您下单。'
    messages.value.push({ role: 'ai', text: askAddr })
    scrollToBottom()
    if (ttsEnabled.value) {
      await nextTick()
      speakText(askAddr, messages.value.length - 1)
    }
    pendingOrder.value = null
    sending.value = false
    return
  }

  try {
    const items = order.medicines.map(m => ({
      medicineId: m.medicineId,
      quantity: m.quantity
    }))
    const res = await orderApi.create({
      items,
      deliveryAddress: address,
      remark: undefined
    })

    // Mark the source message as ordered (disable manual order button)
    if (order.sourceIdx >= 0) {
      orderedMessages.value = new Set([...orderedMessages.value, order.sourceIdx])
    }

    pendingOrder.value = null
    const orderId = res.data?.id

    const successText = `订单已创建成功！合计${order.total}元，配送至${address}。正在为您跳转到支付页面...`
    messages.value.push({ role: 'ai', text: successText })
    ElMessage.success('订单创建成功')
    scrollToBottom()

    if (ttsEnabled.value) {
      await nextTick()
      speakText(successText, messages.value.length - 1)
    }

    // Navigate to order detail page for payment
    if (orderId) {
      setTimeout(() => router.push(`/user/orders/${orderId}`), 1500)
    }
  } catch (err) {
    console.error('[AutoOrder] 创建订单失败:', err)
    pendingOrder.value = null
    const failText = '抱歉，订单创建失败了，请稍后再试或手动下单。'
    messages.value.push({ role: 'ai', text: failText })
    scrollToBottom()
    if (ttsEnabled.value) {
      await nextTick()
      speakText(failText, messages.value.length - 1)
    }
  } finally {
    sending.value = false
  }
}

// ═══════════════════════════════════════════
//  Medicine selection & Order creation
// ═══════════════════════════════════════════
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
    if (orderSourceIdx.value >= 0) {
      orderedMessages.value = new Set([...orderedMessages.value, orderSourceIdx.value])
    }
    selectedMedicines.value = []
    const orderId = res.data?.id
    const confirmText = '订单已创建成功！正在为您跳转到支付页面...'
    messages.value.push({ role: 'ai', text: confirmText })
    scrollToBottom()
    // TTS for order confirmation
    if (ttsEnabled.value) {
      await nextTick()
      speakText(confirmText, messages.value.length - 1)
    }
    // Navigate to order detail page for payment
    if (orderId) {
      setTimeout(() => router.push(`/user/orders/${orderId}`), 1500)
    }
  } catch {
    // handled by interceptor
  } finally {
    creatingOrder.value = false
  }
}

// ═══════════════════════════════════════════
//  Clear history
// ═══════════════════════════════════════════
async function handleClearHistory() {
  try {
    await ElMessageBox.confirm('确定清空所有对话记录？', '提示', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    stopSpeaking()
    await chatApi.clearHistory()
    messages.value = []
    selectedMedicines.value = []
    ElMessage.success('对话已清空')
  } catch {
    // cancelled
  }
}

// ═══════════════════════════════════════════
//  Helpers
// ═══════════════════════════════════════════
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
.voice-chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
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
  flex-shrink: 0;
}

.toolbar-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: #6B7280;
}

.toolbar-right {
  display: flex;
  align-items: center;
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
  position: relative;
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

/* TTS replay button */
.tts-replay-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(79, 70, 229, 0.1);
  color: #4F46E5;
  border-radius: 50%;
  cursor: pointer;
  margin-left: 8px;
  vertical-align: middle;
  transition: background 0.2s, transform 0.2s;
}

.tts-replay-btn:hover {
  background: rgba(79, 70, 229, 0.2);
  transform: scale(1.1);
}

.tts-replay-btn.speaking {
  background: rgba(79, 70, 229, 0.2);
  animation: spin-slow 2s linear infinite;
}

@keyframes spin-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
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
.input-area {
  flex-shrink: 0;
  padding: 16px 20px 28px;
  border-top: 1px solid #F3F4F6;
  background: #FAFBFC;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.mic-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.mic-btn {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: none;
  background: var(--user-primary, #4F46E5);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s, box-shadow 0.15s, background 0.15s;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
}

.mic-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.45);
}

.mic-btn.active {
  background: #EF4444;
  box-shadow: 0 4px 20px rgba(239, 68, 68, 0.45);
  animation: pulse-red 1.2s ease-in-out infinite;
}

.mic-btn.disabled {
  background: #D1D5DB;
  box-shadow: none;
  cursor: not-allowed;
}

/* Ripple rings for recording state */
.mic-btn {
  position: relative;
}

.ripple-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid rgba(239, 68, 68, 0.4);
  transform: translate(-50%, -50%);
  animation: ripple 1.8s ease-out infinite;
  pointer-events: none;
}

.ripple-ring.ring-2 {
  animation-delay: 0.6s;
}

.ripple-ring.ring-3 {
  animation-delay: 1.2s;
}

@keyframes ripple {
  0% {
    width: 100%;
    height: 100%;
    opacity: 0.6;
  }
  100% {
    width: 220%;
    height: 220%;
    opacity: 0;
  }
}

/* Recording overlay bar */
.recording-overlay {
  width: 100%;
  max-width: 500px;
  margin-top: 16px;
}

.recording-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 12px;
  animation: fade-in 0.2s ease;
}

.rec-dot {
  width: 10px;
  height: 10px;
  background: #EF4444;
  border-radius: 50%;
  flex-shrink: 0;
  animation: blink 1s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.rec-label {
  font-size: 0.82rem;
  color: #DC2626;
  font-weight: 500;
  flex-shrink: 0;
}

.rec-text {
  font-size: 0.9rem;
  color: #1F2937;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rec-text.placeholder {
  color: #9CA3AF;
  font-style: italic;
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.mic-hint {
  font-size: 0.82rem;
  color: #9CA3AF;
}

@keyframes pulse-red {
  0%, 100% { box-shadow: 0 4px 20px rgba(239, 68, 68, 0.45); }
  50% { box-shadow: 0 4px 32px rgba(239, 68, 68, 0.75); }
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
