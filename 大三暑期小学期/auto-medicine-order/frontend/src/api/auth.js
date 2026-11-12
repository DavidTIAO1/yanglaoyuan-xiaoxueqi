import api from './index'

export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  refresh: () => api.post('/auth/refresh')
}

export const userApi = {
  getProfile: () => api.get('/users/profile'),
  updateProfile: (data) => api.put('/users/profile', data),
  searchByFamilyId: (familyId) => api.get('/users/search', { params: { familyId } })
}

export const chatApi = {
  send: (data) => api.post('/chat/send', data),
  getHistory: () => api.get('/chat/history'),
  clearHistory: () => api.delete('/chat/history')
}

export const orderApi = {
  create: (data) => api.post('/orders', data),
  getMyOrders: (params) => api.get('/orders', { params }),
  getProxyPayable: () => api.get('/orders/proxy-payable'),
  getOrder: (id) => api.get(`/orders/${id}`),
  pay: (id, data) => api.post(`/orders/${id}/pay`, data),
  accept: (id) => api.post(`/orders/${id}/accept`),
  updateStatus: (id, status) => api.put(`/orders/${id}/status`, null, { params: { status } }),
  abandon: (id) => api.post(`/orders/${id}/abandon`)
}

export const merchantApi = {
  getProfile: () => api.get('/merchants/profile'),
  updateProfile: (data) => api.put('/merchants/profile', data),
  getNearby: (params) => api.get('/merchants/nearby', { params }),
  getMedicines: (merchantId) => api.get(`/merchants/${merchantId}/medicines`)
}

export const medicineApi = {
  list: (params) => api.get('/medicines', { params }),
  create: (data) => api.post('/medicines', data),
  update: (id, data) => api.put(`/medicines/${id}`, data),
  delete: (id) => api.delete(`/medicines/${id}`)
}

export const familyApi = {
  search: (familyId) => api.get('/users/search', { params: { familyId } }),
  bind: (data) => api.post('/family/bind', data),
  accept: (id) => api.post(`/family/${id}/accept`),
  reject: (id) => api.post(`/family/${id}/reject`),
  members: () => api.get('/family/members')
}

export const notificationApi = {
  list: (params) => api.get('/notifications', { params }),
  unreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id) => api.put(`/notifications/${id}/read`)
}
