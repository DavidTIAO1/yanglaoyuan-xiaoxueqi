import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/',
    name: 'role-select',
    component: () => import('../views/RoleSelect.vue')
  },
  {
    path: '/login/:role',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register/:role',
    name: 'register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/user',
    component: () => import('../layouts/UserLayout.vue'),
    meta: { requiresAuth: true, role: 'USER' },
    children: [
      { path: '', redirect: '/user/voice-chat' },
      { path: 'voice-chat', name: 'user-voice-chat', component: () => import('../views/user/VoiceChat.vue') },
      { path: 'chat', name: 'user-chat', component: () => import('../views/user/Chat.vue') },
      { path: 'orders', name: 'user-orders', component: () => import('../views/user/OrderList.vue') },
      { path: 'orders/:id', name: 'user-order-detail', component: () => import('../views/user/OrderDetail.vue') },
      { path: 'proxy-payment', name: 'user-proxy-payment', component: () => import('../views/user/ProxyPayment.vue') },
      { path: 'family/add', name: 'user-family-add', component: () => import('../views/user/FamilyAdd.vue') },
      { path: 'family/members', name: 'user-family-members', component: () => import('../views/user/FamilyMembers.vue') },
      { path: 'notifications', name: 'user-notifications', component: () => import('../views/user/Notifications.vue') },
      { path: 'profile', name: 'user-profile', component: () => import('../views/user/Profile.vue') }
    ]
  },
  {
    path: '/merchant',
    component: () => import('../layouts/MerchantLayout.vue'),
    meta: { requiresAuth: true, role: 'MERCHANT' },
    children: [
      { path: '', redirect: '/merchant/dashboard' },
      { path: 'dashboard', name: 'merchant-dashboard', component: () => import('../views/merchant/Dashboard.vue') },
      { path: 'orders', name: 'merchant-orders', component: () => import('../views/merchant/OrderManage.vue') },
      { path: 'medicines', name: 'merchant-medicines', component: () => import('../views/merchant/MedicineManage.vue') },
      { path: 'shop', name: 'merchant-shop', component: () => import('../views/merchant/ShopSettings.vue') },
      { path: 'notifications', name: 'merchant-notifications', component: () => import('../views/merchant/Notifications.vue') }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth) {
    if (!auth.token) {
      return next('/')
    }
    if (to.meta.role && auth.user?.role !== to.meta.role) {
      return next('/')
    }
  }

  // Redirect logged-in users away from login/register pages
  if (['login', 'register'].includes(to.name) && auth.token) {
    const role = auth.user?.role
    if (role === 'USER') return next('/user/voice-chat')
    if (role === 'MERCHANT') return next('/merchant/dashboard')
  }

  next()
})

export default router
