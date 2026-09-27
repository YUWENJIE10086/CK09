<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'
import { getBarnAssignList } from '@/api/barn'
import { getReservationList } from '@/api/reservation'

// ========== Loading ==========
const loading = ref(false)

// ========== 烤房原始数据 ==========
const barnList = ref<any[]>([])

// ========== 最近预约 ==========
const recentReservations = ref<any[]>([])

// ========== 实时时钟 ==========
const currentTime = ref('')
const currentDate = ref('')
let clockTimer: ReturnType<typeof setInterval> | null = null

function updateClock() {
  const now = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${h}:${m}:${s}`
  currentDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${week}`
}

// ========== 衍生统计 ==========
const totalCount = computed(() => barnList.value.length)

/** 从API数据中获取健康分 */
function getHealthScore(b: any): number {
  return Number(b.currentHealthScore ?? b.healthScore ?? 0)
}

/** 从API数据中获取剩余寿命 */
function getLifeResidual(b: any): number {
  return Number(b.currentLifeResidual ?? b.predictedLifeYears ?? b.lifeResidual ?? 0)
}

/** 计算健康等级（中文）— 匹配数据库5级：优良/良好/一般/预警/危险 */
function calcHealthLevel(b: any): string {
  const score = getHealthScore(b)
  if (score >= 90) return '优良'
  if (score >= 75) return '良好'
  if (score >= 60) return '一般'
  if (score >= 40) return '预警'
  if (score > 0) return '危险'
  return '未知'
}

const inUseCount = computed(() => barnList.value.filter(b => b.useStatus === '在用').length)
const idleCount = computed(() => barnList.value.filter(b => b.useStatus === '闲置').length)
const damagedCount = computed(() => barnList.value.filter(b => b.useStatus === '损毁').length)

const excellentCount = computed(() => barnList.value.filter(b => calcHealthLevel(b) === '优良').length)
const goodCount = computed(() => barnList.value.filter(b => calcHealthLevel(b) === '良好').length)
const fairCount = computed(() => barnList.value.filter(b => calcHealthLevel(b) === '一般').length)
const warningCount = computed(() => barnList.value.filter(b => calcHealthLevel(b) === '预警').length)
const dangerCount = computed(() => barnList.value.filter(b => calcHealthLevel(b) === '危险').length)

const avgHealthScore = computed(() => {
  const scores = barnList.value.map(b => getHealthScore(b)).filter(n => !isNaN(n) && n > 0)
  if (!scores.length) return 0
  return Math.round(scores.reduce((a, b) => a + b, 0) / scores.length)
})

const avgLifeYears = computed(() => {
  const years = barnList.value.map(b => getLifeResidual(b)).filter(n => !isNaN(n) && n > 0)
  if (!years.length) return '0.0'
  return (years.reduce((a, b) => a + b, 0) / years.length).toFixed(1)
})

// ========== KPI 卡片配置 ==========
const iconPaths: Record<string, string> = {
  factory: 'M3 21h18M5 21V9l4 2V9l4 2V7l4 2v12M9 21v-5h4v5',
  flame: 'M12 3c1.5 3 4 5 4 9a4 4 0 11-8 0c0-2 1-3.5 2-4.5 0 1.5 1 2.5 2 2.5 0-2.5-1-4.5 0-7z',
  check: 'M9 12l2 2 4-4M12 21a9 9 0 100-18 9 9 0 000 18z',
  alert: 'M12 8v5m0 3h.01M12 21a9 9 0 100-18 9 9 0 000 18z',
  pause: 'M9 8v8M15 8v8M12 21a9 9 0 100-18 9 9 0 000 18z',
  broken: 'M15 9l-6 6M9 9l6 6M12 21a9 9 0 100-18 9 9 0 000 18z',
  heart: 'M12 21C12 21 4 13.5 4 8.5C4 6 6 4 8.5 4C10 4 11 5 12 6.5C13 5 14 4 15.5 4C18 4 20 6 20 8.5C20 13.5 12 21 12 21Z',
  clock: 'M12 7v5l3 3M12 21a9 9 0 100-18 9 9 0 000 18z',
}

const kpiCards = computed(() => [
  { label: '烤房总数', value: totalCount.value, color: '#0D5D46', bg: '#E0F2EC', icon: 'factory' },
  { label: '在用', value: inUseCount.value, color: '#2E8B6A', bg: '#E8F5EE', icon: 'flame' },
  { label: '优良', value: excellentCount.value, color: '#1A7A52', bg: '#D4F0E0', icon: 'check' },
  { label: '危险', value: dangerCount.value, color: '#D4604A', bg: '#FDECEA', icon: 'alert' },
  { label: '闲置', value: idleCount.value, color: '#E8A714', bg: '#FFF8E1', icon: 'pause' },
  { label: '损毁', value: damagedCount.value, color: '#D4604A', bg: '#FDECEA', icon: 'broken' },
])

// ========== 健康等级分布（环形图） ==========
const healthPieOption = computed(() => {
  const data = [
    { name: '优良', value: excellentCount.value, color: '#2E8B6A' },
    { name: '良好', value: goodCount.value, color: '#5FB292' },
    { name: '一般', value: fairCount.value, color: '#D4944A' },
    { name: '预警', value: warningCount.value, color: '#D4604A' },
    { name: '危险', value: dangerCount.value, color: '#B71C1C' },
  ].filter(d => d.value > 0)

  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '52%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      data: data.map(d => ({ name: d.name, value: d.value, itemStyle: { color: d.color } })),
    }],
  }
})

const healthLegend = computed(() => [
  { name: '优良', value: excellentCount.value, color: '#2E8B6A' },
  { name: '良好', value: goodCount.value, color: '#5FB292' },
  { name: '一般', value: fairCount.value, color: '#D4944A' },
  { name: '预警', value: warningCount.value, color: '#D4604A' },
  { name: '危险', value: dangerCount.value, color: '#B71C1C' },
])

// ========== 区域分布 - 健康等级筛选 ==========
const healthFilter = ref('')  // 空=全部, 优良/良好/一般/预警/危险

/** 按健康等级筛选后的烤房列表 */
const filteredBarnList = computed(() => {
  if (!healthFilter.value) return barnList.value
  return barnList.value.filter(b => calcHealthLevel(b) === healthFilter.value)
})

/** 筛选后的区域分布数量 */
const filteredCount = computed(() => filteredBarnList.value.length)

/** 健康等级筛选选项 */
const healthFilterOptions = [
  { label: '优良', color: '#2E8B6A' },
  { label: '良好', color: '#5FB292' },
  { label: '一般', color: '#D4944A' },
  { label: '预警', color: '#D4604A' },
  { label: '危险', color: '#B71C1C' },
]

function setHealthFilter(level: string) {
  healthFilter.value = healthFilter.value === level ? '' : level
}

function resetHealthFilter() {
  healthFilter.value = ''
}

// ========== 使用状态分布（柱状图） ==========
const useStatusBarOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '5%', right: '5%', top: '12%', bottom: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['在用', '闲置', '损毁'],
    axisLine: { lineStyle: { color: '#E0E0E0' } },
    axisTick: { show: false },
    axisLabel: { color: '#5A8A7A', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#F0F0F0', type: 'dashed' } },
    axisLabel: { color: '#5A8A7A', fontSize: 10 },
  },
  series: [{
    type: 'bar',
    barWidth: '42%',
    itemStyle: {
      borderRadius: [6, 6, 0, 0],
      color: (params: any) => {
        const colors = ['#2E8B6A', '#E8A714', '#D4604A']
        return colors[params.dataIndex] || '#2E8B6A'
      },
    },
    data: [inUseCount.value, idleCount.value, damagedCount.value],
  }],
}))

// ========== 区域分布（横向柱状图，前5县） ==========
const countyBarOption = computed(() => {
  const source = healthFilter.value ? filteredBarnList.value : barnList.value
  const map = new Map<string, number>()
  source.forEach(b => {
    const name = b.county || '未知'
    map.set(name, (map.get(name) || 0) + 1)
  })
  const arr = Array.from(map.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 5)
    .reverse()

  const barColor = healthFilter.value
    ? healthFilterOptions.find(o => o.label === healthFilter.value)?.color || '#5FB292'
    : '#5FB292'

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '14%', top: '8%', bottom: '8%', containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#F0F0F0', type: 'dashed' } },
      axisLabel: { color: '#5A8A7A', fontSize: 10 },
    },
    yAxis: {
      type: 'category',
      data: arr.map(a => a.name),
      axisLine: { lineStyle: { color: '#E0E0E0' } },
      axisTick: { show: false },
      axisLabel: { color: '#5A8A7A', fontSize: 11 },
    },
    series: [{
      type: 'bar',
      barWidth: '55%',
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: barColor },
          { offset: 1, color: barColor },
        ]),
      },
      label: { show: true, position: 'right', color: '#0D5D46', fontSize: 11, fontWeight: 600 },
      data: arr.map(a => a.value),
    }],
  }
})

// ========== 预警烤房（健康分<60 或 寿命<5年） ==========
const alertBarns = computed(() => {
  return barnList.value
    .filter(b => {
      const score = getHealthScore(b)
      const life = getLifeResidual(b)
      return (score > 0 && score < 60) || (life > 0 && life < 5) || b.facilityStatus === '损坏'
    })
    .sort((a, b) => getHealthScore(a) - getHealthScore(b))
    .slice(0, 10)
    .map(b => {
      const score = getHealthScore(b)
      const life = getLifeResidual(b)
      const reasons: string[] = []
      if (score > 0 && score < 60) reasons.push('健康分低')
      if (life > 0 && life < 5) reasons.push('寿命较短')
      if (b.facilityStatus === '损坏') reasons.push('设备损毁')
      if (b.facilityStatus && b.facilityStatus !== '正常' && b.facilityStatus !== '损坏') reasons.push(b.facilityStatus)
      return {
        id: b.id || b.barnName || '--',
        barnName: b.barnName || b.id || '--',
        county: b.county || '',
        healthScore: score,
        healthLevel: calcHealthLevel(b),
        predictedLifeYears: life,
        useStatus: b.useStatus || '-',
        alertReason: reasons.join('、') || '需关注',
        isUrgent: score > 0 && score < 40,
      }
    })
})

// ========== 预约状态样式 ==========
function reservationStatusClass(status: string): string {
  const s = mapReservationStatus(status)
  if (s === '待审核') return 'st-pending'
  if (s === '已确认' || s === '使用中') return 'st-active'
  if (s === '已完成') return 'st-done'
  if (s === '已取消' || s === '已驳回' || s === '超时') return 'st-cancel'
  return 'st-default'
}

/** 将英文状态映射为中文 */
function mapReservationStatus(status: string): string {
  if (!status) return '--'
  const map: Record<string, string> = {
    pending: '待审核',
    confirmed: '已确认',
    active: '使用中',
    completed: '已完成',
    cancelled: '已取消',
    rejected: '已驳回',
    timeout: '超时',
  }
  return map[status] || status
}

// ========== 数据加载 ==========
async function loadData() {
  loading.value = true
  try {
    // 分别加载，避免一个失败影响另一个
    try {
      const barnRes: any = await getBarnAssignList({ pageNum: 1, pageSize: 9999 })
      const rows = barnRes?.rows || []
      // 直接使用API返回的useStatus（对应数据库use_status字段：在用/闲置/损毁）
      barnList.value = rows
      console.log('[Dashboard] 加载烤房数据:', rows.length, '条')
    } catch (e) {
      console.error('[Dashboard] 加载烤房数据失败:', e)
      barnList.value = []
    }

    try {
      const reservationRes: any = await getReservationList({ pageNum: 1, pageSize: 5 })
      recentReservations.value = (reservationRes?.rows || []).map((r: any) => ({
        userName: r.userName || r.userId || '--',
        barnCode: r.ovenId || r.barnName || '--',
        time: (r.planStartTime || r.reserveStart || r.createdAt || '').toString().substring(0, 16).replace('T', ' '),
        status: mapReservationStatus(r.status),
      }))
      console.log('[Dashboard] 加载预约数据:', recentReservations.value.length, '条')
    } catch (e) {
      console.error('[Dashboard] 加载预约数据失败:', e)
      recentReservations.value = []
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  loadData()
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<template>
  <div class="workspace">
    <!-- Loading 遮罩 -->
    <div v-if="loading" class="loading-mask">
      <div class="loading-spinner"></div>
      <span class="loading-text">数据加载中...</span>
    </div>

    <!-- 1. 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-left">
        <h2 class="banner-title">烤房智慧管理平台</h2>
        <p class="banner-desc">实时监控烤房健康状态，统筹设备维护与资源调度</p>
      </div>
      <div class="banner-center">
        <div class="clock-time">{{ currentTime }}</div>
        <div class="clock-date">{{ currentDate }}</div>
      </div>
      <div class="banner-right">
        <div class="banner-metric">
          <span class="metric-value">{{ totalCount }}</span>
          <span class="metric-label">烤房总数</span>
        </div>
        <div class="metric-divider"></div>
        <div class="banner-metric">
          <span class="metric-value">{{ avgHealthScore }}</span>
          <span class="metric-label">平均健康分</span>
        </div>
        <div class="metric-divider"></div>
        <div class="banner-metric">
          <span class="metric-value">{{ avgLifeYears }}</span>
          <span class="metric-label">平均剩余寿命(年)</span>
        </div>
      </div>
    </div>

    <!-- 2. KPI 指标卡 -->
    <div class="kpi-row">
      <div v-for="card in kpiCards" :key="card.label" class="kpi-card">
        <div class="kpi-icon" :style="{ background: card.bg, color: card.color }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path :d="iconPaths[card.icon]" />
          </svg>
        </div>
        <div class="kpi-info">
          <span class="kpi-value" :style="{ color: card.color }">{{ card.value }}</span>
          <span class="kpi-label">{{ card.label }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 图表区 -->
    <div class="chart-row">
      <!-- 左：健康等级分布 -->
      <div class="panel">
        <div class="panel-header">
          <h3 class="panel-title">健康等级分布</h3>
        </div>
        <div class="panel-body panel-body--pie">
          <div class="pie-wrap">
            <VChart :option="healthPieOption" autoresize class="chart-fill" />
            <div class="pie-center">
              <span class="pie-total">{{ totalCount }}</span>
              <span class="pie-label">总计</span>
            </div>
          </div>
          <div class="pie-legend">
            <div v-for="item in healthLegend" :key="item.name" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <span class="legend-name">{{ item.name }}</span>
              <span class="legend-value">{{ item.value }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 中：使用状态分布 -->
      <div class="panel">
        <div class="panel-header">
          <h3 class="panel-title">使用状态分布</h3>
        </div>
        <div class="panel-body">
          <VChart :option="useStatusBarOption" autoresize class="chart-fill" />
        </div>
      </div>

      <!-- 右：区域分布 -->
      <div class="panel">
        <div class="panel-header">
          <h3 class="panel-title">区域分布 Top5</h3>
          <span class="panel-badge">{{ filteredCount }} 栋</span>
        </div>
        <div class="panel-body">
          <div class="health-filter-bar">
            <button
              v-for="opt in healthFilterOptions"
              :key="opt.label"
              class="health-filter-btn"
              :class="{ active: healthFilter === opt.label }"
              :style="healthFilter === opt.label ? { background: opt.color, borderColor: opt.color } : {}"
              @click="setHealthFilter(opt.label)"
            >
              <span class="filter-dot" :style="{ background: opt.color }"></span>
              {{ opt.label }}
            </button>
            <button
              class="health-filter-btn reset-btn"
              :class="{ active: !healthFilter }"
              @click="resetHealthFilter"
            >
              重置
            </button>
          </div>
          <VChart :option="countyBarOption" autoresize class="chart-fill" />
        </div>
      </div>
    </div>

    <!-- 4. 底部区 -->
    <div class="bottom-row">
      <!-- 左：预警烤房 -->
      <div class="panel">
        <div class="panel-header">
          <h3 class="panel-title">预警烤房</h3>
          <span class="panel-badge panel-badge--alert">{{ alertBarns.length }} 条</span>
        </div>
        <div class="panel-body panel-body--list">
          <div class="alert-table-head">
            <span class="col-barn">烤房编号</span>
            <span class="col-reason">预警原因</span>
            <span class="col-score">健康分</span>
            <span class="col-life">剩余寿命</span>
          </div>
          <div class="alert-list">
            <div v-for="(b, i) in alertBarns" :key="i" class="alert-row" :class="{ 'alert-row--urgent': b.isUrgent }">
              <div class="col-barn">
                <div class="barn-code">{{ b.barnName }}</div>
                <div v-if="b.county" class="barn-county">{{ b.county }}</div>
              </div>
              <span class="col-reason">
                <span class="reason-tag" :class="{ 'reason-urgent': b.isUrgent }">{{ b.alertReason }}</span>
              </span>
              <span class="col-score">
                <span class="score-tag" :class="b.healthScore < 40 ? 'score-danger' : 'score-warning'">{{ b.healthScore > 0 ? b.healthScore : '--' }}</span>
              </span>
              <span class="col-life">{{ b.predictedLifeYears > 0 ? b.predictedLifeYears + '年' : '--' }}</span>
            </div>
            <div v-if="alertBarns.length === 0" class="empty-tip">暂无预警烤房</div>
          </div>
        </div>
      </div>

      <!-- 右：最近预约 -->
      <div class="panel">
        <div class="panel-header">
          <h3 class="panel-title">最近预约</h3>
          <span class="panel-badge">{{ recentReservations.length }} 条</span>
        </div>
        <div class="panel-body panel-body--list">
          <div class="reservation-list">
            <div v-for="(r, i) in recentReservations" :key="i" class="reservation-item">
              <div class="res-avatar">{{ (r.userName || '?').charAt(0) }}</div>
              <div class="res-info">
                <div class="res-main">
                  <span class="res-user">{{ r.userName }}</span>
                  <span class="res-code">{{ r.barnCode }}</span>
                </div>
                <div class="res-time">{{ r.time }}</div>
              </div>
              <span class="res-status" :class="reservationStatusClass(r.status)">{{ r.status }}</span>
            </div>
            <div v-if="recentReservations.length === 0" class="empty-tip">暂无预约记录</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.workspace {
  height: 100%;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-sizing: border-box;
  background: #F5F7F6;
  position: relative;
  overflow: hidden;
}

/* ===== Loading ===== */
.loading-mask {
  position: absolute;
  inset: 0;
  background: rgba(245, 247, 246, 0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  z-index: 100;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #E8F5EE;
  border-top-color: #2E8B6A;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 13px;
  color: #5A8A7A;
}

/* ===== 1. 欢迎横幅 ===== */
.welcome-banner {
  flex-shrink: 0;
  background: linear-gradient(135deg, #0D5D46 0%, #1A6B4F 50%, #2E8B6A 100%);
  border-radius: 10px;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 16px rgba(13, 93, 70, 0.25);
  position: relative;
  overflow: hidden;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  right: -40px;
  top: -40px;
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, transparent 70%);
  border-radius: 50%;
}

.banner-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
  z-index: 1;
}

.banner-title {
  font-size: clamp(15px, 1.4vw, 20px);
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.banner-date {
  font-size: clamp(11px, 0.9vw, 13px);
  color: rgba(255, 255, 255, 0.75);
  margin: 0;
}

.banner-desc {
  font-size: clamp(10px, 0.8vw, 12px);
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.banner-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
  padding: 0 20px;
  border-left: 1px solid rgba(255, 255, 255, 0.15);
  border-right: 1px solid rgba(255, 255, 255, 0.15);
}

.clock-time {
  font-size: 28px;
  font-weight: 700;
  color: #FFFFFF;
  font-family: 'SF Mono', 'Courier New', monospace;
  letter-spacing: 0.05em;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  line-height: 1.2;
}

.clock-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
  margin-top: 2px;
}

.banner-right {
  display: flex;
  align-items: center;
  gap: 20px;
  z-index: 1;
}

.banner-metric {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.metric-value {
  font-size: clamp(22px, 2.4vw, 32px);
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.metric-label {
  font-size: clamp(10px, 0.8vw, 12px);
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
}

.metric-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
}

/* ===== 2. KPI 指标卡 ===== */
.kpi-row {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.kpi-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 2px 8px rgba(26, 107, 79, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 14px rgba(26, 107, 79, 0.15);
}

.kpi-icon {
  width: clamp(34px, 3.2vw, 42px);
  height: clamp(34px, 3.2vw, 42px);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-icon svg {
  width: 55%;
  height: 55%;
}

.kpi-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.kpi-value {
  font-size: clamp(20px, 2.2vw, 28px);
  font-weight: 700;
  line-height: 1.1;
}

.kpi-label {
  font-size: clamp(10px, 0.85vw, 12px);
  color: #5A8A7A;
  white-space: nowrap;
}

/* ===== 3. 图表区 ===== */
.chart-row {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(26, 107, 79, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px 0;
  flex-shrink: 0;
}

.panel-title {
  font-size: clamp(12px, 1vw, 14px);
  font-weight: 600;
  color: #0D3D30;
  margin: 0;
}

.panel-badge {
  font-size: clamp(10px, 0.8vw, 11px);
  color: #1A6B4F;
  font-weight: 500;
  background: #E8F5EE;
  padding: 2px 8px;
  border-radius: 10px;
}

.panel-badge--alert {
  color: #D4604A;
  background: #FDECEA;
}

.panel-body {
  flex: 1;
  min-height: 0;
  padding: 6px 10px 10px;
  display: flex;
  flex-direction: column;
}

.chart-fill {
  width: 100%;
  height: 100%;
  min-height: 0;
}

/* 健康等级筛选条 */
.health-filter-bar {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.health-filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border: 1px solid #DCDFE6;
  border-radius: 10px;
  background: #F5F7FA;
  color: #606266;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
}

.health-filter-btn:hover {
  border-color: #0D5D46;
  color: #0D5D46;
}

.health-filter-btn.active {
  color: #fff;
  font-weight: 600;
}

.health-filter-btn .filter-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.health-filter-btn.active .filter-dot {
  background: #fff !important;
}

.reset-btn {
  margin-left: auto;
  border-color: #D4604A;
  color: #D4604A;
  background: #FDECEA;
}

.reset-btn:hover {
  background: #D4604A;
  color: #fff;
  border-color: #D4604A;
}

.reset-btn.active {
  background: #D4604A;
  color: #fff;
  border-color: #D4604A;
}

/* 健康等级环形图布局 */
.panel-body--pie {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  padding: 6px 10px 10px;
}

.pie-wrap {
  position: relative;
  width: 55%;
  height: 100%;
  min-height: 0;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}

.pie-total {
  font-size: clamp(18px, 1.8vw, 26px);
  font-weight: 700;
  color: #0D5D46;
  display: block;
  line-height: 1.1;
}

.pie-label {
  font-size: clamp(9px, 0.75vw, 11px);
  color: #5A8A7A;
}

.pie-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: clamp(6px, 0.8vw, 12px);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: clamp(8px, 0.7vw, 10px);
  height: clamp(8px, 0.7vw, 10px);
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-name {
  font-size: clamp(10px, 0.85vw, 12px);
  color: #5A8A7A;
}

.legend-value {
  font-size: clamp(11px, 0.9vw, 13px);
  color: #0D5D46;
  font-weight: 600;
  margin-left: auto;
}

/* ===== 4. 底部区 ===== */
.bottom-row {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  height: clamp(180px, 22vh, 230px);
}

.panel-body--list {
  padding: 4px 12px 8px;
  overflow: hidden;
}

/* 预警烤房列表 */
.alert-table-head {
  display: flex;
  align-items: center;
  padding: 4px 6px;
  font-size: clamp(9px, 0.75vw, 11px);
  color: #5A8A7A;
  font-weight: 500;
  border-bottom: 1px solid #EDF2EF;
  flex-shrink: 0;
}

.alert-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.alert-list::-webkit-scrollbar {
  width: 4px;
}

.alert-list::-webkit-scrollbar-thumb {
  background: #E8F5EE;
  border-radius: 2px;
}

.alert-row {
  display: flex;
  align-items: center;
  padding: 5px 6px;
  font-size: clamp(10px, 0.85vw, 12px);
  border-bottom: 1px solid #F5F7F6;
  transition: background 0.15s;
}

.alert-row:hover {
  background: #F5F7F6;
}

.col-barn {
  flex: 1;
  color: #0D3D30;
  min-width: 0;
}

.barn-code {
  font-family: 'Courier New', monospace;
  font-weight: 500;
  font-size: clamp(10px, 0.8vw, 12px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.barn-county {
  font-size: clamp(9px, 0.7vw, 10px);
  color: #5A8A7A;
  margin-top: 1px;
}

.col-reason {
  width: 90px;
  text-align: center;
}

.reason-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: clamp(9px, 0.75vw, 10px);
  background: #FEF3E7;
  color: #D4944A;
  white-space: nowrap;
}

.reason-urgent {
  background: #FDECEA;
  color: #E74C3C;
}

.alert-row--urgent {
  background: rgba(253, 236, 234, 0.3);
}

.col-score {
  width: 50px;
  text-align: center;
}

.col-life {
  width: 60px;
  text-align: right;
  color: #5A8A7A;
}

.score-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 8px;
  font-weight: 600;
  font-size: clamp(10px, 0.8vw, 11px);
}

.score-danger {
  background: #FDECEA;
  color: #E74C3C;
}

.score-warning {
  background: #FEF3E7;
  color: #D4944A;
}

/* 预约列表 */
.reservation-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 2px;
}

.reservation-list::-webkit-scrollbar {
  width: 4px;
}

.reservation-list::-webkit-scrollbar-thumb {
  background: #E8F5EE;
  border-radius: 2px;
}

.reservation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #F5F7F6;
  border-radius: 6px;
  transition: background 0.15s;
}

.reservation-item:hover {
  background: #EDF2EF;
}

.res-avatar {
  width: clamp(26px, 2.2vw, 32px);
  height: clamp(26px, 2.2vw, 32px);
  border-radius: 6px;
  background: #E8F5EE;
  color: #1A6B4F;
  font-size: clamp(11px, 0.9vw, 13px);
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.res-info {
  flex: 1;
  min-width: 0;
}

.res-main {
  display: flex;
  align-items: center;
  gap: 6px;
}

.res-user {
  font-size: clamp(11px, 0.9vw, 13px);
  font-weight: 500;
  color: #0D3D30;
}

.res-code {
  font-size: clamp(10px, 0.8vw, 11px);
  color: #5A8A7A;
  font-family: 'Courier New', monospace;
}

.res-time {
  font-size: clamp(9px, 0.75vw, 10px);
  color: #5A8A7A;
  margin-top: 1px;
}

.res-status {
  font-size: clamp(9px, 0.75vw, 11px);
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 8px;
  flex-shrink: 0;
  white-space: nowrap;
}

.st-pending { background: #FEF3E7; color: #D4944A; }
.st-active { background: #E8F5EE; color: #2E8B6A; }
.st-done { background: #EDF2EF; color: #5A8A7A; }
.st-cancel { background: #FDECEA; color: #D4604A; }
.st-default { background: #EDF2EF; color: #5A8A7A; }

.empty-tip {
  text-align: center;
  font-size: clamp(11px, 0.9vw, 13px);
  color: #5A8A7A;
  padding: 20px 0;
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .kpi-row {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  .chart-row {
    grid-template-columns: 1fr 1fr;
  }
  .chart-row .panel:nth-child(3) {
    display: none;
  }
}

@media (max-width: 768px) {
  .welcome-banner {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-row {
    grid-template-columns: 1fr;
  }
  .bottom-row {
    grid-template-columns: 1fr;
    height: auto;
  }
  .panel-body--pie {
    flex-direction: column;
  }
  .pie-wrap {
    width: 100%;
    height: 60%;
  }
}
</style>
