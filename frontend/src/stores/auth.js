import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/index.js'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const userType = computed(() => user.value?.userType || 'normal')
  const isEnterprise = computed(() => userType.value === 'enterprise')
  const isAdmin = computed(() => userType.value === 'platform_admin')
  const isNormal = computed(() => userType.value === 'normal')
  const currentEnterpriseId = computed(() => user.value?.currentEnterpriseId || null)

  function setAuth(authData) {
    token.value = authData.token
    user.value = {
      phone: authData.phone,
      nickname: authData.nickname,
      role: authData.role,
      userType: authData.userType || 'normal',
      currentEnterpriseId: authData.currentEnterpriseId || null,
      enterpriseName: authData.enterpriseName || null,
      hasData: authData.hasData !== false,
      realName: authData.realName,
      department: authData.department,
      employeeId: authData.employeeId
    }
    localStorage.setItem('token', authData.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function updateEnterprise(newToken, enterpriseId, enterpriseName) {
    token.value = newToken
    if (user.value) {
      user.value.currentEnterpriseId = enterpriseId
      user.value.enterpriseName = enterpriseName
    }
    localStorage.setItem('token', newToken)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, userType, isEnterprise, isAdmin, isNormal, currentEnterpriseId, setAuth, updateEnterprise, logout }
})
