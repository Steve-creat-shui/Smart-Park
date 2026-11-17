export const buildings = [
  {
    name: '教学楼', nameShort: '教学楼', bgIcon: '🏫', buildingId: 'B004',
    color: '#00d4ff', tag: '高能耗',
    area: '15,000 m²', floors: '10 层', devices: '324 台',
    desc: '园区主教学区，供暖制冷需求大，日均能耗最高，空调系统为主要耗能来源。',
    path: '/building/B004'
  },
  {
    name: '实验楼', nameShort: '实验楼', bgIcon: '🔬', buildingId: 'B003',
    color: '#52c41a', tag: '设备密集',
    area: '9,500 m²', floors: '6 层', devices: '256 台',
    desc: '精密实验设备集中，能耗稳定，故障监测需求高，需重点关注设备异常。',
    path: '/building/B003'
  },
  {
    name: '图书馆', nameShort: '图书馆', bgIcon: '📚', buildingId: 'B002',
    color: '#9254de', tag: '常开运营',
    area: '8,000 m²', floors: '5 层', devices: '118 台',
    desc: '开放时间长，照明与空调为主要能耗来源，节假日有明显能耗下降。',
    path: '/building/B002'
  },
  {
    name: '餐厅', nameShort: '餐厅', bgIcon: '🍽️', buildingId: 'B006',
    color: '#ff7c3c', tag: '餐饮区',
    area: '4,000 m²', floors: '3 层', devices: '186 台',
    desc: '用餐高峰期能耗激增，厨房设备为主要耗能源，峰谷差异显著。',
    path: '/building/B006'
  },
  {
    name: '行政楼', nameShort: '行政楼', bgIcon: '🏛️', buildingId: 'B001',
    color: '#36cfc9', tag: '办公区',
    area: '10,000 m²', floors: '8 层', devices: '142 台',
    desc: '行政办公区，能耗数据完整，工作日与非工作日能耗差异明显。',
    path: '/building/B001'
  },
  {
    name: '室内体育馆', nameShort: '室内体育馆', bgIcon: '🏟️', buildingId: 'B007',
    color: '#8bafc9', tag: '周期性',
    area: '12,000 m²', floors: '2 层', devices: '198 台',
    desc: '赛事期间能耗激增，日常低谷，峰谷差异大，照明与通风为主要能耗。',
    path: '/building/B007'
  },
  {
    name: '宿舍楼', nameShort: '宿舍楼', bgIcon: '🏠', buildingId: 'B005',
    color: '#f759ab', tag: '住宿区',
    area: '20,000 m²', floors: '12 层', devices: '520 台',
    desc: '全园区最大建筑，以用电为主，热水消耗突出，夜间能耗占比高。',
    path: '/building/B005'
  },
]

export const buildingMap = Object.fromEntries(buildings.map(b => [b.buildingId, b]))
