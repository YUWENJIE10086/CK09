<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getBarnOptions, getHealthStats, getCountyStats } from '@/api/barn'
import { getReservationList } from '@/api/reservation'

const currentTime = ref('')
let timer: ReturnType<typeof setInterval> | null = null

function updateTime() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${y}-${m}-${d} ${h}:${min}:${s}`
}

const healthStats = ref<any>({})
const countyStats = ref<any[]>([])
const barnList = ref<any[]>([])
const reservationList = ref<any[]>([])
const mapLevel = ref<'hubei' | 'yichang' | 'zigui'>('hubei')
const mapRef = ref<HTMLElement>()
const ziguiBoundaryGeoJson = ref<any>(null)
let mapInstance: L.Map | null = null
let barnLayer: L.LayerGroup | null = null
let boundaryLayer: L.LayerGroup | null = null

const maintainCount = computed(() => healthStats.value.maintain ?? healthStats.value.maintenance ?? 0)

const bigCards = computed(() => {
  const s = healthStats.value
  const active = Math.max((s.total ?? 0) - (s.retired ?? 0), 0)
  const alerts = maintainCount.value + (s.urgent ?? 0)
  return [
    { label: '资产总量', value: s.total ?? 0, unit: '座', color: '#7CFFB2', mom: `在役 ${active} 座` },
    { label: '健康优良', value: s.excellent ?? 0, unit: '座', color: '#2DD4A3', mom: `优良率 ${s.total ? (((s.excellent ?? 0) / s.total) * 100).toFixed(1) : 0}%` },
    { label: '风险烤房', value: alerts, unit: '座', color: '#F4C430', mom: `需维护 ${maintainCount.value} 座` },
    { label: '综合评分', value: s.avgScore ?? 0, unit: '分', color: '#70C1B3', mom: `退出 ${s.retired ?? 0} 座` },
  ]
})

const commandStats = computed(() => {
  const total = healthStats.value.total ?? barnList.value.length
  const excellent = healthStats.value.excellent ?? 0
  const urgent = healthStats.value.urgent ?? 0
  const maintain = maintainCount.value
  const active = Math.max(total - (healthStats.value.retired ?? 0), 0)
  return [
    { label: '运行在线率', value: total ? `${((active / total) * 100).toFixed(1)}%` : '0%', tone: 'good' },
    { label: '健康达标率', value: total ? `${((excellent / total) * 100).toFixed(1)}%` : '0%', tone: 'good' },
    { label: '预警占比', value: total ? `${(((urgent + maintain) / total) * 100).toFixed(1)}%` : '0%', tone: urgent ? 'danger' : 'warn' },
    { label: '今日预约', value: reservationList.value.length, tone: 'info' },
  ]
})

const sortedBarnsByRisk = computed(() => {
  return [...barnList.value]
    .map((barn: any) => ({
      ...barn,
      score: Number(barn.healthScore ?? barn.currentHealthScore ?? 0),
      life: Number(barn.predictedLifeYears ?? barn.currentLifeResidual ?? 0),
    }))
    .sort((a: any, b: any) => {
      const aLife = a.life > 0 ? a.life : 99
      const bLife = b.life > 0 ? b.life : 99
      return a.score - b.score || aLife - bLife
    })
})

const healthWarnings = computed(() => sortedBarnsByRisk.value.filter((b: any) => b.score > 0 && b.score < 70).slice(0, 5))

const lifeWarnings = computed(() => sortedBarnsByRisk.value.filter((b: any) => b.life > 0 && b.life <= 5).slice(0, 5))

const countyRanking = computed(() => {
  return countyStats.value
    .map((item: any) => ({
      name: item.county || '未知区域',
      total: item.total ?? item.count ?? 0,
      inUse: item.inUse ?? 0,
      idle: item.idle ?? 0,
      rate: item.total ? Math.round(((item.inUse ?? 0) / item.total) * 100) : 0,
    }))
    .sort((a: any, b: any) => b.total - a.total)
    .slice(0, 6)
})

const repairModules = computed(() => {
  const urgent = healthStats.value.urgent ?? 0
  const maintain = maintainCount.value
  const total = healthStats.value.total ?? 0
  return [
    { title: '健康预警', value: urgent, desc: '急需修复烤房', level: urgent > 0 ? 'red' : 'green' },
    { title: '维护队列', value: maintain, desc: '需纳入年度养护', level: maintain > 0 ? 'yellow' : 'green' },
    { title: '资源调剂', value: reservationList.value.length, desc: '预约与调剂需求', level: reservationList.value.length > total * 0.2 ? 'yellow' : 'blue' },
  ]
})

const progressRows = computed(() => {
  const total = healthStats.value.total ?? barnList.value.length
  const active = Math.max(total - (healthStats.value.retired ?? 0), 0)
  const excellent = healthStats.value.excellent ?? 0
  const alerts = maintainCount.value + (healthStats.value.urgent ?? 0)
  return [
    { label: '烤房建档完成进度', value: total ? Math.min(100, Math.round((active / total) * 100)) : 0 },
    { label: '健康评估完成进度', value: total ? Math.min(100, Math.round(((excellent + alerts) / total) * 100)) : 0 },
    { label: '维护处置完成进度', value: alerts ? Math.max(0, Math.round((maintainCount.value / alerts) * 100)) : 100 },
    { label: '预约调剂响应进度', value: reservationList.value.length ? 82 : 100 },
  ]
})

const eventRows = computed(() => {
  const rows = sortedBarnsByRisk.value.slice(0, 8).map((barn: any, index: number) => ({
    no: index + 1,
    name: barn.barnName || barn.id || '未知烤房',
    area: barn.township || barn.county || '-',
    status: barn.score < 50 ? '高风险' : barn.score < 70 ? '待维护' : '正常',
    level: barn.score < 50 ? 'danger' : barn.score < 70 ? 'warn' : 'good',
    score: Math.round(barn.score || 0),
    life: barn.life > 0 ? `${barn.life.toFixed(1)}年` : '待评估',
    rate: Math.max(8, Math.min(100, Math.round(barn.score || 0))),
  }))
  if (rows.length) return rows
  return [
    { no: 1, name: '暂无烤房风险事件', area: '-', status: '正常', level: 'good', score: 100, life: '充足', rate: 100 },
  ]
})

const areaRows = computed(() => {
  const rows = countyRanking.value.map((item: any, index: number) => ({
    rank: index + 1,
    name: item.name,
    total: item.total,
    inUse: item.inUse,
    idle: item.idle,
    rate: item.rate,
    status: item.rate >= 80 ? '高负荷' : item.rate >= 55 ? '平稳' : '待盘活',
  }))
  if (rows.length) return rows
  return [{ rank: 1, name: '秭归县', total: healthStats.value.total ?? 0, inUse: healthStats.value.excellent ?? 0, idle: healthStats.value.retired ?? 0, rate: 0, status: '待同步' }]
})

const riskRows = computed(() => {
  const merged = [...healthWarnings.value, ...lifeWarnings.value]
  const unique = Array.from(new Map(merged.map((barn: any) => [barn.id || barn.barnName, barn])).values())
  const rows = unique.slice(0, 6).map((barn: any, index: number) => {
    const score = Math.round(barn.score || 0)
    const life = Number(barn.life || 0)
    const level = score < 50 || (life > 0 && life <= 2) ? '紧急' : score < 70 || (life > 0 && life <= 5) ? '跟进' : '观察'
    return {
      no: String(index + 1).padStart(2, '0'),
      name: barn.barnName || barn.id || '未知烤房',
      area: barn.township || barn.county || '-',
      score,
      life: life > 0 ? `${life.toFixed(1)}年` : '待评估',
      level,
      rate: Math.max(8, Math.min(100, score)),
    }
  })
  if (rows.length) return rows
  return [{ no: '01', name: '暂无重点风险', area: '-', score: 100, life: '充足', level: '正常', rate: 100 }]
})

const yichangFeatures = [
  { name: '秭归县', points: [[110.2, 30.9], [110.85, 31.04], [111.04, 30.72], [110.78, 30.38], [110.22, 30.42], [109.98, 30.66]] },
  { name: '夷陵区', points: [[110.95, 31.1], [111.58, 31.05], [111.72, 30.72], [111.18, 30.58], [111.02, 30.84]] },
  { name: '西陵区', points: [[111.18, 30.75], [111.42, 30.74], [111.44, 30.55], [111.22, 30.52], [111.12, 30.62]] },
  { name: '点军区', points: [[111.0, 30.55], [111.26, 30.5], [111.34, 30.22], [110.98, 30.2], [110.84, 30.38]] },
  { name: '伍家岗区', points: [[111.43, 30.64], [111.73, 30.62], [111.77, 30.38], [111.45, 30.38], [111.32, 30.5]] },
  { name: '猇亭区', points: [[111.38, 30.34], [111.78, 30.34], [111.84, 30.08], [111.48, 30.02], [111.28, 30.16]] },
  { name: '宜都市', points: [[110.78, 30.18], [111.28, 30.12], [111.26, 29.74], [110.72, 29.68], [110.52, 29.92]] },
  { name: '枝江市', points: [[111.72, 30.36], [112.35, 30.26], [112.42, 29.88], [111.84, 29.86], [111.66, 30.08]] },
  { name: '当阳市', points: [[111.55, 30.92], [112.28, 30.9], [112.34, 30.42], [111.72, 30.42], [111.62, 30.68]] },
  { name: '远安县', points: [[111.4, 31.42], [112.05, 31.34], [112.18, 30.94], [111.56, 31.0], [111.28, 31.18]] },
  { name: '兴山县', points: [[110.55, 31.46], [111.28, 31.34], [111.18, 30.98], [110.72, 31.02], [110.3, 31.22]] },
  { name: '长阳土家族自治县', points: [[110.42, 30.36], [111.18, 30.18], [111.1, 29.62], [110.34, 29.62], [109.96, 29.92]] },
  { name: '五峰土家族自治县', points: [[110.78, 29.62], [111.6, 29.76], [111.52, 29.22], [110.64, 29.12], [110.22, 29.38]] },
]

const hubeiFeatures = [
  { name: '宜昌市', points: [[109.65, 31.7], [112.55, 31.45], [112.8, 29.85], [111.6, 29.0], [109.45, 29.25], [108.95, 30.35]] },
  { name: '恩施州', points: [[108.0, 31.25], [109.55, 31.35], [109.35, 29.25], [108.45, 29.1], [107.55, 29.75]] },
  { name: '荆州市', points: [[111.1, 29.55], [113.7, 29.58], [113.82, 30.48], [112.6, 30.72], [111.4, 30.28]] },
  { name: '荆门市', points: [[111.75, 31.4], [113.35, 31.38], [113.5, 30.5], [112.48, 30.58], [111.65, 30.9]] },
  { name: '襄阳市', points: [[110.55, 32.55], [112.6, 32.68], [113.0, 31.48], [111.65, 31.36], [110.28, 31.72]] },
  { name: '十堰市', points: [[109.0, 33.2], [111.1, 33.05], [110.95, 31.8], [109.42, 31.55], [108.6, 32.15]] },
  { name: '随州市', points: [[112.62, 32.15], [114.0, 32.02], [113.8, 31.28], [112.9, 31.32]] },
  { name: '孝感市', points: [[113.65, 31.35], [114.62, 31.25], [114.55, 30.65], [113.42, 30.56]] },
  { name: '武汉市', points: [[113.95, 30.82], [114.78, 30.86], [114.9, 30.22], [114.0, 30.18], [113.72, 30.48]] },
  { name: '黄冈市', points: [[114.78, 31.2], [116.15, 31.0], [116.25, 30.0], [114.92, 30.1]] },
  { name: '黄石市', points: [[114.75, 30.25], [115.35, 30.22], [115.28, 29.78], [114.72, 29.82]] },
  { name: '咸宁市', points: [[113.65, 30.18], [114.95, 30.05], [114.78, 29.08], [113.55, 29.18]] },
]

const ziguiFeatures = [
  { name: '茅坪镇', points: [[110.62, 30.95], [110.86, 30.92], [110.88, 30.68], [110.62, 30.66], [110.5, 30.78]] },
  { name: '归州镇', points: [[110.34, 30.92], [110.58, 30.94], [110.54, 30.7], [110.26, 30.68], [110.18, 30.82]] },
  { name: '沙镇溪镇', points: [[110.1, 30.72], [110.36, 30.68], [110.34, 30.44], [110.08, 30.4], [109.94, 30.58]] },
  { name: '水田坝乡', points: [[110.4, 30.66], [110.62, 30.64], [110.58, 30.4], [110.34, 30.42]] },
  { name: '郭家坝镇', points: [[110.62, 30.64], [110.92, 30.66], [111.02, 30.42], [110.72, 30.34], [110.58, 30.4]] },
  { name: '两河口镇', points: [[110.22, 31.08], [110.52, 31.12], [110.58, 30.96], [110.34, 30.94]] },
  { name: '泄滩乡', points: [[109.96, 30.58], [110.12, 30.4], [110.02, 30.22], [109.78, 30.34]] },
]

function buildGeoJson(features: { name: string; points: number[][] }[]) {
  return {
    type: 'FeatureCollection',
    features: features.map(item => ({
      type: 'Feature',
      properties: { name: item.name },
      geometry: { type: 'Polygon', coordinates: [[...item.points, item.points[0]]] },
    })),
  }
}

echarts.registerMap('yichang-custom', buildGeoJson(yichangFeatures) as any)
echarts.registerMap('zigui-custom', buildGeoJson(ziguiFeatures) as any)

function getAreaBarnCount(areaName: string) {
  if (areaName === '秭归县') return healthStats.value.total ?? barnList.value.length
  const matched = countyStats.value.find((item: any) => String(item.county || '').includes(areaName) || areaName.includes(String(item.county || '')))
  return matched?.total ?? matched?.count ?? 0
}

function getTownBarnCount(townName: string) {
  const matched = barnList.value.filter((barn: any) => String(barn.township || barn.town || '').includes(townName) || townName.includes(String(barn.township || barn.town || '')))
  return matched.length
}

function getHealthColor(barn: any) {
  const score = Number(barn.healthScore ?? barn.currentHealthScore ?? 0)
  if (score >= 85) return '#48ffd0'
  if (score >= 70) return '#dfff7a'
  if (score >= 50) return '#ffd66b'
  return '#ff716b'
}

function createBarnMarker(barn: any) {
  const color = getHealthColor(barn)
  return L.divIcon({
    className: 'bi-barn-marker',
    html: `<span style="background:${color};box-shadow:0 0 14px ${color};"></span>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7],
  })
}

function renderBiMap() {
  if (!mapInstance || !barnLayer || !boundaryLayer) return
  barnLayer.clearLayers()
  boundaryLayer.clearLayers()

  const ziguiPolygon = ziguiFeatures.flatMap(item => item.points.map(([lng, lat]) => [lat, lng] as [number, number]))
  const yichangPolygon = yichangFeatures.flatMap(item => item.points.map(([lng, lat]) => [lat, lng] as [number, number]))
  if (mapLevel.value === 'hubei') {
    mapInstance.setView([30.9, 112.9], 7)
    hubeiFeatures.forEach((item) => {
      const latLngs = item.points.map(([lng, lat]) => [lat, lng] as [number, number])
      const isYichang = item.name === '宜昌市'
      L.polygon(latLngs, {
        color: isYichang ? '#eafff4' : '#35c98b',
        weight: isYichang ? 2.4 : 1,
        fillColor: isYichang ? '#19d88f' : '#0a4a34',
        fillOpacity: isYichang ? 0.28 : 0.12,
        dashArray: isYichang ? undefined : '4,6',
      })
        .bindTooltip(isYichang ? '宜昌市：点击下钻' : item.name, { permanent: isYichang, direction: 'center', className: 'bi-map-label' })
        .on('click', () => { if (isYichang) mapLevel.value = 'yichang' })
        .addTo(boundaryLayer!)
    })
  } else if (mapLevel.value === 'yichang') {
    mapInstance.setView([30.69, 111.28], 9)
    L.polygon(yichangPolygon, {
      color: '#9affcf',
      weight: 1.8,
      fillColor: '#0c5a3f',
      fillOpacity: 0.08,
    }).addTo(boundaryLayer)
    L.polygon(ziguiPolygon, {
      color: '#f1ffe0',
      weight: 2.6,
      fillColor: '#21d992',
      fillOpacity: 0.2,
    })
      .bindTooltip('秭归县：点击下钻', { permanent: true, direction: 'center', className: 'bi-map-label' })
      .on('click', () => { mapLevel.value = 'zigui' })
      .addTo(boundaryLayer)
  } else {
    mapInstance.setView([30.72, 110.63], 10)
    if (ziguiBoundaryGeoJson.value) {
      L.geoJSON(ziguiBoundaryGeoJson.value, {
        style: {
          color: '#d7ffe8',
          weight: 1.4,
          opacity: 0.95,
          fillColor: '#128b63',
          fillOpacity: 0.1,
          className: 'bi-boundary-glow',
        },
        onEachFeature: (feature, layer) => {
          const props: any = feature.properties || {}
          layer.bindTooltip(`${props.name || props.xiang || '乡镇'} ${getTownBarnCount(props.name || props.xiang || '')}座`, {
            permanent: true,
            direction: 'center',
            className: 'bi-map-label',
          })
        },
      }).addTo(boundaryLayer)
    } else {
      ziguiFeatures.forEach((item) => {
        L.polygon(item.points.map(([lng, lat]) => [lat, lng] as [number, number]), {
          color: '#d7ffe8',
          weight: 1.2,
          fillColor: '#128b63',
          fillOpacity: 0.11,
          className: 'bi-boundary-glow',
        })
          .bindTooltip(`${item.name} ${getTownBarnCount(item.name)}座`, { permanent: true, direction: 'center', className: 'bi-map-label' })
          .addTo(boundaryLayer!)
      })
    }
  }

  barnList.value
    .filter((barn: any) => Number(barn.latitude) > 0 && Number(barn.longitude) > 0)
    .forEach((barn: any) => {
      L.marker([Number(barn.latitude), Number(barn.longitude)], { icon: createBarnMarker(barn) })
        .bindPopup(`
          <b>${barn.barnName || barn.id || '烤房'}</b><br/>
          区县：${barn.county || '-'}<br/>
          乡镇：${barn.township || '-'}<br/>
          健康分：${barn.healthScore ?? barn.currentHealthScore ?? '-'}
        `)
        .addTo(barnLayer!)
    })

  setTimeout(() => mapInstance?.invalidateSize(), 80)
}

function initBiMap() {
  if (!mapRef.value || mapInstance) return
  mapInstance = L.map(mapRef.value, {
    center: [30.9, 112.9],
    zoom: 7,
    zoomControl: false,
    attributionControl: false,
    preferCanvas: true,
  })
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    minZoom: 7,
  }).addTo(mapInstance)
  boundaryLayer = L.layerGroup().addTo(mapInstance)
  barnLayer = L.layerGroup().addTo(mapInstance)
  renderBiMap()
}

async function loadBoundaryGeoJson() {
  try {
    const res = await fetch('/map/zigui-township-boundaries.geojson')
    ziguiBoundaryGeoJson.value = await res.json()
    renderBiMap()
  } catch (e) {
    console.warn('秭归边界数据加载失败，使用内置概览轮廓', e)
  }
}

function backToYichang() {
  mapLevel.value = mapLevel.value === 'zigui' ? 'yichang' : 'hubei'
}

const mapChartOption = computed(() => {
  const countyMap = new Map<string, { lngs: number[]; lats: number[]; count: number }>()
  barnList.value.forEach(barn => {
    const name = barn.county || '未知'
    if (!countyMap.has(name)) countyMap.set(name, { lngs: [], lats: [], count: 0 })
    const entry = countyMap.get(name)!
    if (barn.longitude && barn.latitude) {
      entry.lngs.push(Number(barn.longitude))
      entry.lats.push(Number(barn.latitude))
    }
    entry.count++
  })

  const scatterData: { name: string; value: number[] }[] = []
  let minLng = 200, maxLng = 0, minLat = 200, maxLat = 0
  countyMap.forEach((val, name) => {
    if (val.lngs.length === 0) return
    const avgLng = val.lngs.reduce((a, b) => a + b, 0) / val.lngs.length
    const avgLat = val.lats.reduce((a, b) => a + b, 0) / val.lats.length
    scatterData.push({ name, value: [Number(avgLng.toFixed(4)), Number(avgLat.toFixed(4)), val.count] })
    minLng = Math.min(minLng, avgLng)
    maxLng = Math.max(maxLng, avgLng)
    minLat = Math.min(minLat, avgLat)
    maxLat = Math.max(maxLat, avgLat)
  })
  if (scatterData.length === 0) {
    minLng = 110; maxLng = 111; minLat = 30; maxLat = 31
  }
  const lngPad = Math.max((maxLng - minLng) * 0.15, 0.1)
  const latPad = Math.max((maxLat - minLat) * 0.15, 0.1)
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(7,20,13,0.96)',
      borderColor: 'rgba(124,255,178,0.35)',
      borderWidth: 1,
      textStyle: { color: '#E7FFF0', fontSize: 13 },
      formatter: (params: any) => `<b>${params.name}</b><br/>烤房数量：${params.value[2]} 座`,
    },
    backgroundColor: 'transparent',
    xAxis: {
      type: 'value',
      min: Number((minLng - lngPad).toFixed(2)),
      max: Number((maxLng + lngPad).toFixed(2)),
      axisLabel: { color: '#8AB4A0', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(124,255,178,0.06)', type: 'dashed' } },
      axisLine: { lineStyle: { color: 'rgba(124,255,178,0.16)' } },
    },
    yAxis: {
      type: 'value',
      min: Number((minLat - latPad).toFixed(2)),
      max: Number((maxLat + latPad).toFixed(2)),
      axisLabel: { color: '#8AB4A0', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(124,255,178,0.06)', type: 'dashed' } },
      axisLine: { lineStyle: { color: 'rgba(124,255,178,0.16)' } },
    },
    series: [
      {
        type: 'effectScatter',
        symbolSize: (val: number[]) => Math.max(14, val[2] / 6),
        data: scatterData,
        showEffectOn: 'render',
        rippleEffect: { brushType: 'stroke', scale: 3, period: 4 },
        itemStyle: {
          color: (params: any) => ['#7CFFB2', '#2DD4A3', '#F4C430', '#70C1B3', '#C0F8D9'][params.dataIndex % 5],
          shadowBlur: 16,
          shadowColor: 'rgba(124,255,178,0.45)',
        },
        label: {
          show: true,
          formatter: '{b}',
          position: 'right',
          color: '#E7FFF0',
          fontSize: 11,
          fontWeight: 600,
        },
      },
    ],
  }
})

const healthRingOption = computed(() => {
  const s = healthStats.value
  const data = [
    { name: '优良', value: s.excellent ?? 0, color1: '#2DD4A3', color2: '#0E8F63' },
    { name: '需维护', value: maintainCount.value, color1: '#F4C430', color2: '#B8860B' },
    { name: '急需修复', value: s.urgent ?? 0, color1: '#F56565', color2: '#C53030' },
    { name: '退出', value: s.retired ?? 0, color1: '#7B8A99', color2: '#55606C' },
  ]
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(7,20,13,0.96)',
      borderColor: 'rgba(124,255,178,0.35)',
      borderWidth: 1,
      textStyle: { color: '#E7FFF0', fontSize: 13 },
      formatter: '{b}: {c} ({d}%)',
    },
    backgroundColor: 'transparent',
    legend: {
      orient: 'vertical',
      right: 0,
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#b6f8d4', fontSize: 11 },
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '72%'],
        center: ['35%', '54%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 4, borderColor: '#07140D', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#E7FFF0' } },
        data: data.map(item => ({
          name: item.name,
          value: item.value,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
              { offset: 0, color: item.color1 },
              { offset: 1, color: item.color2 },
            ]),
          },
        })),
      },
    ],
  }
})

const fundBarOption = computed(() => {
  const counties = countyStats.value.map(item => item.county)
  const inUseData = countyStats.value.map(item => item.inUse ?? 0)
  const idleData = countyStats.value.map(item => item.idle ?? 0)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(7,20,13,0.96)',
      borderColor: 'rgba(124,255,178,0.35)',
      borderWidth: 1,
      textStyle: { color: '#E7FFF0', fontSize: 13 },
    },
    backgroundColor: 'transparent',
    legend: {
      data: ['已使用', '剩余'],
      top: 0,
      right: 0,
      textStyle: { color: '#A7C8B8', fontSize: 12 },
    },
    grid: { left: 34, right: 10, bottom: 18, top: 28 },
    xAxis: {
      type: 'category',
      data: counties,
      axisLabel: { color: '#b6f8d4', fontSize: 9, rotate: 15 },
      axisLine: { lineStyle: { color: 'rgba(124,255,178,0.16)' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#b6f8d4', fontSize: 9 },
      splitLine: { lineStyle: { color: 'rgba(124,255,178,0.06)', type: 'dashed' } },
    },
    series: [
      {
        name: '已使用',
        type: 'bar',
        stack: 'fund',
        barWidth: 12,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#7CFFB2' },
            { offset: 1, color: '#145A32' },
          ]),
        },
        data: inUseData,
      },
      {
        name: '剩余',
        type: 'bar',
        stack: 'fund',
        barWidth: 12,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#2DD4A3' },
            { offset: 1, color: '#0B4330' },
          ]),
          borderRadius: [3, 3, 0, 0],
        },
        data: idleData,
      },
    ],
  }
})

const trendLineOption = computed(() => {
  const monthMap = new Map<string, number>()
  const monthLabels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  monthLabels.forEach(m => monthMap.set(m, 0))
  reservationList.value.forEach((r: any) => {
    const dateStr = r.reserveStart || r.createdAt || ''
    if (!dateStr) return
    const month = new Date(dateStr).getMonth()
    const label = monthLabels[month]
    if (label) monthMap.set(label, (monthMap.get(label) || 0) + 1)
  })
  const data = monthLabels.map(m => monthMap.get(m) || 0)
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(7,20,13,0.96)',
      borderColor: 'rgba(124,255,178,0.35)',
      borderWidth: 1,
      textStyle: { color: '#E7FFF0', fontSize: 13 },
    },
    backgroundColor: 'transparent',
    legend: {
      data: ['预约数'],
      top: 0,
      right: 0,
      textStyle: { color: '#A7C8B8', fontSize: 12 },
    },
    grid: { left: 28, right: 10, bottom: 18, top: 28 },
    xAxis: {
      type: 'category',
      data: monthLabels,
      boundaryGap: false,
      axisLabel: { color: '#b6f8d4', fontSize: 9 },
      axisLine: { lineStyle: { color: 'rgba(124,255,178,0.16)' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#b6f8d4', fontSize: 9 },
      splitLine: { lineStyle: { color: 'rgba(124,255,178,0.06)', type: 'dashed' } },
    },
    series: [
      {
        name: '预约数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2.5, color: '#7CFFB2' },
        itemStyle: { color: '#7CFFB2' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(124,255,178,0.25)' },
            { offset: 1, color: 'rgba(124,255,178,0.02)' },
          ]),
        },
        data,
      },
    ],
  }
})

const radarOption = computed(() => {
  const s = healthStats.value
  const avgScore = s.avgScore ?? 0
  const base = Math.min(avgScore, 100)
  return {
    tooltip: {
      backgroundColor: 'rgba(7,20,13,0.96)',
      borderColor: 'rgba(124,255,178,0.35)',
      borderWidth: 1,
      textStyle: { color: '#E7FFF0', fontSize: 13 },
    },
    backgroundColor: 'transparent',
    radar: {
      indicator: [
        { name: '加热系统', max: 100 },
        { name: '散热系统', max: 100 },
        { name: '装炕系统', max: 100 },
        { name: '自控系统', max: 100 },
        { name: '风机系统', max: 100 },
      ],
      shape: 'polygon',
      splitNumber: 4,
      axisName: { color: '#A7C8B8', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(124,255,178,0.1)' } },
      splitArea: { areaStyle: { color: ['rgba(124,255,178,0.02)', 'rgba(124,255,178,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(124,255,178,0.16)' } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [
              Math.min(100, Math.round(base * 1.02)),
              Math.min(100, Math.round(base * 0.88)),
              Math.min(100, Math.round(base * 0.95)),
              Math.min(100, Math.round(base * 0.82)),
              Math.min(100, Math.round(base * 0.9)),
            ],
            name: '部件健康评分',
            lineStyle: { color: '#7CFFB2', width: 2 },
            itemStyle: { color: '#7CFFB2' },
            areaStyle: { color: 'rgba(124,255,178,0.16)' },
          },
        ],
      },
    ],
  }
})

const warningMessages = computed(() => {
  const msgs: string[] = []
  const urgentBarns = barnList.value.filter((b: any) => b.healthLevel === '急需修复')
  urgentBarns.forEach((barn: any) => {
    msgs.push(`⚠️ ${barn.county || ''}${barn.township || ''}${barn.id || ''}急需修复，健康评分${barn.healthScore ?? '未知'}分`)
  })
  const maintainBarns = barnList.value.filter((b: any) => b.healthLevel === '需维护')
  maintainBarns.slice(0, 5).forEach((barn: any) => {
    msgs.push(`🔧 ${barn.county || ''}${barn.township || ''}${barn.id || ''}需维护，健康评分${barn.healthScore ?? '未知'}分`)
  })
  if (msgs.length === 0) msgs.push('✅ 当前无紧急预警信息，所有烤房运行正常')
  return msgs
})

const scrollText = computed(() => warningMessages.value.join('　　　│　　　'))

async function loadData() {
  try {
    const [healthRes, countyRes, barnRes, reservationRes] = await Promise.allSettled([
      getHealthStats(),
      getCountyStats(),
      getBarnOptions(),
      getReservationList({ pageNum: 1, pageSize: 500 }),
    ])
    if (healthRes.status === 'fulfilled') healthStats.value = healthRes.value || {}
    if (countyRes.status === 'fulfilled') countyStats.value = countyRes.value || []
    if (barnRes.status === 'fulfilled') barnList.value = barnRes.value || []
    if (reservationRes.status === 'fulfilled') {
      const resData = reservationRes.value
      reservationList.value = resData?.rows || resData || []
    }
  } catch (e) {
    console.error('Dashboard数据加载失败', e)
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  loadData()
  loadBoundaryGeoJson()
  nextTick(() => initBiMap())
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})

watch([mapLevel, barnList, ziguiBoundaryGeoJson], () => {
  nextTick(() => renderBiMap())
})
</script>

<template>
  <div class="bi-dashboard">
    <header class="bi-header">
      <div class="header-brand">
        <div class="header-title">宜昌市烤房数智运营中心</div>
      </div>
      <div class="header-center">
        <span>资源态势</span>
        <i></i>
        <span>健康诊断</span>
        <i></i>
        <span>寿命预警</span>
        <i></i>
        <span>投建推演</span>
      </div>
      <div class="header-right">
        <div class="header-time">{{ currentTime }}</div>
      </div>
    </header>

    <div class="bi-body">
      <section class="fine-screen">
        <aside class="fine-column">
          <div class="screen-panel metric-panel">
          <div class="panel-head">
            <div class="panel-title">运行作业态势</div>
            <div class="panel-tools"><i></i><i></i></div>
          </div>
          <div class="progress-list">
            <div v-for="item in progressRows" :key="item.label" class="progress-row">
              <div class="progress-meta">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}%</strong>
              </div>
              <div class="progress-track"><i :style="{ width: item.value + '%' }"></i></div>
            </div>
          </div>
        </div>

        <div class="screen-panel">
          <div class="panel-head">
            <div class="panel-title">发起申请趋势</div>
            <div class="panel-tools"><i></i><i></i></div>
          </div>
          <VChart :option="trendLineOption" autoresize class="panel-chart small-chart" />
        </div>

        <div class="screen-panel">
          <div class="panel-head">
            <div class="panel-title">健康等级占比</div>
            <div class="panel-tools"><i></i><i></i></div>
          </div>
          <VChart :option="healthRingOption" autoresize class="panel-chart small-chart" />
        </div>

        <div class="screen-panel">
          <div class="panel-head">
            <div class="panel-title">设备状态统计</div>
            <div class="panel-tools"><i></i><i></i></div>
          </div>
          <div class="status-donut">
            <div class="donut-core">{{ healthStats.total ?? 0 }}</div>
            <div class="status-legend">
              <span><i class="green"></i>正常 {{ healthStats.excellent ?? 0 }}</span>
              <span><i class="yellow"></i>维护 {{ maintainCount }}</span>
              <span><i class="red"></i>预警 {{ healthStats.urgent ?? 0 }}</span>
            </div>
          </div>
          <div class="mini-rank">
            <div v-for="row in areaRows.slice(0, 3)" :key="row.name" class="mini-rank-row">
              <span>{{ row.rank }}</span>
              <strong>{{ row.name }}</strong>
              <em>{{ row.inUse }}/{{ row.total }}</em>
              <i :style="{ width: row.rate + '%' }"></i>
            </div>
          </div>
        </div>
        </aside>

        <main class="fine-center">
          <div class="center-summary">
            <div v-for="item in commandStats" :key="'top-' + item.label" class="summary-chip">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
        </div>

          <div class="holo-stage">
            <div class="scan-ring ring-a"></div>
            <div class="scan-ring ring-b"></div>
            <div class="fine-orbit orbit-a"></div>
            <div class="fine-orbit orbit-b"></div>
            <div class="map-toolbar">
              <strong>{{ mapLevel === 'hubei' ? '湖北省' : mapLevel === 'yichang' ? '宜昌市' : '秭归县' }}</strong>
              <button v-if="mapLevel !== 'hubei'" type="button" @click="backToYichang">
                {{ mapLevel === 'zigui' ? '返回宜昌市' : '返回湖北省' }}
              </button>
            </div>
            <div ref="mapRef" class="city-map"></div>
            <div class="map-hint">
              {{ mapLevel === 'hubei' ? '点击「宜昌市」下钻查看市域烤房态势' : mapLevel === 'yichang' ? '点击「秭归县」下钻查看乡镇烤房分布' : '秭归县乡镇烤房分布' }}
            </div>
            <div class="holo-title">
              <strong>{{ mapLevel === 'hubei' ? '湖北省烤房态势地图' : mapLevel === 'yichang' ? '宜昌市烤房态势地图' : '秭归县烤房态势地图' }}</strong>
              <span>健康评分 {{ healthStats.avgScore ?? 0 }} · 风险烤房 {{ maintainCount + (healthStats.urgent ?? 0) }} · 数据库实时统计</span>
            </div>
          </div>

          <div class="bottom-console">
            <div v-for="card in bigCards" :key="'console-' + card.label">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}<small>{{ card.unit }}</small></strong>
            </div>
          </div>
        </main>

        <aside class="fine-column">
          <div class="screen-panel">
          <div class="panel-head">
            <div class="panel-title">维修收益分析</div>
            <div class="panel-tools"><i></i><i></i></div>
          </div>
          <VChart :option="fundBarOption" autoresize class="panel-chart small-chart" />
        </div>

        <div class="screen-panel">
          <div class="panel-head">
            <div class="panel-title">设备预警分析</div>
            <div class="panel-tools"><i></i><i></i></div>
          </div>
          <div class="risk-table">
            <div class="risk-head">
              <span>等级</span><span>烤房/区域</span><span>健康</span><span>寿命</span>
            </div>
            <div class="risk-body">
              <div v-for="row in riskRows" :key="row.no + row.name" class="risk-data-row">
                <span class="risk-badge">{{ row.level }}</span>
                <div class="risk-name">
                  <strong>{{ row.name }}</strong>
                  <em>{{ row.area }}</em>
                </div>
                <div class="risk-score">
                  <span>{{ row.score }}分</span>
                  <i><b :style="{ width: row.rate + '%' }"></b></i>
                </div>
                <span class="risk-life">{{ row.life }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="screen-panel">
          <div class="panel-head">
            <div class="panel-title">部件诊断分析</div>
            <div class="panel-tools"><i></i><i></i></div>
          </div>
          <VChart :option="radarOption" autoresize class="panel-chart small-chart" />
        </div>

        <div class="screen-panel">
          <div class="panel-head">
            <div class="panel-title">运营事件</div>
            <div class="panel-tools"><i></i><i></i></div>
          </div>
          <div class="event-table">
            <div class="event-head">
              <span>序号</span><span>烤房/区域</span><span>状态</span><span>健康评分</span><span>剩余寿命</span>
            </div>
            <div class="event-body">
              <div v-for="row in eventRows" :key="row.no + row.name" class="event-row">
                <span>{{ row.no }}</span>
                <span class="event-name"><strong>{{ row.name }}</strong><em>{{ row.area }}</em></span>
                <span :class="['event-tag', row.level]">{{ row.status }}</span>
                <span class="event-progress"><i><b :style="{ width: row.rate + '%' }"></b></i>{{ row.score }}</span>
                <strong>{{ row.life }}</strong>
              </div>
            </div>
          </div>
        </div>
        </aside>
      </section>

      <footer class="bi-footer">
        <div class="footer-label">运行预警</div>
        <div class="footer-scroll">
          <div class="scroll-content">{{ scrollText }}</div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.bi-dashboard {
  position: relative;
  width: 100%;
  height: calc(100vh - 108px);
  min-height: 680px;
  overflow: hidden;
  color: #dffef4;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(ellipse at center, rgba(18, 138, 91, 0.2) 0%, rgba(4, 32, 22, 0.78) 48%, #020a07 78%),
    linear-gradient(180deg, #03150f 0%, #02110c 52%, #000604 100%);
}
.bi-dashboard::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(92, 255, 181, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(92, 255, 181, 0.04) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(circle at center, #000 0%, transparent 72%);
  pointer-events: none;
}
.bi-dashboard::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 0%, rgba(36, 255, 165, 0.055) 50%, transparent 100%);
  animation: pageScan 5.6s linear infinite;
  pointer-events: none;
}
.bi-header {
  position: relative;
  z-index: 2;
  height: 54px;
  display: grid;
  grid-template-columns: 330px minmax(420px, 1fr) 210px;
  align-items: center;
  padding: 0 22px;
  background: linear-gradient(180deg, rgba(3, 31, 22, 0.96), rgba(1, 18, 13, 0.9));
  border-bottom: 1px solid rgba(47, 255, 171, 0.3);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35), inset 0 -1px 22px rgba(47, 255, 171, 0.1);
}
.bi-header::after {
  content: '';
  position: absolute;
  left: 15%;
  right: 15%;
  top: 0;
  height: 44px;
  opacity: 0.82;
  background:
    radial-gradient(ellipse at center, rgba(112, 255, 190, 0.36) 0%, transparent 58%),
    linear-gradient(96deg, transparent 0%, rgba(25, 210, 129, 0.22) 22%, rgba(213, 255, 116, 0.5) 50%, rgba(25, 210, 129, 0.22) 78%, transparent 100%);
  filter: blur(1px);
  animation: ribbonFlow 6s ease-in-out infinite;
  pointer-events: none;
}
.bi-header::before {
  content: '';
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: -1px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #18f2a4, #d9ffe9, #18f2a4, transparent);
}
.header-title {
  position: relative;
  z-index: 1;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
  color: #f2fff9;
  text-shadow: 0 0 18px rgba(82, 255, 184, 0.72);
}
.header-center {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: rgba(210, 255, 232, 0.78);
  font-size: 13px;
}

.header-center i {
  width: 34px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(54, 255, 174, 0.86), transparent);
  box-shadow: 0 0 8px rgba(54, 255, 174, 0.72);
}
.header-right {
  display: flex;
  justify-content: flex-end;
  position: relative;
  z-index: 1;
}
.header-time {
  font-size: 13px;
  color: #d7ffe8;
  font-variant-numeric: tabular-nums;
}
.bi-body {
  position: relative;
  z-index: 1;
  height: calc(100% - 54px);
  padding: 10px 14px 12px;
  display: grid;
  grid-template-rows: minmax(0, 1fr) 34px;
  gap: 10px;
}
.fine-screen {
  min-height: 0;
  display: grid;
  grid-template-columns: 30% minmax(360px, 40%) 30%;
  gap: 12px;
}
.fine-column {
  min-height: 0;
  display: grid;
  grid-template-rows: 1.15fr 1fr 1fr 1fr;
  gap: 10px;
}
.fine-center {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: 50px minmax(0, 1fr) 54px;
  gap: 10px;
}
.screen-panel {
  position: relative;
  min-height: 0;
  padding: 10px 12px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(5, 51, 35, 0.76), rgba(1, 16, 11, 0.9));
  border: 1px solid rgba(66, 255, 178, 0.28);
  box-shadow: inset 0 0 24px rgba(74, 255, 181, 0.1), 0 0 22px rgba(0, 0, 0, 0.32);
  border-bottom-color: transparent;
  clip-path: polygon(10px 0, 100% 0, 100% 100%, 0 100%, 0 10px);
}
.screen-panel::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 5px;
  background: linear-gradient(90deg, transparent 0%, rgba(45, 255, 172, 0.88) 14%, rgba(118, 255, 202, 0.34) 48%, rgba(45, 255, 172, 0.88) 86%, transparent 100%);
  filter: blur(0.2px);
  animation: panelTopFlow 4.8s linear infinite;
}
.screen-panel::after {
  content: '';
  position: absolute;
  left: 8px;
  right: 8px;
  top: 26px;
  height: 24px;
  opacity: 0.72;
  background:
    linear-gradient(90deg, rgba(213, 255, 116, 0.72), rgba(50, 255, 174, 0.18) 24%, transparent 66%),
    linear-gradient(90deg, transparent, rgba(50, 255, 174, 0.22), transparent);
  filter: blur(3px);
  pointer-events: none;
}
.panel-head {
  position: relative;
  z-index: 1;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.panel-title {
  position: relative;
  font-size: 13px;
  font-weight: 800;
  color: #eafff4;
  text-shadow: 0 0 12px rgba(72, 255, 181, 0.5);
  padding-left: 12px;
}
.panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 5px;
  height: 5px;
  background: #eafff4;
  box-shadow: 0 0 8px #d7ff74, 0 0 14px rgba(45, 255, 172, 0.72);
  transform: translateY(-50%);
}
.panel-tools { display: flex; gap: 5px; }
.panel-tools i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2dffb1;
  box-shadow: 0 0 8px #2dffb1;
}
.panel-chart {
  width: 100%;
  height: calc(100% - 28px);
  min-height: 0;
}
.small-chart { height: calc(100% - 30px); }
.quad-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  height: calc(100% - 28px);
}
.quad-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 6px 8px;
  background: linear-gradient(135deg, rgba(10, 78, 75, 0.62), rgba(3, 28, 31, 0.72));
  border: 1px solid rgba(61, 255, 216, 0.14);
}
.quad-item span, .quad-item em {
  font-size: 11px;
  color: rgba(220, 255, 247, 0.62);
  font-style: normal;
}
.quad-item strong {
  margin: 4px 0;
  font-size: 24px;
  line-height: 1;
  font-weight: 900;
  text-shadow: 0 0 12px currentColor;
}
.quad-item small { margin-left: 2px; font-size: 11px; }

.progress-list {
  height: calc(100% - 30px);
  display: grid;
  align-content: space-evenly;
  gap: 8px;
}

.progress-row {
  display: grid;
  gap: 6px;
}

.progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: rgba(218, 255, 235, 0.72);
  font-size: 12px;
}

.progress-meta strong {
  color: #d7ffe8;
  font-size: 12px;
}

.progress-track {
  height: 7px;
  overflow: hidden;
  border-radius: 99px;
  background: rgba(25, 112, 77, 0.34);
  box-shadow: inset 0 0 8px rgba(0, 0, 0, 0.38);
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #13d98c, #c8ffd9);
  box-shadow: 0 0 12px rgba(132, 255, 190, 0.62);
}
.status-donut {
  height: 58%;
  display: grid;
  grid-template-columns: 88px 1fr;
  align-items: center;
  gap: 12px;
}
.donut-core {
  width: 78px;
  height: 78px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 21px;
  font-weight: 900;
  color: #9effdf;
  background: conic-gradient(#22f7d3 0 62%, rgba(31, 107, 105, 0.35) 62% 100%);
  box-shadow: 0 0 20px rgba(34, 247, 211, 0.32), inset 0 0 0 10px #031113;
}
.status-legend { display: grid; gap: 6px; font-size: 11px; color: rgba(220, 255, 247, 0.76); }
.status-legend span { display: flex; align-items: center; gap: 6px; }
.status-legend i { width: 7px; height: 7px; border-radius: 50%; }
.status-legend .green { background: #66ffd0; }
.status-legend .yellow { background: #ffe16b; }
.status-legend .red { background: #ff716b; }
.mini-rank {
  height: calc(42% - 8px);
  display: grid;
  gap: 5px;
  overflow: hidden;
}
.mini-rank-row {
  position: relative;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) 54px;
  align-items: center;
  gap: 6px;
  min-height: 20px;
  padding: 0 6px;
  overflow: hidden;
  color: rgba(226, 255, 239, 0.78);
  font-size: 10px;
  background: rgba(7, 55, 37, 0.52);
  border: 1px solid rgba(75, 255, 173, 0.12);
}
.mini-rank-row span { color: #d9ffe9; font-weight: 900; }
.mini-rank-row strong, .mini-rank-row em {
  position: relative;
  z-index: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-style: normal;
}
.mini-rank-row i {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 0;
  background: linear-gradient(90deg, rgba(21, 232, 147, 0.34), rgba(215, 255, 116, 0.08));
}
.center-summary, .bottom-console {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.summary-chip, .bottom-console div {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  background: linear-gradient(180deg, rgba(4, 46, 50, 0.82), rgba(3, 19, 22, 0.88));
  border: 1px solid rgba(61, 255, 216, 0.18);
  clip-path: polygon(12px 0, 100% 0, calc(100% - 12px) 100%, 0 100%);
}
.summary-chip span, .bottom-console span { font-size: 11px; color: rgba(220, 255, 247, 0.62); }
.summary-chip strong, .bottom-console strong { font-size: 18px; color: #8ffff0; text-shadow: 0 0 12px rgba(34, 247, 211, 0.5); }
.bottom-console small { font-size: 10px; margin-left: 2px; }
.holo-stage {
  position: relative;
  min-height: 0;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 58%, rgba(18, 255, 205, 0.22) 0%, rgba(18, 255, 205, 0.05) 38%, transparent 64%),
    linear-gradient(180deg, rgba(3, 25, 27, 0.25), rgba(0, 5, 6, 0.18));
}

.city-map {
  position: absolute;
  z-index: 3;
  inset: 42px 28px 78px;
}

.city-map :deep(.leaflet-container) {
  background: rgba(1, 14, 16, 0.95);
  font-family: inherit;
}

.city-map :deep(.leaflet-tile-pane) {
  filter: invert(0.92) hue-rotate(165deg) brightness(0.78) contrast(1.18) saturate(0.75);
}

.city-map :deep(.leaflet-control-attribution) {
  display: none;
}

.city-map :deep(.leaflet-popup-content-wrapper) {
  color: #dffef4;
  background: rgba(2, 20, 22, 0.96);
  border: 1px solid rgba(34, 247, 211, 0.22);
}

.city-map :deep(.leaflet-popup-tip) {
  background: rgba(2, 20, 22, 0.96);
}

.city-map :deep(.bi-barn-marker) {
  background: transparent;
  border: 0;
}

.city-map :deep(.bi-barn-marker span) {
  display: block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.72);
}

.city-map :deep(.bi-map-label) {
  color: #eafff8;
  background: rgba(2, 24, 26, 0.68);
  border: 1px solid rgba(34, 247, 211, 0.24);
  box-shadow: 0 0 10px rgba(34, 247, 211, 0.18);
  font-size: 11px;
  font-weight: 800;
}

.city-map :deep(.bi-boundary-glow) {
  filter: drop-shadow(0 0 4px rgba(215, 255, 232, 0.9)) drop-shadow(0 0 12px rgba(45, 255, 172, 0.68));
  animation: boundaryPulse 3.8s ease-in-out infinite;
}

.map-toolbar {
  position: absolute;
  z-index: 5;
  left: 28px;
  top: 18px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.map-toolbar strong {
  font-size: 16px;
  color: #effff8;
  text-shadow: 0 0 14px rgba(46, 255, 219, 0.7);
}

.map-toolbar button {
  height: 26px;
  padding: 0 10px;
  color: #061315;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  border: 0;
  background: linear-gradient(180deg, #9effdf, #16d7c3);
  clip-path: polygon(8px 0, 100% 0, calc(100% - 8px) 100%, 0 100%);
}

.map-hint {
  position: absolute;
  z-index: 5;
  right: 28px;
  top: 22px;
  font-size: 12px;
  color: rgba(214, 255, 244, 0.7);
}
.holo-stage::before {
  content: '';
  position: absolute;
  left: 7%; right: 7%; top: 15%; bottom: 10%;
  border: 1px solid rgba(71, 255, 224, 0.18);
  clip-path: polygon(8% 0, 92% 0, 100% 14%, 100% 82%, 92% 100%, 8% 100%, 0 82%, 0 14%);
}
.holo-stage::after {
  content: '';
  position: absolute;
  left: 14%; right: 14%; top: 24%; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(113, 255, 228, 0.88), transparent);
  box-shadow: 0 0 16px rgba(113, 255, 228, 0.8);
  animation: scanLine 4.2s ease-in-out infinite;
}
.scan-ring, .fine-orbit {
  position: absolute;
  left: 50%; top: 58%;
  border-radius: 50%;
  transform: translate(-50%, -50%) rotateX(68deg);
}
.scan-ring {
  width: 58%;
  aspect-ratio: 1;
  border: 1px solid rgba(103, 255, 222, 0.34);
  box-shadow: inset 0 0 38px rgba(31, 255, 210, 0.12), 0 0 35px rgba(31, 255, 210, 0.18);
  animation: ringPulse 4.8s ease-in-out infinite;
}
.ring-b { width: 38%; animation-delay: 1.2s; }
.fine-orbit {
  width: 72%;
  aspect-ratio: 1 / 0.42;
  border: 1px dashed rgba(103, 255, 222, 0.18);
  animation: orbitRotate 18s linear infinite;
}
.orbit-b { width: 48%; animation-duration: 12s; animation-direction: reverse; }
.holo-title { position: absolute; left: 50%; bottom: 14px; transform: translateX(-50%); text-align: center; }
.holo-title strong { display: block; font-size: 28px; font-weight: 900; color: #f2fff9; text-shadow: 0 0 22px rgba(46, 255, 219, 0.72); }
.holo-title span { display: block; margin-top: 4px; font-size: 12px; color: rgba(190, 255, 236, 0.76); }
.warning-blocks, .warning-list { display: grid; gap: 8px; }
.warning-blocks {
  height: calc(100% - 30px);
  grid-template-rows: 1fr 1fr;
  min-height: 0;
}
.warning-list {
  height: calc(100% - 30px);
  overflow: hidden;
  align-content: start;
}
.warning-block {
  min-height: 0;
  overflow: hidden;
  padding: 8px;
  background: rgba(4, 40, 41, 0.62);
  border: 1px solid rgba(61, 255, 216, 0.12);
}
.warning-block-title { margin-bottom: 6px; font-size: 12px; color: #8ffff0; font-weight: 800; }
.risk-row, .warning-item { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; min-height: 22px; gap: 8px; font-size: 11px; color: rgba(226, 255, 247, 0.82); }
.risk-row span, .warning-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.risk-row strong { color: #ffe06d; }
.empty-line { font-size: 11px; color: rgba(226, 255, 247, 0.48); }
.warning-dot { width: 6px; height: 6px; border-radius: 50%; background: #22f7d3; box-shadow: 0 0 9px #22f7d3; }
.warning-item { grid-template-columns: 8px minmax(0, 1fr); padding: 6px 0; }

.risk-table {
  height: calc(100% - 30px);
  display: grid;
  grid-template-rows: 24px minmax(0, 1fr);
  overflow: hidden;
  font-size: 10px;
}

.risk-head,
.risk-data-row {
  display: grid;
  grid-template-columns: 42px minmax(82px, 1fr) 78px 50px;
  align-items: center;
  gap: 6px;
}

.risk-head {
  color: rgba(210, 255, 232, 0.58);
  border-bottom: 1px solid rgba(66, 255, 178, 0.2);
}

.risk-body {
  min-height: 0;
  overflow: hidden;
  animation: tableFloat 10s linear infinite;
}

.risk-data-row {
  min-height: 29px;
  color: rgba(234, 255, 244, 0.82);
  border-bottom: 1px solid rgba(66, 255, 178, 0.08);
}

.risk-data-row:nth-child(even) {
  background: rgba(10, 72, 48, 0.18);
}

.risk-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 18px;
  color: #04140e;
  font-weight: 900;
  background: linear-gradient(180deg, #d7ff74, #26e092);
}

.risk-name {
  min-width: 0;
  display: grid;
  gap: 1px;
}

.risk-name strong,
.risk-name em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-style: normal;
}

.risk-name em {
  color: rgba(214, 255, 232, 0.55);
}

.risk-score {
  display: grid;
  gap: 3px;
}

.risk-score span,
.risk-life {
  color: #d9ffe9;
  font-variant-numeric: tabular-nums;
}

.risk-score i,
.event-progress i {
  display: block;
  height: 5px;
  overflow: hidden;
  background: rgba(25, 112, 77, 0.34);
}

.risk-score b,
.event-progress b {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #15e893, #d7ff74);
  box-shadow: 0 0 10px rgba(119, 255, 174, 0.56);
}

.event-table {
  height: calc(100% - 30px);
  display: grid;
  grid-template-rows: 24px minmax(0, 1fr);
  overflow: hidden;
  font-size: 11px;
}

.event-head,
.event-row {
  display: grid;
  grid-template-columns: 28px minmax(84px, 1fr) 48px 62px 54px;
  align-items: center;
  gap: 6px;
}

.event-head {
  color: rgba(215, 255, 232, 0.64);
  border-bottom: 1px solid rgba(66, 255, 178, 0.22);
}

.event-body {
  overflow: hidden;
  animation: tableFloat 12s linear infinite;
}

.event-row {
  min-height: 24px;
  color: rgba(232, 255, 242, 0.84);
  border-bottom: 1px solid rgba(66, 255, 178, 0.08);
}

.event-row:nth-child(even) {
  background: rgba(37, 132, 91, 0.18);
}

.event-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-name {
  display: grid;
  gap: 1px;
}

.event-name strong,
.event-name em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-style: normal;
}

.event-name em {
  color: rgba(214, 255, 232, 0.52);
}

.event-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 18px;
  color: #06140e;
  font-size: 10px;
  font-weight: 900;
  background: #75ffb0;
}

.event-tag.warn { background: #ffe16b; }
.event-tag.danger { background: #ff716b; color: #fff; }

.event-progress {
  display: grid;
  grid-template-columns: minmax(22px, 1fr) 22px;
  align-items: center;
  gap: 4px;
  color: #d9ffe9;
}

.event-row strong {
  overflow: hidden;
  color: #d7ffe8;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bi-footer {
  height: 38px;
  display: flex;
  align-items: center;
  overflow: hidden;
  background: linear-gradient(90deg, rgba(5, 48, 50, 0.9), rgba(2, 17, 20, 0.9));
  border: 1px solid rgba(61, 255, 216, 0.2);
}
.footer-label {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 18px;
  color: #041012;
  font-size: 12px;
  font-weight: 900;
  background: linear-gradient(180deg, #9effdf, #12d5c0);
}
.footer-scroll { flex: 1; overflow: hidden; }
.scroll-content { display: inline-block; padding-left: 100%; white-space: nowrap; color: #cffff4; font-size: 13px; animation: scrollLeft 36s linear infinite; }
@keyframes pageScan { 0% { transform: translateY(-100%); opacity: 0; } 20%, 70% { opacity: 1; } 100% { transform: translateY(100%); opacity: 0; } }
@keyframes scanLine { 0%, 100% { transform: translateY(0); opacity: 0; } 50% { transform: translateY(250px); opacity: 0.9; } }
@keyframes ringPulse { 0%, 100% { opacity: 0.45; transform: translate(-50%, -50%) rotateX(68deg) scale(0.92); } 50% { opacity: 1; transform: translate(-50%, -50%) rotateX(68deg) scale(1.08); } }
@keyframes orbitRotate { to { transform: translate(-50%, -50%) rotateX(68deg) rotateZ(360deg); } }
@keyframes scrollLeft { to { transform: translateX(-100%); } }
@keyframes panelTopFlow { 0% { transform: translateX(-22%); } 100% { transform: translateX(22%); } }
@keyframes ribbonFlow { 0%, 100% { opacity: 0.58; transform: translateX(-2%); } 50% { opacity: 0.95; transform: translateX(2%); } }
@keyframes boundaryPulse { 0%, 100% { opacity: 0.72; } 50% { opacity: 1; } }
@keyframes tableFloat { 0%, 18% { transform: translateY(0); } 50%, 68% { transform: translateY(-10px); } 100% { transform: translateY(0); } }
@media (max-width: 1500px) {
  .fine-screen { grid-template-columns: 310px minmax(560px, 1fr) 310px; }
  .header-title { font-size: 19px; }
}
</style>
