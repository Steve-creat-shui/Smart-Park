<template>
  <div class="home-page">
    <div class="city-3d-wrap">
      <iframe ref="iframeRef" src="/smart-park-v3.html" class="city-3d-iframe" frameborder="0" allowfullscreen></iframe>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const iframeRef = ref(null)

const nameToId = {
  '宿舍楼A': 'B005', '宿舍楼B': 'B005',
  '行政楼': 'B001', '图书馆': 'B002',
  '实验楼': 'B003', '教学楼': 'B004',
  '室内体育馆': 'B007', '餐厅': 'B006'
}

function onMessage(e) {
  if (e.data?.type === 'building-click') {
    const id = nameToId[e.data.name] || 'B001'
    router.push(`/building/${id}`)
  }
}

onMounted(() => window.addEventListener('message', onMessage))
onBeforeUnmount(() => window.removeEventListener('message', onMessage))
</script>

<style scoped>
.home-page {
  display: flex; flex-direction: column;
  color: #e0f0ff; overflow: hidden; height: 100%;
}
.city-3d-wrap {
  flex: 1; position: relative; overflow: hidden;
  border: 1px solid #1a3a5c; border-radius: 12px; min-height: 0;
}
.city-3d-iframe {
  width: 100%; height: 100%; display: block;
  border: none; border-radius: 12px;
}
</style>
