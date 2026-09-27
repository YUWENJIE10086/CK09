<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, DataAnalysis, CircleCheck, TrendCharts, CircleClose, PieChart as PieChartIcon, DataLine } from '@element-plus/icons-vue'
import { Contact, UserPlus, Trophy, TrendingUp, BarChart3, TrendingDown } from 'lucide-vue-next'
import * as echarts from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getFarmerStats } from '@/api/farmer'
import type { FarmerStats } from '@/api/farmer'

echarts.use([PieChart, BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

// 统计数据
const stats = reactive<FarmerStats>({
  total: 0,
  active: 0,
  creditA: 0,
  creditB: 0,
  creditC: 0,
  creditD: 0
})

// 统计卡片配置（每张卡片独立配色 + 图标）
const statCards = [
  { key: 'total', title: '总烟农', icon: Contact, color: '#0D5D46', bg: '#E8F5EE' },
  { key: 'active', title: '在用烟农', icon: UserPlus, color: '#2E8B6A', bg: '#E0F2EA' },
  { key: 'creditA', title: '信用A级', icon: Trophy, color: '#1565C0', bg: '#E3F2FD' },
  { key: 'creditB', title: '信用B级', icon: BarChart3, color: '#0891B2', bg: '#E0F7FA' },
  { key: 'creditC', title: '信用C级', icon: TrendingUp, color: '#D4944A', bg: '#FBEEDB' },
  { key: 'creditD', title: '信用D级', icon: TrendingDown, color: '#D4604A', bg: '#FCE8E5' },
] as const

// 增长趋势数据
const growthTrendData = ref<{ month: string; count: number }[]>([])

// 图表实例
let creditChart: echarts.ECharts | null = null
let growthChart: echarts.ECharts | null = null

const creditChartRef = ref<HTMLDivElement>()
const growthChartRef = ref<HTMLDivElement>()

// 初始化信用等级分布饼图
function initCreditChart() {
  if (!creditChartRef.value) return
  creditChart = echarts.init(creditChartRef.value)
  updateCreditChart()
}

// 更新信用等级分布饼图
function updateCreditChart() {
  if (!creditChart) return
  const total = stats.creditA + stats.creditB + stats.creditC + stats.creditD
  creditChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { fontSize: 12 }
    },
    series: [{
      name: '信用等级',
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        position: 'center',
        formatter: `{a}\n\n总数: ${total}`,
        fontSize: 14,
        fontWeight: 'bold',
        color: '#0A4D3E'
      },
      emphasis: {
        label: { show: true, fontSize: 16, fontWeight: 'bold' }
      },
      data: [
        { value: stats.creditA, name: 'A级', itemStyle: { color: '#2E8B6A' } },
        { value: stats.creditB, name: 'B级', itemStyle: { color: '#5A8A7A' } },
        { value: stats.creditC, name: 'C级', itemStyle: { color: '#8A9A8A' } },
        { value: stats.creditD, name: 'D级', itemStyle: { color: '#D4A017' } }
      ]
    }]
  })
}

// 初始化烟农增长趋势折线图
function initGrowthChart() {
  if (!growthChartRef.value) return
  growthChart = echarts.init(growthChartRef.value)
  updateGrowthChart()
}

// 更新烟农增长趋势折线图
function updateGrowthChart() {
  if (!growthChart) return
  const months = growthTrendData.value.map(item => item.month)
  const counts = growthTrendData.value.map(item => item.count)

  // 如果没有数据，使用默认数据
  const xData = months.length > 0 ? months : ['无数据']
  const yData = counts.length > 0 ? counts : [0]

  growthChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>新增烟农: {c} 户'
    },
    grid: {
      left: '3%',
      right: '5%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { fontSize: 11 }
    },
    series: [{
      name: '新增烟农',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#2E8B6A' },
      itemStyle: { color: '#0A4D3E' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(46, 139, 106, 0.5)' },
          { offset: 1, color: 'rgba(46, 139, 106, 0.05)' }
        ])
      },
      data: yData
    }]
  })
}

// 窗口大小变化时重绘图表
function handleResize() {
  creditChart?.resize()
  growthChart?.resize()
}

// 加载统计数据
async function loadStats() {
  try {
    const res: any = await getFarmerStats()
    Object.assign(stats, res)

    // 处理增长趋势数据
    const trend = res.growthTrend || []
    growthTrendData.value = Array.isArray(trend)
      ? trend.map((item: any) => ({
          month: String(item.month || ''),
          count: Number(item.cnt || 0)
        }))
      : []

    await nextTick()
    updateCreditChart()
    updateGrowthChart()
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

onMounted(() => {
  loadStats()
  nextTick(() => {
    initCreditChart()
    initGrowthChart()
  })
  window.addEventListener('resize', handleResize)
})

// 监听数据变化更新图表
watch(() => stats.total, () => {
  nextTick(() => {
    updateCreditChart()
    updateGrowthChart()
  })
})
</script>

<template>
  <div class="farmer-stats">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-icon">
        <el-icon :size="22"><DataAnalysis /></el-icon>
      </div>
      <div class="page-title-text">
        <h2 class="page-title">烟农数据分析</h2>
        <p class="page-desc">查看烟农统计信息、信用等级分布和增长趋势</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-bar" :style="{ background: card.color }"></div>
        <div class="stat-icon" :style="{ background: card.bg }">
          <component :is="card.icon" :size="18" :color="card.color" />
        </div>
        <div class="stat-body">
          <div class="stat-value" :style="{ color: card.color }">{{ stats[card.key] }}</div>
          <div class="stat-label">{{ card.title }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-container">
      <!-- 信用等级分布饼图 -->
      <div class="chart-card">
        <div class="chart-title">
          <el-icon :size="18"><PieChartIcon /></el-icon>
          <span>信用等级分布</span>
        </div>
        <div ref="creditChartRef" class="chart-content"></div>
      </div>

      <!-- 烟农增长趋势折线图 -->
      <div class="chart-card">
        <div class="chart-title">
          <el-icon :size="18"><DataLine /></el-icon>
          <span>烟农增长趋势（近6个月）</span>
        </div>
        <div ref="growthChartRef" class="chart-content"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.farmer-stats {
  padding: 11px;
  min-height: 100%;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.page-title-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.25);
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0D3D30;
  margin: 0;
}
.page-desc {
  font-size: 13px;
  color: #6B8A7A;
  margin: 2px 0 0 0;
}

/* 统计卡片（图标圆角方块 + 左侧竖条 + 多色区分） */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px 10px 14px;
  background: #ffffff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  overflow: hidden;
  position: relative;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 16px rgba(0, 0, 0, 0.1);
}
.stat-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  flex-shrink: 0;
}
.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  font-size: 12px;
  color: #6B7280;
  margin-top: 2px;
}

/* 图表区域 */
.charts-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
  gap: 20px;
}
.chart-card {
  background: linear-gradient(135deg, #ffffff, #f8fcfb);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(10, 77, 62, 0.08);
}
.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #0D5D46;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(10, 77, 62, 0.1);
}
.chart-content {
  width: 100%;
  height: 360px;
}

@media (max-width: 768px) {
  .charts-container {
    grid-template-columns: 1fr;
  }
}
</style>
