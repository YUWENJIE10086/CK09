<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, DataAnalysis, Setting, Document, Check } from '@element-plus/icons-vue'
import { Database, CheckCheck, ChartBar, ArrowDown, ArrowUp } from 'lucide-vue-next'
import { getHealthAlgoDesc, applyHealthAlgo, getHealthAlgoList, getHealthAlgoStats } from '@/api/ai'

// ========== 算法列表 ==========
const algorithms = [
  { key: 'bmhi', name: 'A. BMHI', fullName: 'AHP层次加权', desc: '5部件×N子部件层次化评估', color: '#0A4D3E' },
  { key: 'fche', name: 'B. FCHE', fullName: '模糊综合评价', desc: '"较好/较差"模糊语言数学化', color: '#1A6B4F' },
  { key: 'topsis', name: 'C. TOPSIS', fullName: '熵权-TOPSIS排序', desc: '信息熵客观赋权+理想解贴近度', color: '#3A8B70' },
  { key: 'cdci', name: 'D. CDCI', fullName: '退化曲线积分', desc: '三段式退化函数(稳定→线性→加速)', color: '#8B6914' },
  { key: 'crhe', name: 'E. CRHE', fullName: '组合赋权融合', desc: 'AHP主观×熵权客观乘法合成', color: '#5A6A5A' },
]

const currentAlgo = ref('bmhi')
const algoDesc = ref<any>(null)
const algoParams = ref<any[]>([])
const paramValues = reactive<Record<string, number>>({})

// ========== 数据 ==========
const loading = ref(false)
const applyLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const stats = ref<any>(null)
const pagination = reactive({ pageNum: 1, pageSize: 10 })
const searchKeyword = ref('')

// ========== 计算属性 ==========
const currentAlgoInfo = computed(() => algorithms.find(a => a.key === currentAlgo.value))

// ========== 加载算法描述 ==========
async function loadAlgoDesc() {
  try {
    const res: any = await getHealthAlgoDesc(currentAlgo.value)
    algoDesc.value = res
    // 解析参数
    try {
      const params = JSON.parse(res.params || '[]')
      algoParams.value = params
      params.forEach((p: any) => {
        if (!(p.key in paramValues)) {
          paramValues[p.key] = p.default
        }
      })
    } catch { algoParams.value = [] }
  } catch {
    algoDesc.value = null
  }
}

// ========== 切换算法 ==========
async function switchAlgo(key: string) {
  currentAlgo.value = key
  pagination.pageNum = 1
  await loadAlgoDesc()
  await loadData()
  await loadStats()
}

// ========== 重置参数 ==========
function resetParams() {
  algoParams.value.forEach(p => {
    paramValues[p.key] = p.default
  })
  ElMessage.success('参数已重置为默认值')
}

// ========== 应用到所有烤房 ==========
async function handleApply() {
  try {
    await ElMessageBox.confirm(
      `确认使用「${currentAlgoInfo.value?.fullName}」算法计算所有烤房的健康分？\n这将写入数据库 health_${currentAlgo.value} 字段，不影响其他算法数据。`,
      '应用到所有烤房',
      { type: 'warning', confirmButtonText: '开始计算', cancelButtonText: '取消' }
    )
    applyLoading.value = true
    const params: any = { algo: currentAlgo.value, ...paramValues }
    const res: any = await applyHealthAlgo(params)
    ElMessage.success(`计算完成！成功${res.success}栋，失败${res.fail}栋`)
    await loadData()
    await loadStats()
  } catch { /* cancel */ } finally {
    applyLoading.value = false
  }
}

// ========== 加载列表 ==========
async function loadData() {
  loading.value = true
  try {
    const res: any = await getHealthAlgoList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      algo: currentAlgo.value,
      keyword: searchKeyword.value || undefined,
    })
    tableData.value = res?.rows || []
    total.value = res?.total ?? 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ========== 加载统计 ==========
async function loadStats() {
  try {
    stats.value = await getHealthAlgoStats(currentAlgo.value)
  } catch { stats.value = null }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handlePageChange(p: number) { pagination.pageNum = p; loadData() }
function handleSizeChange(s: number) { pagination.pageSize = s; pagination.pageNum = 1; loadData() }

function getScoreColor(score: number | null): string {
  if (score === null) return '#999'
  if (score >= 85) return '#2E8B6A'
  if (score >= 70) return '#5FB292'
  if (score >= 50) return '#D4944A'
  return '#D4604A'
}

function getLevelColor(level: string): string {
  const map: Record<string, string> = { '优良': '#2E8B6A', '良好': '#5FB292', '一般': '#D4944A', '较差': '#D4604A', '未计算': '#999' }
  return map[level] || '#999'
}

onMounted(() => {
  loadAlgoDesc()
  loadData()
  loadStats()
})
</script>

<template>
<div class="health-algo-page">
  <!-- 页面标题 -->
  <div class="page-header">
    <div class="header-left">
      <el-icon class="header-icon"><DataAnalysis /></el-icon>
      <div>
        <h2>烤房健康分算法</h2>
        <p>选择不同算法计算烤房健康度，支持参数调整和一键应用到全部烤房</p>
      </div>
    </div>
  </div>

  <!-- 算法选择卡片 -->
  <div class="algo-cards">
    <div
      v-for="algo in algorithms"
      :key="algo.key"
      class="algo-card"
      :class="{ active: currentAlgo === algo.key }"
      :style="{ '--algo-color': algo.color }"
      @click="switchAlgo(algo.key)"
    >
      <div class="algo-card-name">{{ algo.name }}</div>
      <div class="algo-card-full">{{ algo.fullName }}</div>
      <div class="algo-card-desc">{{ algo.desc }}</div>
      <el-icon v-if="currentAlgo === algo.key" class="algo-check"><Check /></el-icon>
    </div>
  </div>

  <!-- 统计概览 -->
  <div class="stats-row" v-if="stats">
    <div class="stat-item">
      <div class="stat-icon-wrapper"><Database :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">烤房总数</div>
      <div class="stat-value">{{ stats.total }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><CheckCheck :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">已计算</div>
      <div class="stat-value" style="color: #2E8B6A;">{{ stats.calculated }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><ChartBar :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">平均分</div>
      <div class="stat-value" style="color: #1A6B4F;">{{ stats.avgScore }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><ArrowDown :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">最低分</div>
      <div class="stat-value" style="color: #D4604A;">{{ stats.minScore ?? '-' }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><ArrowUp :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">最高分</div>
      <div class="stat-value" style="color: #2E8B6A;">{{ stats.maxScore ?? '-' }}</div>
    </div>
    <!-- 分布 -->
    <div class="stat-dist">
      <div class="stat-label">等级分布</div>
      <div class="dist-tags">
        <el-tag
          v-for="d in (stats.distribution || [])"
          :key="d.level"
          :type="d.level === '优良' ? 'success' : d.level === '良好' ? '' : d.level === '一般' ? 'warning' : d.level === '较差' ? 'danger' : 'info'"
          size="small"
          style="margin-right: 6px;"
        >
          {{ d.level }}: {{ d.cnt }}
        </el-tag>
      </div>
    </div>
  </div>

  <!-- 算法描述 + 参数调整 -->
  <div class="desc-section">
    <div class="desc-card">
      <div class="desc-header">
        <el-icon><Document /></el-icon>
        <span>算法描述</span>
        <el-tag size="small" type="success" style="margin-left: 8px;">当前: {{ currentAlgoInfo?.fullName }}</el-tag>
      </div>
      <div class="desc-body" v-if="algoDesc">
        <div class="desc-item">
          <span class="desc-label">模型名称：</span>{{ algoDesc.name }}
        </div>
        <div class="desc-item">
          <span class="desc-label">全称：</span>{{ algoDesc.fullName }}
        </div>
        <div class="desc-item">
          <span class="desc-label">算法原理：</span>{{ algoDesc.principle }}
        </div>
        <div class="desc-item">
          <span class="desc-label">核心公式：</span>
          <pre class="formula-pre">{{ algoDesc.formula }}</pre>
        </div>
        <div class="desc-item">
          <span class="desc-label">权重配置：</span>{{ algoDesc.weights }}
        </div>
        <div class="desc-item">
          <span class="desc-label">计算示例：</span>
          <pre class="example-pre">{{ algoDesc.example }}</pre>
        </div>
      </div>
      <el-empty v-else description="加载中..." :image-size="60" />
    </div>

    <!-- 参数调整面板 -->
    <div class="param-card" v-if="algoParams.length > 0">
      <div class="param-header">
        <el-icon><Setting /></el-icon>
        <span>参数调整</span>
        <el-button size="small" text @click="resetParams">重置默认</el-button>
      </div>
      <div class="param-body">
        <div class="param-item" v-for="p in algoParams" :key="p.key">
          <label>{{ p.label }}</label>
          <el-input-number
            v-model="paramValues[p.key]"
            :step="0.05"
            :min="0"
            :max="p.key.includes('alpha') || p.key.includes('beta') || p.key.includes('gamma') || p.key.startsWith('w') ? 1 : 100"
            :precision="p.key.includes('alpha') || p.key.includes('beta') || p.key.includes('gamma') || p.key.startsWith('w') || p.key.includes('Ratio') || p.key.includes('rMin') || p.key.includes('rMaint') ? 2 : 1"
            size="small"
            style="width: 130px;"
          />
          <span class="param-default">默认: {{ p.default }}</span>
        </div>
      </div>
      <div class="param-footer">
        <el-button
          type="primary"
          :loading="applyLoading"
          @click="handleApply"
          style="width: 100%; background: linear-gradient(135deg, #0A4D3E, #1A6B4F); border: none;"
        >
          <el-icon style="margin-right: 4px;"><Refresh /></el-icon>
          应用到所有烤房 ({{ stats?.total ?? 0 }}栋)
        </el-button>
        <p class="param-tip">结果将存入 health_{{ currentAlgo }} 字段，不影响其他算法数据</p>
      </div>
    </div>
  </div>

  <!-- 烤房健康分列表 -->
  <div class="table-section">
    <div class="table-header">
      <span class="table-title">健康分列表（{{ currentAlgoInfo?.fullName }}）</span>
      <div class="table-tools">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索烤房编号/乡镇/村"
          size="small"
          style="width: 220px;"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button size="small" @click="handleSearch">搜索</el-button>
        <el-button size="small" @click="loadData" :icon="Refresh">刷新</el-button>
      </div>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe style="width: 100%;">
      <el-table-column prop="projectId" label="烤房编号" width="180" />
      <el-table-column prop="county" label="县区" width="100" />
      <el-table-column prop="township" label="乡镇" width="100" />
      <el-table-column prop="village" label="村" width="100" />
      <el-table-column prop="useStatus" label="使用状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.useStatus === '在用' ? 'success' : row.useStatus === '闲置' ? 'info' : 'warning'" size="small">
            {{ row.useStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="healthScore" label="健康分" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.healthScore != null && typeof row.healthScore === 'number'" class="score-value" :style="{ color: getScoreColor(row.healthScore) }">
            {{ row.healthScore.toFixed(1) }}
          </span>
          <span v-else style="color: #ccc;">未计算</span>
        </template>
      </el-table-column>
      <el-table-column prop="healthLevel" label="健康等级" width="90" align="center">
        <template #default="{ row }">
          <el-tag :color="getLevelColor(row.healthLevel)" effect="dark" size="small" style="border: none;">
            {{ row.healthLevel }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-row">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</div>
</template>

<style scoped>
.health-algo-page { padding: 12px; }
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { font-size: 28px; color: #0A4D3E; }
.page-header h2 { font-size: 20px; color: #0A4D3E; margin: 0; }
.page-header p { font-size: 13px; color: #6c7a89; margin: 4px 0 0; }

.algo-cards {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 20px;
}
.algo-card {
  position: relative; padding: 16px 14px; border-radius: 8px; cursor: pointer;
  background: white; border: 2px solid #e0e5e3; transition: all 0.3s;
}
.algo-card:hover {
  border-color: var(--algo-color); box-shadow: 0 4px 16px rgba(10, 77, 62, 0.12);
  transform: translateY(-2px);
}
.algo-card.active {
  border-color: var(--algo-color); background: linear-gradient(135deg, var(--algo-color)08, white);
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.2);
}
.algo-card-name { font-size: 16px; font-weight: 700; color: var(--algo-color); }
.algo-card-full { font-size: 13px; font-weight: 600; color: #2c3e50; margin: 4px 0; }
.algo-card-desc { font-size: 12px; color: #6c7a89; line-height: 1.5; }
.algo-check {
  position: absolute; top: 8px; right: 8px; color: var(--algo-color); font-size: 18px;
}

.stats-row {
  display: flex; gap: 16px; margin-bottom: 20px; padding: 16px 20px;
  background: white; border-radius: 8px; border: 1px solid #e0e5e3;
  flex-wrap: wrap; align-items: center;
}
.stat-item { text-align: center; min-width: 80px; }
.stat-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0D5D46, #3A9B7A);
  margin: 0 auto 8px;
}
.stat-label { font-size: 12px; color: #6c7a89; }
.stat-value { font-size: 24px; font-weight: 700; color: #0A4D3E; margin-top: 2px; }
.stat-dist { flex: 1; min-width: 200px; }
.dist-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }

.desc-section { display: grid; grid-template-columns: 1fr 320px; gap: 16px; margin-bottom: 20px; }
.desc-card, .param-card {
  background: white; border-radius: 8px; border: 1px solid #e0e5e3; overflow: hidden;
}
.desc-header, .param-header {
  display: flex; align-items: center; gap: 8px; padding: 12px 16px;
  background: #f5f7f6; border-bottom: 1px solid #e0e5e3;
  font-size: 14px; font-weight: 600; color: #0A4D3E;
}
.param-header { justify-content: space-between; }
.desc-body, .param-body { padding: 16px; }
.desc-item { margin-bottom: 12px; font-size: 13px; line-height: 1.8; }
.desc-label { font-weight: 600; color: #0A4D3E; }
.formula-pre {
  background: #f0f7f4; border-left: 3px solid #1A6B4F; padding: 10px 14px;
  border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap;
  font-family: 'Cambria Math', monospace; margin-top: 6px; line-height: 1.6;
}
.example-pre {
  background: #fff8e1; border-left: 3px solid #B8860B; padding: 10px 14px;
  border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap;
  margin-top: 6px; line-height: 1.6;
}

.param-item {
  display: flex; align-items: center; gap: 8px; margin-bottom: 12px;
}
.param-item label { font-size: 13px; min-width: 110px; color: #2c3e50; }
.param-default { font-size: 11px; color: #999; }
.param-footer { padding: 12px 16px; border-top: 1px solid #e0e5e3; }
.param-tip { font-size: 11px; color: #999; text-align: center; margin-top: 8px; }

.table-section {
  background: white; border-radius: 8px; border: 1px solid #e0e5e3; overflow: hidden;
}
.table-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid #e0e5e3;
}
.table-title { font-size: 14px; font-weight: 600; color: #0A4D3E; }
.table-tools { display: flex; gap: 8px; }
.score-value { font-size: 18px; font-weight: 700; }
.pagination-row { padding: 12px 16px; display: flex; justify-content: flex-end; }

@media (max-width: 1024px) {
  .algo-cards { grid-template-columns: repeat(2, 1fr); }
  .desc-section { grid-template-columns: 1fr; }
}
</style>
