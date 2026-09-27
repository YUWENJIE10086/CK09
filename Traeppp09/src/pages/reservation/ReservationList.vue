<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, View, Delete, Calendar } from '@element-plus/icons-vue'
import { getReservationList, approveReservation, addReservation, deleteReservation } from '@/api/reservation'
import { getBarnOptions } from '@/api/barn'
import { useReservationStore } from '@/store/reservation'
import StatusTag from '@/components/StatusTag.vue'
import BarnCode from '@/components/BarnCode.vue'

/** 共享预约数据 store */
const reservationStore = useReservationStore()

/** 状态选项映射（中文 → 英文key） */
const statusOptions: { label: string; value: string }[] = [
  { label: '待审核', value: '待审核' },
  { label: '已确认', value: '已确认' },
  { label: '使用中', value: '使用中' },
  { label: '已完成', value: '已完成' },
  { label: '已取消', value: '已取消' },
  { label: '超时', value: '超时' },
]

/** 中文状态 → StatusTag key映射 */
const statusKeyMap: Record<string, string> = {
  '待审核': 'pending',
  '已确认': 'confirmed',
  '使用中': 'baking',
  '烘烤中': 'baking',
  '已完成': 'finished',
  '已取消': 'rejected',
  '已驳回': 'rejected',
  '超时': 'timeout',
}

/** 筛选表单 */
const filterForm = reactive({
  status: '',
  ovenId: undefined as string | undefined,
  dateRange: [] as string[],
})

/** 分页参数 */
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

/** 表格数据 */
const tableData = ref<any[]>([])
const loading = ref(false)

/** 烤房下拉 */
const barnOptions = ref<any[]>([])

/** 多选数据 */
const selectedRows = ref<any[]>([])

/** 审核对话框 */
const auditDialog = ref(false)
const auditForm = reactive({
  id: '' as string,
  status: '',
  opinion: '',
})
const auditLoading = ref(false)

/** 详情对话框 */
const detailDialog = ref(false)
const detailData = ref<any | null>(null)

/** 新增预约对话框 */
const createDialog = ref(false)
const createForm = reactive({
  ovenId: undefined as string | undefined,
  userName: '',
  userPhone: '',
  reserveStart: '' as string | '',
  reserveEnd: '' as string | '',
  leafWeight: 0,
})
const createLoading = ref(false)
const createRef = ref()

/** 时间字符串处理（数组/字符串都支持） */
function parseTime(v: any): string {
  if (!v) return ''
  if (typeof v === 'string') return v
  if (Array.isArray(v) && v.length >= 3) {
    const [y, m, d, hh = 0, mm = 0, ss = 0] = v
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
  }
  return String(v)
}

/** 加载烤房选项 */
async function loadBarns() {
  try {
    barnOptions.value = (await getBarnOptions()) as any[]
  } catch {
    barnOptions.value = []
  }
}

/** 获取列表数据 */
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    }
    if (filterForm.status) params.status = filterForm.status
    if (filterForm.ovenId) params.ovenId = filterForm.ovenId

    const res: any = await getReservationList(params)
    tableData.value = res.rows || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  fetchData()
}

/** 重置 */
function handleReset() {
  filterForm.status = ''
  filterForm.ovenId = undefined
  filterForm.dateRange = []
  pagination.pageNum = 1
  fetchData()
}

/** 分页变化 */
function handlePageChange(page: number) {
  pagination.pageNum = page
  fetchData()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchData()
}

/** 多选变化 */
function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

/** 批量通过 - 同步到 MySQL + 刷新日历 */
async function handleBatchApprove() {
  const pendingRows = selectedRows.value.filter(r => r.status === '待审核')
  if (pendingRows.length === 0) {
    ElMessage.warning('请选择待审核的预约记录')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认批量通过 ${pendingRows.length} 条待审核预约？`,
      '批量审核',
      { type: 'warning' }
    )
    for (const row of pendingRows) {
      await reservationStore.approve(row.id, '已确认', '批量通过')
    }
    ElMessage.success('批量审核通过成功，数据已写入数据库')
    fetchData()
  } catch {
    // 用户取消
  }
}

/** 打开审核对话框 */
function openAuditDialog(row: any, status: string) {
  auditForm.id = row.id
  auditForm.status = status
  auditForm.opinion = ''
  auditDialog.value = true
}

/** 提交审核 - 同步到 MySQL + 刷新日历 */
async function submitAudit() {
  auditLoading.value = true
  try {
    await reservationStore.approve(auditForm.id, auditForm.status, auditForm.opinion)
    ElMessage.success(auditForm.status === '已确认' ? '审核通过，已写入数据库' : '已驳回')
    auditDialog.value = false
    fetchData()
  } finally {
    auditLoading.value = false
  }
}

/** 查看详情 */
function handleView(row: any) {
  detailData.value = row
  detailDialog.value = true
}

/** 打开新增对话框 */
function openCreateDialog() {
  Object.assign(createForm, {
    ovenId: undefined,
    userName: '',
    userPhone: '',
    reserveStart: '',
    reserveEnd: '',
    leafWeight: 0,
  })
  createDialog.value = true
}

/** 格式化日期为 yyyy-MM-dd HH:mm:ss */
function formatDatetime(d: Date | string): string {
  if (!d) return ''
  if (typeof d === 'string') return d.replace('T', ' ')
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 提交新增 - 同步到 MySQL + 刷新日历 */
async function submitCreate() {
  if (!createForm.ovenId) { ElMessage.warning('请选择烤房'); return }
  if (!createForm.userName) { ElMessage.warning('请输入烟农姓名'); return }
  if (!createForm.reserveStart || !createForm.reserveEnd) { ElMessage.warning('请选择预约时间'); return }
  createLoading.value = true
  try {
    const barn: any = barnOptions.value.find((b: any) => b.id === createForm.ovenId)
    await reservationStore.createReservation({
      ovenId: createForm.ovenId,
      barnName: barn?.barnName,
      id: barn?.id,
      userName: createForm.userName,
      userPhone: createForm.userPhone,
      reserveStart: formatDatetime(createForm.reserveStart),
      reserveEnd: formatDatetime(createForm.reserveEnd),
      reserveDate: formatDatetime(createForm.reserveStart).split(' ')[0],
      leafWeight: createForm.leafWeight,
      status: '待审核',
      isTimeout: 0,
    })
    ElMessage.success('预约创建成功，已写入数据库')
    createDialog.value = false
    fetchData()
  } finally {
    createLoading.value = false
  }
}

/** 删除预约 - 同步到 MySQL + 刷新日历 */
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除预约「${row.userName} - ${row.barnName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await reservationStore.remove(row.id)
    ElMessage.success('删除成功，已同步到数据库')
    fetchData()
  } catch {
    // 用户取消
  }
}

/** 行样式：超时记录红色高亮 */
function tableRowClassName({ row }: { row: any }): string {
  return row.isTimeout === 1 ? 'timeout-row' : ''
}

onMounted(() => {
  loadBarns()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><Calendar /></el-icon>
        </div>
        <h2 class="page-title">预约管理</h2>
      </div>
      <p class="page-desc">烤房预约记录查询、审核与管理</p>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-container">
      <el-form :model="filterForm" inline>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="烤房">
          <el-select v-model="filterForm.ovenId" placeholder="全部烤房" clearable filterable style="width: 220px">
            <el-option
              v-for="barn in barnOptions"
              :key="barn.id"
              :label="barn.barnName"
              :value="barn.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约时间">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 + 表格 -->
    <div class="table-container">
      <div class="table-toolbar">
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>新增预约
        </el-button>
        <el-button type="success" :disabled="selectedRows.length === 0" @click="handleBatchApprove">
          <el-icon><CircleCheck /></el-icon>批量通过
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        :row-class-name="tableRowClassName"
        @selection-change="handleSelectionChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column prop="id" label="预约编号" width="100" align="center" />
        <el-table-column prop="userName" label="烟农姓名" width="100" align="center" />
        <el-table-column label="烤房编号" width="160" align="center">
          <template #default="{ row }">
            <BarnCode :code="row.ovenId" />
          </template>
        </el-table-column>
        <el-table-column prop="barnName" label="烤房名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="预约开始时间" width="160" align="center">
          <template #default="{ row }">{{ parseTime(row.reserveStart).slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="预约结束时间" width="160" align="center">
          <template #default="{ row }">{{ parseTime(row.reserveEnd).slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="statusKeyMap[row.status]" type="reservation" />
          </template>
        </el-table-column>
        <el-table-column label="是否超时" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isTimeout === 1" type="danger" size="small" effect="light">超时</el-tag>
            <el-tag v-else type="success" size="small" effect="light">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '待审核'">
              <button class="table-action-btn table-action-btn--success" @click="openAuditDialog(row, '已确认')">
                <el-icon><Check /></el-icon>通过
              </button>
              <span class="action-divider"></span>
              <button class="table-action-btn table-action-btn--danger" @click="openAuditDialog(row, '已驳回')">
                <el-icon><Close /></el-icon>驳回
              </button>
            </template>
            <span v-if="row.status === '待审核'" class="action-divider"></span>
            <button class="table-action-btn table-action-btn--primary" @click="handleView(row)">
              <el-icon><View /></el-icon>查看
            </button>
            <span class="action-divider"></span>
            <button class="table-action-btn table-action-btn--danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 审核对话框 -->
    <el-dialog v-model="auditDialog" :title="auditForm.status === '已确认' ? '审核通过' : '驳回预约'" width="480px" destroy-on-close class="stripe-dialog" append-to-body align-center>
      <el-form :model="auditForm" label-width="80px">
        <el-form-item label="预约编号">
          <span>{{ auditForm.id }}</span>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="auditForm.opinion"
            type="textarea"
            :rows="3"
            placeholder="请输入审核意见（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="submitAudit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialog" title="预约详情" width="560px" destroy-on-close class="stripe-dialog" append-to-body align-center>
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="预约编号">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="烟农姓名">{{ detailData.userName }}</el-descriptions-item>
          <el-descriptions-item label="烤房编号">
            <BarnCode :code="detailData.ovenId" />
          </el-descriptions-item>
          <el-descriptions-item label="烤房名称">{{ detailData.barnName }}</el-descriptions-item>
          <el-descriptions-item label="预约开始时间">{{ parseTime(detailData.reserveStart).slice(0, 19) }}</el-descriptions-item>
          <el-descriptions-item label="预约结束时间">{{ parseTime(detailData.reserveEnd).slice(0, 19) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="statusKeyMap[detailData.status]" type="reservation" />
          </el-descriptions-item>
          <el-descriptions-item label="是否超时">
            <el-tag :type="detailData.isTimeout === 1 ? 'danger' : 'success'" size="small" effect="light">
              {{ detailData.isTimeout === 1 ? '超时' : '正常' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增预约对话框 -->
    <el-dialog v-model="createDialog" title="新增预约" width="540px" destroy-on-close class="stripe-dialog" append-to-body align-center>
      <el-form ref="createRef" :model="createForm" label-width="100px">
        <el-form-item label="选择烤房" required>
          <el-select v-model="createForm.ovenId" placeholder="请选择烤房" filterable style="width: 100%">
            <el-option
              v-for="barn in barnOptions"
              :key="barn.id"
              :label="`${barn.barnName} (${barn.id || ''})`"
              :value="barn.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="烟农姓名" required>
          <el-input v-model="createForm.userName" placeholder="请输入烟农姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="createForm.userPhone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="预约开始" required>
          <el-date-picker v-model="createForm.reserveStart" type="datetime" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预约结束" required>
          <el-date-picker v-model="createForm.reserveEnd" type="datetime" placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="烟叶重量">
          <el-input-number v-model="createForm.leafWeight" :min="0" :step="100" />
          <span style="margin-left: 8px; color: var(--text-muted)">公斤</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ========================================
   毛玻璃质感设计系统
   ======================================== */

/* 页面容器 */
.page-container {
  padding: clamp(16px, 2vw, 24px);
  background: linear-gradient(180deg, #F8FAFB 0%, #EEF2F6 100%);
  min-height: calc(100vh - var(--navbar-height));
}

/* 页面标题 */
.page-header {
  margin-bottom: clamp(16px, 2vw, 24px);
}

.page-title-wrap {
  display: flex;
  align-items: center;
  gap: clamp(8px, 1vw, 12px);
  margin-bottom: 6px;
}

.page-title-icon {
  width: clamp(32px, 4vw, 40px);
  height: clamp(32px, 4vw, 40px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.page-title-icon .el-icon {
  color: #FFFFFF;
}

.page-title {
  font-size: clamp(18px, 2.5vw, 24px);
  font-weight: 700;
  color: #1A2A3A;
  margin: 0;
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.5vw, 14px);
  color: #6B7C8D;
  margin: 0;
  padding-left: clamp(40px, 5vw, 52px);
}

/* 毛玻璃卡片基础样式 */
.glass-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.glass-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 32px rgba(10, 77, 62, 0.12), 0 2px 4px rgba(0, 0, 0, 0.04);
}

/* 筛选区域 */
.filter-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(16px, 2vw, 24px);
  margin-bottom: clamp(16px, 2vw, 24px);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

/* 筛选区宽松间距 */
.filter-container :deep(.el-form-item) {
  margin-right: clamp(16px, 2vw, 24px);
  margin-bottom: 0;
}

/* 表格容器 */
.table-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(16px, 2vw, 24px);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

/* 操作栏 */
.table-toolbar {
  display: flex;
  align-items: center;
  gap: clamp(8px, 1vw, 12px);
  margin-bottom: clamp(12px, 1.5vw, 16px);
  flex-wrap: wrap;
}

/* 轻拟态按钮 - 主按钮 */
.table-toolbar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.2s ease;
  font-weight: 500;
}

.table-toolbar :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #0A4D3E 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.table-toolbar :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(10, 77, 62, 0.3), inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 成功按钮 */
.table-toolbar :deep(.el-button--success) {
  background: linear-gradient(135deg, #059669 0%, #10B981 100%);
  border: none;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(5, 150, 105, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.2s ease;
  font-weight: 500;
}

.table-toolbar :deep(.el-button--success:hover:not(:disabled)) {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.table-toolbar :deep(.el-button--success:active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(5, 150, 105, 0.3), inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 表格样式 */
.table-container :deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

.table-container :deep(.el-table th) {
  background: linear-gradient(135deg, #F8FAFB 0%, #EEF2F6 100%);
  color: #374151;
  font-weight: 600;
}

.table-container :deep(.el-table tr:hover > td) {
  background: rgba(10, 77, 62, 0.04) !important;
}

/* 表格操作按钮 */
.table-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  background: linear-gradient(135deg, #F8FAFB 0%, #EEF2F6 100%);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.table-action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.table-action-btn:active {
  transform: translateY(0);
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

.table-action-btn--primary {
  color: #0A4D3E;
  background: linear-gradient(135deg, rgba(10, 77, 62, 0.08) 0%, rgba(10, 77, 62, 0.04) 100%);
}

.table-action-btn--success {
  color: #059669;
  background: linear-gradient(135deg, rgba(5, 150, 105, 0.12) 0%, rgba(5, 150, 105, 0.06) 100%);
}

.table-action-btn--danger {
  color: #DC2626;
  background: linear-gradient(135deg, rgba(220, 38, 38, 0.1) 0%, rgba(220, 38, 38, 0.05) 100%);
}

.action-divider {
  display: inline-block;
  width: 1px;
  height: 14px;
  background: #E5E7EB;
  margin: 0 6px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: clamp(12px, 1.5vw, 16px);
}

.pagination-wrapper :deep(.el-pagination) {
  gap: 8px;
}

.pagination-wrapper :deep(.el-pagination .el-pager li) {
  border-radius: 6px;
  transition: all 0.2s ease;
}

.pagination-wrapper :deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  color: #fff;
  box-shadow: 0 2px 6px rgba(10, 77, 62, 0.25);
}

/* 超时行高亮 */
:deep(.timeout-row) {
  --el-table-tr-bg-color: rgba(220, 38, 38, 0.06);
}

:deep(.timeout-row:hover > td) {
  --el-table-tr-bg-color: rgba(220, 38, 38, 0.1) !important;
}

/* 筛选区按钮 */
.filter-container :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.2s ease;
}

.filter-container :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #0A4D3E 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.4);
  transform: translateY(-2px);
}

.filter-container :deep(.el-button:not(.el-button--primary)) {
  border-radius: 8px;
  background: linear-gradient(135deg, #F8FAFB 0%, #EEF2F6 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
}

.filter-container :deep(.el-button:not(.el-button--primary):hover) {
  background: linear-gradient(135deg, #EEF2F6 0%, #E5E9EF 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

/* 对话框毛玻璃样式 */
.stripe-dialog :deep(.el-dialog) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0.85) 100%);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stripe-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 20px 24px !important;
  background: transparent;
}

.stripe-dialog :deep(.el-dialog__body) {
  padding: 24px !important;
  background: transparent;
}

.stripe-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px !important;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
}

.stripe-dialog :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.3);
}

.stripe-dialog :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #0A4D3E 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.4);
}

.stripe-dialog :deep(.el-button:not(.el-button--primary)) {
  border-radius: 8px;
  background: linear-gradient(135deg, #F8FAFB 0%, #EEF2F6 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .page-container {
    padding: 12px;
  }
  
  .filter-container :deep(.el-form-item) {
    margin-right: 0;
    width: 100%;
  }
  
  .filter-container :deep(.el-form-item .el-select),
  .filter-container :deep(.el-form-item .el-date-editor) {
    width: 100% !important;
  }
  
  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .table-toolbar :deep(.el-button) {
    width: 100%;
  }
}
</style>
