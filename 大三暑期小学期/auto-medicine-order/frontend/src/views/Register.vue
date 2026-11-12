<template>
  <div class="register-page" :class="isMerchant ? 'theme-merchant' : 'theme-user'">
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
          <h1>{{ isMerchant ? '商家注册' : '用户注册' }}</h1>
          <p>{{ isMerchant ? '加入社区药品配送平台，拓展服务半径' : '享受智能问诊，药品快速配送到家' }}</p>
        </div>
      </div>
    </div>

    <!-- Bottom form area -->
    <div class="form-area">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>创建账号</h2>
          <p>
            注册为
            <el-tag :type="isMerchant ? 'success' : ''" effect="dark" round size="small">
              {{ isMerchant ? '商家' : '用户' }}
            </el-tag>
          </p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名（3-50个字符）"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="realName">
            <el-input
              v-model="form.realName"
              placeholder="真实姓名"
              :prefix-icon="Postcard"
            />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="手机号"
              :prefix-icon="Phone"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码（至少6个字符）"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item prop="address">
            <el-input
              v-model="form.address"
              placeholder="地址（选填）"
              :prefix-icon="Location"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="submit-btn"
              @click="handleRegister"
            >
              注 册
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>已有账号？</span>
          <router-link :to="`/login/${role}`" class="link">立即登录</router-link>
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
import { User, Lock, UserFilled, Shop, Postcard, Phone, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const role = computed(() => route.params.role)
const isMerchant = computed(() => role.value === 'MERCHANT')

const formRef = ref(null)
const loading = ref(false)
const form = ref({
  username: '',
  realName: '',
  phone: '',
  password: '',
  confirmPassword: '',
  address: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度需在3-50个字符之间', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.value.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.register({
      username: form.value.username,
      password: form.value.password,
      realName: form.value.realName,
      phone: form.value.phone,
      address: form.value.address || undefined,
      role: role.value
    })
    ElMessage.success('注册成功')
    if (role.value === 'MERCHANT') {
      router.push('/merchant/dashboard')
    } else {
      router.push('/user/chat')
    }
  } catch (e) {
    // Error handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #fff;
}

/* ── Top brand banner ── */
.brand-banner {
  position: relative;
  padding: 40px 32px 38px;
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
  width: 60px;
  height: 60px;
  border-radius: 16px;
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
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.brand-text p {
  font-size: 0.9rem;
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

.c1 { width: 180px; height: 180px; top: -50px; left: 5%; }
.c2 { width: 120px; height: 120px; top: -20px; right: 10%; }
.c3 { width: 90px; height: 90px; bottom: -25px; left: 45%; }

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
  margin-bottom: 22px;
}

.form-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 8px;
}

.form-header p {
  color: #6B7280;
  font-size: 0.95rem;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Compact form items for register (more fields) */
:deep(.el-form-item) {
  margin-bottom: 18px;
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
  .brand-banner { padding: 32px 20px 28px; }
  .brand-content { gap: 14px; }
  .brand-icon { width: 48px; height: 48px; }
  .brand-text h1 { font-size: 1.2rem; }
  .brand-text p { font-size: 0.8rem; }
}
</style>
