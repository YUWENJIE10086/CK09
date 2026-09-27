<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBarnOptions } from '@/api/barn'
import {
  getSiteSelectionAll,
  addSiteSelection,
  deleteSiteSelection,
  clearSiteSelection,
  autoGenerateSiteSelection,
  evaluateSiteSelection,
  getSiteSelectionStats,
  getSiteSelectionAlgoDesc,
} from '@/api/siteSelection'
import { WarningFilled, Location, Aim, MagicStick, Delete, Refresh } from '@element-plus/icons-vue'

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

/** 选址适宜等级颜色 */
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
  const map = new Map<string, { name: string; total: number; high: number; avg: number }>()
  candidates.value.forEach(c => {
    const key = c.township || '未知'
    if (!map.has(key)) map.set(key, { name: key, total: 0, high: 0, avg: 0 })
    const g = map.get(key)!
    g.total++
    if (c.suitabilityLevel === '高度适宜') g.high++
  })
  return Array.from(map.values()).sort((a, b) => b.total - a.total)
})

/** 选中候选点详情 */
const selectedCandidate = ref<any | null>(null)
const detailVisible = ref(false)

/** 新增候选点弹窗 */
const addVisible = ref(false)
const addForm = ref({
  candidateName: '',
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
const descVisible = ref(false)

/** 乡镇选项 */
const townOptions = computed(() => {
  const set = new Set<string>()
  barnList.value.forEach((b: any) => { if (b.township) set.add(b.township) })
  candidates.value.forEach((c: any) => { if (c.township) set.add(c.township) })
  return Array.from(set)
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
  const color = levelColor[level] || '#94a3b8'
  const size = score != null ? 11 + (score / 100) * 7 : 11
  return L.divIcon({
    className: 'candidate-marker',
    html: `<svg width="${size * 1.5}" height="${size * 2}" viewBox="0 0 24 32" style="filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));">
      <path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0zm0 16c-2.2 0-4-1.8-4-4s1.8-4 4-4 4 1.8 4 4-1.8 4-4 4z" 
        fill="${color}" stroke="rgba(255,255,255,0.8)" stroke-width="1.5"/>
      <circle cx="12" cy="12" r="2.5" fill="rgba(255,255,255,0.85)"/>
    </svg>`,
    iconSize: [size * 1.5, size * 2],
    iconAnchor: [size * 0.75, size * 2],
    popupAnchor: [0, -size * 2],
  })
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

    // 点击地图添加候选点
    mapInstance.on('click', (e: any) => {
      mapClickPos.value = { lng: e.latlng.lng, lat: e.latlng.lat }
      addForm.value.longitude = Number(e.latlng.lng.toFixed(6))
      addForm.value.latitude = Number(e.latlng.lat.toFixed(6))
      addVisible.value = true
    })
  }

  barnLayer?.clearLayers()
  candidateLayer?.clearLayers()

  // 现有烤房（小圆点）
  const barnLatlngs: [number, number][] = []
  barnList.value.forEach((b: any) => {
    const lng = Number(b.longitude)
    const lat = Number(b.latitude)
    if (lng <= 0 || lat <= 0) return
    barnLatlngs.push([lat, lng])
    const color = b.healthLevel ? ({ '优良': '#2E8B6A', '良好': '#5FB292', '一般': '#D4944A', '预警': '#D4604A', '危险': '#B71C1C' } as any)[b.healthLevel] || '#5a8a72' : '#5a8a72'
    const marker = L.marker([lat, lng], { icon: createBarnDot(color), interactive: false })
    barnLayer?.addLayer(marker)
  })

  // 候选点
  const candLatlngs: [number, number][] = []
  candidates.value.forEach((c: any) => {
    const lng = Number(c.longitude)
    const lat = Number(c.latitude)
    if (lng <= 0 || lat <= 0) return
    candLatlngs.push([lat, lng])
    const level = c.suitabilityLevel || '待评估'
    const score = c.totalScore != null ? Number(c.totalScore) : null
    const marker = L.marker([lat, lng], {
      icon: createCandidateIcon(level, score),
    })
    marker.bindPopup(`
      <div style="min-width:200px;font-family:sans-serif;">
        <div style="font-weight:700;font-size:14px;margin-bottom:6px;color:#1a3d30;">${c.candidateName || '候选点'}</div>
        <div style="margin-bottom:3px;font-size:12px;color:#555;">乡镇：${c.township || '-'} · ${c.village || '-'}</div>
        <div style="margin-bottom:3px;font-size:12px;color:#555;">适宜等级：<span style="color:${levelColor[level]};font-weight:600;">${level}</span></div>
        <div style="font-size:12px;color:#555;">综合得分：<b style="color:${levelColor[level]};font-size:14px;">${score != null ? score : '-'}</b></div>
      </div>
    `, { maxWidth: 260 })
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
    await nextTick()
    renderMap()
  } finally {
    loading.value = false
  }
}

async function loadCandidates() {
  candidatesLoading.value = true
  try {
    const list = await getSiteSelectionAll().catch(() => [])
    candidates.value = (list || []) as any[]
  } finally {
    candidatesLoading.value = false
  }
}

/** 新增候选点 */
async function submitAdd() {
  if (!addForm.value.longitude || !addForm.value.latitude) {
    ElMessage.warning('请在地图上点击选择位置')
    return
  }
  try {
    await addSiteSelection({ ...addForm.value })
    ElMessage.success('候选点添加成功')
    addVisible.value = false
    resetAddForm()
    await loadCandidates()
    renderMap()
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  }
}

function resetAddForm() {
  addForm.value = {
    candidateName: '', township: '', village: '', longitude: 0, latitude: 0,
    altitude: null, areaSqm: null, slopeDegree: null,
    distanceRoad: null, distancePower: null, distanceWater: null,
    tobaccoArea: null, landType: '', remark: '',
  }
}

/** 删除候选点 */
async function removeCandidate(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该候选点吗？', '删除确认', { type: 'warning' })
    await deleteSiteSelection(id)
    ElMessage.success('删除成功')
    await loadCandidates()
    renderMap()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '删除失败')
  }
}

/** 清空所有候选点 */
async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清空所有候选点吗？此操作不可恢复！', '清空确认', { type: 'warning' })
    await clearSiteSelection()
    ElMessage.success('已清空')
    await loadCandidates()
    renderMap()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '清空失败')
  }
}

/** 自动生成网格候选点 */
async function submitGenerate() {
  try {
    const res = await autoGenerateSiteSelection({ ...genForm.value })
    ElMessage.success(res?.msg || '生成完成')
    genVisible.value = false
    await loadCandidates()
    renderMap()
  } catch (e: any) {
    ElMessage.error(e?.message || '生成失败')
  }
}

/** 运行选址评价算法 */
async function runEvaluate() {
  try {
    await ElMessageBox.confirm('将对全部候选点运行选址评价算法，确定继续？', '运行算法', { type: 'info' })
    const res = await evaluateSiteSelection({})
    ElMessage.success(res?.msg || '评估完成')
    await loadCandidates()
    renderMap()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '评估失败')
  }
}

/** 查看算法描述 */
async function showAlgoDesc() {
  try {
    algoDesc.value = await getSiteSelectionAlgoDesc()
    descVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.message || '获取算法描述失败')
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
          <h1 class="header-title">烤房选址</h1>
          <div class="header-sub">BARN SITE SELECTION</div>
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
          <span>秭归县烤房选址 · AHP+熵权法+TOPSIS综合评价</span>
          <span class="map-info">
            现有烤房 {{ barnList.length }} 座 · 候选点 {{ candidates.length }} 个
          </span>
        </div>

        <!-- 操作工具栏 -->
        <div class="toolbar">
          <el-button size="small" type="primary" :icon="MagicStick" @click="runEvaluate">运行选址算法</el-button>
          <el-button size="small" :icon="Aim" @click="genVisible = true">自动生成候选点</el-button>
          <el-button size="small" :icon="Location" @click="showAlgoDesc">算法说明</el-button>
          <span class="toolbar-tip">提示：点击地图任意位置可添加候选点</span>
          <el-button size="small" class="clear-btn" :icon="Delete" @click="clearAll">清空</el-button>
        </div>

        <div class="map-chart-wrap">
          <div ref="mapRef" class="map-chart"></div>
        </div>

        <!-- 图例 -->
        <div class="map-legend">
          <div v-for="(color, level) in levelColor" :key="level" class="legend-item">
            <svg viewBox="0 0 24 32" width="10" height="14"><path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0z" :fill="color" /></svg>
            {{ level }}
          </div>
          <div class="legend-item"><span class="legend-barn-dot"></span>现有烤房</div>
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="side-panel">
        <!-- 乡镇分布 -->
        <div class="side-section">
          <div class="section-head"><span class="dot"></span>乡镇候选点分布</div>
          <div class="town-list">
            <div v-for="g in townGroups" :key="g.name" class="town-item">
              <div class="town-name">{{ g.name }} <span class="town-high">{{ g.high }}个高度适宜</span></div>
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
            <span class="alert-count">{{ candidates.length }}</span>
          </div>
          <div class="candidate-list" v-loading="candidatesLoading">
            <div v-for="c in candidates" :key="c.id" class="candidate-item" @click="selectedCandidate = c; detailVisible = true">
              <div class="candidate-status-bar" :style="{ background: levelColor[c.suitabilityLevel || '待评估'] }"></div>
              <div class="candidate-rank" v-if="c.ranking">{{ c.ranking }}</div>
              <div class="candidate-info">
                <div class="candidate-name">{{ c.candidateName || '候选点' }}</div>
                <div class="candidate-meta">{{ c.township || '-' }} · {{ c.village || '-' }}</div>
                <div class="candidate-tags">
                  <span class="candidate-tag" :style="{ background: (levelColor[c.suitabilityLevel || '待评估'] || '#78909c') + '22', color: levelColor[c.suitabilityLevel || '待评估'] || '#78909c' }">{{ c.suitabilityLevel || '待评估' }}</span>
                  <span v-if="c.totalScore != null" class="candidate-tag" :style="{ color: scoreColor(Number(c.totalScore)) }">得分 {{ Number(c.totalScore).toFixed(1) }}</span>
                  <span v-if="c.topsisScore != null" class="candidate-tag" style="color:#D4944A">TOPSIS {{ Number(c.topsisScore).toFixed(3) }}</span>
                </div>
              </div>
              <el-button size="small" text type="danger" :icon="Delete" class="del-btn" @click.stop="removeCandidate(c.id)"></el-button>
            </div>
            <div v-if="!candidates.length" class="no-data">暂无候选点，点击地图添加或自动生成</div>
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
      <el-form :model="addForm" label-width="110px" size="small">
        <el-form-item label="候选点名称">
          <el-input v-model="addForm.candidateName" placeholder="留空自动命名" />
        </el-form-item>
        <el-form-item label="所属乡镇">
          <el-select v-model="addForm.township" placeholder="选择乡镇" clearable filterable style="width:100%">
            <el-option v-for="t in townOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属村">
          <el-input v-model="addForm.village" placeholder="所在村名" />
        </el-form-item>
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
      </el-form>
      <template #footer>
        <el-button size="small" @click="addVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="submitAdd">保存候选点</el-button>
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

    <!-- 算法说明弹窗 -->
    <el-dialog v-model="descVisible" title="烤房选址算法说明" width="640px" destroy-on-close class="detail-dialog" append-to-body align-center>
      <template v-if="algoDesc">
        <div class="algo-name">{{ algoDesc.name }}</div>
        <div class="algo-fullname">{{ algoDesc.fullName }}</div>
        <div class="algo-section">
          <div class="algo-section-title">指标体系（8大一级指标）</div>
          <div class="algo-text">{{ algoDesc.indicators }}</div>
        </div>
        <div class="algo-section">
          <div class="algo-section-title">算法原理</div>
          <div class="algo-text">{{ algoDesc.principle }}</div>
        </div>
        <div class="algo-section">
          <div class="algo-section-title">计算公式</div>
          <pre class="algo-formula">{{ algoDesc.formula }}</pre>
        </div>
        <div class="algo-section">
          <div class="algo-section-title">适宜等级划分</div>
          <div class="algo-text">{{ algoDesc.levels }}</div>
        </div>
      </template>
    </el-dialog>
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
  overflow: hidden;
  color: #b8d4c8;
  perspective: 1200px;
}

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

.map-chart-wrap { flex: 1; min-height: 0; position: relative; overflow: hidden; }
.map-chart { width: 100%; height: 100%; }

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
.side-section:first-child { flex: 0 0 auto; max-height: 220px; }
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

/* 乡镇列表 */
.town-list { overflow-y: auto; padding: 10px 14px; }
.town-list::-webkit-scrollbar { width: 3px; }
.town-list::-webkit-scrollbar-thumb { background: rgba(46, 139, 106, 0.25); border-radius: 2px; }
.town-item { margin-bottom: 10px; }
.town-name { font-size: 13px; color: #7aac96; margin-bottom: 5px; letter-spacing: 0.3px; }
.town-high { font-size: 11px; color: #2E8B6A; margin-left: 6px; }
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
.del-btn { flex-shrink: 0; }
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
</style>
