<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Timer, Setting, Document, Check, Warning, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { Timer as LucideTimer, ClipboardCheck, CalendarClock, AlarmClock, Infinity as InfinityIcon } from 'lucide-vue-next'
import { applyLifePredictFinal, getLifePredictFinalList, getLifePredictFinalStats } from '@/api/ai'

// ========== 算法信息（硬编码） ==========
const algorithmInfo = {
  name: 'BDRL 双段退化剩余寿命模型',
  fullName: 'Bi-segment Degradation Residual Life',
  principle: '以烤房健康分（三维融合算法结果）为核心输入，用「真实健康分反推等效健康年龄」预测剩余寿命（BDRL双段退化·升级版），摆脱旧版"按物理房龄定曲线、超龄即归零"的缺陷。核心逻辑：①维修真实计数——读取维修台账中「已验收/审核通过 + 完工日期非空」的记录条数，每条真实延寿ΔT年（替代旧版"每8年估1次"）；②健康分反推等效年龄——健康分越高（修缮维护越好）等效越年轻，退化曲线位置由健康分而非物理房龄决定，因此"老而健康者不再被误判为0年"；③沿退化曲线外推到失效阈值H_min，得到剩余寿命与退役年份，并映射为紧急维修/建议维修/需关注/正常四级预警。',
  formula: '退化参数: 转折年Tc=8, 转折分Hc=60, 理论寿命Tend=12, 失效阈H_min=40, 单次维修延寿ΔT年\n\n① 维修真实计数: realRepair = COUNT(维修台账 已验收/审核通过 且 完工日期非空)\n   理论寿命调整: Tendt = Tend + realRepair×ΔT\n\n② 健康分反推等效年龄 t_eq (h0为三维健康分):\n   若 h0≥Hc: t_eq=(100-h0)/k1, 其中 k1=(100-Hc)/Tc\n   否则: t_eq=Tc+√(1-h0/Hc)×(Tendt-Tc)\n\n③ 失效点外推 (按t_eq所在段):\n   t_eq≤Tc: t_end=(100-H_min)/k1\n   否则: t_end=Tc+√(1-H_min/Hc)×(Tendt-Tc)\n\n剩余寿命: RUL = t_end - t_eq, 0≤RUL≤12年',
  weights: '退化参数: Tc=8(转折年), Hc=60(转折分), Tend=12(理论寿命), H_min=40(失效阈)\n维修效应: 每条真实维修记录延寿ΔT=2.5年\n寿命上限: 12年（极限寿命）\n\n剩余寿命分级（预警）:\n  ≤0年: 紧急维修\n  0-4年: 建议维修\n  4-7年: 需关注\n  >7年: 正常',
  example: '两个真实示例烤房（与系统计算结果一致）:\n\n① 老房 09-420527KF00018 (2009建, 当前2026年):\n   健康分 h0=85.2（三维融合算法算得）\n   该房无"已验收"维修 → realRepair=0, Tendt=12\n   h0≥Hc → t_eq=(100-85.2)/5=2.96年\n   t_eq≤Tc → t_end=(100-40)/5=12\n   RUL = 12-2.96 = 9.04 → 约9.0年 → 正常 (退役2035)\n   升级前误判为0年/紧急维修，升级后修正\n\n② 新房 25-420527KF00023 (2025建):\n   健康分 h0=95.2， realRepair=0\n   t_eq=(100-95.2)/5=0.96年\n   RUL=12-0.96=11.04 → 11.0年 → 正常 (退役2037)',
  params: [
    { key: 'tc', label: '转折点Tc(年)', default: 8 },
    { key: 'hc', label: '转折点健康分Hc', default: 60 },
    { key: 'tend', label: '极限寿命Tend(年)', default: 12 },
    { key: 'hMin', label: '退役阈值H_min', default: 40 },
    { key: 'repairBoost', label: '维修回弹ΔH', default: 12 },
    { key: 'repairExtend', label: '维修延寿ΔT(年)', default: 2.5 },
  ],
}

// ========== 预警等级 ==========
const warningLevels = [
  { range: '>7年剩余', level: '正常', color: '#2E8B6A', bg: '#E8F5E9', desc: '烤房状态良好，可继续使用' },
  { range: '4-7年剩余', level: '需关注', color: '#5FB292', bg: '#E0F2EE', desc: '建议制定维护计划' },
  { range: '0-4年剩余', level: '建议维修', color: '#D4944A', bg: '#FFF3E0', desc: '需要安排维修更换' },
  { range: '≤0年剩余', level: '紧急维修', color: '#D4604A', bg: '#FFEBEE', desc: '剩余寿命≤0年，健康分跌破失效阈值，需立即维修或评估报废' },
]

// ========== 参数 ==========
const algoParams = computed(() => algorithmInfo.params)
const paramValues = reactive<Record<string, number>>({})
algorithmInfo.params.forEach((p: any) => { paramValues[p.key] = p.default })

// ========== 数据 ==========
const loading = ref(false)
const applyLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const stats = ref<any>(null)
const pagination = reactive({ pageNum: 1, pageSize: 10 })
const searchKeyword = ref('')
const warnLevelFilter = ref('')

// ========== 算法描述折叠（整体展开/折叠，默认折叠） ==========
const algoExpanded = ref(false)
function toggleAlgoDesc() {
  algoExpanded.value = !algoExpanded.value
}

// ========== 重置参数 ==========
function resetParams() {
  algorithmInfo.params.forEach((p: any) => { paramValues[p.key] = p.default })
  ElMessage.success('参数已重置为默认值')
}

// ========== 应用到所有烤房 ==========
async function handleApply() {
  try {
    await ElMessageBox.confirm(
      `确认使用「${algorithmInfo.fullName}」算法计算所有烤房的剩余寿命？\n这将写入数据库 life_residual_final 字段，需先计算健康分。`,
      '应用到所有烤房',
      { type: 'warning', confirmButtonText: '开始计算', cancelButtonText: '取消' }
    )
    applyLoading.value = true
    const params: any = { ...paramValues }
    const res: any = await applyLifePredictFinal(params)
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
    const res: any = await getLifePredictFinalList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchKeyword.value || undefined,
      warnLevel: warnLevelFilter.value || undefined,
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
    stats.value = await getLifePredictFinalStats()
  } catch { stats.value = null }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handlePageChange(p: number) { pagination.pageNum = p; loadData() }
function handleSizeChange(s: number) { pagination.pageSize = s; pagination.pageNum = 1; loadData() }

function getLifeColor(years: number | null): string {
  if (years === null) return '#999'
  if (years <= 0) return '#D4604A'
  if (years <= 4) return '#D4944A'
  if (years <= 7) return '#5FB292'
  return '#2E8B6A'
}

function getWarnColor(level: string): string {
  const map: Record<string, string> = { '紧急维修': '#D4604A', '建议维修': '#D4944A', '需关注': '#5FB292', '正常': '#2E8B6A', '未计算': '#999' }
  return map[level] || '#999'
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<template>
<div class="barn-life-predict-page">
  <!-- 页面标题 -->
  <div class="page-header">
    <div class="header-left">
      <el-icon class="header-icon"><Timer /></el-icon>
      <div>
        <h2>烤房寿命预测算法</h2>
        <p>BDRL 双段退化剩余寿命模型 — 12年极限寿命+维修延寿+分级预警</p>
      </div>
    </div>
  </div>

  <!-- 单算法横幅 -->
  <div class="algo-banner">
    <div class="banner-left">
      <el-icon class="banner-icon"><Timer /></el-icon>
      <div>
        <div class="banner-name">BDRL 双段退化模型</div>
        <div class="banner-full">{{ algorithmInfo.fullName }}</div>
      </div>
      <el-icon class="banner-check"><Check /></el-icon>
    </div>
  </div>

  <!-- 统计概览 -->
  <div class="stats-row" v-if="stats">
    <div class="stat-item">
      <div class="stat-icon-wrapper"><LucideTimer :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">烤房总数</div>
      <div class="stat-value">{{ stats.total }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><ClipboardCheck :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">已计算</div>
      <div class="stat-value" style="color: #2E8B6A;">{{ stats.calculated }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><CalendarClock :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">平均剩余寿命</div>
      <div class="stat-value" style="color: #1A6B4F;">{{ stats.avgLife }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><AlarmClock :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">最短寿命</div>
      <div class="stat-value" style="color: #D4604A;">{{ stats.minLife ?? '-' }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><InfinityIcon :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">最长寿命</div>
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

  <!-- 预警等级说明 -->
  <div class="warn-section">
    <div class="warn-header">
      <el-icon><Warning /></el-icon>
      <span>预警等级说明</span>
    </div>
    <div class="warn-cards">
      <div
        v-for="w in warningLevels"
        :key="w.level"
        class="warn-card"
        :style="{ '--w-color': w.color, '--w-bg': w.bg }"
      >
        <div class="warn-card-top">
          <span class="warn-range" :style="{ color: w.color }">{{ w.range }}</span>
          <el-icon
            v-if="w.level === '建议维修' || w.level === '紧急维修'"
            class="warn-icon"
            :style="{ color: w.color }"
          >
            <Warning />
          </el-icon>
        </div>
        <div class="warn-level" :style="{ color: w.color }">{{ w.level }}</div>
        <div class="warn-desc">{{ w.desc }}</div>
      </div>
    </div>
  </div>

  <!-- 寿命预测列表 -->
  <div class="table-section">
    <div class="table-header">
      <span class="table-title">剩余寿命列表（BDRL）</span>
      <div class="table-tools">
        <el-select
          v-model="warnLevelFilter"
          placeholder="全部等级"
          size="small"
          style="width: 120px;"
          clearable
          @change="handleSearch"
        >
          <el-option label="正常" value="正常" />
          <el-option label="需关注" value="需关注" />
          <el-option label="建议维修" value="建议维修" />
          <el-option label="紧急维修" value="紧急维修" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索烤房编号/乡镇"
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
          <el-tag :type="row.useStatus === '在烤' ? 'success' : 'info'" size="small">{{ row.useStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lifeResidual" label="剩余寿命(年)" width="120" align="center">
        <template #default="{ row }">
          <span
            v-if="row.lifeResidual != null && typeof row.lifeResidual === 'number'"
            class="score-value"
            :style="{ color: getLifeColor(row.lifeResidual) }"
          >
            {{ row.lifeResidual.toFixed(1) }}
          </span>
          <span v-else style="color: #ccc;">未计算</span>
        </template>
      </el-table-column>
      <el-table-column prop="retireYear" label="预测退役年份" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.retireYear">{{ row.retireYear }}</span>
          <span v-else-if="row.lifeResidual != null">{{ new Date().getFullYear() + Math.ceil(row.lifeResidual) }}</span>
          <span v-else style="color: #ccc;">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="warnLevel" label="预警状态" width="110" align="center">
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

  <!-- 算法描述 + 参数调整（放在列表下方） -->
  <div class="desc-section">
    <div class="desc-card">
      <div class="desc-header">
        <el-icon><Document /></el-icon>
        <span>算法描述</span>
        <el-tag size="small" type="success" style="margin-left: 8px;">当前: BDRL</el-tag>
      </div>
      <div class="desc-body">
        <div class="desc-item">
          <span class="desc-label">模型名称：</span>{{ algorithmInfo.name }}
        </div>
        <div class="desc-item">
          <span class="desc-label">全称：</span>{{ algorithmInfo.fullName }}
        </div>
        <div class="algo-toggle-bar">
          <el-button
            :type="algoExpanded ? 'primary' : 'default'"
            size="small"
            plain
            @click="toggleAlgoDesc"
          >
            <el-icon style="margin-right: 4px;">
              <component :is="algoExpanded ? 'ArrowUp' : 'ArrowDown'" />
            </el-icon>
            {{ algoExpanded ? '收起算法详情' : '展开算法详情' }}
          </el-button>
        </div>
        <transition name="algo-slide">
          <div v-show="algoExpanded" class="algo-detail-content">
            <div class="algo-section">
              <h4 class="algo-section-title">算法原理</h4>
              <div class="desc-item-text">{{ algorithmInfo.principle }}</div>
            </div>
            <div class="algo-section">
              <h4 class="algo-section-title">计算公式</h4>
              <pre class="formula-pre">{{ algorithmInfo.formula }}</pre>
            </div>
            <div class="algo-section">
              <h4 class="algo-section-title">参数说明</h4>
              <pre class="weights-pre">{{ algorithmInfo.weights }}</pre>
            </div>
            <div class="algo-section">
              <h4 class="algo-section-title">计算示例</h4>
              <pre class="example-pre">{{ algorithmInfo.example }}</pre>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- 参数调整面板 -->
    <div class="param-card">
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
            :step="p.key === 'repairBoost' || p.key === 'repairExtend' ? 0.1 : 1"
            :min="0"
            :precision="p.key === 'repairBoost' || p.key === 'repairExtend' ? 1 : 0"
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
        <p class="param-tip">结果将存入 life_residual_final 字段，需先计算健康分</p>
      </div>
    </div>
  </div>
</div>
</template>

<style scoped>
.barn-life-predict-page { padding: 12px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { font-size: 28px; color: #0A4D3E; }
.page-header h2 { font-size: 20px; color: #0A4D3E; margin: 0; }
.page-header p { font-size: 13px; color: #6c7a89; margin: 4px 0 0; }

/* 单算法横幅 */
.algo-banner {
  background: linear-gradient(135deg, #0A4D3E 0%, #1A6B4F 60%, #2E8B6A 100%);
  border-radius: 10px;
  padding: 12px 18px;
  margin-bottom: 12px;
  color: #fff;
  box-shadow: 0 6px 20px rgba(10, 77, 62, 0.25);
  position: relative;
  overflow: hidden;
}
.algo-banner::after {
  content: '';
  position: absolute;
  right: -40px;
  top: -40px;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(255,255,255,0.12) 0%, transparent 70%);
  border-radius: 50%;
}
.banner-left { display: flex; align-items: center; gap: 14px; margin-bottom: 10px; }
.banner-icon { font-size: 32px; color: #fff; }
.banner-name { font-size: 18px; font-weight: 700; color: #fff; }
.banner-full { font-size: 13px; color: rgba(255,255,255,0.85); margin-top: 2px; }
.banner-check { margin-left: auto; font-size: 22px; color: #fff; }
.banner-desc { font-size: 13px; color: rgba(255,255,255,0.9); line-height: 1.7; }

/* 统计概览 */
.stats-row {
  display: flex; gap: 16px; margin-bottom: 20px; padding: 16px 20px;
  background: white; border-radius: 8px; border: 1px solid #e0e5e3;
  flex-wrap: wrap; align-items: center;
}
.stat-item { text-align: center; min-width: 90px; }
.stat-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1A6B4F, #5FB292);
  margin: 0 auto 8px;
}
.stat-label { font-size: 12px; color: #6c7a89; }
.stat-value { font-size: 24px; font-weight: 700; color: #0A4D3E; margin-top: 2px; }
.stat-dist { flex: 1; min-width: 200px; }
.dist-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }

/* 算法描述 + 参数 */
.desc-section { display: grid; grid-template-columns: 1fr 320px; gap: 12px; margin-top: 12px; }
.desc-card, .param-card {
  background: white; border-radius: 8px; border: 1px solid #e0e5e3; overflow: hidden;
}
.desc-header, .param-header {
  display: flex; align-items: center; gap: 8px; padding: 8px 14px;
  background: #f5f7f6; border-bottom: 1px solid #e0e5e3;
  font-size: 14px; font-weight: 600; color: #0A4D3E;
}
.param-header { justify-content: space-between; }
.desc-body, .param-body { padding: 10px 14px; }
.desc-item { margin-bottom: 6px; font-size: 13px; line-height: 1.6; }
.desc-label { font-weight: 600; color: #0A4D3E; }
.desc-item-text { font-size: 13px; line-height: 1.8; color: #2c3e50; }

/* 算法详情整体折叠 */
.algo-toggle-bar {
  margin: 8px 0 4px;
}
.algo-detail-content {
  margin-top: 8px;
}
.algo-section {
  margin-bottom: 12px;
}
.algo-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #0A4D3E;
  margin: 0 0 4px;
  padding-left: 8px;
  border-left: 3px solid #2E8B6A;
}
.algo-slide-enter-active, .algo-slide-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.algo-slide-enter-from, .algo-slide-leave-to {
  opacity: 0;
  max-height: 0;
}
.algo-slide-enter-to, .algo-slide-leave-from {
  opacity: 1;
  max-height: 2000px;
}

.formula-pre {
  background: #f0f7f4; border-left: 3px solid #1A6B4F; padding: 10px 14px;
  border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap;
  font-family: 'Cambria Math', monospace; margin-top: 6px; line-height: 1.6;
}
.weights-pre {
  background: #f0f7f4; border-left: 3px solid #3A8B70; padding: 10px 14px;
  border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap;
  font-family: 'Cambria Math', monospace; margin-top: 6px; line-height: 1.6;
}
.example-pre {
  background: #fff8e1; border-left: 3px solid #B8860B; padding: 10px 14px;
  border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap;
  margin-top: 6px; line-height: 1.6;
}
.param-item { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.param-item label { font-size: 13px; min-width: 130px; color: #2c3e50; }
.param-default { font-size: 11px; color: #999; }
.param-footer { padding: 12px 16px; border-top: 1px solid #e0e5e3; }
.param-tip { font-size: 11px; color: #999; text-align: center; margin-top: 8px; }

/* 预警等级说明 */
.warn-section {
  background: white; border-radius: 8px; border: 1px solid #e0e5e3;
  overflow: hidden; margin-bottom: 20px;
}
.warn-header {
  display: flex; align-items: center; gap: 8px; padding: 12px 16px;
  background: #f5f7f6; border-bottom: 1px solid #e0e5e3;
  font-size: 14px; font-weight: 600; color: #0A4D3E;
}
.warn-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; padding: 16px; }
.warn-card {
  background: var(--w-bg);
  border: 1px solid var(--w-color);
  border-radius: 8px;
  padding: 14px 16px;
  position: relative;
  transition: all 0.3s;
}
.warn-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.warn-card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.warn-range { font-size: 13px; font-weight: 700; }
.warn-icon { font-size: 18px; }
.warn-level { font-size: 18px; font-weight: 700; margin-bottom: 4px; }
.warn-desc { font-size: 12px; color: #555; line-height: 1.5; }

/* 表格 */
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
  .desc-section { grid-template-columns: 1fr; }
  .warn-cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
