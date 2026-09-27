<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getBarnOptions } from '@/api/barn'
import { WarningFilled, Picture } from '@element-plus/icons-vue'
import BarnCode from '@/components/BarnCode.vue'

/** 烤房图片列表 */
const barnImages = [
  '/barn-images/068f5b4954c2aa6ca2a90762281a12f.jpg',
  '/barn-images/20bef3cbac804ded7e2a272db682498.jpg',
  '/barn-images/23515701961d290bdff1094822adc2c.jpg',
  '/barn-images/4098c1673d6b94097a6beab300fad17.jpg',
  '/barn-images/485314d2b4247eb008b4e17d282c4b3.jpg',
  '/barn-images/6d85e2c76606f591ba7ad92647fafff.jpg',
  '/barn-images/d2d3feca8561f60b6d3236337870f1b.jpg',
  '/barn-images/d7ac703eb09a81c9f4e030b9f25b231.jpg',
  '/barn-images/d2ff9d77167276e2b2a9a87e1835a63.jpg',
]

/** 根据烤房ID获取图片 */
function getBarnImage(barn: any): string {
  const idx = (Number(barn.id) || 0) % barnImages.length
  return barnImages[idx]
}

/** 根据id获取图片URL（用于tooltip等无法调用组件函数的场景） */
function getBarnImageFromIdx(id: any): string {
  const idx = (Number(id) || 0) % barnImages.length
  return barnImages[idx]
}

/** 烤房数据 */
const barnList = ref<any[]>([])
const loading = ref(false)
const currentTime = ref('')
let timeTimer: any = null

/** 三级联动筛选 */
const selectedCity = ref('')
const selectedCounty = ref('')
const selectedTown = ref('')

/** 健康等级筛选 — 点击图例筛选，再次点击取消 */
const healthFilter = ref('')  // '', '优良', '良好', '一般', '预警', '危险'

/** 被隐藏的健康等级（点击图例切换显示/隐藏） */
const hiddenHealthLevels = ref<Set<string>>(new Set())

/** 健康等级选项 — 匹配数据库实际值：优良/良好/一般/预警/危险 */
const healthLevelOptions = [
  { label: '优良', color: '#2E8B6A' },
  { label: '良好', color: '#5FB292' },
  { label: '一般', color: '#D4944A' },
  { label: '预警', color: '#D4604A' },
  { label: '危险', color: '#B71C1C' },
]

/** 点击健康等级筛选按钮 */
function toggleHealthFilter(level: string) {
  healthFilter.value = healthFilter.value === level ? '' : level
}

/** 点击图例切换显示/隐藏 */
function toggleLegendItem(level: string) {
  const s = new Set(hiddenHealthLevels.value)
  if (s.has(level)) s.delete(level)
  else s.add(level)
  hiddenHealthLevels.value = s
}

/** 重置所有筛选 */
function resetAllFilters() {
  healthFilter.value = ''
  hiddenHealthLevels.value = new Set()
  selectedCity.value = ''
  selectedCounty.value = ''
  selectedTown.value = ''
}

/** 选中烤房详情 */
const selectedBarn = ref<any | null>(null)
const detailVisible = ref(false)

/** 健康等级颜色 - 统一颜色规范（匹配数据库实际值：优良/良好/一般/预警/危险） */
const healthColor: Record<string, string> = {
  '优良': '#2E8B6A',
  '良好': '#5FB292',
  '一般': '#D4944A',
  '预警': '#D4604A',
  '危险': '#B71C1C',
}

/** 有经纬度的烤房 */
const geoBarns = computed(() =>
  barnList.value.filter((b: any) => Number(b.longitude) > 0 && Number(b.latitude) > 0)
)

/** 根据三级联动筛选 + 健康等级筛选条件过滤数据 */
const filteredBarns = computed(() => {
  let list = barnList.value
  if (selectedCity.value) list = list.filter((b: any) => b.cityCode === selectedCity.value)
  if (selectedCounty.value) list = list.filter((b: any) => b.countyCode === selectedCounty.value)
  if (selectedTown.value) list = list.filter((b: any) => (b.townCode || b.township) === selectedTown.value)
  // 健康等级筛选：只显示选中的等级
  if (healthFilter.value) list = list.filter((b: any) => b.healthLevel === healthFilter.value)
  // 隐藏被关闭的等级
  if (hiddenHealthLevels.value.size > 0) {
    list = list.filter((b: any) => !hiddenHealthLevels.value.has(b.healthLevel))
  }
  return list
})

/** 筛选后的有经纬度烤房 */
const filteredGeoBarns = computed(() =>
  filteredBarns.value.filter((b: any) => Number(b.longitude) > 0 && Number(b.latitude) > 0)
)

/** 市级选项 */
const cityOptions = computed(() => {
  const set = new Map<string, string>()
  barnList.value.forEach((b: any) => {
    if (b.cityCode) set.set(b.cityCode, b.cityName || b.cityCode)
  })
  return [{ value: '', label: '全部市' }, ...Array.from(set.entries()).map(([value, label]) => ({ value, label }))]
})

/** 县级选项（受市级筛选影响） */
const countyOptions = computed(() => {
  let source = barnList.value
  if (selectedCity.value) source = source.filter((b: any) => b.cityCode === selectedCity.value)
  const set = new Map<string, string>()
  source.forEach((b: any) => {
    if (b.countyCode) set.set(b.countyCode, b.county || b.countyCode)
  })
  return [{ value: '', label: '全部县区' }, ...Array.from(set.entries()).map(([value, label]) => ({ value, label }))]
})

/** 乡级选项（受市、县级筛选影响） */
const townOptions = computed(() => {
  let source = barnList.value
  if (selectedCity.value) source = source.filter((b: any) => b.cityCode === selectedCity.value)
  if (selectedCounty.value) source = source.filter((b: any) => b.countyCode === selectedCounty.value)
  const set = new Map<string, string>()
  source.forEach((b: any) => {
    const key = b.townCode || b.township || ''
    if (key) set.set(key, b.township || key)
  })
  return [{ value: '', label: '全部乡镇' }, ...Array.from(set.entries()).map(([value, label]) => ({ value, label }))]
})

/** 市变化时重置县和乡 */
watch(selectedCity, () => {
  selectedCounty.value = ''
  selectedTown.value = ''
})

/** 县变化时重置乡 */
watch(selectedCounty, () => {
  selectedTown.value = ''
})

/** 统计卡片 — 烤房总数/在用/优良/危险/正常/另作他用 */
const statCards = computed(() => {
  const list = filteredBarns.value
  return [
    { label: '烤房总数', value: list.length, color: '#5FB292' },
    { label: '在用', value: list.filter((b: any) => b.useStatus === '在用').length, color: '#2E8B6A' },
    { label: '优良', value: list.filter((b: any) => b.healthLevel === '优良').length, color: '#6ee7b7' },
    { label: '危险', value: list.filter((b: any) => b.healthLevel === '危险').length, color: '#fca5a5' },
    { label: '正常', value: list.filter((b: any) => (b.facilityStatus || '正常') === '正常').length, color: '#4ade80' },
    { label: '另作他用', value: list.filter((b: any) => b.facilityStatus === '另作他用').length, color: '#c084fc' },
  ]
})

/** 统计卡片动画值 */
const animatedStats = ref<Record<string, number>>({})
const prevStats = ref<Record<string, number>>({})

/** 数字滚动动画 */
watch(statCards, (newCards, oldCards) => {
  const oldMap: Record<string, number> = {}
  oldCards?.forEach(c => { oldMap[c.label] = c.value })
  prevStats.value = { ...oldMap }

  newCards.forEach(card => {
    const from = prevStats.value[card.label] ?? 0
    const to = card.value
    if (from === to) {
      animatedStats.value[card.label] = to
      return
    }
    const duration = 600
    const startTime = performance.now()
    const animate = (now: number) => {
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      animatedStats.value[card.label] = Math.round(from + (to - from) * eased)
      if (progress < 1) requestAnimationFrame(animate)
    }
    requestAnimationFrame(animate)
  })
}, { immediate: true })

/** 关键统计数据 */
const keyStats = computed(() => {
  const list = filteredBarns.value
  const total = list.length
  const avgScore = total ? Math.round(list.reduce((s: number, b: any) => s + Number(b.healthScore || 0), 0) / total) : 0
  const lifeValues = list.map((b: any) => Number(b.predictedLifeYears || 0)).filter((v: number) => v > 0)
  const avgLife = lifeValues.length ? (lifeValues.reduce((a: number, v: number) => a + v, 0) / lifeValues.length).toFixed(1) : '0'
  const inUse = list.filter((b: any) => b.useStatus === '在用').length
  const excellent = list.filter((b: any) => b.healthLevel === '优良').length
  return {
    avgScore,
    avgLife,
    inUseRate: total ? ((inUse / total) * 100).toFixed(1) : '0',
    excellentRate: total ? ((excellent / total) * 100).toFixed(1) : '0',
  }
})

/** 预警列表 — 健康分低(<60)或寿命较短(<5年) */
const alertList = computed(() => {
  const list = filteredBarns.value
  return list
    .filter((b: any) => {
      const score = Number(b.healthScore || 0)
      const life = Number(b.predictedLifeYears || 0)
      return score < 60 || (life > 0 && life < 5)
    })
    .sort((a: any, b: any) => (a.healthScore || 0) - (b.healthScore || 0))
    .slice(0, 15)
})

/** 按乡镇分组 */
const townGroups = computed(() => {
  const map = new Map<string, { name: string; county: string; barns: any[] }>()
  const list = filteredBarns.value
  list.forEach((b: any) => {
    const key = b.township || b.townCode || '未知'
    if (!map.has(key)) map.set(key, { name: key, county: b.county || '', barns: [] })
    map.get(key)!.barns.push(b)
  })
  return Array.from(map.values()).sort((a, b) => b.barns.length - a.barns.length)
})

/** 乡镇条形图颜色 */
const townColors = [
  'linear-gradient(90deg, #0A4D3E, #0D5D46)',
  'linear-gradient(90deg, #0D5D46, #1A6B4F)',
  'linear-gradient(90deg, #1A6B4F, #2E8B6A)',
  'linear-gradient(90deg, #2E8B6A, #3DA07A)',
  'linear-gradient(90deg, #3DA07A, #5FB292)',
  'linear-gradient(90deg, #5FB292, #7DC4A8)',
  'linear-gradient(90deg, #0D5D46, #2E8B6A)',
  'linear-gradient(90deg, #1A6B4F, #5FB292)',
  'linear-gradient(90deg, #0A4D3E, #1A6B4F)',
  'linear-gradient(90deg, #2E8B6A, #7DC4A8)',
  'linear-gradient(90deg, #0D5D46, #3DA07A)',
  'linear-gradient(90deg, #1A6B4F, #3DA07A)',
  'linear-gradient(90deg, #0A4D3E, #2E8B6A)',
  'linear-gradient(90deg, #0D5D46, #5FB292)',
  'linear-gradient(90deg, #1A6B4F, #7DC4A8)',
]

/** 乡镇最大烤房数 */
const maxTownCount = computed(() => {
  const groups = townGroups.value
  return groups.length ? Math.max(...groups.map(g => g.barns.length)) : 1
})

/** 地图引用 */
const mapRef = ref<HTMLDivElement>()
let mapInstance: L.Map | null = null
let markerLayer: L.LayerGroup | null = null
let townLayer: L.LayerGroup | null = null

/** 根据健康等级获取颜色 */
function getHealthSymbolColor(level: string): string {
  return healthColor[level] || '#94a3b8'
}

/** 创建烤房水滴图标的 SVG DivIcon */
function createBarnIcon(color: string, score: number): L.DivIcon {
  const size = 7 + (score / 100) * 5
  return L.divIcon({
    className: 'barn-marker',
    html: `<svg width="${size * 1.5}" height="${size * 2}" viewBox="0 0 24 32" style="filter: drop-shadow(0 2px 3px rgba(0,0,0,0.4));">
      <path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0zm0 16c-2.2 0-4-1.8-4-4s1.8-4 4-4 4 1.8 4 4-1.8 4-4 4z" 
        fill="${color}" stroke="rgba(255,255,255,0.7)" stroke-width="1.5"/>
    </svg>`,
    iconSize: [size * 1.5, size * 2],
    iconAnchor: [size * 0.75, size * 2],
    popupAnchor: [0, -size * 2],
  })
}

/** 渲染瓦片地图 */
function renderMap() {
  if (!mapRef.value) return

  // 初始化地图（只创建一次）
  if (!mapInstance) {
    mapInstance = L.map(mapRef.value, {
      center: [30.73, 110.5],
      zoom: 12,
      zoomControl: true,
      attributionControl: false,
    })

    // 高德地图标准瓦片
    L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      subdomains: ['1', '2', '3', '4'],
      maxZoom: 18,
      minZoom: 3,
    }).addTo(mapInstance)

    markerLayer = L.layerGroup().addTo(mapInstance)
    townLayer = L.layerGroup().addTo(mapInstance)
  }

  // 清除旧标记
  markerLayer?.clearLayers()
  townLayer?.clearLayers()

  const data = filteredGeoBarns.value

  // 添加烤房标记
  const latlngs: [number, number][] = []
  data.forEach((b: any) => {
    const lng = Number(b.longitude)
    const lat = Number(b.latitude)
    if (lng <= 0 || lat <= 0) return
    latlngs.push([lat, lng])

    const color = getHealthSymbolColor(b.healthLevel)
    const score = Math.round(Number(b.healthScore || 0))
    const marker = L.marker([lat, lng], {
      icon: createBarnIcon(color, score),
    })

    // 弹窗内容
    marker.bindPopup(`
      <div style="min-width:220px;font-family:sans-serif;">
        <img src="${getBarnImageFromIdx(b.id)}" style="width:100%;height:100px;object-fit:cover;border-radius:4px;margin-bottom:6px;" onerror="this.style.display='none'" />
        <div style="font-weight:700;font-size:14px;margin-bottom:6px;color:#1a3d30;">${b.barnName}</div>
        <div style="margin-bottom:3px;font-size:12px;color:#555;">编号：<span style="color:#2E8B6A;font-weight:600;">${b.id || b.barnCode || '-'}</span></div>
        <div style="margin-bottom:3px;font-size:12px;color:#555;">地址：${b.address || b.gpsAddress || '-'}</div>
        <div style="margin-bottom:3px;font-size:12px;color:#555;">县区：${b.county || '-'} · ${b.township || '-'}</div>
        <div style="margin-bottom:3px;font-size:12px;color:#555;">健康等级：<span style="color:${color};font-weight:600;">${b.healthLevel || '-'}</span></div>
        <div style="font-size:12px;color:#555;">健康评分：<b style="color:${color};font-size:14px;">${score}</b> 分</div>
      </div>
    `, { maxWidth: 280 })

    marker.on('click', () => {
      selectedBarn.value = b
      detailVisible.value = true
    })

    markerLayer?.addLayer(marker)
  })

  // 添加乡镇标注（圆环脉冲效果用 CSS 模拟）
  townGroups.value.forEach(t => {
    const barnsWithGeo = t.barns.filter((b: any) => Number(b.longitude) > 0 && Number(b.latitude) > 0)
    if (!barnsWithGeo.length) return
    const avgLat = barnsWithGeo.reduce((s: number, b: any) => s + Number(b.latitude), 0) / barnsWithGeo.length
    const avgLng = barnsWithGeo.reduce((s: number, b: any) => s + Number(b.longitude), 0) / barnsWithGeo.length

    const townMarker = L.marker([avgLat, avgLng], {
      icon: L.divIcon({
        className: 'town-marker',
        html: `<div class="town-pulse"><span class="town-pulse-ring"></span><span class="town-pulse-label">${t.name}</span></div>`,
        iconSize: [0, 0],
      }),
    })
    townLayer?.addLayer(townMarker)
  })

  // 自动适配视图到所有标记点
  if (latlngs.length) {
    mapInstance.fitBounds(latlngs, { padding: [60, 60], maxZoom: 14 })
  }

  // 延迟刷新地图尺寸，确保容器渲染完成后地图正确显示
  setTimeout(() => { mapInstance?.invalidateSize() }, 200)
}

function handleResize() { mapInstance?.invalidateSize() }

function updateTime() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  currentTime.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function healthScoreColor(score: number): string {
  if (score >= 90) return '#2E8B6A' // 优良：绿色
  if (score >= 75) return '#5FB292' // 良好：浅绿色
  if (score >= 60) return '#D4944A' // 一般：橙色
  if (score >= 40) return '#D4604A' // 预警：橙红色
  return '#B71C1C' // 危险：红色
}

function selectBarn(barn: any) {
  selectedBarn.value = barn
  detailVisible.value = true
}

watch([selectedCity, selectedCounty, selectedTown, healthFilter, hiddenHealthLevels], () => { nextTick(() => renderMap()) })

const loadError = ref('')

async function loadAll() {
  loading.value = true
  loadError.value = ''
  try {
    const barns = await getBarnOptions().catch((e: any) => {
      console.error('[DigitalTwin] getBarnOptions failed:', e)
      loadError.value = '获取烤房数据失败，请确认已登录后刷新页面'
      return []
    })
    console.log('[DigitalTwin] loaded barns count:', barns?.length)
    barnList.value = (barns || []) as any[]
    const geoCount = barnList.value.filter((b: any) => Number(b.longitude) > 0 && Number(b.latitude) > 0).length
    console.log('[DigitalTwin] barns with geo:', geoCount)
    if (geoCount === 0 && barnList.value.length > 0) {
      loadError.value = '烤房数据中无经纬度信息'
    } else if (barnList.value.length === 0 && !loadError.value) {
      loadError.value = '未获取到烤房数据，请确认已登录'
    }
    await nextTick()
    renderMap()
  } catch (e: any) {
    console.error('[DigitalTwin] loadAll error:', e)
    loadError.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (timeTimer) clearInterval(timeTimer)
  window.removeEventListener('resize', handleResize)
  mapInstance?.remove()
  mapInstance = null
})
</script>

<template>
  <div class="twin-page" v-loading="loading">
    <!-- 顶部 -->
    <header class="twin-header">
      <div class="header-deco-line"></div>
      <div class="header-deco-line header-deco-line--right"></div>
      <div class="header-left">
        <div class="header-logo">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
            <path d="M12 2 L14 2 L14 6 L18 6 L18 8 L14 8 L14 18 L16 18 L16 20 L8 20 L8 18 L10 18 L10 8 L6 8 L6 6 L10 6 L10 2 Z" fill="rgba(255,255,255,0.85)" />
          </svg>
        </div>
        <div>
          <h1 class="header-title">烤房分布图</h1>
          <div class="header-sub">TOBACCO BARN DISTRIBUTION MAP</div>
        </div>
      </div>
      <div class="header-center">
        <div v-for="card in statCards" :key="card.label" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ animatedStats[card.label] ?? card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-card-accent" :style="{ background: card.color }"></div>
        </div>
      </div>
      <div class="header-right">
        <div class="header-time">{{ currentTime }}</div>
        <div class="filter-row">
          <el-select v-model="selectedCity" size="small" class="filter-select" placeholder="全部市">
            <el-option v-for="opt in cityOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="selectedCounty" size="small" class="filter-select" placeholder="全部县区">
            <el-option v-for="opt in countyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="selectedTown" size="small" class="filter-select" placeholder="全部乡镇">
            <el-option v-for="opt in townOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
      </div>
    </header>

    <!-- 主体 -->
    <div class="twin-body">
      <!-- 错误提示 -->
      <div v-if="loadError && !loading" class="load-error">
        <el-icon :size="48" color="#fca5a5"><WarningFilled /></el-icon>
        <div class="error-text">{{ loadError }}</div>
        <el-button type="primary" size="small" @click="loadAll">重新加载</el-button>
      </div>
      <!-- 地图区域 -->
      <div v-if="!loadError || loading" class="map-panel">
        <div class="map-header">
          <span class="dot"></span>
          <span>烤房地理分布 · 实时孪生</span>
          <span class="map-info">
            共 {{ filteredGeoBarns.length }} 座烤房
            <span v-if="healthFilter" class="filter-active-tag" :style="{ color: healthLevelOptions.find(o => o.label === healthFilter)?.color }">
              · 筛选: {{ healthFilter }}
            </span>
          </span>
        </div>
        <!-- 健康等级筛选栏 -->
        <div class="health-filter-bar">
          <span class="filter-label">健康等级：</span>
          <div
            v-for="opt in healthLevelOptions"
            :key="opt.label"
            class="health-filter-chip"
            :class="{ 'chip-active': healthFilter === opt.label }"
            :style="healthFilter === opt.label ? { background: opt.color, borderColor: opt.color } : {}"
            @click="toggleHealthFilter(opt.label)"
          >
            <span class="chip-dot" :style="{ background: opt.color }"></span>
            {{ opt.label }}
          </div>
          <div class="health-filter-chip chip-reset" @click="resetAllFilters">重置</div>
        </div>
        <div class="map-chart-wrap">
          <div ref="mapRef" class="map-chart"></div>
        </div>
        <!-- 图例 + 健康等级筛选 -->
        <div class="map-legend">
          <div
            v-for="opt in healthLevelOptions"
            :key="opt.label"
            class="legend-item legend-clickable"
            :class="{
              'legend-active': healthFilter === opt.label,
              'legend-dimmed': hiddenHealthLevels.has(opt.label),
            }"
            @click="toggleHealthFilter(opt.label)"
            @contextmenu.prevent="toggleLegendItem(opt.label)"
          >
            <svg viewBox="0 0 24 32" width="10" height="14" class="legend-barn-icon"><path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0zm0 16c-2.2 0-4-1.8-4-4s1.8-4 4-4 4 1.8 4 4-1.8 4-4 4z" :fill="opt.color" /></svg>
            {{ opt.label }}
          </div>
          <div class="legend-item"><span class="legend-ring"></span>乡镇中心</div>
          <div class="legend-reset-btn" @click="resetAllFilters">重置</div>
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="side-panel">
        <!-- 关键数据 -->
        <div class="side-section key-stats-section">
          <div class="section-head"><span class="dot"></span>关键数据</div>
          <div class="key-stats-grid">
            <div class="key-stat-item">
              <div class="key-stat-value" style="color: #5FB292;">{{ keyStats.avgScore }}</div>
              <div class="key-stat-label">平均健康分</div>
            </div>
            <div class="key-stat-item">
              <div class="key-stat-value" style="color: #7DC4A8;">{{ keyStats.avgLife }}<span class="key-stat-unit">年</span></div>
              <div class="key-stat-label">平均剩余寿命</div>
            </div>
            <div class="key-stat-item">
              <div class="key-stat-value" style="color: #2E8B6A;">{{ keyStats.inUseRate }}<span class="key-stat-unit">%</span></div>
              <div class="key-stat-label">在用率</div>
            </div>
            <div class="key-stat-item">
              <div class="key-stat-value" style="color: #6ee7b7;">{{ keyStats.excellentRate }}<span class="key-stat-unit">%</span></div>
              <div class="key-stat-label">优良率</div>
            </div>
          </div>
        </div>

        <!-- 预警烤房 -->
        <div class="side-section">
          <div class="section-head"><span class="dot alert"></span>预警烤房 <span class="alert-count">{{ alertList.length }}</span></div>
          <div class="alert-list">
            <div v-for="barn in alertList" :key="barn.id" class="alert-item" @click="selectBarn(barn)">
              <div class="alert-status-bar" :style="{ background: healthColor[barn.healthLevel] || '#94a3b8' }"></div>
              <el-image
                :src="getBarnImage(barn)"
                fit="cover"
                class="alert-thumb"
              >
                <template #error>
                  <div class="alert-thumb-placeholder">
                    <el-icon :size="14" color="#5a7a9a"><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="alert-info">
                <div class="alert-name">{{ barn.barnName }}</div>
                <div class="alert-meta">{{ barn.county }} · {{ barn.township }}</div>
                <div class="alert-tags">
                  <span class="alert-tag" :style="{ background: (healthColor[barn.healthLevel] || '#78909c') + '22', color: healthColor[barn.healthLevel] || '#78909c' }">{{ barn.healthLevel || '未知' }}</span>
                  <span class="alert-tag" :style="{ color: healthScoreColor(Number(barn.healthScore || 0)) }">健康分 {{ Math.round(Number(barn.healthScore || 0)) }}</span>
                  <span v-if="Number(barn.predictedLifeYears) > 0" class="alert-tag life-tag">剩余 {{ Number(barn.predictedLifeYears).toFixed(1) }}年</span>
                </div>
              </div>
            </div>
            <div v-if="!alertList.length" class="no-alert">暂无预警</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="selectedBarn?.barnName" width="520px" destroy-on-close class="detail-dialog" append-to-body align-center>
      <template v-if="selectedBarn">
        <div class="detail-score-ring">
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="10" />
            <circle cx="60" cy="60" r="50" fill="none"
              :stroke="healthScoreColor(Number(selectedBarn.healthScore || 0))"
              stroke-width="10" stroke-linecap="round"
              :stroke-dasharray="2 * Math.PI * 50"
              :stroke-dashoffset="2 * Math.PI * 50 * (1 - Number(selectedBarn.healthScore || 0) / 100)"
              transform="rotate(-90 60 60)" />
          </svg>
          <div class="detail-score-num" :style="{ color: healthScoreColor(Number(selectedBarn.healthScore || 0)) }">
            {{ Math.round(Number(selectedBarn.healthScore || 0)) }}
          </div>
        </div>
        <!-- 烤房图片 -->
        <div class="detail-barn-image">
          <el-image
            :src="getBarnImage(selectedBarn)"
            fit="cover"
            :preview-src-list="barnImages"
            :initial-index="(Number(selectedBarn.id) || 0) % barnImages.length"
            style="width: 100%; height: 200px; border-radius: 6px;"
          >
            <template #error>
              <div class="image-placeholder">
                <el-icon :size="32"><Picture /></el-icon>
                <span>暂无图片</span>
              </div>
            </template>
          </el-image>
        </div>
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="烤房编号"><BarnCode :code="selectedBarn.id || selectedBarn.barnCode" dark /></el-descriptions-item>
          <el-descriptions-item label="健康等级">
            <span :style="{ color: healthColor[selectedBarn.healthLevel] || '#78909c', fontWeight: 600 }">{{ selectedBarn.healthLevel }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="所属县区">{{ selectedBarn.county }}</el-descriptions-item>
          <el-descriptions-item label="所属乡镇">{{ selectedBarn.township }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ selectedBarn.address || selectedBarn.gpsAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="使用状态">{{ selectedBarn.useStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ selectedBarn.projectType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经度">{{ Number(selectedBarn.longitude).toFixed(6) }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ Number(selectedBarn.latitude).toFixed(6) }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ==================== 森林绿色系 · 玻璃拟态风格 ==================== */

.twin-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  background: #0A1F18;
  background-image:
    radial-gradient(ellipse at 20% 50%, rgba(13, 93, 70, 0.06) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(46, 139, 106, 0.04) 0%, transparent 50%),
    linear-gradient(rgba(46, 139, 106, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 139, 106, 0.025) 1px, transparent 1px);
  background-size: 100% 100%, 100% 100%, 48px 48px, 48px 48px;
  border-radius: 8px;
  overflow: hidden;
  color: #b8d4c8;
  perspective: 1200px;
}

/* ==================== 头部 ==================== */
.twin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 24px;
  background: rgba(10, 31, 24, 0.94);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(46, 139, 106, 0.12);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  position: relative;
  flex-shrink: 0;
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.15),
    0 10px 20px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

/* 头部装饰线 */
.header-deco-line {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 180px;
  height: 2px;
  background: linear-gradient(90deg, rgba(95, 178, 146, 0.5), transparent);
  animation: decoSlide 4s ease-in-out infinite alternate;
}
.header-deco-line--right {
  left: auto;
  right: 0;
  background: linear-gradient(270deg, rgba(95, 178, 146, 0.5), transparent);
  animation-delay: 2s;
}

@keyframes decoSlide {
  0% { width: 120px; opacity: 0.6; }
  100% { width: 220px; opacity: 1; }
}

.twin-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 180px;
  right: 180px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(46, 139, 106, 0.2) 50%, transparent);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-logo {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0D5D46, #2E8B6A);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.15),
    0 10px 20px rgba(0, 0, 0, 0.1);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  animation: logoPulse 3s ease-in-out infinite;
}

@keyframes logoPulse {
  0%, 100% { box-shadow: 0 4px 6px rgba(0,0,0,0.15), 0 10px 20px rgba(0,0,0,0.1); }
  50% { box-shadow: 0 4px 6px rgba(0,0,0,0.15), 0 10px 20px rgba(0,0,0,0.1), 0 0 0 3px rgba(46,139,106,0.08); }
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #d4e8dc;
  margin: 0;
  letter-spacing: 3px;
  position: relative;
}
.header-sub {
  font-size: 10px;
  color: #4a7a62;
  letter-spacing: 2px;
  margin-top: 2px;
  text-transform: uppercase;
}

.header-center {
  display: flex;
  gap: 12px;
}

/* 统计卡片 - 玻璃拟态增强 */
.stat-card {
  position: relative;
  background: rgba(13, 45, 34, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 8px;
  padding: 10px 20px 10px 18px;
  text-align: center;
  min-width: 92px;
  overflow: hidden;
  transform: translateZ(0);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 6px 10px rgba(0, 0, 0, 0.12),
    0 14px 28px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 8px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(13, 93, 70, 0.35), rgba(46, 139, 106, 0.15), rgba(13, 93, 70, 0.05));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(46, 139, 106, 0.25), transparent);
}

.stat-card-accent {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40%;
  height: 2px;
  border-radius: 1px;
  opacity: 0.5;
  transition: width 0.4s ease, opacity 0.4s ease;
}

.stat-card:hover .stat-card-accent {
  width: 70%;
  opacity: 0.8;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.2;
  transition: color 0.3s ease;
}
.stat-label {
  font-size: 11px;
  color: #5a8a72;
  margin-top: 3px;
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.header-time {
  font-size: 15px;
  color: #5FB292;
  font-family: 'Consolas', monospace;
  font-weight: 600;
  letter-spacing: 1px;
}

.filter-row {
  display: flex;
  gap: 6px;
}

.filter-select {
  width: 110px;
}
.filter-select :deep(.el-input__wrapper) {
  background: rgba(13, 45, 34, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-color: rgba(46, 139, 106, 0.15);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}
.filter-select :deep(.el-input__wrapper:hover),
.filter-select :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(46, 139, 106, 0.35);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1), 0 0 0 2px rgba(46, 139, 106, 0.08);
}
.filter-select :deep(.el-input__inner) {
  color: #7aac96;
  font-size: 12px;
}
.filter-select :deep(.el-select__placeholder) {
  color: #4a7a62;
  font-size: 12px;
}

/* ==================== 主体 ==================== */
.twin-body {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 14px;
  padding: 14px;
  min-height: 0;
}

/* ==================== 地图面板 ==================== */
.map-panel {
  display: flex;
  flex-direction: column;
  background: rgba(13, 45, 34, 0.5);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(46, 139, 106, 0.15);
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  transform: translateZ(0);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.map-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 8px;
  padding: 1px;
  background: linear-gradient(160deg, rgba(13, 93, 70, 0.25), rgba(46, 139, 106, 0.08), transparent 60%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  z-index: 1;
}

.map-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  font-size: 15px;
  font-weight: 600;
  color: #d4e8dc;
  border-bottom: 1px solid rgba(46, 139, 106, 0.1);
  flex-shrink: 0;
  position: relative;
}

.map-header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 20px;
  width: 60px;
  height: 1px;
  background: rgba(95, 178, 146, 0.4);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #5FB292;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  flex-shrink: 0;
}
.dot.alert {
  background: #fca5a5;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.map-info {
  margin-left: auto;
  font-size: 12px;
  color: #5a8a72;
  font-weight: 400;
}

.filter-active-tag {
  font-weight: 600;
  margin-left: 4px;
}

/* 健康等级筛选栏 */
.health-filter-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 20px;
  border-bottom: 1px solid rgba(46, 139, 106, 0.08);
  flex-shrink: 0;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 12px;
  color: #5a8a72;
  margin-right: 2px;
}

.health-filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid rgba(46, 139, 106, 0.2);
  background: rgba(13, 45, 34, 0.4);
  color: #7aac96;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.health-filter-chip:hover {
  border-color: rgba(46, 139, 106, 0.4);
  color: #d4e8dc;
  background: rgba(46, 139, 106, 0.1);
}

.chip-active {
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.chip-active .chip-dot {
  background: rgba(255, 255, 255, 0.8) !important;
}

.chip-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.chip-reset {
  margin-left: auto;
  border-color: rgba(212, 96, 74, 0.3);
  background: rgba(212, 96, 74, 0.1);
  color: #fca5a5;
}

.chip-reset:hover {
  background: rgba(212, 96, 74, 0.25);
  color: #fff;
  border-color: rgba(212, 96, 74, 0.5);
}

.map-chart-wrap {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

.map-chart {
  width: 100%;
  height: 100%;
}

/* ==================== Leaflet 地图深色适配 ==================== */
.map-chart :deep(.leaflet-container) {
  background: #0A1F18;
  font-family: inherit;
}

/* 瓦片层深色滤镜 */
.map-chart :deep(.leaflet-tile-pane) {
  filter: invert(0.92) hue-rotate(170deg) brightness(0.95) contrast(0.9) saturate(0.7);
}

/* 缩放控件样式 */
.map-chart :deep(.leaflet-control-zoom) {
  border: 1px solid rgba(46, 139, 106, 0.2) !important;
  border-radius: 8px !important;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
}
.map-chart :deep(.leaflet-control-zoom a) {
  background: rgba(13, 45, 34, 0.9) !important;
  color: #7aac96 !important;
  border-bottom: 1px solid rgba(46, 139, 106, 0.12) !important;
  backdrop-filter: blur(8px);
  transition: all 0.2s ease;
}
.map-chart :deep(.leaflet-control-zoom a:hover) {
  background: rgba(46, 139, 106, 0.2) !important;
  color: #d4e8dc !important;
}

/* 弹窗样式 */
.map-chart :deep(.leaflet-popup-content-wrapper) {
  background: rgba(10, 31, 24, 0.96);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(46, 139, 106, 0.2);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  color: #b8d4c8;
}
.map-chart :deep(.leaflet-popup-content) {
  margin: 12px 16px;
  line-height: 1.5;
}
.map-chart :deep(.leaflet-popup-tip) {
  background: rgba(10, 31, 24, 0.96);
  border: 1px solid rgba(46, 139, 106, 0.2);
}
.map-chart :deep(.leaflet-popup-close-button) {
  color: #5a8a72 !important;
  font-size: 18px !important;
  padding: 6px 8px !important;
}
.map-chart :deep(.leaflet-popup-close-button:hover) {
  color: #d4e8dc !important;
}

/* 烤房标记 */
.map-chart :deep(.barn-marker) {
  background: transparent !important;
  border: none !important;
  cursor: pointer;
  transition: transform 0.2s ease;
}
.map-chart :deep(.barn-marker:hover) {
  z-index: 1000;
  transform: scale(1.2);
}

/* 乡镇标注脉冲 */
.map-chart :deep(.town-marker) {
  background: transparent !important;
  border: none !important;
}
.map-chart :deep(.town-pulse) {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  pointer-events: none;
}
.map-chart :deep(.town-pulse-ring) {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 6px rgba(46, 139, 106, 0.3);
  animation: townPulseAnim 3s ease-in-out infinite;
}
.map-chart :deep(.town-pulse-label) {
  font-size: 12px;
  font-weight: 600;
  color: #a0d4c0;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8), 0 0 6px rgba(10, 31, 24, 0.8);
  white-space: nowrap;
}

@keyframes townPulseAnim {
  0%, 100% { box-shadow: 0 0 4px rgba(46, 139, 106, 0.3); opacity: 0.7; }
  50% { box-shadow: 0 0 12px rgba(46, 139, 106, 0.6); opacity: 1; }
}

/* 坐标网格装饰叠加 */
.grid-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(46, 139, 106, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 139, 106, 0.04) 1px, transparent 1px);
  background-size: 60px 60px;
  z-index: 0;
}

.map-legend {
  position: absolute;
  bottom: 14px;
  left: 14px;
  display: flex;
  gap: 16px;
  background: rgba(10, 31, 24, 0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(46, 139, 106, 0.12);
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 12px;
  color: #7aac96;
  z-index: 1000;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.12);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.legend-clickable {
  cursor: pointer;
  padding: 3px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
  user-select: none;
}

.legend-clickable:hover {
  background: rgba(46, 139, 106, 0.15);
  color: #d4e8dc;
}

.legend-active {
  background: rgba(46, 139, 106, 0.25) !important;
  color: #d4e8dc !important;
  font-weight: 600;
  box-shadow: 0 0 0 1px rgba(46, 139, 106, 0.3), 0 2px 6px rgba(0, 0, 0, 0.2);
}

.legend-dimmed {
  opacity: 0.35;
  text-decoration: line-through;
}

.legend-reset-btn {
  margin-left: 8px;
  padding: 3px 12px;
  border-radius: 6px;
  background: rgba(212, 96, 74, 0.15);
  color: #fca5a5;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
  border: 1px solid rgba(212, 96, 74, 0.25);
}

.legend-reset-btn:hover {
  background: rgba(212, 96, 74, 0.3);
  color: #fff;
  border-color: rgba(212, 96, 74, 0.5);
}

.legend-barn-icon {
  flex-shrink: 0;
}

.legend-ring {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.5);
  background: rgba(255,255,255,0.15);
  flex-shrink: 0;
}

/* ==================== 右侧面板 ==================== */
.side-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.side-section {
  display: flex;
  flex-direction: column;
  background: rgba(13, 45, 34, 0.5);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(46, 139, 106, 0.12);
  border-radius: 8px;
  overflow: hidden;
  min-height: 0;
  transform: translateZ(0);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  position: relative;
}

/* 右侧面板玻璃拟态边框渐变 */
.side-section::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 8px;
  padding: 1px;
  background: linear-gradient(180deg, rgba(13, 93, 70, 0.3), rgba(46, 139, 106, 0.08), rgba(13, 93, 70, 0.15));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.side-section:first-child { flex: 0 0 auto; }
.side-section:last-child { flex: 1; min-height: 0; }

.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #d4e8dc;
  border-bottom: 1px solid rgba(46, 139, 106, 0.1);
  flex-shrink: 0;
}

.alert-count {
  font-size: 11px;
  background: rgba(252, 165, 165, 0.12);
  color: #fca5a5;
  padding: 2px 10px;
  border-radius: 8px;
  margin-left: auto;
  font-weight: 600;
}

/* ==================== 乡镇列表 ==================== */
.town-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px;
}
.town-list::-webkit-scrollbar { width: 3px; }
.town-list::-webkit-scrollbar-thumb { background: rgba(46, 139, 106, 0.25); border-radius: 2px; }

.town-item { margin-bottom: 10px; }
.town-name {
  font-size: 13px;
  color: #7aac96;
  margin-bottom: 5px;
  letter-spacing: 0.3px;
}
.town-bar-wrap { display: flex; align-items: center; gap: 10px; }
.town-bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  overflow: hidden;
  display: flex;
}
.town-fill {
  height: 100%;
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 4px;
}
.town-count {
  font-size: 13px;
  color: #d4e8dc;
  font-family: 'Consolas', monospace;
  font-weight: 600;
  min-width: 28px;
  text-align: right;
}

/* ==================== 预警列表 ==================== */
.alert-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.alert-list::-webkit-scrollbar { width: 3px; }
.alert-list::-webkit-scrollbar-thumb { background: rgba(252, 165, 165, 0.25); border-radius: 2px; }

.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

/* 左侧状态指示条 */
.alert-status-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 0 2px 2px 0;
  opacity: 0.7;
  transition: opacity 0.25s ease, width 0.25s ease;
}

.alert-item:hover {
  background: rgba(252, 165, 165, 0.06);
  transform: translateX(2px);
}

.alert-item:hover .alert-status-bar {
  opacity: 1;
  width: 4px;
}

.alert-thumb {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.alert-thumb-placeholder {
  width: 38px;
  height: 38px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-level {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
  flex-shrink: 0;
  white-space: nowrap;
}

.alert-info { flex: 1; min-width: 0; }
.alert-name {
  font-size: 13px;
  color: #d4e8dc;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: 0.2px;
}
.alert-meta {
  font-size: 11px;
  color: #5a8a72;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.alert-score {
  font-size: 16px;
  font-weight: 700;
  font-family: 'Consolas', monospace;
  flex-shrink: 0;
}
.no-alert {
  text-align: center;
  padding: 24px;
  color: #5a8a72;
  font-size: 13px;
}

/* ==================== 关键数据统计 ==================== */
.key-stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  padding: 1px;
}
.key-stat-item {
  background: rgba(13, 45, 34, 0.4);
  padding: 16px 12px;
  text-align: center;
}
.key-stat-value {
  font-size: 24px;
  font-weight: 700;
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.3;
}
.key-stat-unit {
  font-size: 12px;
  font-weight: 400;
  margin-left: 2px;
  opacity: 0.7;
}
.key-stat-label {
  font-size: 11px;
  color: #5a8a72;
  margin-top: 4px;
  letter-spacing: 0.3px;
}

/* ==================== 预警标签 ==================== */
.alert-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}
.alert-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 600;
  white-space: nowrap;
  background: rgba(255,255,255,0.06);
  color: #7aac96;
}
.alert-tag.life-tag {
  background: rgba(252, 165, 165, 0.12);
  color: #fca5a5;
}

/* ==================== 详情弹窗 ==================== */
.detail-dialog :deep(.el-dialog) {
  background: linear-gradient(180deg, #0D2B22, #0A1F18);
  border: 1px solid rgba(46, 139, 106, 0.2);
  border-radius: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.2),
    0 20px 40px rgba(0, 0, 0, 0.15);
}
.detail-dialog :deep(.el-dialog__title) {
  color: #d4e8dc;
  font-weight: 600;
  font-size: 16px;
}
.detail-dialog :deep(.el-dialog__body) { color: #b8d4c8; }

.detail-score-ring {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
}
.detail-score-ring svg { width: 100%; height: 100%; }
.detail-score-num {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
  font-weight: 700;
  font-family: 'Consolas', monospace;
}

.detail-barn-image { margin-bottom: 16px; }

.image-placeholder {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 6px;
  color: #5a8a72;
  gap: 8px;
  font-size: 14px;
}

.detail-desc :deep(.el-descriptions__label) { color: #5a8a72; }
.detail-desc :deep(.el-descriptions__content) { color: #b8d4c8; }
.detail-desc :deep(.el-descriptions__cell) { border-color: rgba(46, 139, 106, 0.1); }

/* ==================== 错误提示 ==================== */
.load-error {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  z-index: 10;
}
.error-text { color: #fca5a5; font-size: 15px; }
</style>
