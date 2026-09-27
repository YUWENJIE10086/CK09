<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, View, SetUp } from '@element-plus/icons-vue'
import BarnCode from '@/components/BarnCode.vue'
import { getRepairList, deleteRepair } from '@/api/repair'
import { listCounty, listTownship } from '@/api/dict'

const router = useRouter()

/** 更换类型枚举 */
type ReplacementType = '新采购' | '改造升级' | '维护修复' | '防护加固'

/** 更换类型选项 */
const replacementTypeOptions: ReplacementType[] = ['新采购', '改造升级', '维护修复', '防护加固']

/** 更换类型颜色规则 */
const replacementTypeColorMap: Record<string, { bg: string; color: string }> = {
  '新采购': { bg: '#E8F5EE', color: '#2E8B6A' },
  '改造升级': { bg: '#E3F2FD', color: '#1565C0' },
  '维护修复': { bg: '#FEF3E7', color: '#D4944A' },
  '防护加固': { bg: '#F3E5F5', color: '#7B1FA2' },
}

/** 获取更换类型样式 */
function getReplacementTypeStyle(type: string) {
  return replacementTypeColorMap[type] || { bg: '#F0F0F0', color: '#606266' }
}

/** 筛选表单 */
const filterForm = reactive({
  countyCode: '' as string,
  townCode: '' as string,
  replacementType: '' as ReplacementType | '',
  componentName: '' as string,
  ovenId: '' as string,
  repairYear: '' as string,
})

/** 县区/乡镇联动 */
const countyOptions = ref<{ value: string; label: string }[]>([])
const townOptions = ref<{ value: string; label: string }[]>([])

async function loadCounties() {
  try {
    const res: any = await listCounty()
    countyOptions.value = (res || []).map((c: any) => ({ value: c.countyCode, label: c.countyName }))
  } catch { /* ignore */ }
}

function handleCountyChange(countyCode: string) {
  filterForm.townCode = ''
  if (countyCode) {
    loadTowns(countyCode)
  } else {
    townOptions.value = []
  }
}

async function loadTowns(countyCode: string) {
  if (!countyCode) { townOptions.value = []; return }
  try {
    const res: any = await listTownship(countyCode)
    townOptions.value = (res || []).map((t: any) => ({ value: t.townshipName, label: t.townshipName }))
  } catch { townOptions.value = [] }
}

function getCountyName(countyCode: string): string {
  const c = countyOptions.value.find(o => o.value === countyCode)
  return c ? c.label : ''
}

/** 表格数据 */
const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
})

/** 预警窗口：待审核维修申请数 */
const repairAlertClosed = ref(false)
const repairApplyCount = ref(0)

async function loadApplyCount() {
  try {
    const res: any = await getRepairList({ pageNum: 1, pageSize: 9999 })
    repairApplyCount.value = (res?.rows || []).filter((r: any) => r.repairStatus === '待审核').length
  } catch { repairApplyCount.value = 0 }
}

/** 加载数据 */
async function loadData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      county: getCountyName(filterForm.countyCode) || undefined,
      township: filterForm.townCode || undefined,
      replacementType: filterForm.replacementType || undefined,
      componentName: filterForm.componentName || undefined,
      ovenId: filterForm.ovenId || undefined,
      repairYear: filterForm.repairYear || undefined,
    }
    const res: any = await getRepairList(params)
    tableData.value = res?.rows || []
    total.value = res?.total ?? 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

/** 重置 */
function handleReset() {
  filterForm.countyCode = ''
  filterForm.townCode = ''
  filterForm.replacementType = ''
  filterForm.componentName = ''
  filterForm.ovenId = ''
  filterForm.repairYear = ''
  townOptions.value = []
  pagination.pageNum = 1
  loadData()
}

/** 分页变化 */
function handlePageChange(page: number) {
  pagination.pageNum = page
  loadData()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

/** 维修提报 */
function handleApply() {
  router.push('/repair/submit')
}

/** 查看详情 */
function handleView(row: any) {
  router.push(`/repair/approve/${row.id}`)
}

/** 删除 */
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除维修记录「${row.ovenId || ''} - ${row.componentName || ''}」吗？`, '提示', {
      type: 'warning',
    })
    await deleteRepair(row.id)
    ElMessage.success('删除成功，已同步到 MySQL')
    loadData()
  } catch {
    // 用户取消
  }
}

/** 格式化金额 */
function formatCost(val: number) {
  return val != null ? `¥${Number(val).toLocaleString()}` : '-'
}

/** 格式化数量+单位 */
function formatQuantity(row: any) {
  const q = row.componentQuantity
  const u = row.componentUnit || ''
  if (q == null || q === '') return '-'
  return `${q}${u}`
}

onMounted(() => {
  loadCounties()
  loadData()
  loadApplyCount()
})
</script>

<template>
  <div class="page-container repair-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><SetUp /></el-icon>
        </div>
        <h2 class="page-title">维修管理</h2>
      </div>
      <p class="page-desc">烤房维修记录查询、部件级更换明细与维修资金分析</p>
    </div>

    <!-- 预警窗口：已提报维修申请 -->
    <el-alert
      v-if="!repairAlertClosed && repairApplyCount > 0"
      class="warning-alert"
      title="预警提醒"
      type="warning"
      show-icon
      :closable="true"
      @close="repairAlertClosed = true"
    >
      <div class="warning-alert-msg">
        <span><span class="warning-num">{{ repairApplyCount }}</span>人已提报维修申请，请及时处理。</span>
      </div>
    </el-alert>

    <!-- 筛选区 -->
    <div class="filter-container">
      <el-form :model="filterForm" inline>
        <el-form-item label="县区">
          <el-select v-model="filterForm.countyCode" placeholder="全部" clearable style="width: 130px" @change="handleCountyChange">
            <el-option v-for="c in countyOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="乡镇">
          <el-select
            v-model="filterForm.townCode"
            :placeholder="filterForm.countyCode ? '全部' : '请先选县区'"
            clearable
            style="width: 130px"
            :disabled="!filterForm.countyCode"
          >
            <el-option v-for="t in townOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="更换类型">
          <el-select v-model="filterForm.replacementType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="opt in replacementTypeOptions" :key="opt" :label="opt" :value="opt">
              <div style="display:flex;align-items:center;gap:8px">
                <span :style="{ width: '10px', height: '10px', borderRadius: '50%', background: getReplacementTypeStyle(opt).color }"></span>
                <span>{{ opt }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="部件名称">
          <el-input v-model="filterForm.componentName" placeholder="搜索部件名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="烤房编号">
          <el-input v-model="filterForm.ovenId" placeholder="模糊搜索烤房编号" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="维修年度">
          <el-input v-model="filterForm.repairYear" placeholder="如 2024" clearable style="width: 110px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格区 -->
    <div class="table-container">
      <div class="action-bar">
        <el-button type="primary" :icon="Plus" @click="handleApply">维修提报</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" size="small">
        <el-table-column label="烤房编号" width="130" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <BarnCode :code="row.ovenId" />
          </template>
        </el-table-column>
        <el-table-column label="区域" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            {{ [row.county, row.township, row.village].filter(Boolean).join(' ') }}
          </template>
        </el-table-column>
        <el-table-column prop="componentName" label="部件名称" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.componentName" class="comp-tag">{{ row.componentName }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="repairAction" label="维修动作" width="65" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.repairAction || '-' }}</template>
        </el-table-column>
        <el-table-column prop="replacementType" label="更换类型" width="80" align="center">
          <template #default="{ row }">
            <span
              v-if="row.replacementType"
              class="replace-type-tag"
              :style="{ background: getReplacementTypeStyle(row.replacementType).bg, color: getReplacementTypeStyle(row.replacementType).color }"
            >{{ row.replacementType }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="50" align="center">
          <template #default="{ row }">{{ formatQuantity(row) }}</template>
        </el-table-column>
        <el-table-column prop="repairYear" label="维修年度" width="65" align="center">
          <template #default="{ row }">{{ row.repairYear || '-' }}</template>
        </el-table-column>
        <el-table-column prop="repairCode" label="维修编码" width="110" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.repairCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="estimatedCost" label="工程造价" width="80" align="center">
          <template #default="{ row }">{{ formatCost(row.estimatedCost) }}</template>
        </el-table-column>
        <el-table-column prop="industryAmount" label="行业投入" width="80" align="center">
          <template #default="{ row }">{{ formatCost(row.industryAmount) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btn-group">
              <button class="table-action-btn table-action-btn--primary" @click="handleView(row)">
                <el-icon><View /></el-icon>查看
              </button>
              <button class="table-action-btn table-action-btn--danger" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== 页面标题样式 ========== */
.page-title-wrap {
  display: flex;
  align-items: center;
  gap: clamp(10px, 2vw, 14px);
  margin-bottom: 6px;
}

.page-title-icon {
  width: clamp(36px, 3vw, 44px);
  height: clamp(36px, 3vw, 44px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.page-title-icon .el-icon {
  color: #FFFFFF;
  font-size: clamp(16px, 1.5vw, 20px);
}

.page-title {
  font-size: clamp(18px, 2vw, 22px);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.2vw, 14px);
  color: var(--text-muted);
  margin: 0;
  padding-left: clamp(44px, 4vw, 56px);
}

/* ========== 卡片容器（紧凑 12px） ========== */
.filter-container,
.table-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 12px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.filter-container {
  margin-bottom: 12px;
}

/* ========== 预警窗口 ========== */
.warning-alert {
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 3px 12px rgba(212, 100, 74, 0.15);
}

.warning-alert-msg {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 13px;
  color: #6B7280;
}

.warning-alert-msg .warning-num {
  font-weight: 700;
  color: #D4604A;
  padding: 0 2px;
}

.table-container {
  margin-bottom: 12px;
}

/* 筛选区紧凑间距 */
.filter-container :deep(.el-form-item) {
  margin-right: 8px;
  margin-bottom: 8px;
}

.action-bar {
  margin-bottom: 8px;
}

/* ========== 按钮轻拟态 ========== */
.action-bar :deep(.el-button--primary),
.filter-container :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 2px 8px rgba(10, 77, 62, 0.25),
    0 1px 2px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transition: all 0.2s ease;
}

.action-bar :deep(.el-button--primary:hover),
.filter-container :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #107055);
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.35),
    0 2px 4px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.action-bar :deep(.el-button:not(.el-button--primary)),
.filter-container :deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(145deg, #ffffff, #f5f5f5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.08),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  transition: all 0.2s ease;
}

.action-bar :deep(.el-button:not(.el-button--primary):hover),
.filter-container :deep(.el-button:not(.el-button--primary):hover) {
  background: linear-gradient(145deg, #ffffff, #fafafa);
  transform: translateY(-1px);
  box-shadow:
    0 4px 10px rgba(0, 0, 0, 0.1),
    0 2px 3px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

/* ========== 表格操作按钮 ========== */
.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.table-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 5px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  background: linear-gradient(145deg, #ffffff, #f0f0f0);
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.08),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  white-space: nowrap;
}

.table-action-btn:hover {
  transform: translateY(-1px);
  box-shadow:
    0 4px 10px rgba(0, 0, 0, 0.1),
    0 2px 3px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.table-action-btn--primary {
  color: #0D5D46;
}

.table-action-btn--danger {
  color: #D4604A;
}

/* ========== 部件名称色块标签 ========== */
.comp-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  background: #E8F5EE;
  color: #2E8B6A;
  white-space: nowrap;
}

/* ========== 更换类型标签 ========== */
.replace-type-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  white-space: nowrap;
  font-weight: 500;
}

/* ========== 分页样式 ========== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 表格内取消烤房编号方框样式 */
.table-container :deep(.barn-code) {
  background: none;
  border: none;
  padding: 0;
  border-radius: 0;
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .filter-container :deep(.el-form-item) {
    margin-right: 0;
    width: 100%;
  }

  .filter-container :deep(.el-select),
  .filter-container :deep(.el-input) {
    width: 100% !important;
  }
}
</style>
