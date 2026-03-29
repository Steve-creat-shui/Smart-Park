import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.response.use(
  response => response.data,
  error => {
    ElMessage.error(error.response?.data?.message || '请求失败，请检查网络连接')
    return Promise.reject(error)
  }
)

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
  }
}

export const buildingApi = {
  getAllBuildings() {
    return request.get('/buildings')
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
  getReport(buildingId) {
    return request.get(`/report/${buildingId}`)
  },
  exportReport(buildingId) {
    return axios.get(`/api/report/${buildingId}/export`, {
      responseType: 'blob'
    })
  }
}

export default request
