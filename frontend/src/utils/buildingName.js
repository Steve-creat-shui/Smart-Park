import { buildingApi } from '../api/index.js'

const DEFAULT_MAP = {
  B001: '行政楼',
  B002: '图书馆',
  B003: '实验楼',
  B004: '教学楼',
  B005: '宿舍楼',
  B006: '餐厅',
  B007: '室内体育馆'
}

export const buildingNameMap = { ...DEFAULT_MAP }

export async function loadBuildingNames() {
  try {
    const res = await buildingApi.getBuildingList()
    if (res.code === 200 && Array.isArray(res.data)) {
      res.data.forEach(b => { buildingNameMap[b.id] = b.name })
    }
  } catch (e) {}
  return buildingNameMap
}

export function getBuildingName(id) {
  return buildingNameMap[id] || id
}
