<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBarnOptions } from '@/api/barn'
import {
  getSiteNewBuildAll,
  addSiteNewBuild,
  deleteSiteNewBuild,
  clearSiteNewBuild,
  autoGenerateSiteNewBuild,
  evaluateSiteNewBuild,
  getSiteNewBuildAlgoDesc,
  getVillageLoad,
  getVillageLoadRecommend,
} from '@/api/siteNewBuild'
import { Location, MagicStick, Delete, Refresh } from '@element-plus/icons-vue'

/** 单座烤房设计产能（公斤/季，可调） */
const designCapacity = ref(5000)
/** 村负载均衡推演数据 */
const loadData = ref<any>(null)
const loadLoading = ref(false)
const loadPanelOpen = ref(true)
const recommend = ref<any[]>([])
const addLoading = ref(false)
const mapFilter = ref({ province: '湖北省', city: '宜昌市', county: '秭归县', township: '' })

function withTimeout<T>(promise: Promise<T>, ms: number, message: string): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((_, reject) => setTimeout(() => reject(new Error(message)), ms)),
  ])
}

/** 村负载预警等级颜色（分级预警法） */
const loadLevelColor: Record<string, string> = {
  '严重超负荷': '#D4604A',
  '超负荷': '#E08A3C',
  '饱和': '#E8A714',
  '正常': '#2E8B6A',
  '富余': '#3B9BDD',
  '严重缺烤房': '#B71C1C',
}

/** 加载村负载推演数据（候选点变化时自动刷新） */
async function loadVillageData() {
  loadLoading.value = true
  try {
    const [data, rec] = await withTimeout(
      Promise.all([getVillageLoad(designCapacity.value), getVillageLoadRecommend(designCapacity.value)]),
      12000,
      '负载接口响应超时',
    )
    loadData.value = data
    recommend.value = (rec || []) as any[]
    renderBarnDots()
  } catch (e: any) {
    ElMessage.warning(e?.message || '负载数据刷新失败，候选点已保留')
  } finally {
    loadLoading.value = false
  }
}

async function runSandbox() {
  if (latestCandidate.value) {
    selectedCandidate.value = latestCandidate.value
  }
  await loadVillageData()
  loadPanelOpen.value = true
  if (!selectedCandidate.value && latestCandidate.value) {
    selectedCandidate.value = latestCandidate.value
  }
}

/** 负载率文案 */
function loadRateText(v: number | null | undefined): string {
  if (v == null) return '--'
  return Number(v).toFixed(3)
}
function loadLevelStyle(level: string): Record<string, string> {
  const c = loadLevelColor[level] || '#5FB292'
  return { color: c, background: c + '22' }
}

/** 是否按村负载等级给烤房打点着色（默认开启） */
const useVillageLoadColor = ref(true)
/** 地图图例是否展开（默认收起，避免遮挡地图） */
const legendOpen = ref(false)

/** 地名归一化（与后端一致，用于把烤房村名匹配到负载村） */
function normPlace(s: string): string {
  return String(s || '').replace(/(村委会|居委会|社区居民委员会|村民委员会|街道|社区|村|镇|乡)$/, '').trim()
}

function normTownship(s: string): string {
  return String(s || '').replace(/(街道办事处|街道|镇|乡)$/, '').trim()
}

function shortVillageName(s: string): string {
  const text = String(s || '').trim()
  if (!text) return ''
  const normalized = text
    .replace(/(社区居民委员会|村民委员会|居民委员会|村委会|居委会)$/, '')
    .trim()
  if (/[社区]$/.test(normalized)) return normalized
  if (/[村]$/.test(normalized)) return normalized
  return normalized ? `${normalized}村` : ''
}

function calcDistanceScore(lng1: number, lat1: number, lng2: number, lat2: number): number {
  const dx = (lng1 - lng2) * Math.cos(((lat1 + lat2) / 2) * Math.PI / 180)
  const dy = lat1 - lat2
  return dx * dx + dy * dy
}

/** 村名 -> 村负载等级 映射 */
const villageLoadLookup = computed(() => {
  const m = new Map<string, string>()
  ;(loadData.value?.villages || []).forEach((v: any) => {
    m.set(normPlace(v.village), v.levelAfter || v.levelBefore || '正常')
  })
  return m
})

const loadVillageProfiles = computed(() => {
  return (loadData.value?.villages || []).map((v: any) => ({
    township: String(v.township || ''),
    village: String(v.village || ''),
    townshipNorm: normTownship(v.township),
    villageNorm: normPlace(v.village),
    raw: v,
  }))
})

const loadVillageCenters = computed(() => {
  const profileMap = new Map<string, { township: string; village: string; townshipNorm: string; villageNorm: string }>()
  loadVillageProfiles.value.forEach((item: any) => {
    profileMap.set(`${item.townshipNorm}__${item.villageNorm}`, item)
  })
  const grouped = new Map<string, { township: string; village: string; townshipNorm: string; villageNorm: string; lngSum: number; latSum: number; count: number }>()
  barnList.value.forEach((b: any) => {
    const lng = toFiniteNumber(b.longitude)
    const lat = toFiniteNumber(b.latitude)
    if (lng == null || lat == null || !isValidLngLat(lng, lat) || lng <= 0 || lat <= 0) return
    const townshipNorm = normTownship(b.township)
    const villageNorm = normPlace(b.village)
    const profile = profileMap.get(`${townshipNorm}__${villageNorm}`)
    if (!profile) return
    const key = `${profile.townshipNorm}__${profile.villageNorm}`
    const existing = grouped.get(key) || {
      township: profile.township,
      village: profile.village,
      townshipNorm: profile.townshipNorm,
      villageNorm: profile.villageNorm,
      lngSum: 0,
      latSum: 0,
      count: 0,
    }
    existing.lngSum += lng
    existing.latSum += lat
    existing.count += 1
    grouped.set(key, existing)
  })
  return Array.from(grouped.values()).map(item => ({
    township: item.township,
    village: item.village,
    townshipNorm: item.townshipNorm,
    villageNorm: item.villageNorm,
    lng: item.lngSum / item.count,
    lat: item.latSum / item.count,
    count: item.count,
  }))
})

function resolveLoadVillage(lng: number, lat: number, township?: string, village?: string) {
  const townshipNorm = normTownship(township)
  const villageNorm = normPlace(village)
  const exact = loadVillageProfiles.value.find((item: any) => {
    return item.townshipNorm === townshipNorm && item.villageNorm === villageNorm
  })
  if (exact) {
    return { township: exact.township, village: exact.village, source: 'exact' as const }
  }

  const inTownship = loadVillageCenters.value.filter(item => !townshipNorm || item.townshipNorm === townshipNorm)
  const pool = inTownship.length ? inTownship : loadVillageCenters.value
  if (!pool.length) return null

  let nearest = pool[0]
  let bestScore = calcDistanceScore(lng, lat, pool[0].lng, pool[0].lat)
  pool.slice(1).forEach(item => {
    const score = calcDistanceScore(lng, lat, item.lng, item.lat)
    if (score < bestScore) {
      bestScore = score
      nearest = item
    }
  })
  return { township: nearest.township, village: nearest.village, source: 'nearest_load' as const }
}

watch(useVillageLoadColor, () => { renderBarnDots() })

/** 现有烤房数据 */
const barnList = ref<any[]>([])
const loading = ref(false)
const currentTime = ref('')
let timeTimer: any = null

/** 候选点数据 */
const candidates = ref<any[]>([])
const candidatesLoading = ref(false)

/** 地图引用 */
const mapRef = ref<HTMLDivElement>()
let mapInstance: L.Map | null = null
let barnLayer: L.LayerGroup | null = null
let candidateLayer: L.LayerGroup | null = null
let townshipBoundaryLayer: L.GeoJSON | null = null
let villagePointLayer: L.LayerGroup | null = null
const villagePoints = ref<any[]>([])
const townshipBoundaries = ref<any>(null)
const townshipOptions = computed(() => (townshipBoundaries.value?.features || [])
  .map((f: any) => f.properties?.name)
  .filter(Boolean)
  .sort((a: string, b: string) => a.localeCompare(b, 'zh-Hans-CN')))
const filteredTownshipBoundaries = computed(() => {
  const features = townshipBoundaries.value?.features || []
  return {
    type: 'FeatureCollection',
    features: mapFilter.value.township
      ? features.filter((f: any) => f.properties?.name === mapFilter.value.township)
      : features,
  }
})
const filteredVillagePoints = computed(() => villagePoints.value.filter((f: any) => {
  if (!mapFilter.value.township) return true
  return f.properties?.xiang === mapFilter.value.township
}))
const filteredCandidates = computed(() => candidates.value.filter((c: any) => {
  return !mapFilter.value.township || c.township === mapFilter.value.township
}))
const visibleCandidates = computed(() => filteredCandidates.value)
const filteredBarnList = computed(() => barnList.value.filter((b: any) => {
  return !mapFilter.value.township || b.township === mapFilter.value.township
}))

function toFiniteNumber(value: unknown): number | null {
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function isValidLngLat(lng: unknown, lat: unknown): lng is number {
  const x = toFiniteNumber(lng)
  const y = toFiniteNumber(lat)
  return x != null && y != null && x >= -180 && x <= 180 && y >= -90 && y <= 90
}

/** 新建适宜等级颜色 */
const levelColor: Record<string, string> = {
  '高度适宜': '#2E8B6A',
  '适宜': '#5FB292',
  '一般': '#D4944A',
  '不适宜': '#D4604A',
  '待评估': '#94a3b8',
}

/** 统计卡片 */
const statCards = computed(() => {
  const evaluated = candidates.value.filter(c => c.totalScore != null)
  const high = candidates.value.filter(c => c.suitabilityLevel === '高度适宜').length
  const good = candidates.value.filter(c => c.suitabilityLevel === '适宜').length
  const normal = candidates.value.filter(c => c.suitabilityLevel === '一般').length
  const bad = candidates.value.filter(c => c.suitabilityLevel === '不适宜').length
  const avg = evaluated.length
    ? (evaluated.reduce((s, c) => s + Number(c.totalScore), 0) / evaluated.length).toFixed(1)
    : '0'
  return [
    { label: '候选点', value: candidates.value.length, color: '#5FB292' },
    { label: '高度适宜', value: high, color: '#2E8B6A' },
    { label: '适宜', value: good, color: '#5FB292' },
    { label: '一般', value: normal, color: '#D4944A' },
    { label: '不适宜', value: bad, color: '#D4604A' },
    { label: '平均分', value: avg, color: '#7DC4A8' },
  ]
})

/** 数字滚动动画 */
const animatedStats = ref<Record<string, number | string>>({})
watch(statCards, (newCards) => {
  newCards.forEach(card => {
    if (typeof card.value === 'number') {
      const from = Number(animatedStats.value[card.label] ?? 0)
      const to = card.value
      const duration = 500
      const startTime = performance.now()
      const animate = (now: number) => {
        const progress = Math.min((now - startTime) / duration, 1)
        const eased = 1 - Math.pow(1 - progress, 3)
        animatedStats.value[card.label] = Math.round(from + (to - from) * eased)
        if (progress < 1) requestAnimationFrame(animate)
      }
      requestAnimationFrame(animate)
    } else {
      animatedStats.value[card.label] = card.value
    }
  })
}, { immediate: true })

/** 按乡镇分组统计 */
const townGroups = computed(() => {
  const map = new Map<string, { name: string; total: number; villages: string[] }>()
  filteredCandidates.value.forEach(c => {
    const key = c.township || '未知'
    if (!map.has(key)) map.set(key, { name: key, total: 0, villages: [] })
    const g = map.get(key)!
    g.total++
    const village = String(c.village || '').trim()
    if (village && !g.villages.includes(village)) g.villages.push(village)
  })
  return Array.from(map.values()).sort((a, b) => b.total - a.total)
})

/** 选中候选点详情 */
const selectedCandidate = ref<any | null>(null)
const detailVisible = ref(false)
const latestCandidate = computed(() => {
  if (!candidates.value.length) return null
  return [...candidates.value].sort((a: any, b: any) => Number(b.id || 0) - Number(a.id || 0))[0] || null
})
const selectedCandidateResolved = computed(() => {
  if (!selectedCandidate.value) return null
  const lng = toFiniteNumber(selectedCandidate.value.longitude)
  const lat = toFiniteNumber(selectedCandidate.value.latitude)
  if (lng == null || lat == null || !isValidLngLat(lng, lat)) return null
  return resolveLoadVillage(lng, lat, selectedCandidate.value.township, selectedCandidate.value.village)
})
const selectedVillageImpact = computed(() => {
  if (!selectedCandidate.value || !loadData.value?.villages?.length) return null
  const matchedTownship = selectedCandidateResolved.value?.township || selectedCandidate.value.township
  const matchedVillage = selectedCandidateResolved.value?.village || selectedCandidate.value.village
  const villages = loadData.value.villages as any[]
  const exact = villages.find((v: any) => {
    return normTownship(v.township) === normTownship(matchedTownship)
      && normPlace(v.village) === normPlace(matchedVillage)
  })
  if (exact) return exact

  const sameTownship = villages
    .filter((v: any) => normTownship(v.township) === normTownship(matchedTownship))
    .sort((a: any, b: any) => Number(b.addedCount ?? 0) - Number(a.addedCount ?? 0)
      || Number(b.loadRateAfter ?? 0) - Number(a.loadRateAfter ?? 0))
  if (sameTownship.length) return sameTownship[0]

  const sameVillage = villages
    .filter((v: any) => normPlace(v.village) === normPlace(matchedVillage))
    .sort((a: any, b: any) => Number(b.addedCount ?? 0) - Number(a.addedCount ?? 0)
      || Number(b.loadRateAfter ?? 0) - Number(a.loadRateAfter ?? 0))
  if (sameVillage.length) return sameVillage[0]

  return [...villages]
    .sort((a: any, b: any) => Number(b.addedCount ?? 0) - Number(a.addedCount ?? 0)
      || Number(b.loadRateAfter ?? 0) - Number(a.loadRateAfter ?? 0))[0] || null
})
const selectedVillageImpactMatched = computed(() => {
  if (!selectedCandidate.value || !loadData.value?.villages?.length) return null
  const matchedTownship = selectedCandidateResolved.value?.township || selectedCandidate.value.township
  const matchedVillage = selectedCandidateResolved.value?.village || selectedCandidate.value.village
  return loadData.value.villages.find((v: any) => {
    return normTownship(v.township) === normTownship(matchedTownship)
      && normPlace(v.village) === normPlace(matchedVillage)
  }) || null
})
const selectedVillageImpactHint = computed(() => {
  if (!selectedCandidate.value) return ''
  if (!loadData.value?.villages?.length) return '负载数据尚未加载'
  if (selectedVillageImpactMatched.value) return ''
  if (selectedCandidateResolved.value?.source === 'nearest_load' && selectedCandidateResolved.value.township && selectedCandidateResolved.value.village) {
    return `当前村暂无直接负载数据，已按就近村“${selectedCandidateResolved.value.township} · ${selectedCandidateResolved.value.village}”展示`
  }
  if (selectedTownshipVillageRefs.value.length) {
    const row = selectedVillageImpact.value
    return row ? `当前村暂无直接负载数据，已按同乡镇可参考村“${row.township} · ${row.village}”展示` : '当前村暂无直接负载数据，已按同乡镇可参考村展示'
  }
  return '当前村暂无直接负载数据，已按全县可参考村展示'
})
const selectedTownshipVillageRefs = computed(() => {
  if (!selectedCandidate.value || !loadData.value?.villages?.length) return []
  const matchedTownship = selectedCandidateResolved.value?.township || selectedCandidate.value.township
  return loadData.value.villages.filter((v: any) => {
    return normTownship(v.township) === normTownship(matchedTownship)
  }).sort((a: any, b: any) => Number(b.loadRateAfter ?? 0) - Number(a.loadRateAfter ?? 0)).slice(0, 5)
})
const impactedVillages = computed(() => {
  const villages = (loadData.value?.villages || []).filter((v: any) => Number(v.addedCount ?? 0) > 0)
  return villages.sort((a: any, b: any) => {
    const aSelected = selectedVillageImpact.value && normPlace(a.village) === normPlace(selectedVillageImpact.value.village) ? 1 : 0
    const bSelected = selectedVillageImpact.value && normPlace(b.village) === normPlace(selectedVillageImpact.value.village) ? 1 : 0
    if (aSelected !== bSelected) return bSelected - aSelected
    if (Number(a.addedCount ?? 0) !== Number(b.addedCount ?? 0)) return Number(b.addedCount ?? 0) - Number(a.addedCount ?? 0)
    return Number(b.loadRateAfter ?? 0) - Number(a.loadRateAfter ?? 0)
  })
})
const visibleVillageRows = computed(() => {
  const villages = (loadData.value?.villages || []).filter((v: any) => {
    return Number(v.purchaseKg ?? 0) > 0 || Number(v.inUseBarns ?? 0) > 0 || Number(v.addedCount ?? 0) > 0
  })
  return villages.sort((a: any, b: any) => {
    const aChanged = Number(a.addedCount ?? 0) > 0 ? 1 : 0
    const bChanged = Number(b.addedCount ?? 0) > 0 ? 1 : 0
    if (aChanged !== bChanged) return bChanged - aChanged
    if (Number(a.loadRateAfter ?? 0) !== Number(b.loadRateAfter ?? 0)) return Number(b.loadRateAfter ?? 0) - Number(a.loadRateAfter ?? 0)
    return String(a.township || '').localeCompare(String(b.township || ''), 'zh-Hans-CN')
      || String(a.village || '').localeCompare(String(b.village || ''), 'zh-Hans-CN')
  })
})

/** 新增候选点弹窗 */
const addVisible = ref(false)
const addForm = ref({
  candidateName: '',
  count: 1,
  township: '',
  village: '',
  longitude: 0,
  latitude: 0,
  altitude: null as number | null,
  areaSqm: null as number | null,
  slopeDegree: null as number | null,
  distanceRoad: null as number | null,
  distancePower: null as number | null,
  distanceWater: null as number | null,
  tobaccoArea: null as number | null,
  landType: '',
  remark: '',
})
const mapClickPos = ref<{ lng: number; lat: number } | null>(null)

/** 自动生成参数 */
const genVisible = ref(false)
const genForm = ref({
  step: 0.03,
  maxDistKm: 5,
})

/** 算法描述 */
const algoDesc = ref<any>(null)
const algoPanelOpen = ref<string[]>([])

/** 乡镇选项 */
const townOptions = computed(() => {
  const set = new Set<string>()
  barnList.value.forEach((b: any) => { if (b.township) set.add(b.township) })
  candidates.value.forEach((c: any) => { if (c.township) set.add(c.township) })
  return Array.from(set)
})

const villageOptions = computed(() => {
  const map = new Map<string, { township: string; village: string }>()
  filteredBarnList.value.forEach((b: any) => {
    if (!b.village) return
    const key = `${b.township || ''}__${b.village}`
    if (!map.has(key)) map.set(key, { township: b.township || '', village: b.village })
  })
  return Array.from(map.values()).sort((a, b) => a.township.localeCompare(b.township, 'zh-Hans-CN') || a.village.localeCompare(b.village, 'zh-Hans-CN'))
})

/** 创建现有烤房图标（小圆点） */
function createBarnDot(color: string): L.DivIcon {
  return L.divIcon({
    className: 'barn-dot-marker',
    html: `<div class="barn-dot" style="background:${color};"></div>`,
    iconSize: [8, 8],
    iconAnchor: [4, 4],
  })
}

/** 创建候选点图标 */
function createCandidateIcon(level: string, score: number | null): L.DivIcon {
  const color = '#D4604A'
  const size = score != null ? 18 + (score / 100) * 8 : 18
  return L.divIcon({
    className: 'candidate-marker candidate-marker--new',
    html: `<svg width="${size * 1.55}" height="${size * 2.05}" viewBox="0 0 24 32" style="filter: drop-shadow(0 4px 10px rgba(212,96,74,0.65));">
      <path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0zm0 16c-2.2 0-4-1.8-4-4s1.8-4 4-4 4 1.8 4 4-1.8 4-4 4z" 
        fill="${color}" stroke="rgba(255,255,255,0.95)" stroke-width="2"/>
      <circle cx="12" cy="12" r="3" fill="rgba(255,255,255,0.95)"/>
    </svg>`,
    iconSize: [size * 1.55, size * 2.05],
    iconAnchor: [size * 0.78, size * 2.05],
    popupAnchor: [0, -size * 2.05],
  })
}

function safeAddMarker(factory: () => L.Layer, context: string, payload: Record<string, unknown>) {
  try {
    return factory()
  } catch (error) {
    console.warn(`[site-new-build] skip invalid map point: ${context}`, payload, error)
    return null
  }
}

/** 渲染地图 */
function renderMap() {
  if (!mapRef.value) return
  if (!mapInstance) {
    mapInstance = L.map(mapRef.value, {
      center: [30.83, 110.68],
      zoom: 10,
      zoomControl: true,
      attributionControl: false,
    })
    L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      subdomains: ['1', '2', '3', '4'],
      maxZoom: 18,
      minZoom: 3,
    }).addTo(mapInstance)

    barnLayer = L.layerGroup().addTo(mapInstance)
    candidateLayer = L.layerGroup().addTo(mapInstance)
    villagePointLayer = L.layerGroup().addTo(mapInstance)

    // 点击地图添加候选点
    mapInstance.on('click', (e: any) => {
      mapClickPos.value = { lng: e.latlng.lng, lat: e.latlng.lat }
      addForm.value.longitude = Number(e.latlng.lng.toFixed(6))
      addForm.value.latitude = Number(e.latlng.lat.toFixed(6))
      const rawVillage = nearestVillage(e.latlng.lng, e.latlng.lat)
      const alignedVillage = resolveLoadVillage(
        e.latlng.lng,
        e.latlng.lat,
        rawVillage?.xiang || '',
        rawVillage?.cun || '',
      )
      if (alignedVillage) {
        addForm.value.township = alignedVillage.township || ''
        addForm.value.village = alignedVillage.village || ''
        addForm.value.remark = alignedVillage.source === 'exact'
          ? `按负载测算口径自动归属：${alignedVillage.township}-${alignedVillage.village}`
          : `按地图点位就近对齐负载测算村：${alignedVillage.township}-${alignedVillage.village}`
      } else if (rawVillage) {
        addForm.value.township = String(rawVillage.xiang || '').trim()
        addForm.value.village = shortVillageName(rawVillage.cun || '')
        addForm.value.remark = `按地图点位简化归属：${addForm.value.township}-${addForm.value.village || shortVillageName(rawVillage.fullname || '')}`
      }
      addVisible.value = true
    })
  }

  barnLayer?.clearLayers()
  candidateLayer?.clearLayers()
  renderLocalBoundaries()

  // 现有烤房坐标（用于自动适配视图）
  const barnLatlngs: [number, number][] = []
  filteredBarnList.value.forEach((b: any) => {
    const lng = toFiniteNumber(b.longitude)
    const lat = toFiniteNumber(b.latitude)
    if (lng == null || lat == null || !isValidLngLat(lng, lat) || lng <= 0 || lat <= 0) return
    barnLatlngs.push([lat, lng])
  })
  renderBarnDots()

  // 候选点
  const candLatlngs: [number, number][] = []
  filteredCandidates.value.forEach((c: any) => {
    const lng = toFiniteNumber(c.longitude)
    const lat = toFiniteNumber(c.latitude)
    if (lng == null || lat == null || !isValidLngLat(lng, lat) || lng <= 0 || lat <= 0) return
    candLatlngs.push([lat, lng])
    const level = c.suitabilityLevel || '待评估'
    const score = c.totalScore != null ? Number(c.totalScore) : null
    const marker = safeAddMarker(() => L.marker([lat, lng], {
      icon: createCandidateIcon(level, score),
    }), 'candidate', { id: c.id, candidateName: c.candidateName, lng, lat })
    if (!marker) return
    marker.bindPopup(`
      <div style="min-width:200px;font-family:sans-serif;">
        <div style="font-weight:700;font-size:14px;margin-bottom:6px;color:#1a3d30;">${c.candidateName || '候选点'}</div>
        <div style="margin-bottom:3px;font-size:12px;color:#555;">乡镇：${c.township || '-'} · ${c.village || '-'}</div>
        <div style="margin-bottom:3px;font-size:12px;color:#555;">适宜等级：<span style="color:${levelColor[level]};font-weight:600;">${level}</span></div>
        <div style="font-size:12px;color:#555;">综合得分：<b style="color:${levelColor[level]};font-size:14px;">${score != null ? score : '-'}</b></div>
        <button type="button" class="map-candidate-delete" data-candidate-id="${c.id}">删除</button>
      </div>
    `, { maxWidth: 260 })
    marker.on('mouseover', () => {
      selectedCandidate.value = c
      marker.openPopup()
    })
    marker.on('popupopen', (event: any) => {
      const button = event.popup.getElement()?.querySelector('.map-candidate-delete') as HTMLButtonElement | null
      button?.addEventListener('click', () => removeCandidate(c.id))
    })
    marker.on('click', () => {
      selectedCandidate.value = c
      detailVisible.value = true
    })
    candidateLayer?.addLayer(marker)
  })

  // 自动适配视图
  const all = [...barnLatlngs, ...candLatlngs]
  if (all.length) {
    mapInstance.fitBounds(all, { padding: [50, 50], maxZoom: 13 })
  }
  setTimeout(() => { mapInstance?.invalidateSize() }, 200)
}

/** 页面切换或异步布局完成后，确保 Leaflet 获取到真实容器尺寸。 */
function refreshMapLayout() {
  nextTick(() => {
    if (!mapInstance) {
      renderMap()
      return
    }
    mapInstance.invalidateSize({ pan: false })
    renderMap()
  })
}

/** 渲染现有烤房打点（默认按村负载等级着色） */
function renderBarnDots() {
  barnLayer?.clearLayers()
  const lookup = villageLoadLookup.value
  filteredBarnList.value.forEach((b: any) => {
    const lng = toFiniteNumber(b.longitude)
    const lat = toFiniteNumber(b.latitude)
    if (lng == null || lat == null || !isValidLngLat(lng, lat) || lng <= 0 || lat <= 0) return
    let color: string
    if (useVillageLoadColor.value) {
      const lv = lookup.get(normPlace(b.village))
      color = lv ? (loadLevelColor[lv] || '#94a3b8') : '#b5c8bd'
    } else {
      color = b.healthLevel ? ({ '优良': '#2E8B6A', '良好': '#5FB292', '一般': '#D4944A', '预警': '#D4604A', '危险': '#B71C1C' } as any)[b.healthLevel] || '#5a8a72' : '#5a8a72'
    }
    const marker = safeAddMarker(
      () => L.marker([lat, lng], { icon: createBarnDot(color), interactive: false }),
      'barn',
      { id: b.id, barnName: b.barnName, lng, lat },
    )
    if (!marker) return
    barnLayer?.addLayer(marker)
  })
}

function handleResize() { mapInstance?.invalidateSize() }

function updateTime() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  currentTime.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 加载数据 */
async function loadAll() {
  loading.value = true
  try {
    const barns = await getBarnOptions().catch(() => [])
    barnList.value = (barns || []) as any[]
    await loadCandidates()
    refreshMapLayout()
  } finally {
    loading.value = false
  }
}

async function loadCandidates() {
  candidatesLoading.value = true
  try {
    const list = await withTimeout(getSiteNewBuildAll(), 12000, '候选点接口响应超时').catch(() => [])
    candidates.value = (list || []) as any[]
  } finally {
    candidatesLoading.value = false
  }
}

function randomPick<T>(list: T[]): T | null {
  if (!list.length) return null
  return list[Math.floor(Math.random() * list.length)] || null
}

function randomAround(center: { lng: number; lat: number }, radius = 0.015) {
  const angle = Math.random() * Math.PI * 2
  const r = Math.random() * radius
  return {
    lng: Number((center.lng + Math.cos(angle) * r).toFixed(6)),
    lat: Number((center.lat + Math.sin(angle) * r).toFixed(6)),
  }
}

function nearestVillage(lng: number, lat: number): any | null {
  let nearest: any = null
  let bestDistance = Number.POSITIVE_INFINITY
  filteredVillagePoints.value.forEach((feature: any) => {
    const coords = feature.geometry?.coordinates
    if (!Array.isArray(coords) || coords.length < 2) return
    const pointLng = toFiniteNumber(coords[0])
    const pointLat = toFiniteNumber(coords[1])
    if (pointLng == null || pointLat == null || !isValidLngLat(pointLng, pointLat)) return
    const dx = (pointLng - lng) * Math.cos(lat * Math.PI / 180)
    const dy = pointLat - lat
    const distance = dx * dx + dy * dy
    if (distance < bestDistance) {
      bestDistance = distance
      nearest = feature.properties
    }
  })
  return nearest
}

async function loadLocalBoundaries() {
  try {
    const [townships, villages] = await Promise.all([
      fetch('/map/zigui-township-boundaries.geojson').then(res => res.json()),
      fetch('/map/zigui-village-points.geojson').then(res => res.json()),
    ])
    townshipBoundaries.value = townships
    villagePoints.value = villages.features || []
    refreshMapLayout()
  } catch (e) {
    ElMessage.warning('村级点位数据加载失败，地图仍可使用')
  }
}

function renderLocalBoundaries() {
  if (!mapInstance || !townshipBoundaries.value) return
  townshipBoundaryLayer?.remove()
  villagePointLayer?.clearLayers()
  townshipBoundaryLayer = L.geoJSON(filteredTownshipBoundaries.value as any, {
    style: { color: '#2E8B6A', weight: 2, opacity: 0.9, fillColor: '#5FB292', fillOpacity: 0.08 },
    onEachFeature: (feature, layer) => {
      layer.bindTooltip(feature.properties?.name || '乡镇边界', { sticky: true })
    },
  }).addTo(mapInstance)
  filteredVillagePoints.value.forEach((feature: any) => {
    const coords = feature.geometry?.coordinates
    if (!Array.isArray(coords) || coords.length < 2) return
    const pointLng = toFiniteNumber(coords[0])
    const pointLat = toFiniteNumber(coords[1])
    if (pointLng == null || pointLat == null || !isValidLngLat(pointLng, pointLat)) return
    const props = feature.properties || {}
    const marker = safeAddMarker(() => L.circleMarker([pointLat, pointLng], {
      radius: 3, color: '#2E8B6A', weight: 1, fillColor: '#fff', fillOpacity: 0.9,
    }), 'village-point', { village: props.cun, township: props.xiang, lng: pointLng, lat: pointLat })
    if (!marker) return
    marker.bindTooltip(props.cun || props.fullname || '村', { direction: 'top', offset: [0, -3] })
    villagePointLayer?.addLayer(marker)
  })
}

/** 新增候选点 */
async function submitAdd() {
  if (!addForm.value.longitude || !addForm.value.latitude) {
    ElMessage.warning('请在地图上点击选择位置')
    return
  }
  addLoading.value = true
  try {
    const count = Math.max(1, Number(addForm.value.count || 1))
    if (count === 1) {
      await withTimeout(addSiteNewBuild({ ...addForm.value }), 15000, '新增接口响应超时')
    } else {
      const seed = { lng: addForm.value.longitude, lat: addForm.value.latitude }
      const tasks = Array.from({ length: count }, (_, idx) => {
        const point = idx === 0 ? seed : randomAround(seed, 0.004 + Math.random() * 0.008)
        return addSiteNewBuild({
          ...addForm.value,
          candidateName: `${addForm.value.candidateName || addForm.value.village || '新增点'}-${idx + 1}`,
          longitude: point.lng,
          latitude: point.lat,
        })
      })
      await withTimeout(Promise.all(tasks), 15000, '批量新增接口响应超时')
    }
    ElMessage.success(`候选点添加成功${count > 1 ? `，已新增 ${count} 个点` : ''}`)
    addVisible.value = false
    resetAddForm()
    await loadCandidates()
    selectedCandidate.value = latestCandidate.value
    await runSandbox()
    renderMap()
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  } finally {
    addLoading.value = false
  }
}

function resetAddForm() {
  addForm.value = {
    candidateName: '', count: 1, township: '', village: '', longitude: 0, latitude: 0,
    altitude: null, areaSqm: null, slopeDegree: null,
    distanceRoad: null, distancePower: null, distanceWater: null,
    tobaccoArea: null, landType: '', remark: '',
  }
}

/** 删除候选点 */
async function removeCandidate(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该候选点吗？', '删除确认', { type: 'warning' })
    await deleteSiteNewBuild(id)
    ElMessage.success('删除成功')
    await loadCandidates()
    if (selectedCandidate.value?.id === id) {
      selectedCandidate.value = latestCandidate.value
    }
    await runSandbox()
    renderMap()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '删除失败')
  }
}

/** 清空所有候选点 */
async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清空所有候选点吗？此操作不可恢复！', '清空确认', { type: 'warning' })
    await clearSiteNewBuild()
    ElMessage.success('已清空')
    await loadCandidates()
    await loadVillageData()
    renderMap()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '清空失败')
  }
}

/** 自动生成网格候选点 */
async function submitGenerate() {
  try {
    const res = await autoGenerateSiteNewBuild({ ...genForm.value })
    ElMessage.success(res?.msg || '生成完成')
    genVisible.value = false
    await loadCandidates()
    await loadVillageData()
    renderMap()
  } catch (e: any) {
    ElMessage.error(e?.message || '生成失败')
  }
}

/** 运行候选点评估 */
async function runEvaluate() {
  try {
    await ElMessageBox.confirm('将对全部候选点执行综合评价并刷新排序结果，确定继续？', '候选点评估', { type: 'info' })
    const res = await evaluateSiteNewBuild({})
    ElMessage.success(res?.msg || '候选点评估完成，排序与得分已更新')
    await loadCandidates()
    await runSandbox()
    renderMap()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '候选点评估失败，请稍后重试')
  }
}

/** 得分颜色 */
function scoreColor(score: number | null): string {
  if (score == null) return '#94a3b8'
  if (score >= 85) return '#2E8B6A'
  if (score >= 70) return '#5FB292'
  if (score >= 55) return '#D4944A'
  return '#D4604A'
}

function villageRowClass(v: any) {
  if (!loadData.value) return ''
  const after = Number(v.loadRateAfter ?? 0)
  const county = Number(loadData.value.countyBaselineAfter ?? 0)
  const changed = Number(v.addedCount ?? 0) > 0
  return `${after > county ? 'row-overload' : 'row-normal'}${changed ? ' row-added' : ''}`
}

watch(() => mapFilter.value.township, () => {
  refreshMapLayout()
})

onMounted(() => {
  // 先初始化地图，避免等待接口返回期间容器没有 Leaflet 实例。
  nextTick(() => renderMap())
  loadLocalBoundaries()
  loadAll()
  loadVillageData()
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  window.addEventListener('resize', handleResize)
})

/** 候选点变化时自动刷新村负载推演 */
watch(candidates, () => {
  if (!candidates.value.length) {
    selectedCandidate.value = null
  } else if (selectedCandidate.value?.id) {
    selectedCandidate.value = candidates.value.find((item: any) => item.id === selectedCandidate.value.id) || latestCandidate.value
  } else if (latestCandidate.value) {
    selectedCandidate.value = latestCandidate.value
  }
  loadVillageData()
  refreshMapLayout()
})

onBeforeUnmount(() => {
  if (timeTimer) clearInterval(timeTimer)
  window.removeEventListener('resize', handleResize)
  mapInstance?.remove()
  mapInstance = null
  townshipBoundaryLayer = null
  villagePointLayer = null
})
</script>

<template>
  <div class="site-page" v-loading="loading">
    <!-- 顶部 -->
    <header class="site-header">
      <div class="header-deco-line"></div>
      <div class="header-deco-line header-deco-line--right"></div>
      <div class="header-left">
        <div class="header-logo">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
            <path d="M12 2 L14 2 L14 6 L18 6 L18 8 L14 8 L14 18 L16 18 L16 20 L8 20 L8 18 L10 18 L10 8 L6 8 L6 6 L10 6 L10 2 Z" fill="rgba(255,255,255,0.85)" />
            <circle cx="12" cy="13" r="2.5" fill="#2E8B6A" />
          </svg>
        </div>
        <div>
          <h1 class="header-title">烤房新建</h1>
          <div class="header-sub">BARN NEW BUILD</div>
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
      </div>
    </header>

    <!-- 主体 -->
    <div class="site-body">
      <!-- 地图区域 -->
      <div class="map-panel">
        <div class="map-header">
          <span class="dot"></span>
          <span>烤房新建点沙盘演练</span>
          <span class="map-info">
            现有烤房 {{ barnList.length }} 座 · 候选点 {{ candidates.length }} 个
          </span>
        </div>

        <!-- 操作工具栏 -->
        <div class="toolbar">
          <el-button size="small" type="primary" :icon="MagicStick" @click="runEvaluate">候选点评估</el-button>
          <el-button size="small" :icon="Refresh" @click="runSandbox">沙盘推演</el-button>
          <div class="map-filter-group" aria-label="地图筛选范围">
            <span class="map-filter-label">范围</span>
            <el-select v-model="mapFilter.province" size="small" class="map-filter-select" placeholder="省" disabled>
              <el-option label="湖北省" value="湖北省" />
            </el-select>
            <el-select v-model="mapFilter.city" size="small" class="map-filter-select" placeholder="市" disabled>
              <el-option label="宜昌市" value="宜昌市" />
            </el-select>
            <el-select v-model="mapFilter.county" size="small" class="map-filter-select" placeholder="县" disabled>
              <el-option label="秭归县" value="秭归县" />
            </el-select>
            <el-select v-model="mapFilter.township" size="small" class="map-filter-select" clearable placeholder="乡镇">
              <el-option v-for="town in townshipOptions" :key="town" :label="town" :value="town" />
            </el-select>
          </div>
          <label class="load-toggle-map">
            <el-switch v-model="useVillageLoadColor" size="small" /> 按村负载着色
          </label>
          <span class="toolbar-tip">提示：点击地图任意位置可添加候选点</span>
          <el-button size="small" class="clear-btn" :icon="Delete" @click="clearAll">清空</el-button>
        </div>

        <div class="map-chart-wrap">
          <div ref="mapRef" class="map-chart"></div>
        </div>

        <!-- 图例（默认收起，点击展开） -->
        <div class="map-legend">
          <div class="legend-toggle" @click="legendOpen = !legendOpen">
            <span class="legend-toggle-dot"></span>
            <span class="legend-toggle-text">图例</span>
            <span class="legend-toggle-arrow">{{ legendOpen ? '▾' : '▸' }}</span>
          </div>
          <div v-show="legendOpen" class="legend-body">
            <div class="legend-group">
              <span class="legend-title">新建适宜</span>
              <div class="legend-row">
                <div v-for="(color, level) in levelColor" :key="level" class="legend-item">
                  <svg viewBox="0 0 24 32" width="9" height="12"><path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0z" :fill="color" /></svg>{{ level }}
                </div>
              </div>
            </div>
            <div class="legend-group">
              <span class="legend-title">村负载</span>
              <div class="legend-row">
                <div v-for="(color, level) in loadLevelColor" :key="level" class="legend-item">
                  <span class="legend-load-dot" :style="{ background: color }"></span>{{ level }}
                </div>
              </div>
            </div>
            <div class="legend-group">
              <span class="legend-title">其他</span>
              <div class="legend-row">
                <div class="legend-item"><span class="legend-barn-dot"></span>现有烤房</div>
                <div class="legend-item"><span class="legend-cand"></span>候选点</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="side-panel">
        <div class="side-section side-section--impact">
          <div class="section-head">
            <span class="dot warn"></span>当前候选点影响村
          </div>
          <div v-if="selectedCandidate && selectedVillageImpact" class="impact-card">
            <div class="impact-candidate">{{ selectedCandidate.candidateName || '候选点' }}</div>
            <div class="impact-meta">{{ selectedCandidateResolved?.township || selectedCandidate.township || '-' }} · {{ selectedCandidateResolved?.village || selectedCandidate.village || '-' }}</div>
            <div v-if="selectedVillageImpactHint" class="impact-hint">{{ selectedVillageImpactHint }}</div>
            <div class="impact-county">
              <span>秭归县平均负载率</span>
              <b>{{ loadData?.countyBaselineAfter ?? '--' }}</b>
            </div>
            <div class="impact-grid">
              <div class="impact-item">
                <span>村级前负载率</span>
                <b>{{ loadRateText(selectedVillageImpact.loadRateBefore) }}</b>
              </div>
              <div class="impact-item">
                <span>村级后负载率</span>
                <b :class="{ 'impact-over': Number(selectedVillageImpact.loadRateAfter ?? 0) > Number(loadData?.countyBaselineAfter ?? 0) }">{{ loadRateText(selectedVillageImpact.loadRateAfter) }}</b>
              </div>
              <div class="impact-item">
                <span>村级前等级</span>
                <em :style="loadLevelStyle(selectedVillageImpact.levelBefore)">{{ selectedVillageImpact.levelBefore || '--' }}</em>
              </div>
              <div class="impact-item">
                <span>村级后等级</span>
                <em :style="loadLevelStyle(selectedVillageImpact.levelAfter)">{{ selectedVillageImpact.levelAfter || '--' }}</em>
              </div>
              <div class="impact-item">
                <span>本村新增数量</span>
                <b>+{{ Number(selectedVillageImpact.addedCount ?? 0) }}</b>
              </div>
              <div class="impact-item">
                <span>对比结果</span>
                <b :class="{ 'impact-over': Number(selectedVillageImpact.loadRateAfter ?? 0) > Number(loadData?.countyBaselineAfter ?? 0) }">
                  {{ Number(selectedVillageImpact.loadRateAfter ?? 0) > Number(loadData?.countyBaselineAfter ?? 0) ? '高于县均' : '不高于县均' }}
                </b>
              </div>
            </div>
            <div class="impact-list" v-if="impactedVillages.length">
              <div class="impact-list-title">本轮沙盘推演已变化村</div>
              <div v-for="row in impactedVillages" :key="`${row.township}-${row.village}`" class="impact-list-row">
                <span>{{ row.township }} · {{ row.village }}</span>
                <b>+{{ Number(row.addedCount ?? 0) }} / {{ loadRateText(row.loadRateAfter) }}</b>
              </div>
            </div>
          </div>
          <div v-else-if="selectedCandidate" class="impact-card">
            <div class="impact-candidate">{{ selectedCandidate.candidateName || '候选点' }}</div>
            <div class="impact-meta">{{ selectedCandidateResolved?.township || selectedCandidate.township || '-' }} · {{ selectedCandidateResolved?.village || selectedCandidate.village || '-' }}</div>
            <div class="impact-county">
              <span>秭归县平均负载率</span>
              <b>{{ loadData?.countyBaselineAfter ?? '--' }}</b>
            </div>
            <div class="impact-empty impact-empty--inline">
              当前红点已经联动到右侧，但该村暂无可匹配的负载测算数据。
              常见原因是：该村不在当前种烟负载样本内，或村名与负载明细口径不一致。
            </div>
            <div v-if="selectedTownshipVillageRefs.length" class="impact-list">
              <div class="impact-list-title">同乡镇可参考村负载</div>
              <div v-for="row in selectedTownshipVillageRefs" :key="`${row.township}-${row.village}`" class="impact-list-row">
                <span>{{ row.village }}</span>
                <b>{{ loadRateText(row.loadRateAfter) }} / {{ row.levelAfter || '--' }}</b>
              </div>
            </div>
          </div>
          <div v-else class="impact-empty">点击“沙盘推演”后，这里会自动显示新增候选点所在村的前后负载变化、等级变化，以及和秭归县平均值的对比结果。</div>
        </div>

        <!-- 乡镇分布 -->
        <div class="side-section">
          <div class="section-head"><span class="dot"></span>乡镇候选点分布</div>
          <div class="town-list">
            <div v-for="g in townGroups" :key="g.name" class="town-item">
              <div class="town-name">{{ g.name }}</div>
              <div class="town-villages">{{ g.villages.join('、') || '暂无村名' }}</div>
              <div class="town-bar-wrap">
                <div class="town-bar">
                  <div class="town-fill" :style="{ width: (g.total / (townGroups[0]?.total || 1)) * 100 + '%', background: 'linear-gradient(90deg, #0D5D46, #2E8B6A)' }"></div>
                </div>
                <div class="town-count">{{ g.total }}</div>
              </div>
            </div>
            <div v-if="!townGroups.length" class="no-data">暂无候选点</div>
          </div>
        </div>

        <!-- 候选点列表 -->
        <div class="side-section">
          <div class="section-head">
            <span class="dot alert"></span>候选点列表
            <span class="alert-count">{{ visibleCandidates.length }}</span>
          </div>
          <div class="candidate-list" v-loading="candidatesLoading">
            <div v-for="c in visibleCandidates" :key="c.id" class="candidate-item" @click="selectedCandidate = c; detailVisible = true">
              <div class="candidate-status-bar" :style="{ background: levelColor[c.suitabilityLevel || '待评估'] }"></div>
              <div class="candidate-rank" v-if="c.ranking">{{ c.ranking }}</div>
              <div class="candidate-info">
                <div class="candidate-name">{{ c.candidateName || '候选点' }}</div>
                <div class="candidate-meta">{{ c.township || '-' }} · {{ c.village || '-' }}</div>
                <div class="candidate-tags">
                  <span class="candidate-tag candidate-tag--new">新增点</span>
                  <span class="candidate-tag" :style="{ background: (levelColor[c.suitabilityLevel || '待评估'] || '#78909c') + '22', color: levelColor[c.suitabilityLevel || '待评估'] || '#78909c' }">{{ c.suitabilityLevel || '待评估' }}</span>
                  <span v-if="c.totalScore != null" class="candidate-tag" :style="{ color: scoreColor(Number(c.totalScore)) }">得分 {{ Number(c.totalScore).toFixed(1) }}</span>
                  <span v-if="c.topsisScore != null" class="candidate-tag" style="color:#D4944A">TOPSIS {{ Number(c.topsisScore).toFixed(3) }}</span>
                </div>
              </div>
              <el-button size="small" type="danger" plain round :icon="Delete" class="del-btn" @click.stop="removeCandidate(c.id)">删除</el-button>
            </div>
            <div v-if="!visibleCandidates.length" class="no-data">当前范围暂无候选点，点击地图添加或切换乡镇</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增候选点弹窗 -->
    <el-dialog v-model="addVisible" title="新增候选点" width="560px" destroy-on-close class="add-dialog" append-to-body align-center>
      <div class="add-coord-tip">
        <el-icon :size="14" color="#5FB292"><Location /></el-icon>
        已选位置：经度 {{ addForm.longitude }}，纬度 {{ addForm.latitude }}
      </div>
      <div class="gen-desc">地图点击一次即可新增。若数量大于 1，系统会以当前点击位置为中心，在同一村内随机散布多个红色新增点。</div>
      <el-form :model="addForm" label-width="110px" size="small" class="add-form-stack">
        <el-form-item label="候选点名称">
          <el-input v-model="addForm.candidateName" placeholder="留空自动命名" />
        </el-form-item>
        <el-form-item label="新增数量">
          <el-input-number v-model="addForm.count" :min="1" :max="50" style="width:100%" />
        </el-form-item>
        <el-form-item label="所属乡镇">
          <el-select v-model="addForm.township" placeholder="选择乡镇" clearable filterable style="width:100%">
            <el-option v-for="t in townOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属村">
          <el-input v-model="addForm.village" placeholder="所在村名" />
        </el-form-item>
        <el-form-item label="土地类型">
          <el-select v-model="addForm.landType" placeholder="选择土地类型" clearable style="width:100%">
            <el-option label="平地" value="平地" />
            <el-option label="坡地" value="坡地" />
            <el-option label="荒地" value="荒地" />
            <el-option label="林地" value="林地" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="补充说明" />
        </el-form-item>
        <el-collapse class="add-advanced-collapse" accordion>
          <el-collapse-item title="高级参数" name="advanced">
            <div class="add-advanced-grid">
              <el-form-item label="海拔(米)">
                <el-input-number v-model="addForm.altitude" :min="0" :max="3000" style="width:100%" placeholder="如 1100" />
              </el-form-item>
              <el-form-item label="坡度(度)">
                <el-input-number v-model="addForm.slopeDegree" :min="0" :max="60" style="width:100%" placeholder="如 5" />
              </el-form-item>
              <el-form-item label="距公路(米)">
                <el-input-number v-model="addForm.distanceRoad" :min="0" style="width:100%" placeholder="如 200" />
              </el-form-item>
              <el-form-item label="距电力(米)">
                <el-input-number v-model="addForm.distancePower" :min="0" style="width:100%" placeholder="如 300" />
              </el-form-item>
              <el-form-item label="距水源(米)">
                <el-input-number v-model="addForm.distanceWater" :min="0" style="width:100%" placeholder="如 500" />
              </el-form-item>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button size="small" @click="addVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="addLoading" @click="submitAdd">保存候选点</el-button>
      </template>
    </el-dialog>

    <!-- 自动生成弹窗 -->
    <el-dialog v-model="genVisible" title="自动生成网格候选点" width="440px" destroy-on-close append-to-body align-center>
      <div class="gen-desc">
        在秭归县范围（经度110.26~110.99，纬度30.42~31.09）按网格自动生成候选点，
        仅保留距现有烤房一定距离内的点（烟叶种植区域）。
      </div>
      <el-form :model="genForm" label-width="130px" size="small">
        <el-form-item label="网格步长(度)">
          <el-input-number v-model="genForm.step" :min="0.01" :max="0.1" :step="0.01" style="width:100%" />
          <div class="form-tip">约 {{ (genForm.step * 111).toFixed(1) }} 公里，越小越密集</div>
        </el-form-item>
        <el-form-item label="距烤房距离(km)">
          <el-input-number v-model="genForm.maxDistKm" :min="1" :max="10" style="width:100%" />
          <div class="form-tip">仅保留距现有烤房该距离内的点</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="genVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="submitGenerate">开始生成</el-button>
      </template>
    </el-dialog>

    <!-- 候选点详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="selectedCandidate?.candidateName || '候选点详情'" width="520px" destroy-on-close class="detail-dialog" append-to-body align-center>
      <template v-if="selectedCandidate">
        <div class="detail-score-ring">
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="10" />
            <circle cx="60" cy="60" r="50" fill="none"
              :stroke="scoreColor(selectedCandidate.totalScore != null ? Number(selectedCandidate.totalScore) : null)"
              stroke-width="10" stroke-linecap="round"
              :stroke-dasharray="2 * Math.PI * 50"
              :stroke-dashoffset="2 * Math.PI * 50 * (1 - (Number(selectedCandidate.totalScore || 0)) / 100)"
              transform="rotate(-90 60 60)" />
          </svg>
          <div class="detail-score-num" :style="{ color: scoreColor(selectedCandidate.totalScore != null ? Number(selectedCandidate.totalScore) : null) }">
            {{ selectedCandidate.totalScore != null ? Number(selectedCandidate.totalScore).toFixed(1) : '--' }}
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="适宜等级">
            <span :style="{ color: levelColor[selectedCandidate.suitabilityLevel || '待评估'], fontWeight: 600 }">{{ selectedCandidate.suitabilityLevel || '待评估' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ selectedCandidate.status }}</el-descriptions-item>
          <el-descriptions-item label="所属乡镇">{{ selectedCandidate.township || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属村">{{ selectedCandidate.village || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经度">{{ Number(selectedCandidate.longitude).toFixed(6) }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ Number(selectedCandidate.latitude).toFixed(6) }}</el-descriptions-item>
          <el-descriptions-item label="海拔">{{ selectedCandidate.altitude ? selectedCandidate.altitude + ' 米' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="坡度">{{ selectedCandidate.slopeDegree != null ? selectedCandidate.slopeDegree + '°' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="土地类型">{{ selectedCandidate.landType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ selectedCandidate.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 8大指标得分 -->
        <div class="score-breakdown">
          <div class="score-title">8大指标得分 <span class="score-title-sub">（AHP权重：地形15% 水文12% 电力18% 交通10% 烟田20% 环境10% 集群8% 成本7%）</span></div>
          <div class="score-row">
            <span class="score-label">地形地势</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.scoreTerrain || 0)) + '%', background: 'linear-gradient(90deg, #0D5D46, #2E8B6A)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.scoreTerrain != null ? Number(selectedCandidate.scoreTerrain).toFixed(1) : '-' }}</span>
          </div>
          <div class="score-row">
            <span class="score-label">水文条件</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.scoreHydro || 0)) + '%', background: 'linear-gradient(90deg, #0D5D46, #5FB292)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.scoreHydro != null ? Number(selectedCandidate.scoreHydro).toFixed(1) : '-' }}</span>
          </div>
          <div class="score-row">
            <span class="score-label">电力保障</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.scorePower || 0)) + '%', background: 'linear-gradient(90deg, #1A6B4F, #3DA07A)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.scorePower != null ? Number(selectedCandidate.scorePower).toFixed(1) : '-' }}</span>
          </div>
          <div class="score-row">
            <span class="score-label">交通条件</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.scoreTraffic || 0)) + '%', background: 'linear-gradient(90deg, #2E8B6A, #7DC4A8)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.scoreTraffic != null ? Number(selectedCandidate.scoreTraffic).toFixed(1) : '-' }}</span>
          </div>
          <div class="score-row">
            <span class="score-label">烟田匹配</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.scoreTobaccoMatch || 0)) + '%', background: 'linear-gradient(90deg, #3DA07A, #5FB292)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.scoreTobaccoMatch != null ? Number(selectedCandidate.scoreTobaccoMatch).toFixed(1) : '-' }}</span>
          </div>
          <div class="score-row">
            <span class="score-label">环境安全</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.scoreEnvironment || 0)) + '%', background: 'linear-gradient(90deg, #1A6B4F, #7DC4A8)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.scoreEnvironment != null ? Number(selectedCandidate.scoreEnvironment).toFixed(1) : '-' }}</span>
          </div>
          <div class="score-row">
            <span class="score-label">集群效益</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.scoreCluster || 0)) + '%', background: 'linear-gradient(90deg, #0D5D46, #3DA07A)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.scoreCluster != null ? Number(selectedCandidate.scoreCluster).toFixed(1) : '-' }}</span>
          </div>
          <div class="score-row">
            <span class="score-label">成本经济</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.scoreCost || 0)) + '%', background: 'linear-gradient(90deg, #2E8B6A, #A0D4C0)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.scoreCost != null ? Number(selectedCandidate.scoreCost).toFixed(1) : '-' }}</span>
          </div>
          <div class="score-row score-row-total">
            <span class="score-label">TOPSIS贴近度</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: (Number(selectedCandidate.topsisScore || 0) * 100) + '%', background: 'linear-gradient(90deg, #D4944A, #7DC4A8)' }"></div></div>
            <span class="score-val">{{ selectedCandidate.topsisScore != null ? Number(selectedCandidate.topsisScore).toFixed(4) : '-' }}</span>
          </div>
          <div class="score-row score-row-total">
            <span class="score-label">综合排名</span>
            <div class="score-bar"><div class="score-fill" :style="{ width: '100%', background: 'linear-gradient(90deg, #D4944A, #2E8B6A)' }"></div></div>
            <span class="score-val">第 {{ selectedCandidate.ranking || '-' }} 名</span>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 村烤房负载均衡推演（新增功能） -->
    <div class="load-panel" v-loading="loadLoading">
      <!-- 面板头 -->
      <div class="load-head" @click="loadPanelOpen = !loadPanelOpen">
        <div class="load-head-left">
          <span class="dot warn"></span>
          <span class="load-title">村烤房负载均衡推演</span>
          <span class="load-sub">供需缺口指数法 · 分级预警 · 沙盘增删实时计算</span>
        </div>
        <div class="load-head-right">
          <span class="load-cap">
            单座设计产能
            <el-input-number v-model="designCapacity" :min="1000" :max="10000" :step="500" size="small"
              class="cap-input" @change="loadVillageData()" />
            <b>公斤/季</b>
          </span>
          <el-button size="small" :icon="Refresh" @click.stop="loadVillageData(); loadPanelOpen = true">刷新</el-button>
          <span class="load-toggle">{{ loadPanelOpen ? '收起 ▲' : '展开 ▼' }}</span>
        </div>
      </div>

      <!-- 面板内容 -->
      <div v-show="loadPanelOpen" class="load-body">
        <!-- 汇总指标卡 -->
        <div class="load-cards">
          <div class="load-card">
            <div class="lc-label">在用烤房</div>
            <div class="lc-value">{{ loadData?.totalInUseBarns ?? '--' }}</div>
            <div class="lc-sub">新增 <b v-if="loadData">{{ loadData.totalAdded }}</b> 座</div>
          </div>
          <div class="load-card">
            <div class="lc-label">县收购总量(公斤)</div>
            <div class="lc-value">{{ loadData ? Number(loadData.totalPurchaseKg).toLocaleString() : '--' }}</div>
            <div class="lc-sub">涉及 {{ loadData?.totalTobaccoVillages ?? '--' }} 个种烟村</div>
          </div>
          <div class="load-card" v-if="loadData">
            <div class="lc-label">秭归县平均负载率</div>
            <div class="lc-value">{{ loadData.countyBaselineBefore }}
              <span class="lc-arrow" :class="{ better: (loadData.countyBaselineAfter ?? 0) < loadData.countyBaselineBefore }">→ {{ loadData.countyBaselineAfter }}</span>
            </div>
            <div class="lc-sub">推演前后对比</div>
          </div>
          <div class="load-card" v-if="loadData">
            <div class="lc-label">村级平均负载率</div>
            <div class="lc-value">{{ loadData.avgLoadBefore }}
              <span class="lc-arrow" :class="{ better: (loadData.avgLoadAfter ?? 0) < loadData.avgLoadBefore }">→ {{ loadData.avgLoadAfter }}</span>
            </div>
            <div class="lc-sub">村均值</div>
          </div>
          <div class="load-card" v-if="loadData">
            <div class="lc-label">负载均衡指数</div>
            <div class="lc-value lc-good">{{ (loadData.balanceIndexBefore * 100).toFixed(1) }}
              <span class="lc-arrow" :class="{ better: (loadData.balanceIndexAfter ?? 0) > loadData.balanceIndexBefore }">→ {{ (loadData.balanceIndexAfter * 100).toFixed(1) }}%</span>
            </div>
            <div class="lc-sub">越高越均衡 · 标准差 {{ loadData.stddevLoadBefore }}→{{ loadData.stddevLoadAfter }}</div>
          </div>
          <div class="load-card" v-if="loadData">
            <div class="lc-label">超负荷村数(&gt;0.8)</div>
            <div class="lc-value">{{ loadData.overloadVillageCountBefore }}
              <span class="lc-arrow" :class="{ better: (loadData.overloadVillageCountAfter ?? loadData.overloadVillageCountBefore) <= loadData.overloadVillageCountBefore }">→ {{ loadData.overloadVillageCountAfter }}</span>
            </div>
            <div class="lc-sub">富余村不计</div>
          </div>
        </div>

        <!-- 主体：村负载表 + 推荐列表 -->
          <div class="load-main">
          <!-- 推荐新建村 -->
          <div class="rec-panel">
            <div class="load-subhead"><span class="dot"></span>推荐优先新建的村（按优先级指数）</div>
            <div class="rec-list" v-if="recommend.length">
              <div v-for="(r, idx) in recommend" :key="r.village" class="rec-item" :class="{ 'rec-item--hot': Number(r.loadRateAfter ?? 0) > Number(loadData?.countyBaselineAfter ?? 0) }">
                <span class="rec-idx">{{ idx + 1 }}</span>
                <div class="rec-name">{{ r.village }}<span class="rec-town">{{ r.township }}</span></div>
                <div class="rec-load" :style="{ color: loadLevelColor[r.levelBefore] || '#5FB292' }">前 {{ loadRateText(r.loadRateBefore) }}</div>
                <div class="rec-load" :style="{ color: loadLevelColor[r.levelAfter] || '#5FB292' }">后 {{ loadRateText(r.loadRateAfter) }}</div>
                <span class="rec-need">需+{{ Math.max(1, Number(r.needBarns) || 1) }}座</span>
              </div>
            </div>
            <div v-else class="rec-empty">暂无超高负载村，全县负载较均衡 ✓</div>
          </div>

          <!-- 村负载表 -->
          <div class="vl-table-wrap">
            <div class="load-subhead"><span class="dot"></span>分村负载明细（增删候选点后实时更新）</div>
            <table class="vl-table">
              <thead>
                <tr>
                  <th>乡镇</th><th>村</th><th>在用</th><th>新增</th><th>后用</th>
                  <th>收购量(公斤)</th><th>负载率前</th><th>等级</th><th>负载率后</th><th>等级</th><th>优先级</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="v in visibleVillageRows" :key="`${v.township}-${v.village}`" :class="villageRowClass(v)">
                  <td>{{ v.township }}</td>
                  <td class="td-strong">{{ v.village }}</td>
                  <td>{{ v.inUseBarns }}</td>
                  <td class="td-add" :class="{ added: v.addedCount > 0 }">{{ v.addedCount > 0 ? '+' + v.addedCount : '0' }}</td>
                  <td>{{ v.inUseAfter }}</td>
                  <td>{{ Number(v.purchaseKg).toLocaleString() }}</td>
                  <td>{{ loadRateText(v.loadRateBefore) }}</td>
                  <td><span class="vl-badge" :style="loadLevelStyle(v.levelBefore)">{{ v.levelBefore }}</span></td>
                  <td><span :class="{ 'over-badge': Number(v.loadRateAfter ?? 0) > Number(loadData?.countyBaselineAfter ?? 0) }">{{ loadRateText(v.loadRateAfter) }}</span></td>
                  <td><span class="vl-badge" :class="{ 'vl-badge--warn': Number(v.loadRateAfter ?? 0) > Number(loadData?.countyBaselineAfter ?? 0) }" :style="loadLevelStyle(v.levelAfter)">{{ v.levelAfter }}</span></td>
                  <td class="td-pri">{{ v.priorityIndex != null ? v.priorityIndex : '缺数据' }}</td>
                </tr>
                <tr v-if="!visibleVillageRows.length">
                  <td colspan="11" class="no-data">暂无分村数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="algo-panel">
          <div class="algo-panel-head" @click="algoPanelOpen = algoPanelOpen.length ? [] : ['thought', 'formula', 'example', 'flow']">
            <span class="dot"></span>
            <span>算法思想与推演说明</span>
            <span class="algo-panel-tip">{{ algoPanelOpen.length ? '点击收起' : '点击展开' }}</span>
          </div>
          <el-collapse v-model="algoPanelOpen" class="algo-collapse">
            <el-collapse-item name="thought" title="1. 算法思想">
              <div class="algo-block">
                <p>本算法以“县域总量不变、村级均衡重构”为核心，先统计现有烤房产能，再把新增点按村叠加到负载模型中，实时比较每个村的新增前后负载率、县均基准负载率和均衡指数。</p>
                <p>对每个村都计算：现有负载率、加入新增点后的负载率、与县基准的差值、是否超出县均水平。超过县均的村高亮显示，作为优先治理对象。</p>
              </div>
            </el-collapse-item>
            <el-collapse-item name="formula" title="2. 计算公式">
              <pre class="algo-formula">{{ algoDesc?.formula || '负载率 = 村收购量 / (在用烤房数 × 单座设计产能)\n县基准负载率 = 县收购总量 / (县在用烤房总数 × 单座设计产能)\n新增后负载率 = 村收购量 / ((在用烤房数 + 新增数) × 单座设计产能)\n均衡指数 = 1 - 标准差 / 平均值' }}</pre>
            </el-collapse-item>
            <el-collapse-item name="example" title="3. 示例">
              <div class="algo-block">
                <p>示例：某村收购量 40,000 公斤，现有在用烤房 6 座，单座产能 5,000 公斤/季，则新增前负载率 = 40000 / (6 × 5000) = 1.333。</p>
                <p>若新增 2 座后，新增后负载率 = 40000 / (8 × 5000) = 1.000，若县均基准为 0.92，则该村仍高于县均，需继续优先调配。</p>
              </div>
            </el-collapse-item>
            <el-collapse-item name="flow" title="4. 流转思路">
              <div class="algo-block">
                <ol>
                  <li>读取现有烤房、烟叶收购、候选点三类数据。</li>
                  <li>把候选点按村归集，作为“新增数”。</li>
                  <li>重算每个村新增前/后负载率和县均基准。</li>
                  <li>比较村级负载率与县均值，超过则高亮。</li>
                  <li>刷新右侧表格、推荐列表和地图标点，形成沙盘演练闭环。</li>
                </ol>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
/* ==================== 森林绿色系 · 玻璃拟态风格 ==================== */
.site-page {
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
  overflow-y: auto;
  overflow-x: hidden;
  color: #b8d4c8;
  perspective: 1200px;
  scrollbar-width: thin;
}
.site-page::-webkit-scrollbar { width: 8px; }
.site-page::-webkit-scrollbar-thumb { background: rgba(46, 139, 106, 0.25); border-radius: 4px; }

/* ==================== 头部 ==================== */
.site-header {
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
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15), 0 10px 20px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
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
.site-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 180px;
  right: 180px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(46, 139, 106, 0.2) 50%, transparent);
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-logo {
  width: 38px; height: 38px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0D5D46, #2E8B6A);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15), 0 10px 20px rgba(0, 0, 0, 0.1);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}
.header-title { font-size: 20px; font-weight: 700; color: #d4e8dc; margin: 0; letter-spacing: 3px; }
.header-sub { font-size: 10px; color: #4a7a62; letter-spacing: 2px; margin-top: 2px; text-transform: uppercase; }
.header-center { display: flex; gap: 10px; }
.stat-card {
  position: relative;
  background: rgba(13, 45, 34, 0.6);
  backdrop-filter: blur(12px);
  border-radius: 8px;
  padding: 10px 16px 10px 14px;
  text-align: center;
  min-width: 82px;
  overflow: hidden;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 10px 20px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.stat-card:hover { transform: translateY(-2px); }
.stat-card-accent { position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 40%; height: 2px; border-radius: 1px; opacity: 0.5; }
.stat-value { font-size: 24px; font-weight: 700; font-family: 'Consolas', 'Monaco', monospace; line-height: 1.2; }
.stat-label { font-size: 11px; color: #5a8a72; margin-top: 3px; letter-spacing: 0.5px; }
.header-right { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; }
.header-time { font-size: 15px; color: #5FB292; font-family: 'Consolas', monospace; font-weight: 600; letter-spacing: 1px; }

/* ==================== 主体 ==================== */
.site-body {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 14px;
  padding: 14px;
  min-height: 560px;
  flex-shrink: 0;
}

/* ==================== 地图面板 ==================== */
.map-panel {
  display: flex;
  flex-direction: column;
  min-height: 560px;
  background: rgba(13, 45, 34, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(46, 139, 106, 0.15);
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 10px 20px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
.map-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  font-size: 15px;
  font-weight: 600;
  color: #d4e8dc;
  border-bottom: 1px solid rgba(46, 139, 106, 0.1);
  flex-shrink: 0;
}
.dot { width: 7px; height: 7px; border-radius: 50%; background: #5FB292; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2); flex-shrink: 0; }
.dot.alert { background: #fca5a5; }
.map-info { margin-left: auto; font-size: 12px; color: #5a8a72; font-weight: 400; }

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  border-bottom: 1px solid rgba(46, 139, 106, 0.08);
  flex-shrink: 0;
  flex-wrap: wrap;
  position: relative;
  z-index: 1200;
}
.toolbar :deep(.el-button) {
  --el-button-bg-color: rgba(13, 45, 34, 0.6);
  --el-button-border-color: rgba(46, 139, 106, 0.3);
  --el-button-text-color: #7aac96;
  --el-button-hover-bg-color: rgba(46, 139, 106, 0.15);
  --el-button-hover-border-color: rgba(46, 139, 106, 0.5);
  --el-button-hover-text-color: #d4e8dc;
  --el-button-active-bg-color: rgba(46, 139, 106, 0.25);
  --el-button-active-border-color: rgba(46, 139, 106, 0.5);
  --el-button-active-text-color: #d4e8dc;
}
.toolbar :deep(.el-button--primary) {
  --el-button-bg-color: #2E8B6A;
  --el-button-border-color: #2E8B6A;
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: #3DA07A;
  --el-button-hover-border-color: #3DA07A;
  --el-button-hover-text-color: #fff;
}
.toolbar-tip { margin-left: auto; font-size: 11px; color: #5a8a72; }
.clear-btn { --el-button-text-color: #fca5a5 !important; }
.map-filter-group { display: inline-flex; align-items: center; gap: 4px; margin-left: 4px; padding-left: 8px; border-left: 1px solid rgba(46, 139, 106, 0.18); }
.map-filter-label { font-size: 11px; color: #5a8a72; white-space: nowrap; }
.map-filter-select { width: 82px; }
.map-filter-select:last-child { width: 104px; }

.map-chart-wrap { flex: 1 1 auto; min-height: 420px; position: relative; overflow: hidden; }
.map-chart { width: 100%; height: 100%; min-height: 420px; }

/* Leaflet 深色适配 */
.map-chart :deep(.leaflet-container) { background: #0A1F18; font-family: inherit; }
.map-chart :deep(.leaflet-tile-pane) { filter: invert(0.92) hue-rotate(170deg) brightness(0.95) contrast(0.9) saturate(0.7); }
.map-chart :deep(.leaflet-control-zoom) { border: 1px solid rgba(46, 139, 106, 0.2) !important; border-radius: 8px !important; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important; }
.map-chart :deep(.leaflet-control-zoom a) { background: rgba(13, 45, 34, 0.9) !important; color: #7aac96 !important; border-bottom: 1px solid rgba(46, 139, 106, 0.12) !important; }
.map-chart :deep(.leaflet-control-zoom a:hover) { background: rgba(46, 139, 106, 0.2) !important; color: #d4e8dc !important; }
.map-chart :deep(.leaflet-popup-content-wrapper) { background: rgba(10, 31, 24, 0.96); backdrop-filter: blur(16px); border: 1px solid rgba(46, 139, 106, 0.2); border-radius: 8px; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4); color: #b8d4c8; }
.map-chart :deep(.leaflet-popup-content) { margin: 12px 16px; line-height: 1.5; }
.map-chart :deep(.leaflet-popup-tip) { background: rgba(10, 31, 24, 0.96); border: 1px solid rgba(46, 139, 106, 0.2); }
.map-chart :deep(.leaflet-popup-close-button) { color: #5a8a72 !important; font-size: 18px !important; padding: 6px 8px !important; }

/* 现有烤房圆点 */
.map-chart :deep(.barn-dot-marker) { background: transparent !important; border: none !important; }
.map-chart :deep(.barn-dot) { width: 8px; height: 8px; border-radius: 50%; opacity: 0.55; box-shadow: 0 0 3px rgba(0,0,0,0.4); }

/* 候选点标记 */
.map-chart :deep(.candidate-marker) { background: transparent !important; border: none !important; cursor: pointer; transition: transform 0.2s ease; }
.map-chart :deep(.candidate-marker:hover) { z-index: 1000; transform: scale(1.2); }
.map-chart :deep(.candidate-marker--new) { transform: scale(1.1); }
.map-chart :deep(.candidate-marker--new:hover) { transform: scale(1.28); }
.map-chart :deep(.map-candidate-delete) {
  margin-top: 10px;
  padding: 6px 12px;
  border: 1px solid rgba(212, 96, 74, 0.5);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(212, 96, 74, 0.12), rgba(224, 138, 60, 0.16));
  color: #D4604A;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  transition: all 0.18s ease;
}
.map-chart :deep(.map-candidate-delete:hover) {
  background: linear-gradient(135deg, #D4604A, #E08A3C);
  border-color: #D4604A;
  color: #fff;
  transform: translateY(-1px);
}

/* 图例 */
.map-legend {
  position: absolute;
  bottom: 14px;
  left: 14px;
  display: flex;
  gap: 14px;
  background: rgba(10, 31, 24, 0.88);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(46, 139, 106, 0.12);
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 12px;
  color: #7aac96;
  z-index: 1000;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 10px 20px rgba(0, 0, 0, 0.12);
  flex-wrap: wrap;
}
.legend-item { display: flex; align-items: center; gap: 5px; }
.legend-barn-dot { width: 8px; height: 8px; border-radius: 50%; background: #5a8a72; opacity: 0.7; }

/* ==================== 右侧面板 ==================== */
.side-panel { display: flex; flex-direction: column; gap: 14px; min-height: 0; }
.side-section {
  display: flex;
  flex-direction: column;
  background: rgba(13, 45, 34, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(46, 139, 106, 0.12);
  border-radius: 8px;
  overflow: hidden;
  min-height: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 10px 20px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
.side-section--impact { flex: 0 0 auto; }
.side-section:nth-child(2) { flex: 0 0 auto; max-height: 220px; }
.side-section:last-child { flex: 1; min-height: 0; }
.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #d4e8dc;
  border-bottom: 1px solid rgba(46, 139, 106, 0.1);
  flex-shrink: 0;
}
.alert-count { font-size: 11px; background: rgba(252, 165, 165, 0.12); color: #fca5a5; padding: 2px 10px; border-radius: 8px; margin-left: auto; font-weight: 600; }

.impact-card { padding: 14px 16px; display: flex; flex-direction: column; gap: 10px; }
.impact-candidate { font-size: 14px; font-weight: 700; color: #1c3d2f; }
.impact-meta { font-size: 12px; color: #6f8f80; }
.impact-hint {
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(212, 96, 74, 0.06);
  border: 1px dashed rgba(212, 96, 74, 0.2);
  color: #a95a4b;
  font-size: 11px;
  line-height: 1.6;
}
.impact-county {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(46, 139, 106, 0.08);
  color: #3f5f51;
  font-size: 12px;
}
.impact-county b { font-size: 16px; color: #2E8B6A; font-family: 'Consolas', monospace; }
.impact-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.impact-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(46, 139, 106, 0.05);
}
.impact-item span { font-size: 11px; color: #6f8f80; }
.impact-item b { font-size: 13px; color: #1c3d2f; font-family: 'Consolas', monospace; }
.impact-item em {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
}
.impact-list {
  margin-top: 2px;
  border-top: 1px dashed rgba(46, 139, 106, 0.18);
  padding-top: 8px;
}
.impact-list-title { font-size: 12px; color: #6f8f80; margin-bottom: 6px; }
.impact-list-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  background: rgba(46, 139, 106, 0.04);
  margin-bottom: 6px;
}
.impact-list-row span { font-size: 12px; color: #3f5f51; }
.impact-list-row b { font-size: 12px; color: #2E8B6A; font-family: 'Consolas', monospace; }
.impact-over { color: #D4604A !important; }
.impact-empty { padding: 18px 16px; font-size: 12px; color: #6f8f80; line-height: 1.7; }
.impact-empty--inline {
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(212, 96, 74, 0.06);
  border: 1px dashed rgba(212, 96, 74, 0.24);
}

/* 乡镇列表 */
.town-list { overflow-y: auto; padding: 10px 14px; }
.town-list::-webkit-scrollbar { width: 3px; }
.town-list::-webkit-scrollbar-thumb { background: rgba(46, 139, 106, 0.25); border-radius: 2px; }
.town-item { margin-bottom: 10px; }
.town-name { font-size: 13px; color: #7aac96; margin-bottom: 5px; letter-spacing: 0.3px; }
.town-villages { font-size: 11px; color: #6f8f80; line-height: 1.6; margin-bottom: 6px; }
.town-bar-wrap { display: flex; align-items: center; gap: 10px; }
.town-bar { flex: 1; height: 8px; background: rgba(255, 255, 255, 0.05); border-radius: 4px; overflow: hidden; }
.town-fill { height: 100%; transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1); border-radius: 4px; }
.town-count { font-size: 13px; color: #d4e8dc; font-family: 'Consolas', monospace; font-weight: 600; min-width: 28px; text-align: right; }

/* 候选点列表 */
.candidate-list { flex: 1; overflow-y: auto; padding: 8px; }
.candidate-list::-webkit-scrollbar { width: 3px; }
.candidate-list::-webkit-scrollbar-thumb { background: rgba(46, 139, 106, 0.25); border-radius: 2px; }
.candidate-item {
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
.candidate-status-bar { position: absolute; left: 0; top: 0; bottom: 0; width: 3px; border-radius: 0 2px 2px 0; opacity: 0.7; }
.candidate-rank {
  width: 22px; height: 22px; border-radius: 50%;
  background: linear-gradient(135deg, #2E8B6A, #0D5D46);
  color: #fff; font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; box-shadow: 0 2px 6px rgba(13, 93, 70, 0.4);
}
.candidate-item:hover { background: rgba(95, 178, 146, 0.06); transform: translateX(2px); }
.candidate-info { flex: 1; min-width: 0; }
.candidate-name { font-size: 13px; color: #d4e8dc; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; letter-spacing: 0.2px; }
.candidate-meta { font-size: 11px; color: #5a8a72; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.candidate-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.candidate-tag { font-size: 10px; padding: 1px 6px; border-radius: 3px; font-weight: 600; white-space: nowrap; }
.candidate-tag--new { background: rgba(212, 96, 74, 0.16); color: #D4604A; border: 1px solid rgba(212, 96, 74, 0.35); }
.del-btn {
  flex-shrink: 0;
  min-width: 56px;
  --el-button-text-color: #D4604A;
  --el-button-border-color: rgba(212, 96, 74, 0.45);
  --el-button-bg-color: rgba(212, 96, 74, 0.08);
  --el-button-hover-text-color: #fff;
  --el-button-hover-border-color: #D4604A;
  --el-button-hover-bg-color: #D4604A;
}
.no-data { text-align: center; padding: 24px; color: #5a8a72; font-size: 13px; }

/* ==================== 弹窗 ==================== */
.add-dialog :deep(.el-dialog), .detail-dialog :deep(.el-dialog) {
  background: linear-gradient(180deg, #0D2B22, #0A1F18);
  border: 1px solid rgba(46, 139, 106, 0.2);
  border-radius: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 10px 20px rgba(0, 0, 0, 0.2), 0 20px 40px rgba(0, 0, 0, 0.15);
}
.add-dialog :deep(.el-dialog__title), .detail-dialog :deep(.el-dialog__title) { color: #d4e8dc; font-weight: 600; font-size: 16px; }
.add-dialog :deep(.el-dialog__body), .detail-dialog :deep(.el-dialog__body) { color: #b8d4c8; }
.add-dialog :deep(.el-form-item__label), .detail-dialog :deep(.el-form-item__label) { color: #7aac96; }
.add-dialog :deep(.el-input__wrapper), .add-dialog :deep(.el-textarea__inner) {
  background: rgba(13, 45, 34, 0.6);
  box-shadow: 0 0 0 1px rgba(46, 139, 106, 0.2) inset;
}
.add-dialog :deep(.el-input__inner), .add-dialog :deep(.el-textarea__inner) { color: #d4e8dc; }
.add-dialog :deep(.el-select__placeholder) { color: #5a8a72; }
.add-coord-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-bottom: 14px;
  background: rgba(46, 139, 106, 0.08);
  border: 1px solid rgba(46, 139, 106, 0.2);
  border-radius: 6px;
  font-size: 12px;
  color: #7aac96;
}
.add-form-stack {
  display: flex;
  flex-direction: column;
}
.add-form-stack :deep(.el-form-item) {
  margin-bottom: 0;
}
.add-form-stack :deep(.el-form-item__label) {
  width: 92px !important;
}
.add-form-stack :deep(.el-form-item__content) {
  min-width: 0;
}
.add-form-stack :deep(.el-form-item + .el-form-item) {
  margin-top: 8px;
}
.add-advanced-collapse {
  border: 1px solid rgba(46, 139, 106, 0.14);
  border-radius: 8px;
  overflow: hidden;
  background: rgba(46, 139, 106, 0.03);
}
.add-advanced-collapse :deep(.el-collapse-item__header) {
  padding: 0 12px;
  background: transparent;
  color: #4f7d66;
  font-weight: 600;
  border-bottom: 1px solid rgba(46, 139, 106, 0.08);
}
.add-advanced-collapse :deep(.el-collapse-item__wrap) {
  background: transparent;
  border-bottom: none;
}
.add-advanced-collapse :deep(.el-collapse-item__content) {
  padding: 12px;
}
.add-advanced-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 12px;
}
.add-advanced-grid :deep(.el-form-item) {
  margin-bottom: 0;
}
.add-advanced-grid :deep(.el-form-item__label) {
  width: 92px !important;
}
.add-advanced-grid :deep(.el-form-item__content) {
  min-width: 0;
}
.form-tip { font-size: 11px; color: #5a8a72; margin-top: 2px; line-height: 1.4; }
.gen-desc {
  padding: 10px 12px;
  margin-bottom: 14px;
  background: rgba(46, 139, 106, 0.08);
  border: 1px solid rgba(46, 139, 106, 0.2);
  border-radius: 6px;
  font-size: 12px;
  color: #7aac96;
  line-height: 1.6;
}

/* 详情弹窗 */
.detail-score-ring { position: relative; width: 120px; height: 120px; margin: 0 auto 20px; }
.detail-score-ring svg { width: 100%; height: 100%; }
.detail-score-num { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 32px; font-weight: 700; font-family: 'Consolas', monospace; }
.detail-desc :deep(.el-descriptions__label) { color: #5a8a72; }
.detail-desc :deep(.el-descriptions__content) { color: #b8d4c8; }
.detail-desc :deep(.el-descriptions__cell) { border-color: rgba(46, 139, 106, 0.1); }

/* 分项得分 */
.score-breakdown { margin-top: 20px; }
.score-title { font-size: 14px; font-weight: 600; color: #d4e8dc; margin-bottom: 12px; }
.score-title-sub { font-size: 11px; font-weight: 400; color: #5a8a72; }
.score-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.score-row-total { padding-top: 8px; border-top: 1px dashed rgba(46, 139, 106, 0.25); }
.score-label { width: 72px; font-size: 12px; color: #7aac96; flex-shrink: 0; }
.score-bar { flex: 1; height: 8px; background: rgba(255, 255, 255, 0.05); border-radius: 4px; overflow: hidden; }
.score-fill { height: 100%; border-radius: 4px; transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1); }
.score-val { width: 40px; text-align: right; font-size: 12px; color: #d4e8dc; font-family: 'Consolas', monospace; font-weight: 600; }

/* 算法说明 */
.algo-name { font-size: 18px; font-weight: 700; color: #d4e8dc; }
.algo-fullname { font-size: 12px; color: #5a8a72; margin-top: 4px; letter-spacing: 0.5px; }
.algo-section { margin-top: 16px; }
.algo-section-title { font-size: 13px; font-weight: 600; color: #5FB292; margin-bottom: 8px; }
.algo-text { font-size: 13px; color: #b8d4c8; line-height: 1.8; }
.algo-formula {
  background: rgba(13, 45, 34, 0.6);
  border: 1px solid rgba(46, 139, 106, 0.15);
  border-radius: 6px;
  padding: 12px;
  font-size: 12px;
  color: #7DC4A8;
  line-height: 1.8;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', monospace;
}
.algo-panel {
  margin-top: 14px;
  background: rgba(10, 31, 24, 0.5);
  border: 1px solid rgba(46, 139, 106, 0.12);
  border-radius: 6px;
  overflow: hidden;
}
.algo-panel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid rgba(46, 139, 106, 0.1);
}
.algo-panel-tip { margin-left: auto; font-size: 12px; color: #5FB292; }
.algo-collapse { --el-collapse-border-color: transparent; }
.algo-collapse :deep(.el-collapse-item__header) {
  background: transparent;
  color: #d4e8dc;
  border-bottom: 1px solid rgba(46, 139, 106, 0.08);
  padding: 0 14px;
}
.algo-collapse :deep(.el-collapse-item__content) { padding: 12px 14px 14px; color: #b8d4c8; }
.algo-block { font-size: 13px; line-height: 1.8; color: #b8d4c8; }
.algo-block p { margin: 0 0 8px; }
.algo-block ol { margin: 0; padding-left: 18px; }
.algo-block li { margin-bottom: 6px; }

/* ==================== 村烤房负载均衡推演（新增功能） ==================== */
.load-panel {
  margin: 0 14px 14px;
  background: rgba(13, 45, 34, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(46, 139, 106, 0.15);
  border-radius: 8px;
  overflow: hidden;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 10px 20px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.04);
  flex-shrink: 0;
}
.load-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(46, 139, 106, 0.1);
  flex-wrap: wrap;
}
.load-head-left { display: flex; align-items: center; gap: 10px; }
.dot.warn { background: #E8A714; box-shadow: 0 0 0 0 rgba(232, 167, 20, 0.4); animation: pulseWarn 1.6s infinite; }
@keyframes pulseWarn { 0% { box-shadow: 0 0 0 0 rgba(232, 167, 20, 0.5); } 70% { box-shadow: 0 0 0 7px rgba(232, 167, 20, 0); } 100% { box-shadow: 0 0 0 0 rgba(232, 167, 20, 0); } }
.load-title { font-size: 15px; font-weight: 700; color: #d4e8dc; letter-spacing: 1px; }
.load-sub { font-size: 12px; color: #5a8a72; }
.load-head-right { display: flex; align-items: center; gap: 8px; }
.load-cap { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #7aac96; }
.cap-input { width: 110px; }
.load-cap b { color: #5FB292; font-weight: 600; }
.load-toggle { font-size: 12px; color: #5FB292; cursor: pointer; white-space: nowrap; }

.load-body { padding: 14px 20px 18px; }
.load-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin-bottom: 14px; }
.load-card {
  background: rgba(10, 31, 24, 0.6);
  border: 1px solid rgba(46, 139, 106, 0.12);
  border-radius: 6px;
  padding: 10px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
.lc-label { font-size: 11px; color: #5a8a72; letter-spacing: 0.5px; }
.lc-value { font-size: 20px; font-weight: 700; color: #d4e8dc; font-family: 'Consolas', monospace; margin-top: 4px; }
.lc-value.lc-good { color: #5FB292; }
.lc-sub { font-size: 11px; color: #5a8a72; margin-top: 4px; }
.lc-sub b { color: #E8A714; }
.lc-arrow { font-size: 12px; color: #7aac96; font-weight: 600; }
.lc-arrow.better { color: #5FB292; }

.load-main { display: grid; grid-template-columns: 300px 1fr; gap: 14px; }
.rec-panel {
  background: rgba(10, 31, 24, 0.5);
  border: 1px solid rgba(46, 139, 106, 0.12);
  border-radius: 6px;
  padding: 12px;
}
.load-subhead { font-size: 13px; font-weight: 600; color: #d4e8dc; display: flex; align-items: center; gap: 6px; margin-bottom: 10px; }
.rec-list { max-height: 320px; overflow-y: auto; }
.rec-list::-webkit-scrollbar { width: 3px; }
.rec-list::-webkit-scrollbar-thumb { background: rgba(46, 139, 106, 0.25); border-radius: 2px; }
.rec-item { display: flex; align-items: center; gap: 8px; padding: 7px 8px; border-radius: 6px; margin-bottom: 6px; background: rgba(232, 167, 20, 0.04); border: 1px solid rgba(232, 167, 20, 0.1); }
.rec-item--hot { background: rgba(212, 96, 74, 0.08); border-color: rgba(212, 96, 74, 0.22); }
.rec-idx { width: 18px; height: 18px; border-radius: 50%; background: linear-gradient(135deg, #E8A714, #D4604A); color: #fff; font-size: 10px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.rec-name { font-size: 13px; color: #d4e8dc; font-weight: 600; flex: 1; }
.rec-town { font-size: 10px; color: #5a8a72; margin-left: 4px; font-weight: 400; }
.rec-load { font-size: 12px; font-family: 'Consolas', monospace; font-weight: 600; white-space: nowrap; }
.rec-need { font-size: 11px; color: #fff; background: linear-gradient(135deg, #D4604A, #E08A3C); padding: 2px 8px; border-radius: 8px; white-space: nowrap; }
.rec-empty { text-align: center; padding: 20px; color: #5FB292; font-size: 13px; }

.vl-table-wrap { background: rgba(10, 31, 24, 0.5); border: 1px solid rgba(46, 139, 106, 0.12); border-radius: 6px; padding: 12px; overflow: hidden; }
.vl-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.vl-table th {
  text-align: left; color: #5a8a72; font-weight: 600; padding: 6px 8px; white-space: nowrap;
  border-bottom: 1px solid rgba(46, 139, 106, 0.15);
}
.vl-table td { padding: 6px 8px; color: #b8d4c8; white-space: nowrap; border-bottom: 1px solid rgba(46, 139, 106, 0.06); }
.vl-table tbody tr:hover td { background: rgba(95, 178, 146, 0.05); }
.td-strong { color: #d4e8dc; font-weight: 600; }
.td-add { color: #5a8a72; }
.td-add.added { color: #E8A714; font-weight: 700; }
.td-pri { color: #7DC4A8; font-family: 'Consolas', monospace; font-weight: 600; }
.vl-badge { font-size: 11px; padding: 1px 8px; border-radius: 8px; font-weight: 600; white-space: nowrap; }
.vl-badge--warn { border: 1px solid rgba(212, 96, 74, 0.45); box-shadow: 0 0 0 1px rgba(212, 96, 74, 0.15) inset; }
.over-badge { color: #D4604A; font-weight: 700; }
.row-overload td { background: rgba(212, 96, 74, 0.06); }
.row-normal td { background: rgba(46, 139, 106, 0.01); }
.row-added td { box-shadow: inset 3px 0 0 #E8A714; }

/* ==================== 浅色主题（覆盖暗色，森林绿降低饱和度） ==================== */
.load-toggle-map { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: #3f5f51; cursor: pointer; }

.site-page {
  background: #f2f7f4;
  background-image:
    radial-gradient(ellipse at 25% 30%, rgba(46, 139, 106, 0.06) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 10%, rgba(95, 178, 146, 0.05) 0%, transparent 50%),
    linear-gradient(rgba(46, 139, 106, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 139, 106, 0.03) 1px, transparent 1px);
  color: #3f5f51;
}
.site-page::-webkit-scrollbar-thumb { background: rgba(46, 139, 106, 0.25); }

.site-header {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(46, 139, 106, 0.16);
  border-top: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 2px 6px rgba(20, 60, 45, 0.05), 0 8px 20px rgba(20, 60, 45, 0.04);
}
.header-deco-line { background: linear-gradient(90deg, rgba(46, 139, 106, 0.4), transparent); }
.header-deco-line--right { background: linear-gradient(270deg, rgba(46, 139, 106, 0.4), transparent); }
.header-logo { box-shadow: 0 2px 6px rgba(46, 139, 106, 0.25), 0 8px 16px rgba(46, 139, 106, 0.18); border-top: 1px solid rgba(255, 255, 255, 0.25); }
.header-title { color: #1c3d2f; }
.header-sub { color: #7a9c8b; }

.stat-card {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(46, 139, 106, 0.14);
  border-top: 1px solid rgba(46, 139, 106, 0.18);
  box-shadow: 0 1px 2px rgba(20, 60, 45, 0.04), 0 6px 14px rgba(20, 60, 45, 0.05);
}
.stat-card:hover { box-shadow: 0 4px 12px rgba(46, 139, 106, 0.12); }
.stat-label { color: #7a9c8b; }
.header-time { color: #2E8B6A; }

.map-panel, .side-section, .load-panel {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(46, 139, 106, 0.16);
  border-top: 1px solid rgba(46, 139, 106, 0.2);
  box-shadow: 0 1px 3px rgba(20, 60, 45, 0.05), 0 8px 20px rgba(20, 60, 45, 0.05);
}
.map-header, .section-head, .load-head, .load-subhead { color: #1c3d2f; border-bottom: 1px solid rgba(46, 139, 106, 0.12); }
.map-info, .toolbar-tip, .load-sub, .load-cap, .rec-town, .td-add, .form-tip, .gen-desc, .add-coord-tip,
.algo-fullname, .score-title-sub, .score-label, .lc-label, .lc-sub, .no-data { color: #6f8f80; }
.load-cap b, .load-toggle, .lc-arrow.better { color: #2E8B6A; }
.load-title, .lc-value, .rec-name, .td-strong, .candidate-name, .town-name { color: #1c3d2f; }
.town-count, .candidate-count, .score-val { color: #1c3d2f; }
.algo-name { color: #1c3d2f; }
.algo-section-title { color: #2E8B6A; }
.algo-text { color: #4a6a5a; }

.toolbar :deep(.el-button) {
  --el-button-bg-color: rgba(255, 255, 255, 0.9);
  --el-button-border-color: rgba(46, 139, 106, 0.3);
  --el-button-text-color: #3f5f51;
  --el-button-hover-bg-color: rgba(46, 139, 106, 0.08);
  --el-button-hover-border-color: rgba(46, 139, 106, 0.45);
  --el-button-hover-text-color: #2E8B6A;
  --el-button-active-bg-color: rgba(46, 139, 106, 0.14);
  --el-button-active-text-color: #1c3d2f;
}
.toolbar :deep(.el-button--primary) {
  --el-button-bg-color: #2E8B6A;
  --el-button-border-color: #2E8B6A;
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: #247a5e;
  --el-button-hover-border-color: #247a5e;
  --el-button-hover-text-color: #fff;
}
.clear-btn { --el-button-text-color: #D4604A !important; }

/* 浅色 Leaflet 适配 */
.map-chart :deep(.leaflet-container) { background: #e7edf0; font-family: inherit; }
.map-chart :deep(.leaflet-tile-pane) { filter: none; }
.map-chart :deep(.leaflet-control-zoom) { border: 1px solid rgba(46, 139, 106, 0.2) !important; border-radius: 8px !important; overflow: hidden; box-shadow: 0 2px 8px rgba(20, 60, 45, 0.12) !important; }
.map-chart :deep(.leaflet-control-zoom a) { background: rgba(255, 255, 255, 0.95) !important; color: #4a6a5a !important; border-bottom: 1px solid rgba(46, 139, 106, 0.12) !important; }
.map-chart :deep(.leaflet-control-zoom a:hover) { background: rgba(46, 139, 106, 0.1) !important; color: #2E8B6A !important; }
.map-chart :deep(.leaflet-popup-content-wrapper) { background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(46, 139, 106, 0.2); border-radius: 8px; box-shadow: 0 6px 20px rgba(20, 60, 45, 0.14); color: #3f5f51; }
.map-chart :deep(.leaflet-popup-tip) { background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(46, 139, 106, 0.2); }
.map-chart :deep(.leaflet-popup-close-button) { color: #7a9c8b !important; }
.map-chart :deep(.barn-dot) { opacity: 0.85; box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.7), 0 0 4px rgba(20, 60, 45, 0.2); }

/* 浅色图例 */
.map-legend {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(46, 139, 106, 0.16);
  color: #4a6a5a;
  box-shadow: 0 2px 6px rgba(20, 60, 45, 0.08), 0 8px 18px rgba(20, 60, 45, 0.06);
}
.legend-group { display: flex; flex-wrap: wrap; gap: 6px 12px; align-items: center; }
.legend-title { font-size: 11px; color: #7a9c8b; font-weight: 600; margin-right: 2px; }
.legend-item { width: 100%; }
.legend-item { display: flex; align-items: center; gap: 5px; }
.legend-load-dot { width: 10px; height: 10px; border-radius: 50%; }
.legend-barn-dot { background: #2E8B6A; opacity: 0.85; }
.legend-divider { width: 1px; align-self: stretch; background: rgba(46, 139, 106, 0.12); margin: 0 2px; }

/* 浅色右侧面板 */
.town-bar { background: rgba(46, 139, 106, 0.1); }
.candidate-item:hover { background: rgba(95, 178, 146, 0.08); }
.candidate-meta { color: #7a9c8b; }

/* 浅色弹窗 */
.add-dialog :deep(.el-dialog), .detail-dialog :deep(.el-dialog) {
  background: linear-gradient(180deg, #ffffff, #f4faf7);
  border: 1px solid rgba(46, 139, 106, 0.2);
  box-shadow: 0 4px 6px rgba(20, 60, 45, 0.08), 0 20px 50px rgba(20, 60, 45, 0.18);
}
.add-dialog :deep(.el-dialog__title), .detail-dialog :deep(.el-dialog__title) { color: #1c3d2f; }
.add-dialog :deep(.el-dialog__body), .detail-dialog :deep(.el-dialog__body) { color: #3f5f51; }
.add-dialog :deep(.el-form-item__label), .detail-dialog :deep(.el-form-item__label) { color: #4a6a5a; }
.add-dialog :deep(.el-input__wrapper), .add-dialog :deep(.el-textarea__inner) { background: rgba(255, 255, 255, 0.9); box-shadow: 0 0 0 1px rgba(46, 139, 106, 0.18) inset; }
.add-dialog :deep(.el-input__inner), .add-dialog :deep(.el-textarea__inner) { color: #1c3d2f; }
.detail-desc :deep(.el-descriptions__label) { color: #6f8f80; }
.detail-desc :deep(.el-descriptions__content) { color: #3f5f51; }
.detail-desc :deep(.el-descriptions__cell) { border-color: rgba(46, 139, 106, 0.12); }
.detail-score-ring circle[fill="none"] { stroke: rgba(46, 139, 106, 0.1); }
.score-bar { background: rgba(46, 139, 106, 0.1); }

/* 浅色负载推演面板 */
.load-card, .rec-panel, .vl-table-wrap {
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(46, 139, 106, 0.13);
}
.rec-item { background: rgba(232, 167, 20, 0.06); border: 1px solid rgba(232, 167, 20, 0.18); }
.rec-load, .lc-arrow, .td-pri { color: #2E8B6A; }
.lc-arrow { color: #7a9c8b; }
.vl-table th { color: #6f8f80; border-bottom: 1px solid rgba(46, 139, 106, 0.16); }
.vl-table td { color: #3f5f51; border-bottom: 1px solid rgba(46, 139, 106, 0.08); }
.vl-table tbody tr:hover td { background: rgba(95, 178, 146, 0.07); }
.td-strong { color: #1c3d2f; }
.rec-empty { color: #2E8B6A; }
.rec-needed-badge { color: #fff; }

/* ==================== 地图图例：收起的紧凑芯片 ==================== */
.map-legend {
  position: absolute;
  bottom: 14px;
  left: 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 0;
  background: none;
  border: none;
  box-shadow: none;
  color: #4a6a5a;
}
.legend-toggle {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 6px 14px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  color: #3f5f51;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(46, 139, 106, 0.2);
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(20, 60, 45, 0.12);
  user-select: none;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.legend-toggle:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(46, 139, 106, 0.2); }
.legend-toggle-dot { width: 8px; height: 8px; border-radius: 50%; background: linear-gradient(135deg, #2E8B6A, #D4604A); }
.legend-toggle-text { letter-spacing: 1px; }
.legend-toggle-arrow { color: #2E8B6A; font-size: 11px; }
.legend-body {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(46, 139, 106, 0.18);
  border-radius: 8px;
  padding: 10px 14px;
  max-height: 42vh;
  overflow-y: auto;
  box-shadow: 0 6px 18px rgba(20, 60, 45, 0.1);
}
.legend-group { display: flex; align-items: baseline; gap: 6px; margin-bottom: 6px; }
.legend-group:last-child { margin-bottom: 0; }
.legend-title { font-size: 11px; color: #7a9c8b; font-weight: 600; min-width: 48px; white-space: nowrap; }
.legend-row { display: flex; flex-wrap: wrap; gap: 4px 10px; }
.legend-item { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; color: #4a6a5a; font-weight: 500; white-space: nowrap; width: auto; }
.legend-load-dot { width: 8px; height: 8px; border-radius: 50%; }
.legend-barn-dot { width: 8px; height: 8px; border-radius: 50%; background: #2E8B6A; opacity: 0.85; }
.legend-cand { width: 7px; height: 9px; border-radius: 3px; background: #D4944A; }
</style>
