import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    if (error.response?.status === 403) {
      ElMessage.warning(error.response?.data?.message || '权限不足')
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || '请求失败，请检查网络连接')
    return Promise.reject(error)
  }
)

export const authApi = {
  sendCode(phone) {
    return request.post('/auth/send-code', { phone })
  },
  register(data) {
    return request.post('/auth/register', data)
  },
  login(data) {
    return request.post('/auth/login', data)
  }
}

export const energyApi = {
  getEnergyData(buildingId, params) {
    return request.get(`/energy-consumption/${buildingId}`, { params })
  },
  getLatestEnergyData(buildingId, limit = 24) {
    return request.get(`/energy-consumption/${buildingId}/latest`, { params: { limit } })
  },
  getAnomalies(buildingId) {
    return request.get(`/energy-consumption/${buildingId}/anomalies`)
  },
  addEnergyData(data) {
    return request.post('/energy-consumption', data)
  },
  getMonthlyTrend(buildingId) {
    return request.get(`/energy-consumption/${buildingId}/monthly-trend`)
  },
  getLatestData(buildingId, limit = 12) {
    return request.get(`/energy-consumption/${buildingId}/latest`, { params: { limit } })
  },
  getAvailableDates(buildingId) {
    return request.get(`/energy-consumption/${buildingId}/available-dates`)
  }
}

export const buildingApi = {
  getAllBuildings() {
    return request.get('/buildings')
  },
  getBuildingList() {
    return request.get('/buildings/list')
  },
  getBuildingById(buildingId) {
    return request.get(`/buildings/${buildingId}`)
  },
  addBuilding(data) {
    return request.post('/buildings', data)
  },
  getAllBuildingIds() {
    return request.get('/buildings/ids')
  }
}

export const dashboardApi = {
  getStats() {
    return request.get('/dashboard/stats')
  }
}

export const analysisApi = {
  analyzeEnergy(data) {
    return request.post('/energy-analysis', data)
  },
  aiQuery(buildingId, query, contextData = '') {
    return request.post('/ai-query', { buildingId, query, contextData })
  },
  analyzeIssue(buildingId, timeRange) {
    return request.get(`/energy-analysis/${buildingId}/issue`, { params: { timeRange } })
  }
}

export const reportApi = {
  getReport(buildingId, params = {}) {
    return request.get(`/report/${buildingId}`, { params })
  },
  exportReport(buildingId) {
    const token = localStorage.getItem('token')
    return axios.get(`/api/report/${buildingId}/export`, {
      responseType: 'blob',
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
  }
}

export const dataApi = {
  previewFile(formData) {
    return request.post('/data/preview', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  importFile(formData) {
    return request.post('/data/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getPagedData(buildingId, startDate, endDate, page, size) {
    return request.get(`/energy-consumption/${buildingId}`, {
      params: { startDate, endDate, page, size }
    })
  },
  getImportHistory() {
    return request.get('/data/import-history')
  }
}

export const agentApi = {
  query(data) {
    return request.post('/agent/query', data, { timeout: 120000 })
  },
  getKnowledge() {
    return request.get('/agent/knowledge')
  },
  addKnowledge(data) {
    return request.post('/agent/knowledge', data)
  },
  updateKnowledge(id, data) {
    return request.put(`/agent/knowledge/${id}`, data)
  },
  deleteKnowledge(id) {
    return request.delete(`/agent/knowledge/${id}`)
  },
  saveFeedback(data) {
    return request.post('/agent/feedback', data)
  },
  getFeedbackAll() {
    return request.get('/agent/feedback/all')
  },
  deleteFeedback(id) {
    return request.delete(`/agent/feedback/${id}`)
  },
  getStats() {
    return request.get('/agent/stats')
  }
}

export const enterpriseApi = {
  apply(data) {
    return request.post('/enterprise/apply', data)
  },
  myApplications() {
    return request.get('/enterprise/my-applications')
  },
  myEnterprises() {
    return request.get('/enterprise/my-enterprises')
  },
  switchEnterprise(enterpriseId) {
    return request.post(`/enterprise/switch/${enterpriseId}`)
  },
  getInfo() {
    return request.get('/enterprise/info')
  },
  getMembers() {
    return request.get('/enterprise/members')
  },
  addMember(phone, role) {
    return request.post('/enterprise/members/add', { phone, role })
  },
  removeMember(memberId) {
    return request.delete(`/enterprise/members/${memberId}`)
  },
  updateMemberRole(memberId, role) {
    return request.put(`/enterprise/members/${memberId}/role`, { role })
  },
  getNotifications() {
    return request.get('/enterprise/notifications')
  },
  getUnreadCount() {
    return request.get('/enterprise/notifications/unread-count')
  },
  markAsRead(id) {
    return request.put(`/enterprise/notifications/${id}/read`)
  },
  markAllAsRead() {
    return request.put('/enterprise/notifications/read-all')
  },
  hasData() {
    return request.get('/enterprise/has-data')
  }
}

export const adminApi = {
  getPendingUsers() {
    return request.get('/admin/users/pending')
  },
  getAllUsers(params) {
    return request.get('/admin/users', { params })
  },
  approveUser(id) {
    return request.post(`/admin/users/${id}/approve`)
  },
  rejectUser(id, reason) {
    return request.post(`/admin/users/${id}/reject`, { reason })
  },
  disableUser(id) {
    return request.post(`/admin/users/${id}/disable`)
  },
  enableUser(id) {
    return request.post(`/admin/users/${id}/enable`)
  },
  updateRole(id, role) {
    return request.put(`/admin/users/${id}/role`, { role })
  },
  resetPassword(id) {
    return request.post(`/admin/users/${id}/reset-password`)
  },
  getStats() {
    return request.get('/admin/stats')
  },
  getEnterpriseApplications(status) {
    return request.get('/admin/enterprise-applications', { params: { status } })
  },
  approveApplication(id) {
    return request.post(`/admin/enterprise-applications/${id}/approve`)
  },
  rejectApplication(id, reason) {
    return request.post(`/admin/enterprise-applications/${id}/reject`, { reason })
  },
  getEnterprises() {
    return request.get('/admin/enterprises')
  }
}

export const alertApi = {
  list(params = {}) {
    return request.get('/alerts', { params })
  },
  stats() {
    return request.get('/alerts/stats')
  },
  create(data) {
    return request.post('/alerts', data)
  },
  acknowledge(id) {
    return request.put(`/alerts/${id}/acknowledge`)
  },
  resolve(id) {
    return request.put(`/alerts/${id}/resolve`)
  }
}

export const workOrderApi = {
  list(params = {}) {
    return request.get('/work-orders', { params })
  },
  stats() {
    return request.get('/work-orders/stats')
  },
  start(id) {
    return request.put(`/work-orders/${id}/start`)
  },
  complete(id, resolutionNotes) {
    return request.put(`/work-orders/${id}/complete`, { resolutionNotes })
  },
  assign(id, assignedTo) {
    return request.put(`/work-orders/${id}/assign`, { assignedTo })
  },
  assignableUsers() {
    return request.get('/work-orders/assignable-users')
  }
}

export default request
