<script setup lang="ts">
import { ref, reactive, onMounted, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

/** 推荐算法参数 */
const algorithmParams = reactive({
  distanceWeight: 30,
  idleWeight: 25,
  preferenceWeight: 20,
  scoreWeight: 25,
})

/** 推荐效果数据 */
const effectData = reactive({
  successRate: 78.5,
  satisfaction: 4.2,
  maxSatisfaction: 5.0,
})

/** 推荐趋势折线图 */
const trendChartRef = shallowRef<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null

/** 推荐日志Mock数据 */
interface RecommendLog {
  id: number
  time: string
  farmer: string
  barnName: string
  reason: string
  adopted: boolean
}

const recommendLogs = ref<RecommendLog[]>([
  { id: 1, time: '2026-06-10 09:30', farmer: '张伟', barnName: '热水镇营沟村1号烤房', reason: '距离最近(0.5km)、空闲、评分95', adopted: true },
  { id: 2, time: '2026-06-10 08:15', farmer: '李明', barnName: '白水镇尖山村1号烤房', reason: '符合偏好(气流下降式)、评分93', adopted: true },
  { id: 3, time: '2026-06-09 16:45', farmer: '王建国', barnName: '板桥镇小堡子村1号烤房', reason: '距离适中(1.2km)、空闲、评分91', adopted: false },
  { id: 4, time: '2026-06-09 14:20', farmer: '陈明华', barnName: '罗雄镇大明村1号烤房', reason: '评分最高(97)、符合偏好', adopted: true },
  { id: 5, time: '2026-06-09 10:00', farmer: '赵德山', barnName: '中安镇多乐村1号烤房', reason: '距离最近(0.8km)、评分89', adopted: true },
  { id: 6, time: '2026-06-08 15:30', farmer: '刘大勇', barnName: '盘江镇龙凤村1号烤房', reason: '空闲、评分94、符合偏好', adopted: false },
  { id: 7, time: '2026-06-08 11:20', farmer: '孙志强', barnName: '丹凤镇大堵村1号烤房', reason: '距离最近(0.3km)、空闲', adopted: true },
  { id: 8, time: '2026-06-07 09:45', farmer: '周文斌', barnName: '热水镇营沟村2号烤房', reason: '评分82、距离适中、空闲', adopted: true },
])

/** 保存配置 */
function handleSaveConfig() {
  ElMessage.success('推荐算法参数配置已保存')
}

/** 初始化趋势图 */
function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>推荐成功率：{c}%',
    },
    grid: {
      left: 40,
      right: 20,
      top: 20,
      bottom: 30,
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisLabel: { color: '#909399', fontSize: 12 },
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      min: 60,
      max: 100,
      axisLabel: { color: '#909399', fontSize: 12, formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#F2F6FC', type: 'dashed' } },
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: '#1A6B4F' },
        itemStyle: { color: '#1A6B4F' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(26, 60, 110, 0.25)' },
            { offset: 1, color: 'rgba(26, 60, 110, 0.02)' },
          ]),
        },
        data: [72.3, 74.1, 75.8, 76.5, 77.2, 78.5],
      },
    ],
  })
}

onMounted(() => {
  initTrendChart()
})
</script>

<template>
  <div class="recommend-page">
    <!-- 推荐算法参数配置 -->
    <el-card shadow="never">
      <template #header>
        <span class="card-title">推荐算法参数配置</span>
      </template>
      <div class="params-grid">
        <div class="param-item">
          <div class="param-header">
            <span class="param-label">距离权重</span>
            <span class="param-value">{{ algorithmParams.distanceWeight }}</span>
          </div>
          <el-slider v-model="algorithmParams.distanceWeight" :min="0" :max="100" :show-tooltip="false" />
        </div>
        <div class="param-item">
          <div class="param-header">
            <span class="param-label">空闲权重</span>
            <span class="param-value">{{ algorithmParams.idleWeight }}</span>
          </div>
          <el-slider v-model="algorithmParams.idleWeight" :min="0" :max="100" :show-tooltip="false" />
        </div>
        <div class="param-item">
          <div class="param-header">
            <span class="param-label">偏好权重</span>
            <span class="param-value">{{ algorithmParams.preferenceWeight }}</span>
          </div>
          <el-slider v-model="algorithmParams.preferenceWeight" :min="0" :max="100" :show-tooltip="false" />
        </div>
        <div class="param-item">
          <div class="param-header">
            <span class="param-label">评分权重</span>
            <span class="param-value">{{ algorithmParams.scoreWeight }}</span>
          </div>
          <el-slider v-model="algorithmParams.scoreWeight" :min="0" :max="100" :show-tooltip="false" />
        </div>
      </div>
      <div class="save-actions">
        <el-button type="primary" @click="handleSaveConfig">保存配置</el-button>
      </div>
    </el-card>

    <!-- 推荐效果分析 -->
    <el-card shadow="never">
      <template #header>
        <span class="card-title">推荐效果分析</span>
      </template>
      <div class="effect-grid">
        <!-- 推荐成功率 -->
        <div class="effect-item">
          <div class="effect-number">{{ effectData.successRate }}<span class="effect-unit">%</span></div>
          <div class="effect-label">推荐成功率</div>
          <div class="effect-bar">
            <div class="effect-bar-fill" :style="{ width: effectData.successRate + '%' }"></div>
          </div>
        </div>
        <!-- 烟农满意度 -->
        <div class="effect-item">
          <div class="effect-number">{{ effectData.satisfaction }}<span class="effect-unit"> / {{ effectData.maxSatisfaction }}</span></div>
          <div class="effect-label">烟农满意度</div>
          <div class="star-row">
            <span v-for="i in 5" :key="i" class="star" :class="{ 'star-active': i <= Math.floor(effectData.satisfaction), 'star-half': i === Math.ceil(effectData.satisfaction) && effectData.satisfaction % 1 !== 0 }">★</span>
          </div>
        </div>
        <!-- 推荐趋势折线图 -->
        <div class="effect-chart">
          <div ref="trendChartRef" style="width: 100%; height: 220px;"></div>
        </div>
      </div>
    </el-card>

    <!-- 推荐日志 -->
    <el-card shadow="never">
      <template #header>
        <span class="card-title">推荐日志</span>
      </template>
      <el-table :data="recommendLogs" stripe border style="width: 100%">
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column prop="farmer" label="烟农" width="100" align="center" />
        <el-table-column prop="barnName" label="推荐烤房" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reason" label="推荐理由" min-width="260" show-overflow-tooltip />
        <el-table-column label="是否采纳" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.adopted ? 'success' : 'info'" size="default">
              {{ row.adopted ? '已采纳' : '未采纳' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
/* ==================== 毛玻璃质感设计系统 ==================== */
.recommend-page {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: clamp(12px, 1.5vw, 18px);
}

.card-title {
  font-size: clamp(14px, 1.6vw, 16px);
  font-weight: 600;
  color: #303133;
}

/* 毛玻璃卡片基础样式 */
.recommend-page :deep(.el-card) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.recommend-page :deep(.el-card::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(10, 77, 62, 0.2), transparent);
}

.recommend-page :deep(.el-card:hover) {
  box-shadow:
    0 6px 24px rgba(10, 77, 62, 0.08),
    0 12px 40px rgba(10, 77, 62, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
}

/* 参数配置 */
.params-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: clamp(18px, 2.2vw, 28px) clamp(32px, 4vw, 56px);
}

.param-item {
  padding: 0 clamp(6px, 0.8vw, 10px);
}

.param-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: clamp(6px, 0.8vw, 10px);
}

.param-label {
  font-size: clamp(13px, 1.5vw, 15px);
  color: #606266;
  font-weight: 500;
}

.param-value {
  font-size: clamp(15px, 1.8vw, 18px);
  font-weight: 700;
  color: #0A4D3E;
  min-width: 30px;
  text-align: right;
}

/* 轻拟态按钮 */
.save-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: clamp(14px, 1.8vw, 22px);
  padding-top: clamp(12px, 1.5vw, 18px);
  border-top: 1px solid rgba(10, 77, 62, 0.08);
}

.save-actions :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 2px 4px rgba(10, 77, 62, 0.2),
    0 4px 8px rgba(10, 77, 62, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transition: all 0.2s ease;
}

.save-actions :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
  box-shadow:
    0 4px 8px rgba(10, 77, 62, 0.25),
    0 8px 16px rgba(10, 77, 62, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.save-actions :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow:
    0 1px 2px rgba(10, 77, 62, 0.2),
    inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 推荐效果 */
.effect-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 2fr;
  gap: clamp(18px, 2.2vw, 28px);
  align-items: center;
}

.effect-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: clamp(16px, 2vw, 24px);
  background: linear-gradient(135deg, rgba(10, 77, 62, 0.04) 0%, rgba(10, 77, 62, 0.02) 100%);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 8px;
  border: 1px solid rgba(10, 77, 62, 0.08);
}

.effect-number {
  font-size: clamp(32px, 4vw, 48px);
  font-weight: 700;
  color: #0A4D3E;
  line-height: 1.2;
}

.effect-unit {
  font-size: clamp(14px, 1.6vw, 18px);
  font-weight: 400;
  color: #909399;
}

.effect-label {
  font-size: clamp(12px, 1.4vw, 14px);
  color: #909399;
  margin-top: 4px;
}

.effect-bar {
  width: 100%;
  height: 6px;
  background: rgba(10, 77, 62, 0.1);
  border-radius: 3px;
  margin-top: clamp(10px, 1.2vw, 14px);
  overflow: hidden;
}

.effect-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #0A4D3E, #2E8B6A);
  border-radius: 3px;
  transition: width 0.6s ease;
}

.star-row {
  display: flex;
  gap: 4px;
  margin-top: clamp(8px, 1vw, 12px);
}

.star {
  font-size: clamp(18px, 2.2vw, 24px);
  color: #DCDFE6;
}

.star-active {
  color: #F7BA2A;
}

.star-half {
  color: #F7BA2A;
  opacity: 0.5;
}

.effect-chart {
  min-height: clamp(200px, 25vw, 240px);
}

/* 响应式 */
@media (max-width: 1200px) {
  .params-grid {
    grid-template-columns: 1fr;
  }
  .effect-grid {
    grid-template-columns: 1fr;
  }
}
</style>
