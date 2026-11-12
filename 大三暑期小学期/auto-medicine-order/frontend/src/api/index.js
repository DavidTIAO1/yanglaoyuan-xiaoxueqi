import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// Request interceptor: attach JWT token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: unwrap ApiResponse, handle errors
api.interceptors.response.use(
  response => {
    const body = response.data
    if (body.code === 200) {
      return body
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message))
  },
  error => {
    const status = error.response?.status
    const msg = error.response?.data?.message

    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/'
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(msg || '网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default api
export * from './auth'
