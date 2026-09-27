<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Timer, Setting, Document, Check } from '@element-plus/icons-vue'
import { Hourglass, ListChecks, CalendarDays, Clock, Watch } from 'lucide-vue-next'
import { getLifeAlgoDesc, applyLifeAlgo, getLifeAlgoList, getLifeAlgoStats } from '@/api/ai'

// ========== 算法列表 ==========
const algorithms = [
  { key: 'edrl', name: 'A. EDRL', fullName: '指数衰减', desc: 'H(t)=H₀×e^(-λt)，衰减系数反推', color: '#0A4D3E' },
  { key: 'wrrl', name: 'B. WRRL', fullName: '威布尔分布', desc: 'R(t)=e^(-(t/η)^β)，可靠性工程标准', color: '#1A6B4F' },
  { key: 'bdrl', name: 'C. BDRL', fullName: '双段退化(推荐)', desc: '线性+二次退化+维修回弹效应', color: '#8B6914' },
  { key: 'gple', name: 'D. GPLE', fullName: '灰色预测GM(1,1)', desc: '3期健康分序列拟合趋势外推', color: '#5A6A5A' },
]

const currentAlgo = ref('bdrl')
const algoDesc = ref<any>(null)
const algoParams = ref<any[]>([])
const paramValues = reactive<Record<string, number>>({})

const loading = ref(false)
const applyLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const stats = ref<any>(null)
const pagination = reactive({ pageNum: 1, pageSize: 10 })
const searchKeyword = ref('')

const currentAlgoInfo = computed(() => algorithms.find(a => a.key === currentAlgo.value))

async function loadAlgoDesc() {
  try {
    const res: any = await getLifeAlgoDesc(currentAlgo.value)
    algoDesc.value = res
    try {
      const params = JSON.parse(res.params || '[]')
      algoParams.value = params
      params.forEach((p: any) => {
        if (!(p.key in paramValues)) paramValues[p.key] = p.default
      })
    } catch { algoParams.value = [] }
  } catch { algoDesc.value = null }
}

async function switchAlgo(key: string) {
  currentAlgo.value = key
  pagination.pageNum = 1
  await loadAlgoDesc()
  await loadData()
  await loadStats()
}

function resetParams() {
  algoParams.value.forEach(p => { paramValues[p.key] = p.default })
  ElMessage.success('参数已重置为默认值')
}

async function handleApply() {
  try {
    await ElMessageBox.confirm(
      `确认使用「${currentAlgoInfo.value?.fullName}」算法计算所有烤房的剩余寿命？\n这将写入数据库 life_${currentAlgo.value} 字段，不影响其他算法数据。`,
      '应用到所有烤房',
      { type: 'warning', confirmButtonText: '开始计算', cancelButtonText: '取消' }
    )
    applyLoading.value = true
    const params: any = { algo: currentAlgo.value, ...paramValues }
    const res: any = await applyLifeAlgo(params)
    ElMessage.success(`计算完成！成功${res.success}栋，失败${res.fail}栋`)
    await loadData()
    await loadStats()
  } catch { /* cancel */ } finally {
    applyLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getLifeAlgoList({
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
  } finally { loading.value = false }
}

async function loadStats() {
  try { stats.value = await getLifeAlgoStats(currentAlgo.value) }
  catch { stats.value = null }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handlePageChange(p: number) { pagination.pageNum = p; loadData() }
function handleSizeChange(s: number) { pagination.pageSize = s; pagination.pageNum = 1; loadData() }

function getLifeColor(years: number | null): string {
  if (years === null) return '#999'
  if (years <= 1) return '#D4604A'
  if (years <= 3) return '#D4944A'
  if (years <= 5) return '#5FB292'
  return '#2E8B6A'
}

function getWarnColor(level: string): string {
  const map: Record<string, string> = { '紧急维修': '#D4604A', '需维修': '#D4944A', '需关注': '#5FB292', '正常': '#2E8B6A', '未计算': '#999' }
  return map[level] || '#999'
}

onMounted(() => { loadAlgoDesc(); loadData(); loadStats() })
</script>

<template>
<div class="life-predict-page">
  <div class="page-header">
    <div class="header-left">
      <el-icon class="header-icon"><Timer /></el-icon>
      <div>
        <h2>烤房寿命预测</h2>
        <p>选择不同衰减函数预测烤房剩余寿命和维修预警时间</p>
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
      <div class="stat-icon-wrapper"><Hourglass :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">烤房总数</div>
      <div class="stat-value">{{ stats.total }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><ListChecks :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">已计算</div>
      <div class="stat-value" style="color: #2E8B6A;">{{ stats.calculated }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><CalendarDays :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">平均剩余(年)</div>
      <div class="stat-value" style="color: #1A6B4F;">{{ stats.avgLife }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><Clock :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">最短(年)</div>
      <div class="stat-value" style="color: #D4604A;">{{ stats.minLife ?? '-' }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><Watch :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">最长(年)</div>
      <div class="stat-value" style="color: #2E8B6A;">{{ stats.maxLife ?? '-' }}</div>
    </div>
    <div class="stat-dist">
      <div class="stat-label">预警分布</div>
      <div class="dist-tags">
        <el-tag
          v-for="d in (stats.distribution || [])"
          :key="d.level"
          :type="d.level === '紧急维修' ? 'danger' : d.level === '需维修' ? 'warning' : d.level === '需关注' ? '' : d.level === '正常' ? 'success' : 'info'"
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
        <div class="desc-item"><span class="desc-label">模型名称：</span>{{ algoDesc.name }}</div>
        <div class="desc-item"><span class="desc-label">全称：</span>{{ algoDesc.fullName }}</div>
        <div class="desc-item"><span class="desc-label">算法原理：</span>{{ algoDesc.principle }}</div>
        <div class="desc-item">
          <span class="desc-label">核心公式：</span>
          <pre class="formula-pre">{{ algoDesc.formula }}</pre>
        </div>
        <div class="desc-item">
          <span class="desc-label">计算示例：</span>
          <pre class="example-pre">{{ algoDesc.example }}</pre>
        </div>
      </div>
      <el-empty v-else description="加载中..." :image-size="60" />
    </div>

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
            :step="p.key.includes('beta') || p.key.includes('rMin') || p.key.includes('rMaint') || p.key.includes('Ratio') ? 0.1 : 1"
            :min="0"
            :precision="p.key.includes('beta') || p.key.includes('rMin') || p.key.includes('rMaint') || p.key.includes('Ratio') ? 2 : 1"
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
        <p class="param-tip">结果将存入 life_{{ currentAlgo }} 字段，不影响其他算法数据</p>
      </div>
    </div>
  </div>

  <!-- 寿命预测列表 -->
  <div class="table-section">
    <div class="table-header">
      <span class="table-title">剩余寿命列表（{{ currentAlgoInfo?.fullName }}）</span>
      <div class="table-tools">
        <el-input v-model="searchKeyword" placeholder="搜索烤房编号/乡镇" size="small" style="width: 220px;" clearable @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-button size="small" @click="handleSearch">搜索</el-button>
        <el-button size="small" @click="loadData" :icon="Refresh">刷新</el-button>
      </div>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe style="width: 100%;">
      <el-table-column prop="projectId" label="烤房编号" width="180" />
      <el-table-column prop="county" label="县区" width="100" />
      <el-table-column prop="township" label="乡镇" width="100" />
      <el-table-column prop="village" label="村" width="100" />
      <el-table-column prop="finishDate" label="竣工日期" width="120" />
      <el-table-column prop="useStatus" label="使用状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.useStatus === '在用' ? 'success' : 'info'" size="small">{{ row.useStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lifeResidual" label="剩余寿命(年)" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.lifeResidual != null && typeof row.lifeResidual === 'number'" class="score-value" :style="{ color: getLifeColor(row.lifeResidual) }">
            {{ row.lifeResidual.toFixed(1) }}
          </span>
          <span v-else style="color: #ccc;">未计算</span>
        </template>
      </el-table-column>
      <el-table-column prop="warnLevel" label="预警等级" width="100" align="center">
        <template #default="{ row }">
          <el-tag :color="getWarnColor(row.warnLevel)" effect="dark" size="small" style="border: none;">
            {{ row.warnLevel }}
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
.life-predict-page { padding: 12px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { font-size: 28px; color: #0A4D3E; }
.page-header h2 { font-size: 20px; color: #0A4D3E; margin: 0; }
.page-header p { font-size: 13px; color: #6c7a89; margin: 4px 0 0; }
.algo-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.algo-card { position: relative; padding: 16px 14px; border-radius: 8px; cursor: pointer; background: white; border: 2px solid #e0e5e3; transition: all 0.3s; }
.algo-card:hover { border-color: var(--algo-color); box-shadow: 0 4px 16px rgba(10, 77, 62, 0.12); transform: translateY(-2px); }
.algo-card.active { border-color: var(--algo-color); box-shadow: 0 4px 16px rgba(10, 77, 62, 0.2); }
.algo-card-name { font-size: 16px; font-weight: 700; color: var(--algo-color); }
.algo-card-full { font-size: 13px; font-weight: 600; color: #2c3e50; margin: 4px 0; }
.algo-card-desc { font-size: 12px; color: #6c7a89; line-height: 1.5; }
.algo-check { position: absolute; top: 8px; right: 8px; color: var(--algo-color); font-size: 18px; }
.stats-row { display: flex; gap: 16px; margin-bottom: 20px; padding: 16px 20px; background: white; border-radius: 8px; border: 1px solid #e0e5e3; flex-wrap: wrap; align-items: center; }
.stat-item { text-align: center; min-width: 80px; }
.stat-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8B6914, #D4A017);
  margin: 0 auto 8px;
}
.stat-label { font-size: 12px; color: #6c7a89; }
.stat-value { font-size: 24px; font-weight: 700; color: #0A4D3E; margin-top: 2px; }
.stat-dist { flex: 1; min-width: 200px; }
.dist-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.desc-section { display: grid; grid-template-columns: 1fr 320px; gap: 16px; margin-bottom: 20px; }
.desc-card, .param-card { background: white; border-radius: 8px; border: 1px solid #e0e5e3; overflow: hidden; }
.desc-header, .param-header { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: #f5f7f6; border-bottom: 1px solid #e0e5e3; font-size: 14px; font-weight: 600; color: #0A4D3E; }
.param-header { justify-content: space-between; }
.desc-body, .param-body { padding: 16px; }
.desc-item { margin-bottom: 12px; font-size: 13px; line-height: 1.8; }
.desc-label { font-weight: 600; color: #0A4D3E; }
.formula-pre { background: #f0f7f4; border-left: 3px solid #1A6B4F; padding: 10px 14px; border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap; font-family: 'Cambria Math', monospace; margin-top: 6px; line-height: 1.6; }
.example-pre { background: #fff8e1; border-left: 3px solid #B8860B; padding: 10px 14px; border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap; margin-top: 6px; line-height: 1.6; }
.param-item { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.param-item label { font-size: 13px; min-width: 120px; color: #2c3e50; }
.param-default { font-size: 11px; color: #999; }
.param-footer { padding: 12px 16px; border-top: 1px solid #e0e5e3; }
.param-tip { font-size: 11px; color: #999; text-align: center; margin-top: 8px; }
.table-section { background: white; border-radius: 8px; border: 1px solid #e0e5e3; overflow: hidden; }
.table-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid #e0e5e3; }
.table-title { font-size: 14px; font-weight: 600; color: #0A4D3E; }
.table-tools { display: flex; gap: 8px; }
.score-value { font-size: 18px; font-weight: 700; }
.pagination-row { padding: 12px 16px; display: flex; justify-content: flex-end; }
@media (max-width: 1024px) { .algo-cards { grid-template-columns: repeat(2, 1fr); } .desc-section { grid-template-columns: 1fr; } }
</style>
