<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import {
  ClipboardList, UsersRound, FileSpreadsheet, Target, Sparkles, RotateCcw,
  UploadCloud, KeyRound, Columns3, Filter, DatabaseZap
} from 'lucide-vue-next'
import {
  getAnswers, getAnswerStats, importAnswers, genDemo, clearPaper, computeProfiles,
  DIMS, DIM_W, DEFAULT_PAPER
} from '@/api/farmerAlgo'

// ==================== KPI ====================
const stats = reactive({
  paperId: DEFAULT_PAPER,
  farmers: 0,
  rows: 0,
  avgCorrect: 0,
  byDim: {} as Record<string, any>,
  region: [] as any[],
  villages: [] as any[]
})

const dimCorrect = computed(() => DIMS.map((d) => ({
  name: d,
  weight: DIM_W[DIMS.indexOf(d)],
  cnt: stats.byDim[d]?.cnt ?? 0,
  correct: stats.byDim[d]?.correct ?? 0
})))

// ==================== 表格 ====================
const tableData = ref<any[]>([])
const loading = ref(false)
const queryForm = reactive({
  dim: '',
  township: '',
  village: '',
  keyword: ''
})
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

// ==================== 维度配色 ====================
const dimColor = (d: string) => {
  const map: Record<string, string> = {
    栽培: '#2E8B6A', 植保: '#5FB292', 采烤: '#D4944A', 烘烤: '#8A63B8', 综合: '#3A6A5A'
  }
  return map[d] || '#2E8B6A'
}
const dimBg = (d: string) => `${dimColor(d)}22`

// ==================== 数据加载 ====================
async function loadStats() {
  const res = await getAnswerStats(stats.paperId)
  Object.assign(stats, res)
}

async function loadTable() {
  loading.value = true
  try {
    const res = await getAnswers({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, ...queryForm, paperId: stats.paperId })
    tableData.value = res.rows || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.pageNum = 1; loadTable() }
function handleReset() {
  queryForm.dim = ''; queryForm.township = ''; queryForm.village = ''; queryForm.keyword = ''
  handleSearch()
}
function handlePaperChange() { loadStats(); loadTable() }

// ==================== 导入（Excel / JSON） ====================
const importVisible = ref(false)
const importMode = ref<'file' | 'json'>('file')
const jsonText = ref('')
const fileRowCount = ref(0)
const fileRows = ref<any[]>([])

function handleFile(file: File) {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target!.result as ArrayBuffer)
      const wb = XLSX.read(data, { type: 'array' })
      const sheet = wb.Sheets[wb.SheetNames[0]]
      const raw = XLSX.utils.sheet_to_json<Record<string, any>>(sheet)
      fileRows.value = raw.map((r) => mapRow(r))
      fileRowCount.value = raw.length
      if (raw.length === 0) ElMessage.warning('未读取到有效数据')
    } catch (err) {
      ElMessage.error('文件解析失败，请确认为 .xlsx / .csv')
    }
  }
  reader.readAsArrayBuffer(file)
  return false
}

/** 兼容中英文多列名的字段映射 */
function mapRow(r: Record<string, any>) {
  const kv = Object.keys(r)
  const find = (...keys: string[]) => {
    for (const k of keys) {
      const hit = kv.find((x) => x.toLowerCase().includes(k.toLowerCase()))
      if (hit && r[hit] !== undefined && r[hit] !== '') return r[hit]
    }
    return ''
  }
  const correctRaw = String(find('correct', '是否正确', '是否答对', 'right'))
  const isCorrect = /^(true|1|是|对|√|correct)$/i.test(correctRaw)
  return {
    farmerName: find('farmerName', 'farmer_name', '烟农', '姓名', '种植者'),
    farmerPhone: find('farmerPhone', 'farmer_phone', '手机号', '电话'),
    county: find('county', '县', '区县'),
    township: find('township', '乡镇', '站'),
    village: find('village', '村'),
    poundGroup: find('poundGroup', 'pound_group', '磅组', '片区'),
    dim: normalizeDim(find('dim', '维度')),
    questionNo: Number(find('questionNo', 'question_no', '题号')) || 0,
    question: find('question', '题干', '题目'),
    answer: find('answer', '作答', '答案'),
    key: find('key', 'key_ind', '标准', '正确答案', '标准答案'),
    correct: isCorrect ? 1 : 0
  }
}

function normalizeDim(d: string): string {
  const map: Record<string, string> = { 栽培: '栽培', 植保: '植保', 采烤: '采烤', 烘烤: '烘烤', 综合: '综合' }
  return map[d] || ''
}

function beforeUpload(file: File) {
  handleFile(file)
  return false
}

function handleRemove(row: any) {
  fileRows.value = fileRows.value.filter((r) => r !== row)
  fileRowCount.value = fileRows.value.length
}

function parseJsonText() {
  try {
    const arr = JSON.parse(jsonText.value)
    if (!Array.isArray(arr) || arr.length === 0) { ElMessage.warning('JSON 需为对象数组'); return }
    fileRows.value = arr.map((r) => ({
      farmerName: r.farmerName ?? r.farmer_name ?? '',
      farmerPhone: r.farmerPhone ?? r.farmer_phone ?? '',
      county: r.county ?? '',
      township: r.township ?? '',
      village: r.village ?? '',
      poundGroup: r.poundGroup ?? r.pound_group ?? '',
      dim: normalizeDim(r.dim ?? ''),
      questionNo: Number(r.questionNo ?? r.question_no ?? 0),
      question: r.question ?? '',
      answer: r.answer ?? '',
      key: r.key ?? r.key_ind ?? '',
      correct: r.correct === 1 || r.correct === true ? 1 : 0
    }))
    fileRowCount.value = fileRows.value.length
    if (arr.length === 0) ElMessage.warning('JSON 为空')
  } catch (e) {
    ElMessage.error('JSON 解析失败：' + (e as Error).message)
  }
}

async function confirmImport() {
  if (fileRows.value.length === 0) { ElMessage.warning('请先选择文件或粘贴 JSON'); return }
  const res = await importAnswers(fileRows.value, stats.paperId, true)
  ElMessage.success(`成功导入 ${res.success} 条答题数据`)
  importVisible.value = false
  fileRows.value = []; fileRowCount.value = 0; jsonText.value = ''
  await loadStats(); await loadTable()
}

// ==================== 演示 / 清空 / 重建 ====================
async function handleDemo() {
  const res = await genDemo(stats.paperId)
  ElMessage.success(res.message || '已生成演示数据')
  await loadStats(); await loadTable()
}

async function handleClear() {
  const ok = await ElMessageBox.confirm('清空将删除当前批次的全部答题与画像数据，确认继续？', '清空数据', { type: 'warning' }).catch(() => null)
  if (!ok) return
  await clearPaper(stats.paperId)
  ElMessage.success('已清空')
  await loadStats(); await loadTable()
}

async function handleRecompute() {
  const res = await computeProfiles(stats.paperId)
  ElMessage.success(res.message || '画像重建完成')
}

onMounted(() => { loadStats(); loadTable() })

const correctRate = (v: number) => (Number.isFinite(v) ? Number(v.toFixed(1)) : 0)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><ClipboardList :size="20" /></div>
        <div>
          <h2 class="header-title">答题情况</h2>
          <p class="header-sub">五维知识问卷 · 逐户逐题答题明细与正确率洞察</p>
        </div>
      </div>
      <div class="header-actions">
        <el-select v-model="stats.paperId" size="default" class="paper-select" @change="handlePaperChange">
          <el-option label="问卷批次 2026" value="2026" />
          <el-option label="问卷批次 2025" value="2025" />
        </el-select>
        <el-button type="primary" :icon="Sparkles" @click="handleDemo">生成演示数据</el-button>
        <el-button :icon="UploadCloud" @click="importVisible = true">导入答题数据</el-button>
        <el-button :icon="RotateCcw" @click="handleClear" plain type="danger">清空</el-button>
      </div>
    </div>

    <!-- KPI 指标卡 -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon" style="background:rgba(46,139,106,.12)"><UsersRound :size="26" color="#2E8B6A" /></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats.farmers }}</div>
          <div class="kpi-label">覆盖烟农户数</div>
        </div>
        <div class="kpi-trend">答题样本</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon" style="background:rgba(90,138,122,.12)"><FileSpreadsheet :size="26" color="#3A6A5A" /></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats.rows }}</div>
          <div class="kpi-label">有效答题条数</div>
        </div>
        <div class="kpi-trend">23 题/户</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon" style="background:rgba(212,148,74,.14)"><Target :size="26" color="#D4944A" /></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ correctRate(stats.avgCorrect) }}<span class="unit">%</span></div>
          <div class="kpi-label">平均正确率</div>
        </div>
        <div class="kpi-trend">总体水平</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon" style="background:rgba(138,99,184,.14)"><Columns3 :size="26" color="#8A63B8" /></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats.region.length }}</div>
          <div class="kpi-label">答题片区(村)</div>
        </div>
        <div class="kpi-trend">乡镇统筹</div>
      </div>
    </div>

    <!-- 五维正确率 -->
    <div class="section-card">
      <div class="section-head">
        <div class="section-title">
          <span class="title-flag"></span>
          <h3>五维知识答题情况</h3>
        </div>
        <span class="section-hint">五维权重：栽培 .18 · 植保 .18 · 采烤 .19 · 烘烤 .25 · 综合 .20</span>
      </div>
      <div class="dim-bars">
        <div v-for="d in dimCorrect" :key="d.name" class="dim-row">
          <div class="dim-label">
            <span class="dim-name">{{ d.name }}</span>
            <span class="dim-w">{{ (d.weight * 100).toFixed(0) }}%</span>
            <span class="dim-cnt">{{ d.cnt }} 题</span>
          </div>
          <div class="dim-track">
            <div class="dim-fill" :style="{ width: (d.correct ?? 0) + '%', background: dimColor(d.name) }"></div>
          </div>
          <div class="dim-value">{{ correctRate(d.correct) }}%</div>
        </div>
      </div>
    </div>

    <!-- 答题明细 -->
    <div class="section-card nomargin">
      <div class="filter-container">
        <div class="filter-left">
          <el-select v-model="queryForm.dim" placeholder="全部维度" clearable style="width:120px" @change="handleSearch">
            <el-option v-for="d in DIMS" :key="d" :label="d" :value="d" />
          </el-select>
          <el-select v-model="queryForm.township" placeholder="全部乡镇" clearable style="width:130px" @change="handleSearch">
            <el-option v-for="r in stats.region" :key="r.township" :label="r.township" :value="r.township" />
          </el-select>
          <el-select v-model="queryForm.village" placeholder="全部村" clearable style="width:140px" @change="handleSearch">
            <el-option v-for="r in stats.region" :key="r.village" :label="r.village" :value="r.village" />
          </el-select>
          <el-input v-model="queryForm.keyword" placeholder="烟农姓名 / 题干关键词" clearable style="width:200px" @keyup.enter="handleSearch">
            <template #prefix><Filter :size="14" /></template>
          </el-input>
          <el-button type="primary" :icon="Target" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <div class="filter-right">
          <el-button :icon="DatabaseZap" @click="handleRecompute" plain>同步重建画像</el-button>
        </div>
      </div>

      <div class="table-container">
        <el-table :data="tableData" v-loading="loading" size="small" class="data-table" :header-cell-style="{ background: '#F2F8F5', color: '#0D3D30', fontWeight: 600 }">
          <el-table-column label="烟农" width="150">
            <template #default="{ row }">
              <div class="cell-main">{{ row.farmerName }}</div>
              <div class="cell-sub">{{ row.farmerPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column label="区域" min-width="180">
            <template #default="{ row }">
              {{ [row.county, row.township, row.village, row.poundGroup].filter(Boolean).join(' · ') }}
            </template>
          </el-table-column>
          <el-table-column label="维度" width="80" align="center">
            <template #default="{ row }">
              <span class="dim-tag" :style="{ background: dimBg(row.dim), color: dimColor(row.dim) }">{{ row.dim }}</span>
            </template>
          </el-table-column>
          <el-table-column label="题号" width="60" align="center" prop="questionNo" />
          <el-table-column label="题干" prop="question" min-width="220" show-overflow-tooltip />
          <el-table-column label="作答" prop="answer" min-width="90" show-overflow-tooltip />
          <el-table-column label="标准" prop="keyInd" min-width="80" show-overflow-tooltip />
          <el-table-column label="是否答对" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.correct === 1" type="success" size="small" effect="light" round>答对</el-tag>
              <el-tag v-else type="danger" size="small" effect="plain" round>答错</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadTable"
            @current-change="loadTable"
          />
        </div>
      </div>
    </div>

    <!-- 导入对话框 -->
    <el-dialog v-model="importVisible" title="导入答题数据" width="720px" destroy-on-close>
      <div class="import-tabs">
        <el-radio-group v-model="importMode">
          <el-radio-button value="file">Excel / CSV 导入</el-radio-button>
          <el-radio-button value="json">JSON 粘贴</el-radio-button>
        </el-radio-group>
      </div>

      <template v-if="importMode === 'file'">
        <div class="drop-zone">
          <el-upload drag :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls,.csv" :before-upload="beforeUpload">
            <div class="drop-inner">
              <UploadCloud :size="32" color="#2E8B6A" />
              <p class="drop-title">拖拽或点击选择答题明细表</p>
              <p class="drop-sub">支持列名：烟农/姓名 · 手机号 · 乡镇 · 村 · 磅组 · 维度 · 题号 · 题干 · 作答 · 标准答案 · 是否正确</p>
            </div>
          </el-upload>
        </div>
        <p v-if="fileRowCount > 0" class="file-tip">
          <KeyRound :size="14" color="#2E8B6A" /> 已解析 {{ fileRowCount }} 条，覆盖 5 个维度后点击下方「开始导入」
        </p>
        <div v-if="fileRows.length" class="preview-list">
          <div v-for="(r, i) in fileRows.slice(0, 6)" :key="i" class="preview-row">
            <span class="dim-tag" :style="{ background: dimBg(r.dim), color: dimColor(r.dim) }">{{ r.dim || '未识别' }}</span>
            <span class="pv-name">{{ r.farmerName }}</span>
            <span class="pv-q">{{ r.questionNo }}. {{ r.question }}</span>
            <span :class="['pv-flag', r.correct === 1 ? 'ok' : 'no']">{{ r.correct === 1 ? '对' : '错' }}</span>
          </div>
          <button v-if="fileRows.length > 6" class="clear-btn" @click="fileRows = fileRows.slice(0, 6)">展开更多…</button>
        </div>
      </template>

      <template v-else>
        <p class="json-tip">粘贴对象数组，字段：farmerName / farmerPhone / county / township / village / poundGroup / dim / questionNo / question / answer / key / correct(0/1)</p>
        <el-input v-model="jsonText" type="textarea" :rows="10" placeholder='[{"farmerName":"陈1","dim":"栽培","correct":1}, ...]' />
        <el-button size="small" :icon="DatabaseZap" @click="parseJsonText" class="mt8">解析 JSON <span v-if="fileRowCount">（{{ fileRowCount }} 条）</span></el-button>
      </template>

      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :disabled="fileRows.length === 0" :icon="UploadCloud" @click="confirmImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 12px; padding: 12px; }
.page-header { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-icon {
  width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #0D5D46, #2E8B6A); color: #fff; box-shadow: 0 4px 12px rgba(13, 93, 70, .3);
}
.header-title { margin: 0; font-size: 18px; font-weight: 700; color: #0D3D30; }
.header-sub { margin: 2px 0 0; font-size: 12px; color: var(--text-muted); }
.header-actions { display: flex; gap: 8px; align-items: center; }
.paper-select { width: 150px; }

.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.kpi-card {
  background: var(--surface-color); border-radius: var(--radius-xl); padding: 14px 16px;
  display: flex; gap: 12px; align-items: center; border: 1px solid var(--border-light);
  box-shadow: var(--shadow-card); position: relative; overflow: hidden;
}
.kpi-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px; background: linear-gradient(180deg, #0D5D46, #5FB292); }
.kpi-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.kpi-value { font-size: 24px; font-weight: 800; color: #0D3D30; line-height: 1.1; }
.kpi-value .unit { font-size: 14px; font-weight: 600; color: var(--text-muted); }
.kpi-label { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.kpi-trend { position: absolute; right: 12px; top: 12px; font-size: 11px; color: var(--text-placeholder); }

.section-card { background: var(--surface-color); border-radius: var(--radius-xl); border: 1px solid var(--border-light); box-shadow: var(--shadow-md); padding: 14px 16px; }
.section-card.nomargin { margin: 0; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.section-title { display: flex; align-items: center; gap: 8px; }
.title-flag { width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, #2E8B6A, #0D5D46); }
.section-title h3 { margin: 0; font-size: 15px; color: #0D3D30; }
.section-hint { font-size: 12px; color: var(--text-muted); }

.dim-bars { display: flex; flex-direction: column; gap: 11px; }
.dim-row { display: flex; align-items: center; gap: 12px; }
.dim-label { display: flex; align-items: center; gap: 6px; width: 150px; flex-shrink: 0; }
.dim-name { font-size: 13px; font-weight: 600; color: #0D3D30; width: 40px; }
.dim-w { font-size: 11px; color: #fff; background: rgba(26, 107, 79, .75); border-radius: 4px; padding: 1px 5px; }
.dim-cnt { font-size: 11px; color: var(--text-placeholder); }
.dim-track { flex: 1; height: 12px; background: #EEF4F1; border-radius: 6px; overflow: hidden; }
.dim-fill { height: 100%; border-radius: 6px; transition: width .6s cubic-bezier(.16,1,.3,1); box-shadow: 0 2px 6px rgba(0,0,0,.06); }
.dim-value { width: 46px; text-align: right; font-size: 13px; font-weight: 700; color: #0D3D30; }

.filter-container { display: flex; align-items: center; justify-content: space-between; gap: 8px; flex-wrap: wrap; margin-bottom: 0; }
.filter-left { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.toolbar { display: flex; gap: 8px; }
.table-container { padding: 10px 12px; }
.data-table { width: 100%; }
.data-table :deep(.el-table__cell) { padding: 4px 6px; font-size: 12px; white-space: nowrap; }
.cell-main { font-weight: 600; color: #0D3D30; }
.cell-sub { font-size: 11px; color: var(--text-muted); }
.dim-tag { display: inline-flex; padding: 1px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 8px 0 0; }
.mt8 { margin-top: 8px; }

.import-tabs { margin-bottom: 12px; }
.drop-zone :deep(.el-upload-dragger) { border-radius: 10px; padding: 20px; }
.drop-inner { text-align: center; padding: 6px 0; }
.drop-title { margin: 8px 0 2px; font-size: 14px; color: #0D3D30; font-weight: 600; }
.drop-sub { margin: 0; font-size: 12px; color: var(--text-muted); }
.file-tip { display: flex; align-items: center; gap: 4px; color: #2E8B6A; font-size: 13px; margin: 10px 0; }
.preview-list { max-height: 200px; overflow: auto; border: 1px solid var(--border-light); border-radius: 8px; }
.preview-row { display: flex; align-items: center; gap: 8px; padding: 5px 10px; border-bottom: 1px dashed #e6efe9; font-size: 12px; }
.preview-row:last-child { border-bottom: none; }
.pv-name { font-weight: 600; color: #0D3D30; width: 70px; flex-shrink: 0; }
.pv-q { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #3A6A5A; }
.pv-flag { width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; color: #fff; flex-shrink: 0; }
.pv-flag.ok { background: #2E8B6A; }
.pv-flag.no { background: #D4604A; }
.clear-btn { border: none; background: none; color: #2E8B6A; cursor: pointer; font-size: 12px; padding: 6px; }
.json-tip { font-size: 12px; color: var(--text-muted); margin: 0 0 8px; }
</style>