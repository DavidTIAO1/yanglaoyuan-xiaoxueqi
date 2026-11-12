<template>
  <div class="login-page" :class="isMerchant ? 'theme-merchant' : 'theme-user'">
    <!-- Top brand banner -->
    <div class="brand-banner">
      <div class="brand-decoration">
        <div class="circle c1"></div>
        <div class="circle c2"></div>
        <div class="circle c3"></div>
      </div>
      <div class="brand-content">
        <div class="brand-icon">
          <el-icon :size="32"><component :is="isMerchant ? 'Shop' : 'UserFilled'" /></el-icon>
        </div>
        <div class="brand-text">
          <h1>{{ isMerchant ? '商家管理中心' : '用户服务中心' }}</h1>
          <p>{{ isMerchant ? '管理药品、处理订单、服务社区' : '智能问诊、快速下单、送药到家' }}</p>
        </div>
      </div>
    </div>

    <!-- Bottom form area -->
    <div class="form-area">
      <div class="form-wrapper">
        <div class="form-header">
          <p>请输入您的账号信息登录</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="submit-btn"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <router-link :to="`/register/${role}`" class="link">立即注册</router-link>
        </div>
        <div class="form-footer switch-identity">
          <router-link to="/" class="link-muted">← 切换身份</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock, UserFilled, Shop } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const role = computed(() => route.params.role)
const isMerchant = computed(() => role.value === 'MERCHANT')

const formRef = ref(null)
const loading = ref(false)
const form = ref({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const user = await auth.login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    if (user.role === 'MERCHANT') {
      router.push('/merchant/dashboard')
    } else {
      router.push('/user/voice-chat')
    }
  } catch (e) {
    // Error handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #fff;
}

/* ── Top brand banner ── */
.brand-banner {
  position: relative;
  padding: 48px 32px 44px;
  overflow: hidden;
  flex-shrink: 0;
}

.theme-user .brand-banner {
  background: linear-gradient(135deg, #4F46E5 0%, #3B82F6 50%, #1D4ED8 100%);
}

.theme-merchant .brand-banner {
  background: linear-gradient(135deg, #059669 0%, #10B981 50%, #047857 100%);
}

.brand-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  max-width: 520px;
  margin: 0 auto;
}

.brand-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  backdrop-filter: blur(8px);
  flex-shrink: 0;
}

.brand-text {
  color: #fff;
}

.brand-text h1 {
  font-size: 1.65rem;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.brand-text p {
  font-size: 0.95rem;
  opacity: 0.85;
}

/* Decorative circles */
.brand-decoration {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.c1 { width: 200px; height: 200px; top: -60px; left: 5%; }
.c2 { width: 140px; height: 140px; top: -30px; right: 8%; }
.c3 { width: 100px; height: 100px; bottom: -30px; left: 40%; }

/* ── Bottom form area ── */
.form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.form-wrapper {
  width: 100%;
  max-width: 420px;
}

.form-header {
  margin-bottom: 28px;
}

.form-header h2 {
  font-size: 1.6rem;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 6px;
}

.form-header p {
  color: #6B7280;
  font-size: 0.95rem;
}

/* Form item spacing */
:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 1rem;
  border-radius: 10px;
  margin-top: 4px;
}

.theme-user .submit-btn {
  background: var(--user-primary);
  border-color: var(--user-primary);
}

.theme-merchant .submit-btn {
  background: var(--merchant-primary);
  border-color: var(--merchant-primary);
}

.form-footer {
  text-align: center;
  color: #6B7280;
  font-size: 0.9rem;
  margin-top: 4px;
}

.switch-identity {
  margin-top: 10px;
}

.link {
  font-weight: 600;
  margin-left: 4px;
}

.theme-user .link { color: var(--user-primary); }
.theme-merchant .link { color: var(--merchant-primary); }

.link-muted {
  color: #9CA3AF;
  font-size: 0.85rem;
}

.link-muted:hover {
  color: #6B7280;
}

@media (max-width: 480px) {
  .brand-banner { padding: 36px 20px 32px; }
  .brand-content { gap: 14px; }
  .brand-icon { width: 52px; height: 52px; }
  .brand-text h1 { font-size: 1.3rem; }
  .brand-text p { font-size: 0.85rem; }
  .form-wrapper { padding: 0; }
}
</style>
