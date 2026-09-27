<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'
import { calculateHealthScore } from '@/api/ai'
import { getBarnOptions } from '@/api/barn'

// ========== 烤房选项 ==========
const barnOptions = ref<{ label: string; value: string }[]>([])
const selectedBarnId = ref<number | null>(null)

// ========== 评分结果 ==========
const loading = ref(false)
const scoreResult = ref<any>(null)

// ========== 健康等级颜色映射 ==========
const healthColorMap: Record<string, string> = {
  '优良': '#2E8B6A',
  '需维护': '#F39C12',
  '急需修复': '#E74C3C',
  '退出': '#94A3B8',
}

function getScoreColor(score: number): string {
  if (score >= 85) return '#2E8B6A'
  if (score >= 70) return '#5FB292'
  if (score >= 40) return '#F39C12'
  return '#E74C3C'
}

function getHealthLevel(score: number): string {
  if (score >= 85) return '优良'
  if (score >= 70) return '需维护'
  if (score >= 40) return '急需修复'
  return '退出'
}

// ========== 雷达图配置 ==========
const radarOption = computed(() => {
  if (!scoreResult.value || !scoreResult.value.factors) return {}
  const factors = scoreResult.value.factors
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#eee',
      textStyle: { color: '#333', fontSize: 13 },
    },
    radar: {
      indicator: factors.map((f: any) => ({ name: f.name, max: 100 })),
      shape: 'circle',
      splitNumber: 4,
      axisName: {
        color: '#2A5A4E',
        fontSize: 13,
        fontWeight: 600,
      },
      splitLine: {
        lineStyle: { color: 'rgba(46, 139, 106, 0.15)' },
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(46, 139, 106, 0.02)', 'rgba(46, 139, 106, 0.05)', 'rgba(46, 139, 106, 0.02)', 'rgba(46, 139, 106, 0.05)'],
        },
      },
      axisLine: {
        lineStyle: { color: 'rgba(46, 139, 106, 0.2)' },
      },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: factors.map((f: any) => f.score),
            name: '健康评分',
            lineStyle: { color: '#0D5D46', width: 2 },
            itemStyle: { color: '#0D5D46' },
            areaStyle: {
              color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                { offset: 0, color: 'rgba(13, 93, 70, 0.35)' },
                { offset: 1, color: 'rgba(95, 178, 146, 0.1)' },
              ]),
            },
          },
        ],
      },
    ],
  }
})

// ========== 一键评分 ==========
async function handleScore() {
  if (!selectedBarnId.value) {
    ElMessage.warning('请先选择烤房')
    return
  }
  loading.value = true
  scoreResult.value = null
  try {
    const res = await calculateHealthScore(selectedBarnId.value)
    scoreResult.value = res
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

// ========== 加载烤房选项 ==========
async function loadBarnOptions() {
  try {
    const barns = await getBarnOptions()
    barnOptions.value = (barns || []).map((b: any) => ({
      label: `${b.barnName || b.projectName} (${b.id})`,
      value: b.id,
    }))
  } catch {
    // 错误已在拦截器中处理
  }
}

// ========== 评分明细 ==========
const scoreDetails = computed(() => {
  if (!scoreResult.value || !scoreResult.value.factors) return []
  return scoreResult.value.factors.map((f: any) => ({
    name: f.name,
    score: f.score,
    weight: f.weight,
    weightedScore: (f.score * f.weight / 100).toFixed(1),
  }))
})

onMounted(() => {
  loadBarnOptions()
})
</script>

<template>
  <div class="page-container health-scoring-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><TrendCharts /></el-icon>
        </div>
        <h2 class="page-title">自动健康评分</h2>
      </div>
      <p class="page-desc">基于多维度因子自动计算烤房综合健康评分，量化评估烤房状态</p>
    </div>

    <!-- 选择区 -->
    <div class="select-card">
      <el-form inline>
        <el-form-item label="选择烤房">
          <el-select
            v-model="selectedBarnId"
            placeholder="请选择烤房"
            filterable
            style="width: 320px"
          >
            <el-option
              v-for="opt in barnOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleScore">
            <el-icon><TrendCharts /></el-icon>一键评分
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 评分结果 -->
    <div v-if="scoreResult" class="result-section">
      <!-- 评分概览 -->
      <div class="score-overview">
        <div class="score-big-card" :style="{ borderColor: getScoreColor(scoreResult.totalScore) }">
          <div class="score-number" :style="{ color: getScoreColor(scoreResult.totalScore) }">
            {{ scoreResult.totalScore }}
          </div>
          <div class="score-label">综合健康评分</div>
          <el-tag
            :color="healthColorMap[getHealthLevel(scoreResult.totalScore)]"
            effect="dark"
            size="large"
            class="health-tag"
          >
            {{ getHealthLevel(scoreResult.totalScore) }}
          </el-tag>
        </div>

        <!-- 雷达图 -->
        <div class="radar-card">
          <h3 class="card-title">评分因子雷达图</h3>
          <VChart :option="radarOption" autoresize style="height: 320px" />
        </div>
      </div>

      <!-- 评分明细表格 -->
      <div class="detail-card">
        <div class="card-header">
          <h3 class="card-title">评分明细</h3>
        </div>
        <el-table :data="scoreDetails" size="default" style="width: 100%">
          <el-table-column prop="name" label="因子名称" min-width="160" />
          <el-table-column label="得分" width="120" align="center">
            <template #default="{ row }">
              <span :style="{ color: getScoreColor(row.score), fontWeight: 700 }">{{ row.score }}</span>
            </template>
          </el-table-column>
          <el-table-column label="权重(%)" width="120" align="center">
            <template #default="{ row }">{{ row.weight }}%</template>
          </el-table-column>
          <el-table-column label="加权得分" width="120" align="center">
            <template #default="{ row }">
              <span style="font-weight: 700;">{{ row.weightedScore }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <el-icon :size="48" color="#C0D6CC"><TrendCharts /></el-icon>
      <p>请选择烤房并点击"一键评分"开始分析</p>
    </div>

    <!-- 算法说明 -->
    <div class="algo-card">
      <el-collapse>
        <el-collapse-item title="算法说明" name="algo">
          <div class="algo-content">
            <p><strong>综合健康评分计算方法：</strong></p>
            <p>总分 = SUM(因子得分 x 因子权重)，各因子得分范围 0-100，权重总和为 100%。</p>
            <p>评分因子包括：使用年限、使用状态、设备完整度、部件评分、结构安全、环境适应性等。</p>
            <p>健康等级划分：优良(>=85)、需维护(70-84)、急需修复(40-69)、退出(&lt;40)。</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<style scoped>
.health-scoring-page {
  min-height: 100%;
  padding: clamp(16px, 2vw, 24px);
}

/* ========== 页面标题 ========== */
.page-title-wrap {
  display: flex;
  align-items: center;
  gap: clamp(8px, 1vw, 12px);
  margin-bottom: 6px;
}

.page-title-icon {
  width: clamp(32px, 3.6vw, 40px);
  height: clamp(32px, 3.6vw, 40px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.page-title-icon .el-icon {
  color: #FFFFFF;
}

.page-title {
  font-size: clamp(18px, 2vw, 22px);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.3vw, 14px);
  color: var(--text-muted);
  margin: 0;
  padding-left: clamp(40px, 4.8vw, 52px);
}

/* ========== 毛玻璃卡片基础样式 ========== */
.glass-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.glass-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

/* ========== 选择区 ========== */
.select-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(12px, 1.6vw, 20px) clamp(16px, 2.4vw, 28px);
  margin-bottom: clamp(16px, 2vw, 24px);
  transition: all 0.3s ease;
}

.select-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

/* ========== 轻拟态按钮 ========== */
.select-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    inset 0 -2px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.select-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
  transform: translateY(-1px);
  box-shadow:
    0 6px 16px rgba(10, 77, 62, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.25),
    inset 0 -2px 0 rgba(0, 0, 0, 0.1);
}

.select-card :deep(.el-button--primary:active) {
  transform: translateY(1px);
  box-shadow:
    0 2px 6px rgba(10, 77, 62, 0.2),
    inset 0 1px 0 rgba(0, 0, 0, 0.1);
}

/* ========== 评分结果 ========== */
.result-section {
  margin-bottom: clamp(16px, 2vw, 24px);
}

.score-overview {
  display: grid;
  grid-template-columns: minmax(260px, 22vw) 1fr;
  gap: clamp(16px, 2vw, 24px);
  margin-bottom: clamp(16px, 2vw, 24px);
}

.score-big-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 3vw, 36px) clamp(16px, 2vw, 28px);
  border-top: 4px solid #2E8B6A;
  transition: all 0.3s ease;
}

.score-big-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

.score-number {
  font-size: clamp(48px, 5.6vw, 64px);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.score-label {
  font-size: clamp(12px, 1.4vw, 15px);
  color: var(--text-muted);
  margin-top: 8px;
}

.health-tag {
  margin-top: clamp(8px, 1vw, 14px);
  font-size: clamp(13px, 1.4vw, 15px);
  font-weight: 600;
}

.radar-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(16px, 2vw, 24px);
  transition: all 0.3s ease;
}

.radar-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(10, 77, 62, 0.12);
}

.radar-card .card-title {
  font-size: clamp(14px, 1.5vw, 16px);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 clamp(12px, 1.4vw, 16px);
  padding-left: clamp(12px, 1.4vw, 14px);
  border-left: 3px solid #0D5D46;
}

/* ========== 明细卡片 ========== */
.detail-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.detail-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: clamp(12px, 1.6vw, 18px) clamp(16px, 2vw, 24px);
}

.card-title {
  font-size: clamp(14px, 1.5vw, 16px);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  padding-left: clamp(12px, 1.4vw, 14px);
  border-left: 3px solid #0D5D46;
}

/* ========== 空状态 ========== */
.empty-state {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: clamp(40px, 5vw, 64px) clamp(16px, 2vw, 28px);
  margin-bottom: clamp(16px, 2vw, 24px);
  transition: all 0.3s ease;
}

.empty-state:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

.empty-state p {
  margin-top: 16px;
  font-size: clamp(13px, 1.4vw, 15px);
  color: var(--text-muted);
}

/* ========== 算法说明 ========== */
.algo-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 0 4px;
  transition: all 0.3s ease;
}

.algo-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

.algo-content {
  font-size: clamp(12px, 1.3vw, 14px);
  color: var(--text-secondary);
  line-height: 1.8;
}

.algo-content p {
  margin: 4px 0;
}

/* ========== 响应式 ========== */
@media (max-width: 900px) {
  .score-overview {
    grid-template-columns: 1fr;
  }
}
</style>
