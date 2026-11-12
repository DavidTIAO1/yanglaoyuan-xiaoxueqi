<template>
  <div class="role-page">
    <div class="page-content">
      <h1 class="page-title">社区药品配送服务</h1>
      <p class="page-subtitle">请选择您的身份开始使用</p>

      <div class="cards">
        <!-- User Card -->
        <div class="role-card user-card" @click="selectRole('USER')">
          <div class="card-inner">
            <div class="card-icon">
              <el-icon :size="56"><UserFilled /></el-icon>
            </div>
            <h2>我是用户</h2>
            <p>描述药品需求，AI 智能匹配附近药店，快速配送到家</p>
            <div class="card-enter">
              <span>进入</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <!-- Merchant Card -->
        <div class="role-card merchant-card" @click="selectRole('MERCHANT')">
          <div class="card-inner">
            <div class="card-icon">
              <el-icon :size="56"><Shop /></el-icon>
            </div>
            <h2>我是商家</h2>
            <p>管理药品库存，接收周边订单，拓展社区服务半径</p>
            <div class="card-enter">
              <span>进入</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { UserFilled, Shop, ArrowRight } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function selectRole(role) {
  // 清除已有登录状态，避免路由守卫直接跳转到旧角色的页面
  if (auth.token) {
    auth.token = null
    auth.user = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
  router.push(`/login/${role}`)
}
</script>

<style scoped>
.role-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #EEF2FF 0%, #F0FDF4 50%, #EFF6FF 100%);
  padding: 20px;
}

.page-content {
  text-align: center;
  max-width: 820px;
  width: 100%;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: #1F2937;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 1.1rem;
  color: #6B7280;
  margin-bottom: 56px;
}

.cards {
  display: flex;
  gap: 36px;
  justify-content: center;
}

.role-card {
  width: 340px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.role-card:hover {
  transform: translateY(-10px);
}

.card-inner {
  padding: 44px 32px 36px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  border: 2px solid transparent;
  transition: all 0.4s ease;
  height: 100%;
}

.user-card:hover .card-inner {
  border-color: var(--user-primary);
  box-shadow: 0 24px 48px -12px rgba(79, 70, 229, 0.2);
}

.merchant-card:hover .card-inner {
  border-color: var(--merchant-primary);
  box-shadow: 0 24px 48px -12px rgba(5, 150, 105, 0.2);
}

.card-icon {
  width: 88px;
  height: 88px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  transition: transform 0.3s ease;
}

.role-card:hover .card-icon {
  transform: scale(1.05);
}

.user-card .card-icon {
  background: linear-gradient(135deg, #4F46E5, #2563EB);
  color: white;
}

.merchant-card .card-icon {
  background: linear-gradient(135deg, #059669, #10B981);
  color: white;
}

.card-inner h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 10px;
}

.card-inner p {
  color: #6B7280;
  font-size: 0.92rem;
  line-height: 1.7;
  margin-bottom: 28px;
}

.card-enter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 0.95rem;
  transition: gap 0.3s ease;
}

.role-card:hover .card-enter {
  gap: 10px;
}

.user-card .card-enter {
  color: var(--user-primary);
}

.merchant-card .card-enter {
  color: var(--merchant-primary);
}

@media (max-width: 768px) {
  .cards {
    flex-direction: column;
    align-items: center;
  }
  .page-title {
    font-size: 1.8rem;
  }
}
</style>
