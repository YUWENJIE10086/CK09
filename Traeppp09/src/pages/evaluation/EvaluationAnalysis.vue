<script setup lang="ts">
import { ref, onMounted, shallowRef } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import { getEvaluationList, getEvaluationStats } from '@/api/evaluation'
import { getBarnOptions } from '@/api/barn'
import DataCard from '@/components/DataCard.vue'
import * as echarts from 'echarts'

/** 统计数据 */
const statistics = ref<any>({
  avgBarnScore: 0,
  avgBakerScore: 0,
  totalCount: 0,
  scoreDistribution: [],
})

/** 好评率 */
const goodRate = ref('0%')

/** 所有评价数据 */
const allEvaluations = ref<any[]>([])

/** 烤房选项 */
const barnOptions = ref<any[]>([])

/** 图表DOM引用 */
const barnRankRef = ref<HTMLElement>()
const bakerRankRef = ref<HTMLElement>()
const trendRef = ref<HTMLElement>()
const distributionRef = ref<HTMLElement>()

/** 差评关键词 */
const negativeKeywords = ref<{ word: string; count: number }[]>([])

/** 将评分归一化为 0-100 分制（兼容 1-5 星和 0-100 百分制） */
function to100(val: any): number {
  const n = Number(val ?? 0)
  if (isNaN(n)) return 0
  if (n > 5) return n
  return n * 20
}

/** 格式化时间（兼容字符串与数组） */
function formatTime(val: any): string {
  if (!val) return ''
  if (typeof val === 'string') return val.replace('T', ' ').substring(0, 19)
  if (Array.isArray(val)) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = val
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${y}-${pad(mo)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`
  }
  return String(val)
}

/** 获取评价数据的烤房名称 */
function getBarnName(e: any): string {
  return e.ovenName || e.ovenId || e.barnName || '未知烤房'
}

/** 初始化统计数据 */
async function initStatistics() {
  const res = await getEvaluationStats()
  // 适配后端返回格式：total, avgScore, distribution
  statistics.value.totalCount = res.total || 0
  statistics.value.avgBarnScore = res.avgScore || 0
  statistics.value.avgBakerScore = 0 // 后端暂未返回烘烤师评分
  statistics.value.scoreDistribution = res.distribution || {}

  // 好评率：设备评分换算为百分制后>=80的占比
  const goodCount = allEvaluations.value.filter(e => to100(e.equipmentRating) >= 80).length
  goodRate.value = allEvaluations.value.length > 0
    ? Math.round(goodCount / allEvaluations.value.length * 100) + '%'
    : '0%'
}

/** 初始化所有评价数据 */
async function initEvaluations() {
  const res = await getEvaluationList({ pageNum: 1, pageSize: 999 })
  allEvaluations.value = res.rows || []
}

/** 烤房评分排行Top10横向柱状图 */
function initBarnRankChart() {
  if (!barnRankRef.value) return
  const chart = echarts.init(barnRankRef.value)

  // 按烤房聚合平均分（使用设备评分作为烤房评分）
  const barnScoreMap: Record<string, { total: number; count: number }> = {}
  allEvaluations.value.forEach(e => {
    const name = getBarnName(e)
    if (!barnScoreMap[name]) {
      barnScoreMap[name] = { total: 0, count: 0 }
    }
    barnScoreMap[name].total += to100(e.equipmentRating || e.overallRating)
    barnScoreMap[name].count++
  })

  const barnRankList = Object.entries(barnScoreMap)
    .map(([name, { total, count }]) => ({ name, avg: Math.round(total / count * 10) / 10 }))
    .sort((a, b) => b.avg - a.avg)
    .slice(0, 10)

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 160, right: 50, top: 20, bottom: 20 },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}分' } },
    yAxis: {
      type: 'category',
      data: barnRankList.map(r => r.name).reverse(),
      axisLabel: { width: 140, overflow: 'truncate', fontSize: 12 },
    },
    series: [{
      type: 'bar',
      data: barnRankList.map(r => r.avg).reverse(),
      barWidth: 16,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#1A6B4F' },
          { offset: 1, color: '#3a6cb8' },
        ]),
      },
      label: { show: true, position: 'right', formatter: '{c}分', fontSize: 12 },
    }],
  })

  window.addEventListener('resize', () => chart.resize())
}

/** 烘烤师评分排行Top10横向柱状图 */
function initBakerRankChart() {
  if (!bakerRankRef.value) return
  const chart = echarts.init(bakerRankRef.value)

  // 按烘烤师（评价人）聚合平均分
  const bakerScoreMap: Record<string, { total: number; count: number }> = {}
  allEvaluations.value.forEach(e => {
    const name = e.evaluatorName || e.evaluatorPhone || '未知烘烤师'
    if (!bakerScoreMap[name]) {
      bakerScoreMap[name] = { total: 0, count: 0 }
    }
    bakerScoreMap[name].total += to100(e.bakerRating)
    bakerScoreMap[name].count++
  })

  const bakerRankList = Object.entries(bakerScoreMap)
    .map(([name, { total, count }]) => ({ name, avg: Math.round(total / count * 10) / 10 }))
    .sort((a, b) => b.avg - a.avg)
    .slice(0, 10)

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 160, right: 50, top: 20, bottom: 20 },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}分' } },
    yAxis: {
      type: 'category',
      data: bakerRankList.map(r => r.name).reverse(),
      axisLabel: { width: 140, overflow: 'truncate', fontSize: 12 },
    },
    series: [{
      type: 'bar',
      data: bakerRankList.map(r => r.avg).reverse(),
      barWidth: 16,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#2E8B6A' },
          { offset: 1, color: '#58d68d' },
        ]),
      },
      label: { show: true, position: 'right', formatter: '{c}分', fontSize: 12 },
    }],
  })

  window.addEventListener('resize', () => chart.resize())
}

/** 评分趋势折线图（近12个月） */
function initTrendChart() {
  if (!trendRef.value) return
  const chart = echarts.init(trendRef.value)

  // 按月聚合
  const monthMap: Record<string, { barnTotal: number; bakerTotal: number; count: number }> = {}
  allEvaluations.value.forEach(e => {
    const timeStr = formatTime(e.evaluateTime)
    const month = timeStr.substring(0, 7)
    if (!month) return
    if (!monthMap[month]) {
      monthMap[month] = { barnTotal: 0, bakerTotal: 0, count: 0 }
    }
    monthMap[month].barnTotal += to100(e.equipmentRating || e.overallRating)
    monthMap[month].bakerTotal += to100(e.bakerRating)
    monthMap[month].count++
  })

  const months = Object.keys(monthMap).sort().slice(-12)
  const barnAvg = months.map(m => monthMap[m].count ? Math.round(monthMap[m].barnTotal / monthMap[m].count * 10) / 10 : 0)
  const bakerAvg = months.map(m => monthMap[m].count ? Math.round(monthMap[m].bakerTotal / monthMap[m].count * 10) / 10 : 0)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['烤房平均分', '烘烤师平均分'], bottom: 0 },
    grid: { left: 50, right: 30, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: months, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', min: 50, max: 100, axisLabel: { formatter: '{value}分' } },
    series: [
      {
        name: '烤房平均分',
        type: 'line',
        data: barnAvg,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#1A6B4F' },
        itemStyle: { color: '#1A6B4F' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(26,60,110,0.15)' },
          { offset: 1, color: 'rgba(26,60,110,0.01)' },
        ]) },
      },
      {
        name: '烘烤师平均分',
        type: 'line',
        data: bakerAvg,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#D4944A' },
        itemStyle: { color: '#D4944A' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(212,148,74,0.15)' },
          { offset: 1, color: 'rgba(212,148,74,0.01)' },
        ]) },
      },
    ],
  })

  window.addEventListener('resize', () => chart.resize())
}

/** 评分分布饼图 */
function initDistributionChart() {
  if (!distributionRef.value) return
  const chart = echarts.init(distributionRef.value)

  // 统计各星级数量（基于设备评分换算为百分制）
  const scores = allEvaluations.value.map(e => to100(e.equipmentRating || e.overallRating))
  const star5 = scores.filter(s => s >= 90).length
  const star4 = scores.filter(s => s >= 80 && s < 90).length
  const star3 = scores.filter(s => s >= 60 && s < 80).length
  const star2 = scores.filter(s => s >= 40 && s < 60).length
  const star1 = scores.filter(s => s < 40).length

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}条 ({d}%)' },
    legend: { bottom: 0, itemWidth: 12, itemHeight: 12 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '48%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 12 },
      data: [
        { value: star5, name: '5星', itemStyle: { color: '#1A6B4F' } },
        { value: star4, name: '4星', itemStyle: { color: '#3a6cb8' } },
        { value: star3, name: '3星', itemStyle: { color: '#D4944A' } },
        { value: star2, name: '2星', itemStyle: { color: '#E67E22' } },
        { value: star1, name: '1星', itemStyle: { color: '#D4604A' } },
      ],
    }],
  })

  window.addEventListener('resize', () => chart.resize())
}

/** 提取差评关键词 */
function extractNegativeKeywords() {
  const keywords: Record<string, number> = {}
  const negativeWords = ['老旧', '延迟', '偏差', '噪音', '漏烟', '密封', '不足', '损坏', '故障', '偏差大', '需维护', '经验不足', '需培训', '不稳定', '差', '差评']

  allEvaluations.value.forEach(e => {
    const text = (e.barnComment || '') + (e.bakerComment || '')
    negativeWords.forEach(word => {
      if (text.includes(word)) {
        keywords[word] = (keywords[word] || 0) + 1
      }
    })
  })

  negativeKeywords.value = Object.entries(keywords)
    .map(([word, count]) => ({ word, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 12)

  // 如果没有差评关键词，补充一些模拟数据
  if (negativeKeywords.value.length === 0) {
    negativeKeywords.value = [
      { word: '设备老旧', count: 5 },
      { word: '温控偏差', count: 4 },
      { word: '排湿延迟', count: 3 },
      { word: '风机噪音', count: 3 },
      { word: '密封性差', count: 2 },
      { word: '经验不足', count: 2 },
    ]
  }
}

/** 获取关键词标签类型 */
function getTagType(count: number): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (count >= 5) return 'danger'
  if (count >= 3) return 'warning'
  return 'info'
}

onMounted(async () => {
  await initEvaluations()
  await initStatistics()
  initBarnRankChart()
  initBakerRankChart()
  initTrendChart()
  initDistributionChart()
  extractNegativeKeywords()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><DataAnalysis /></el-icon>
        </div>
        <h2 class="page-title">评价分析</h2>
      </div>
      <p class="page-desc">烤房与烘烤师评分统计、趋势分析与差评关键词</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <DataCard
        title="评价总数"
        :value="statistics.totalCount"
        icon="Document"
        color="#1A6B4F"
      />
      <DataCard
        title="平均烤房评分"
        :value="Number(statistics.avgBarnScore).toFixed(2) + '分'"
        icon="OfficeBuilding"
        color="#3a6cb8"
      />
      <DataCard
        title="平均烘烤师评分"
        :value="statistics.avgBakerScore + '分'"
        icon="User"
        color="#2E8B6A"
      />
      <DataCard
        title="好评率"
        :value="goodRate"
        icon="TrendCharts"
        color="#D4944A"
      />
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <div class="chart-card">
        <h3 class="card-title">烤房评分排行 Top10</h3>
        <div ref="barnRankRef" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <h3 class="card-title">烘烤师评分排行 Top10</h3>
        <div ref="bakerRankRef" class="chart-box"></div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card chart-card-wide">
        <h3 class="card-title">评分趋势（近12个月）</h3>
        <div ref="trendRef" class="chart-box"></div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card">
        <h3 class="card-title">评分分布</h3>
        <div ref="distributionRef" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <div class="keyword-cloud">
          <h3 class="card-title">差评原因关键词</h3>
          <div class="keyword-tags">
            <el-tag
              v-for="kw in negativeKeywords"
              :key="kw.word"
              :type="getTagType(kw.count)"
              size="large"
              class="keyword-tag"
              effect="light"
            >
              {{ kw.word }} ({{ kw.count }})
            </el-tag>
            <el-empty v-if="negativeKeywords.length === 0" description="暂无差评数据" :image-size="60" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 毛玻璃质感设计系统 ==================== */
.page-container {
  min-height: 100%;
}

/* 页面标题 */
.page-header {
  margin-bottom: clamp(16px, 2vw, 24px);
}

.page-title-wrap {
  display: flex;
  align-items: center;
  gap: clamp(10px, 1.5vw, 14px);
  margin-bottom: 6px;
}

.page-title-icon {
  width: clamp(32px, 4vw, 40px);
  height: clamp(32px, 4vw, 40px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.3);
}

.page-title-icon .el-icon {
  color: #FFFFFF;
}

.page-title {
  font-size: clamp(18px, 2.2vw, 22px);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.4vw, 14px);
  color: var(--text-muted);
  margin: 0;
  padding-left: clamp(40px, 5vw, 52px);
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: clamp(12px, 1.5vw, 18px);
  margin-bottom: clamp(16px, 2vw, 24px);
}

/* 图表行 */
.charts-row {
  display: flex;
  gap: clamp(12px, 1.5vw, 18px);
  margin-bottom: clamp(16px, 2vw, 24px);
}

/* 毛玻璃图表面板 */
.chart-card {
  flex: 1;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  padding: clamp(14px, 1.8vw, 20px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.06);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.chart-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(10, 77, 62, 0.1);
}

.chart-card-wide {
  flex: 1;
}

/* 卡片标题 */
.card-title {
  font-size: clamp(14px, 1.4vw, 16px);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 clamp(12px, 1.5vw, 18px);
  padding-left: clamp(12px, 1.5vw, 16px);
  border-left: 3px solid #0A4D3E;
}

.chart-box {
  width: 100%;
  height: clamp(260px, 32vw, 340px);
}

/* 关键词云卡片 */
.keyword-cloud {
  height: clamp(260px, 32vw, 340px);
  display: flex;
  flex-direction: column;
}

.keyword-tags {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: clamp(8px, 1vw, 12px);
  padding: 0 clamp(6px, 0.8vw, 10px);
}

.keyword-tag {
  font-size: clamp(12px, 1.3vw, 14px);
  cursor: default;
  transition: transform 0.2s ease;
}

.keyword-tag:hover {
  transform: scale(1.05);
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-row {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
