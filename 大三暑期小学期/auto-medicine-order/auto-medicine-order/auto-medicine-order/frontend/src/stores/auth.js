import { defineStore } from 'pinia'
import { authApi, userApi } from '../api'
import router from '../router'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),

  actions: {
    async login(username, password) {
      const res = await authApi.login({ username, password })
      this.token = res.data
      localStorage.setItem('token', res.data)
      await this.fetchProfile()
      return this.user
    },

    async register(data) {
      const res = await authApi.register(data)
      // Auto-login after registration
      await this.login(data.username, data.password)
      return res.data
    },

    async fetchProfile() {
      const res = await userApi.getProfile()
      this.user = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
    },

    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/')
    }
  }
})
