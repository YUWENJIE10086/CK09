<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, DataAnalysis, Setting, Document, Check, Warning, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { Activity, CircleCheckBig, Gauge, CircleArrowDown, CircleArrowUp } from 'lucide-vue-next'
import { applyHealthScoreFinal, getHealthScoreFinalList, getHealthScoreFinalStats, updateHealthScore3Dim } from '@/api/ai'

// ========== 算法信息（硬编码） ==========
const algorithmInfo = {
  name: '三维融合烤房健康分算法',
  fullName: '部件健康分80% + 管护制度分10% + 人工评价分10%',
  principle: '在 BMHI 层次加权框架与 CDCI 三段式退化函数基础上，融合「客观硬件 + 管理因素 + 人工反馈」三个维度：①部件健康分（权重80%）：由状态分、退化分、评价分三维加权构成，退化分采用三段式模型（稳定→线性→加速劣化）；②管护制度分（10%）：按集中/分户管护赋分；③人工评价分（10%）：按好/中/差赋分。退化年龄通过「维修台账最新完工年份」重置，维修结束后健康分回弹，真实反映"修一修、更健康"的维护效应。',
  formula: 'H = W_comp×C_comp + W_mgmt×S_mgmt + W_manual×S_manual\n（W_comp=0.80 部件, W_mgmt=0.10 管护, W_manual=0.10 人工）\n\n部件健康分 C_comp = Σ(Wi×Ci)，i=JR,SR,ZK,ZT\nCi = α×S_status + β×S_degrade + γ×S_eval\n\nS_status: 正常=100, 异常=50, 损坏=0\nS_eval: 较好=80, 一般=60, 较差=50, 差=30\nS_degrade: CDCI三段式退化函数\n  稳定区(Δt≤0.2L): 100\n  线性区(0.2L<Δt≤0.7L): 100-k1×(Δt-0.2L)\n  加速劣化区(Δt>0.7L): 30×e^(-k2×(Δt-0.7L))\n\n管护制分 S_mgmt: 集中=100, 分户=80\n人工评价分 S_manual: 好=100, 中=80, 差=50\n\n维修联动: 退化起始年 = max(部件修复年份, 维修台账最新完工年份)',
  weights: '三维权重: W_comp=0.80(部件), W_mgmt=0.10(管护制), W_manual=0.10(人工)\n部件组权重: W_JR=0.30(加热器), W_SR=0.26(散热器), W_ZK=0.26(自控), W_ZT=0.18(主体)\n部件内权重: α=0.40(状态), β=0.30(退化), γ=0.30(评价)',
  example: '示例烤房 09-420527KF00018（建成2009年，当前2026年，梅家河肖家坪）:\n\n① 部件健康分（增强BMHI + CDCI三段式退化）: 81.5\n   JR=82.0/SR=86.0/ZK=82.0/ZT=73.2\n   C=0.30×82.0+0.26×86.0+0.26×82.0+0.18×73.2=81.5\n② 管护制分: 集中管护 → 100\n③ 人工评价分: 好 → 100\n\n三维融合: H = 0.80×81.5 + 0.10×100 + 0.10×100\n        = 65.2 + 10 + 10 = 85.2 → 良好\n\n若该烤房完成某项维修，部件退化年龄从维修完工年起算，健康分回弹。',
  params: [
    { key: 'alpha', label: '状态分权重α', default: 0.40 },
    { key: 'beta', label: '退化分权重β', default: 0.30 },
    { key: 'gamma', label: '评价分权重γ', default: 0.30 },
    { key: 'wJr', label: 'JR加热设备权重', default: 0.30 },
    { key: 'wSr', label: 'SR散热器权重', default: 0.26 },
    { key: 'wZk', label: 'ZK自控设备权重', default: 0.26 },
    { key: 'wZt', label: 'ZT烤房主体权重', default: 0.18 },
    { key: 'wComp', label: '部件分权重', default: 0.80 },
    { key: 'wMgmt', label: '管护制权重', default: 0.10 },
    { key: 'wManual', label: '人工评价权重', default: 0.10 },
    { key: 'lJr', label: 'JR设计寿命(年)', default: 10 },
    { key: 'lSr', label: 'SR设计寿命(年)', default: 12 },
    { key: 'lZk', label: 'ZK设计寿命(年)', default: 10 },
    { key: 'lZt', label: 'ZT设计寿命(年)', default: 20 },
  ],
}

// ========== 健康等级阈值 ==========
const healthLevels = [
  { range: '90-100', level: '优良', color: '#2E8B6A', bg: '#E8F5E9', desc: '设备状态良好，可正常运行' },
  { range: '75-89', level: '良好', color: '#5FB292', bg: '#E0F2EE', desc: '设备基本正常，建议定期维护' },
  { range: '60-74', level: '一般', color: '#D4944A', bg: '#FFF3E0', desc: '设备出现老化，需要关注' },
  { range: '40-59', level: '预警', color: '#D4604A', bg: '#FFEBEE', desc: '⚠️ 设备严重老化，需维修' },
  { range: '0-39', level: '危险', color: '#B71C1C', bg: '#FFCDD2', desc: '🔴 设备接近退役，紧急处理' },
]

// ========== 数据 ==========
const loading = ref(false)
const applyLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const stats = ref<any>(null)
const pagination = reactive({ pageNum: 1, pageSize: 10 })
const searchKeyword = ref('')
const healthLevelFilter = ref('')
const paramValues = reactive<Record<string, number>>({})

// ========== 算法描述折叠（整体展开/折叠，默认折叠） ==========
const algoExpanded = ref(false)
function toggleAlgoDesc() {
  algoExpanded.value = !algoExpanded.value
}

// ========== 计算属性 ==========
const algoParams = computed(() => algorithmInfo.params)

// ========== 初始化参数 ==========
function initParams() {
  algorithmInfo.params.forEach(p => {
    paramValues[p.key] = p.default
  })
}

// ========== 重置参数 ==========
function resetParams() {
  algorithmInfo.params.forEach(p => {
    paramValues[p.key] = p.default
  })
  ElMessage.success('参数已重置为默认值')
}

// ========== 应用到所有烤房 ==========
async function handleApply() {
  try {
    await ElMessageBox.confirm(
      `确认使用「${algorithmInfo.fullName}」算法计算所有烤房的健康分？\n这将写入数据库 health_score_final 字段，不影响其他算法数据。`,
      '应用到所有烤房',
      { type: 'warning', confirmButtonText: '开始计算', cancelButtonText: '取消' }
    )
    applyLoading.value = true
    const params: any = { algo: 'enhanced_bmhi', ...paramValues }
    const res: any = await applyHealthScoreFinal(params)
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
    const res: any = await getHealthScoreFinalList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      algo: 'enhanced_bmhi',
      keyword: searchKeyword.value || undefined,
      healthLevel: healthLevelFilter.value || undefined,
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
    stats.value = await getHealthScoreFinalStats('enhanced_bmhi')
  } catch {
    stats.value = null
  }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handlePageChange(p: number) { pagination.pageNum = p; loadData() }
function handleSizeChange(s: number) { pagination.pageSize = s; pagination.pageNum = 1; loadData() }

// ========== 更新管护制度 / 人工评价（三维维度录入） ==========
async function handleUpdate3Dim(row: any, field: string, value: string) {
  const payload: any = { projectId: row.projectId }
  payload[field] = value
  try {
    await updateHealthScore3Dim(payload)
    row[field] = value
    ElMessage.success(`${field === 'manageMode' ? '管护制度' : '人工评价'}已保存，请点击「应用到所有烤房」重新计算`)
  } catch {
    ElMessage.error('保存失败，请稍后重试')
    loadData()
  }
}

// ========== 颜色与等级 ==========
function getScoreColor(score: number | null): string {
  if (score === null || score === undefined) return '#999'
  if (score >= 90) return '#2E8B6A'
  if (score >= 75) return '#5FB292'
  if (score >= 60) return '#D4944A'
  if (score >= 40) return '#D4604A'
  return '#B71C1C'
}

function getLevelInfo(score: number | null) {
  if (score === null || score === undefined) return null
  if (score >= 90) return healthLevels[0]
  if (score >= 75) return healthLevels[1]
  if (score >= 60) return healthLevels[2]
  if (score >= 40) return healthLevels[3]
  return healthLevels[4]
}

// ========== 判断参数是否为权重类型 ==========
function isWeightParam(key: string): boolean {
  return key === 'alpha' || key === 'beta' || key === 'gamma' ||
    key.startsWith('w') && !key.startsWith('l')
}

onMounted(() => {
  initParams()
  loadData()
  loadStats()
})
</script>

<template>
<div class="health-score-page">
  <!-- 页面标题 -->
  <div class="page-header">
    <div class="header-left">
      <el-icon class="header-icon"><DataAnalysis /></el-icon>
      <div>
        <h2>烤房健康分算法</h2>
        <p>三维融合算法 — 部件健康分 80% + 管护制度分 10% + 人工评价分 10%（联动维修台账）</p>
      </div>
    </div>
  </div>

  <!-- 算法信息横幅 -->
  <div class="algo-banner">
    <div class="banner-content">
      <div class="banner-left">
        <el-icon class="banner-icon"><Check /></el-icon>
        <div>
          <div class="banner-name">{{ algorithmInfo.name }}</div>
          <div class="banner-full">{{ algorithmInfo.fullName }}</div>
        </div>
      </div>
    </div>
  </div>

  <!-- 统计概览 -->
  <div class="stats-row" v-if="stats">
    <div class="stat-item">
      <div class="stat-icon-wrapper"><Activity :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">烤房总数</div>
      <div class="stat-value">{{ stats.total }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><CircleCheckBig :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">已计算</div>
      <div class="stat-value" style="color: #2E8B6A;">{{ stats.calculated }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><Gauge :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">平均分</div>
      <div class="stat-value" style="color: #1A6B4F;">{{ stats.avgScore }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><CircleArrowDown :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">最低分</div>
      <div class="stat-value" style="color: #D4604A;">{{ stats.minScore ?? '-' }}</div>
    </div>
    <div class="stat-item">
      <div class="stat-icon-wrapper"><CircleArrowUp :size="20" color="#FFFFFF" /></div>
      <div class="stat-label">最高分</div>
      <div class="stat-value" style="color: #2E8B6A;">{{ stats.maxScore ?? '-' }}</div>
    </div>
    <!-- 等级分布 -->
    <div class="stat-dist">
      <div class="stat-label">等级分布</div>
      <div class="dist-tags">
        <el-tag
          v-for="d in (stats.distribution || [])"
          :key="d.level"
          :type="d.level === '优良' ? 'success' : d.level === '良好' ? '' : d.level === '一般' ? 'warning' : d.level === '预警' ? 'danger' : 'info'"
          size="small"
          style="margin-right: 6px;"
        >
          {{ d.level }}: {{ d.cnt }}
        </el-tag>
      </div>
    </div>
  </div>

  <!-- 健康等级与预警阈值 -->
  <div class="health-levels-card">
    <div class="card-header">
      <el-icon><Warning /></el-icon>
      <span>健康等级与预警阈值</span>
    </div>
    <div class="level-grid">
      <div
        v-for="lv in healthLevels"
        :key="lv.level"
        class="level-item"
        :style="{ background: lv.bg, borderColor: lv.color }"
      >
        <div class="level-top">
          <span class="level-name" :style="{ color: lv.color }">{{ lv.level }}</span>
          <el-icon v-if="lv.level === '预警' || lv.level === '危险'" class="level-warn-icon" :style="{ color: lv.color }">
            <Warning />
          </el-icon>
        </div>
        <div class="level-range" :style="{ color: lv.color }">{{ lv.range }}</div>
        <div class="level-desc">{{ lv.desc }}</div>
      </div>
    </div>
  </div>

  <!-- 烤房健康分列表 -->
  <div class="table-section">
    <div class="table-header">
      <span class="table-title">健康分计算列表</span>
      <div class="table-tools">
        <el-select
          v-model="healthLevelFilter"
          placeholder="全部等级"
          size="small"
          style="width: 120px;"
          clearable
          @change="handleSearch"
        >
          <el-option label="优良" value="优良" />
          <el-option label="良好" value="良好" />
          <el-option label="一般" value="一般" />
          <el-option label="预警" value="预警" />
          <el-option label="危险" value="危险" />
        </el-select>
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
          <el-tag
            :type="row.useStatus === '在烤' ? 'success' : 'info'"
            size="small"
          >
            {{ row.useStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="manageMode" label="管护制度" width="110">
        <template #default="{ row }">
          <el-select
            :model-value="row.manageMode"
            size="small"
            :teleported="false"
            @change="(v: string) => handleUpdate3Dim(row, 'manageMode', v)"
          >
            <el-option label="集中管护" value="集中管护" />
            <el-option label="分户管护" value="分户管护" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="manualEval" label="人工评价" width="100">
        <template #default="{ row }">
          <el-select
            :model-value="row.manualEval"
            size="small"
            :teleported="false"
            @change="(v: string) => handleUpdate3Dim(row, 'manualEval', v)"
          >
            <el-option label="好" value="好" />
            <el-option label="中" value="中" />
            <el-option label="差" value="差" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="healthScore" label="健康分" width="100" align="center">
        <template #default="{ row }">
          <span
            v-if="row.healthScore != null && typeof row.healthScore === 'number'"
            class="score-value"
            :style="{ color: getScoreColor(row.healthScore) }"
          >
            {{ row.healthScore.toFixed(1) }}
          </span>
          <span v-else style="color: #ccc;">未计算</span>
        </template>
      </el-table-column>
      <el-table-column prop="healthLevel" label="健康等级" width="90" align="center">
        <template #default="{ row }">
          <el-tag
            v-if="row.healthScore != null"
            :color="getLevelInfo(row.healthScore)?.color"
            effect="dark"
            size="small"
            style="border: none; color: white;"
          >
            {{ getLevelInfo(row.healthScore)?.level }}
          </el-tag>
          <span v-else style="color: #ccc;">-</span>
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
        <el-tag size="small" type="success" style="margin-left: 8px;">当前: {{ algorithmInfo.fullName }}</el-tag>
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
              <h4 class="algo-section-title">权重设置</h4>
              <pre class="weights-pre">{{ algorithmInfo.weights }}</pre>
            </div>
            <div class="algo-section">
              <h4 class="algo-section-title">计算示例</h4>
              <pre class="example-pre">{{ algorithmInfo.example }}</pre>
            </div>
          </div>
        </transition>
        <div class="desc-note">
          <el-icon><Warning /></el-icon>
          <span>三维融合模型说明：①部件退化采用三段式（稳定区前20%满分→线性区匀速衰减→加速劣化区指数下降），并通过「维修台账最新完工年份」重置退化年龄，维修后健康分回弹；②管护制度集中=100/分户=80；③人工评价好=100/中=80/差=50。三个维度按 80/10/10 融合，耗时与故障部件能够动态反映维护投入。</span>
        </div>
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
            :step="isWeightParam(p.key) ? 0.01 : 1"
            :min="0"
            :max="isWeightParam(p.key) ? 1 : 100"
            :precision="isWeightParam(p.key) ? 2 : 0"
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
          style="width: 100%; background: linear-gradient(135deg, #0A4D3E, #2E8B6A); border: none;"
        >
          <el-icon style="margin-right: 4px;"><Refresh /></el-icon>
          应用到所有烤房 ({{ stats?.total ?? 0 }}栋)
        </el-button>
        <p class="param-tip">结果将存入 health_score_final 字段</p>
      </div>
    </div>
  </div>
</div>
</template>

<style scoped>
.health-score-page { padding: 12px; }

/* 页面头部 */
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { font-size: 28px; color: #0A4D3E; }
.page-header h2 { font-size: 20px; color: #0A4D3E; margin: 0; }
.page-header p { font-size: 13px; color: #6c7a89; margin: 4px 0 0; }

/* 算法信息横幅 */
.algo-banner {
  background: linear-gradient(135deg, #0A4D3E 0%, #2E8B6A 100%);
  border-radius: 8px;
  padding: 12px 18px;
  margin-bottom: 12px;
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.25);
}
.banner-content {
  display: flex; align-items: center; justify-content: space-between;
}
.banner-left { display: flex; align-items: center; gap: 14px; }
.banner-icon { font-size: 32px; color: #ffffff; }
.banner-name {
  font-size: 18px; font-weight: 700; color: #ffffff;
}
.banner-full {
  font-size: 13px; color: #C8E6D5; margin-top: 4px;
}
.banner-badge {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #ffffff;
  font-size: 13px; font-weight: 600;
  padding: 4px 16px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

/* 统计概览 */
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
  background: linear-gradient(135deg, #0A4D3E, #2E8B6A);
  margin: 0 auto 8px;
}
.stat-label { font-size: 12px; color: #6c7a89; }
.stat-value { font-size: 24px; font-weight: 700; color: #0A4D3E; margin-top: 2px; }
.stat-dist { flex: 1; min-width: 200px; }
.dist-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }

/* 算法描述 + 参数调整 */
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
  background: #f0f7f4; border-left: 3px solid #2E8B6A; padding: 10px 14px;
  border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap;
  font-family: 'Cambria Math', monospace; margin-top: 6px; line-height: 1.6;
}
.example-pre {
  background: #fff8e1; border-left: 3px solid #B8860B; padding: 10px 14px;
  border-radius: 0 6px 6px 0; font-size: 12px; white-space: pre-wrap;
  margin-top: 6px; line-height: 1.6;
}
.desc-note {
  display: flex; align-items: flex-start; gap: 8px;
  margin-top: 8px; padding: 12px 14px;
  background: #E8F5E9; border-radius: 6px;
  font-size: 12px; color: #1A6B4F; line-height: 1.6;
}
.desc-note .el-icon {
  color: #2E8B6A; font-size: 16px; flex-shrink: 0; margin-top: 2px;
}

/* 参数调整面板 */
.param-item {
  display: flex; align-items: center; gap: 8px; margin-bottom: 12px;
}
.param-item label { font-size: 13px; min-width: 110px; color: #2c3e50; }
.param-default { font-size: 11px; color: #999; }
.param-footer { padding: 12px 16px; border-top: 1px solid #e0e5e3; }
.param-tip { font-size: 11px; color: #999; text-align: center; margin-top: 8px; }

/* 健康等级与预警阈值 */
.health-levels-card {
  background: white; border-radius: 8px; border: 1px solid #e0e5e3;
  overflow: hidden; margin-bottom: 20px;
}
.health-levels-card .card-header {
  display: flex; align-items: center; gap: 8px; padding: 12px 16px;
  background: #f5f7f6; border-bottom: 1px solid #e0e5e3;
  font-size: 14px; font-weight: 600; color: #0A4D3E;
}
.level-grid {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; padding: 16px;
}
.level-item {
  padding: 14px 12px; border-radius: 8px; border-left: 4px solid;
  transition: transform 0.2s;
}
.level-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.level-top {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 6px;
}
.level-name {
  font-size: 16px; font-weight: 700;
}
.level-warn-icon {
  font-size: 18px;
}
.level-range {
  font-size: 13px; font-weight: 600; margin-bottom: 6px;
}
.level-desc {
  font-size: 11px; color: #555; line-height: 1.5;
}

/* 烤房列表表格 */
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

/* 响应式 */
@media (max-width: 1024px) {
  .desc-section { grid-template-columns: 1fr; }
  .level-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .level-grid { grid-template-columns: 1fr; }
  .banner-content { flex-direction: column; gap: 12px; align-items: flex-start; }
}
</style>
