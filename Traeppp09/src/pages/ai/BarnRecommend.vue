<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts } from '@element-plus/icons-vue'
import { recommendBarns } from '@/api/ai'
import { getBarnOptions } from '@/api/barn'

// ========== 县区选项 ==========
const countyOptions = ref<{ label: string; value: string }[]>([])

// ========== 输入表单 ==========
const form = ref({
  countyCode: '',
  reserveDate: '',
  tobaccoWeight: null as number | null,
})

// ========== 推荐结果 ==========
const loading = ref(false)
const recommendList = ref<any[]>([])

// ========== 匹配度进度条颜色 ==========
function getMatchColor(score: number): string {
  if (score >= 90) return '#2E8B6A'
  if (score >= 70) return '#5FB292'
  if (score >= 50) return '#D4944A'
  return '#E74C3C'
}

// ========== 表格行样式 ==========
function tableRowClassName({ rowIndex }: { rowIndex: number }): string {
  if (rowIndex === 0) return 'top-match-row'
  return ''
}

// ========== 开始推荐 ==========
async function handleRecommend() {
  if (!form.value.countyCode) {
    ElMessage.warning('请选择县区')
    return
  }
  if (!form.value.reserveDate) {
    ElMessage.warning('请选择预约日期')
    return
  }
  if (!form.value.tobaccoWeight) {
    ElMessage.warning('请输入烟叶重量')
    return
  }
  loading.value = true
  recommendList.value = []
  try {
    const res = await recommendBarns(form.value)
    recommendList.value = res || []
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

onMounted(() => {
  loadCountyOptions()
})
</script>

<template>
  <div class="page-container barn-recommend-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><TrendCharts /></el-icon>
        </div>
        <h2 class="page-title">智能烤房推荐</h2>
      </div>
      <p class="page-desc">根据需求条件智能匹配最优烤房，提升烤房利用率与烟叶烘烤质量</p>
    </div>

    <!-- 输入区 -->
    <div class="input-card">
      <el-form :model="form" inline label-width="80px">
        <el-form-item label="县区">
          <el-select v-model="form.countyCode" placeholder="请选择县区" clearable style="width: 180px">
            <el-option v-for="opt in countyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker
            v-model="form.reserveDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="烟叶重量">
          <el-input-number
            v-model="form.tobaccoWeight"
            :min="1"
            :max="5000"
            placeholder="kg"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRecommend">
            <el-icon><TrendCharts /></el-icon>开始推荐
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 推荐结果 -->
    <div v-if="recommendList.length > 0" class="result-card">
      <div class="card-header">
        <h3 class="card-title">推荐结果</h3>
        <el-tag type="success" size="small">共 {{ recommendList.length }} 条推荐</el-tag>
      </div>
      <el-table
        :data="recommendList"
        :row-class-name="tableRowClassName"
        size="default"
        style="width: 100%"
      >
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ $index }">
            <span class="rank-badge" :class="{ 'rank-top': $index === 0 }">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="barnName" label="烤房名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="id" label="编号" width="160" align="center" />
        <el-table-column label="匹配度" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="row.matchScore"
              :stroke-width="14"
              :color="getMatchColor(row.matchScore)"
              :text-inside="true"
              :format="(val: number) => `${val}%`"
            />
          </template>
        </el-table-column>
        <el-table-column label="健康评分" width="100" align="center">
          <template #default="{ row }">
            <span :style="{ color: getMatchColor(row.healthScore), fontWeight: 700 }">
              {{ row.healthScore }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="设备完整度" width="110" align="center">
          <template #default="{ row }">{{ row.equipmentIntegrity }}%</template>
        </el-table-column>
        <el-table-column prop="reason" label="推荐理由" min-width="200" show-overflow-tooltip />
      </el-table>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <el-icon :size="48" color="#C0D6CC"><TrendCharts /></el-icon>
      <p>请填写条件并点击"开始推荐"获取智能推荐结果</p>
    </div>

    <!-- 匹配度因子说明 -->
    <div class="algo-card">
      <el-collapse>
        <el-collapse-item title="匹配度因子说明" name="algo">
          <div class="algo-content">
            <p><strong>匹配度评分计算方法：</strong></p>
            <p>匹配度 = 健康评分权重(30%) + 设备完整度权重(25%) + 日期可用性权重(25%) + 容量匹配度权重(20%)。</p>
            <p>健康评分：烤房当前综合健康评分，越高表示烤房状态越好。</p>
            <p>设备完整度：烤房设备部件完好率，影响烘烤质量稳定性。</p>
            <p>日期可用性：目标日期烤房是否空闲，空闲则满分。</p>
            <p>容量匹配度：烤房容量与烟叶重量的匹配程度，过载或过低均扣分。</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<style scoped>
.barn-recommend-page {
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

/* ========== 结果卡片 ========== */
.result-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: clamp(16px, 2vw, 24px);
  transition: all 0.3s ease;
}

.result-card:hover {
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

/* ========== 最高匹配行高亮 ========== */
:deep(.top-match-row) {
  background-color: rgba(10, 77, 62, 0.04) !important;
}

:deep(.top-match-row:hover > td) {
  background-color: rgba(10, 77, 62, 0.08) !important;
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
</style>
