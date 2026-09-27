<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'
import { allocateFund } from '@/api/ai'
import { getBarnOptions } from '@/api/barn'

// ========== 县区选项 ==========
const countyOptions = ref<{ label: string; value: string }[]>([])

// ========== 输入表单 ==========
const form = ref({
  totalBudget: null as number | null,
  countyCode: '',
})

// ========== 分配结果 ==========
const loading = ref(false)
const allocateResult = ref<any>(null)

// ========== 统计数据 ==========
const totalBudget = computed(() => allocateResult.value?.totalBudget || 0)
const allocatedAmount = computed(() => {
  if (!allocateResult.value?.items) return 0
  return allocateResult.value.items.reduce((sum: number, item: any) => sum + (item.allocatedAmount || 0), 0)
})
const utilizationRate = computed(() => {
  if (!totalBudget.value) return 0
  return Math.round((allocatedAmount.value / totalBudget.value) * 100)
})

// ========== 饼图配置 ==========
const pieOption = computed(() => {
  if (!allocateResult.value?.items) return {}
  const items = allocateResult.value.items
  const colors = ['#0D5D46', '#1A6B4F', '#2E8B6A', '#5FB292', '#8FD4B8', '#B8E4D2', '#D4F0E4']
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#eee',
      textStyle: { color: '#333', fontSize: 13 },
      formatter: '{b}: {c} 万元 ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: '#606266', fontSize: 12 },
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          color: '#2A5A4E',
          fontSize: 12,
        },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
        },
        data: items.map((item: any, index: number) => ({
          name: item.barnName,
          value: item.allocatedAmount,
          itemStyle: { color: colors[index % colors.length] },
        })),
      },
    ],
  }
})

// ========== 开始分配 ==========
async function handleAllocate() {
  if (!form.value.totalBudget) {
    ElMessage.warning('请输入总预算')
    return
  }
  loading.value = true
  allocateResult.value = null
  try {
    const res = await allocateFund(form.value)
    allocateResult.value = res
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

// ========== 加载县区选项 ==========
async function loadCountyOptions() {
  try {
    const barns = await getBarnOptions()
    const cSet = new Map<string, string>()
    ;(barns || []).forEach((b: any) => {
      if (b.countyCode) cSet.set(b.countyCode, b.county || b.countyCode)
    })
    countyOptions.value = Array.from(cSet.entries()).map(([value, label]) => ({ value, label }))
  } catch {
    // 错误已在拦截器中处理
  }
}

// ========== 紧迫性颜色 - 统一颜色规范 ==========
function getUrgencyColor(urgency: string): string {
  if (urgency === '极高') return '#E74C3C' // 红色：急需修复
  if (urgency === '高') return '#F39C12' // 黄色/橙色：需维护
  if (urgency === '中') return '#2E8B6A' // 绿色：正常
  return '#94A3B8' // 灰色：暂缓/退出
}

onMounted(() => {
  loadCountyOptions()
})
</script>

<template>
  <div class="page-container fund-allocate-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><TrendCharts /></el-icon>
        </div>
        <h2 class="page-title">维修资金分配</h2>
      </div>
      <p class="page-desc">基于优先级评分与紧迫性分析，智能分配维修资金以实现最大投入产出比</p>
    </div>

    <!-- 输入区 -->
    <div class="input-card">
      <el-form :model="form" inline label-width="80px">
        <el-form-item label="总预算">
          <el-input-number
            v-model="form.totalBudget"
            :min="1"
            :max="100000"
            :step="1000"
            placeholder="万元"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="县区筛选">
          <el-select v-model="form.countyCode" placeholder="全部县区" clearable style="width: 180px">
            <el-option v-for="opt in countyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleAllocate">
            <el-icon><TrendCharts /></el-icon>开始分配
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 分配结果 -->
    <div v-if="allocateResult" class="result-section">
      <!-- 统计卡片 -->
      <div class="stat-row">
        <div class="stat-card">
          <div class="stat-value" style="color: #0D5D46;">{{ totalBudget }}<span class="stat-unit">万元</span></div>
          <div class="stat-label">总预算</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color: #1A6B4F;">{{ allocatedAmount }}<span class="stat-unit">万元</span></div>
          <div class="stat-label">已分配金额</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" :style="{ color: utilizationRate >= 90 ? '#2E8B6A' : '#F39C12' }">
            {{ utilizationRate }}<span class="stat-unit">%</span>
          </div>
          <div class="stat-label">预算利用率</div>
        </div>
      </div>

      <!-- 分配方案与饼图 -->
      <div class="content-row">
        <!-- 分配方案表格 -->
        <div class="table-card">
          <div class="card-header">
            <h3 class="card-title">分配方案</h3>
            <el-tag type="success" size="small">
              共 {{ allocateResult.items?.length || 0 }} 项
            </el-tag>
          </div>
          <el-table :data="allocateResult.items || []" size="default" style="width: 100%">
            <el-table-column label="排名" width="70" align="center">
              <template #default="{ $index }">
                <span class="rank-badge" :class="{ 'rank-top': $index === 0 }">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="barnName" label="烤房名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="优先级评分" width="110" align="center">
              <template #default="{ row }">
                <span style="font-weight: 700; color: #0D5D46;">{{ row.priorityScore }}</span>
              </template>
            </el-table-column>
            <el-table-column label="紧迫性" width="90" align="center">
              <template #default="{ row }">
                <el-tag
                  :color="getUrgencyColor(row.urgency)"
                  effect="dark"
                  size="small"
                >
                  {{ row.urgency }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="健康分" width="90" align="center">
              <template #default="{ row }">{{ row.healthScore }}</template>
            </el-table-column>
            <el-table-column label="分配金额" width="120" align="center">
              <template #default="{ row }">
                <span style="font-weight: 700; color: #1A6B4F;">{{ row.allocatedAmount }}万</span>
              </template>
            </el-table-column>
            <el-table-column label="预期ROI" width="100" align="center">
              <template #default="{ row }">
                <span style="font-weight: 600; color: #2E8B6A;">{{ row.expectedROI }}%</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 饼图 -->
        <div class="pie-card">
          <div class="card-header">
            <h3 class="card-title">资金分配占比</h3>
          </div>
          <VChart :option="pieOption" autoresize style="height: 400px" />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <el-icon :size="48" color="#C0D6CC"><TrendCharts /></el-icon>
      <p>请输入预算并点击"开始分配"获取智能分配方案</p>
    </div>

    <!-- 算法说明 -->
    <div class="algo-card">
      <el-collapse>
        <el-collapse-item title="算法说明" name="algo">
          <div class="algo-content">
            <p><strong>维修资金分配算法：</strong></p>
            <p>优先级评分 = 健康评分倒数权重(30%) + 紧迫性权重(30%) + 预期ROI权重(25%) + 使用频率权重(15%)。</p>
            <p>紧迫性由健康评分、距最优维修时间差、使用频率综合计算：评分越低、距维修时间越近、使用频率越高，紧迫性越高。</p>
            <p>资金分配采用按优先级评分比例分配法，确保高优先级烤房获得更多资金。</p>
            <p>预期ROI = (维修后健康评分提升值 x 烤房使用年限) / 分配金额，反映每单位投入的健康收益。</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<style scoped>
.fund-allocate-page {
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

/* ========== 输入区 ========== */
.input-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(12px, 1.6vw, 20px) clamp(16px, 2.4vw, 28px);
  margin-bottom: clamp(16px, 2vw, 24px);
  transition: all 0.3s ease;
}

.input-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

/* ========== 轻拟态按钮 ========== */
.input-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    inset 0 -2px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.input-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
  transform: translateY(-1px);
  box-shadow:
    0 6px 16px rgba(10, 77, 62, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.25),
    inset 0 -2px 0 rgba(0, 0, 0, 0.1);
}

.input-card :deep(.el-button--primary:active) {
  transform: translateY(1px);
  box-shadow:
    0 2px 6px rgba(10, 77, 62, 0.2),
    inset 0 1px 0 rgba(0, 0, 0, 0.1);
}

/* ========== 统计卡片行 ========== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: clamp(12px, 1.6vw, 20px);
  margin-bottom: clamp(16px, 2vw, 24px);
}

.stat-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: clamp(18px, 2.4vw, 28px) clamp(12px, 1.6vw, 20px);
  border-top: 3px solid #0D5D46;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

.stat-value {
  font-size: clamp(32px, 3.6vw, 40px);
  font-weight: 800;
  line-height: 1.2;
}

.stat-unit {
  font-size: clamp(12px, 1.3vw, 15px);
  font-weight: 500;
  margin-left: 4px;
}

.stat-label {
  font-size: clamp(12px, 1.3vw, 14px);
  color: var(--text-muted);
  margin-top: 8px;
}

/* ========== 内容行：表格 + 饼图 ========== */
.content-row {
  display: grid;
  grid-template-columns: 1fr minmax(320px, 30vw);
  gap: clamp(16px, 2vw, 24px);
  margin-bottom: clamp(16px, 2vw, 24px);
}

.table-card,
.pie-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.table-card:hover,
.pie-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 32px rgba(10, 77, 62, 0.12),
    0 0 24px rgba(13, 93, 70, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: clamp(12px, 1.6vw, 18px) clamp(16px, 2vw, 24px) clamp(12px, 1.6vw, 18px) clamp(16px, 2vw, 24px);
}

.card-title {
  font-size: clamp(14px, 1.4vw, 16px);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 clamp(12px, 1.5vw, 18px);
  padding-left: clamp(12px, 1.5vw, 16px);
  border-left: 3px solid #0A4D3E;
}

/* ========== 排名徽章 ========== */
.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: clamp(24px, 2.8vw, 30px);
  height: clamp(24px, 2.8vw, 30px);
  border-radius: 50%;
  font-size: clamp(12px, 1.3vw, 14px);
  font-weight: 700;
  background: linear-gradient(135deg, rgba(240, 244, 243, 0.9) 0%, rgba(240, 244, 243, 0.7) 100%);
  color: #4A7A6E;
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.rank-top {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  color: #FFFFFF;
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
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
@media (max-width: 1100px) {
  .content-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: 1fr;
  }

  .input-card :deep(.el-form) {
    flex-direction: column;
  }

  .input-card :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 12px;
  }
}
</style>
