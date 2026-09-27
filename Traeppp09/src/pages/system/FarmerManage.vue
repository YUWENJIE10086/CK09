<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download, Upload, Delete, Refresh, RefreshLeft, Link, User, Edit, Connection } from '@element-plus/icons-vue'
import { Users, UserCheck, Award, BadgeCheck, ShieldCheck, AlertTriangle } from 'lucide-vue-next'
import * as XLSX from 'xlsx'
import {
  getFarmerList,
  getFarmerStats,
  addFarmer,
  updateFarmer,
  deleteFarmer,
  batchDeleteFarmer,
  exportFarmer,
  downloadImportTemplate,
  importFarmerJson
} from '@/api/farmer'
import { get, del } from '@/api/request'
import type { Farmer, FarmerStats } from '@/api/farmer'

// ==================== 数据定义 ====================

// 统计数据
const stats = reactive<FarmerStats>({
  total: 0,
  active: 0,
  creditA: 0,
  creditB: 0,
  creditC: 0,
  creditD: 0
})

// 表格数据
const tableData = ref<Farmer[]>([])
const loading = ref(false)

// 选中的行（用于批量删除）
const selectedRows = ref<Farmer[]>([])

// 查询参数
const queryForm = reactive({
  name: '',
  phone: '',
  creditLevel: '',
  area: ''
})

// 分页参数
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增烟农')
const form = reactive({
  id: undefined as string | undefined,
  name: '',
  phone: '',
  creditLevel: 'B',
  creditScore: 0,
  area: '',
  poundGroupId: 1,
  plantingArea: 0,
  plantingYears: 0,
  totalBakes: 0,
  avatarUrl: '',
  status: 1
})
const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  creditLevel: [{ required: true, message: '请选择信用等级', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}
const formRef = ref()

// ==================== 生命周期 ====================

onMounted(() => {
  loadStats()
  loadTableData()
})

// ==================== 统计数据 ====================

async function loadStats() {
  try {
    const res = await getFarmerStats()
    Object.assign(stats, res)
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// ==================== 表格数据 ====================

async function loadTableData() {
  loading.value = true
  try {
    const res = await getFarmerList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...queryForm
    })
    tableData.value = res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('加载烟农列表失败:', error)
    ElMessage.error('加载烟农列表失败')
  } finally {
    loading.value = false
  }
}

// ==================== 查询操作 ====================

function getCreditLevelType(level: string): 'success' | 'warning' | 'danger' | '' {
  switch (level) {
    case 'A': return 'success'
    case 'B': return 'warning'
    case 'C': return 'danger'
    case 'D': return ''
    default: return ''
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadTableData()
}

function handleReset() {
  queryForm.name = ''
  queryForm.phone = ''
  queryForm.creditLevel = ''
  queryForm.area = ''
  handleSearch()
}

// ==================== 新增操作 ====================

function handleAdd() {
  showFarmerDialog()
}

function showFarmerDialog(farmer?: Farmer) {
  dialogTitle.value = farmer ? '编辑烟农' : '新增烟农'

  // 重置表单
  form.id = farmer?.id
  form.name = farmer?.name || ''
  form.phone = farmer?.phone || ''
  form.creditLevel = farmer?.creditLevel || 'B'
  form.creditScore = farmer?.creditScore || 0
  form.area = farmer?.area || ''
  form.poundGroupId = farmer?.poundGroupId || 1
  form.plantingArea = farmer?.plantingArea || 0
  form.plantingYears = farmer?.plantingYears || 0
  form.totalBakes = farmer?.totalBakes || 0
  form.avatarUrl = farmer?.avatarUrl || ''
  form.status = farmer?.status ?? 1

  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    const farmerData = {
      id: form.id,
      name: form.name,
      phone: form.phone,
      creditLevel: form.creditLevel,
      creditScore: form.creditScore,
      area: form.area,
      poundGroupId: form.poundGroupId,
      plantingArea: form.plantingArea,
      plantingYears: form.plantingYears,
      totalBakes: form.totalBakes,
      avatarUrl: form.avatarUrl,
      status: form.status
    }

    try {
      if (form.id) {
        await updateFarmer(farmerData)
        ElMessage.success('更新成功')
      } else {
        await addFarmer(farmerData)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      loadStats()
      loadTableData()
    } catch (error) {
      console.error('保存失败:', error)
      ElMessage.error(form.id ? '更新失败' : '添加失败')
    }
  })
}

// ==================== 编辑操作 ====================

function handleEdit(row: Farmer) {
  showFarmerDialog(row)
}

// ==================== 删除操作 ====================

/** 表格选择变化事件 */
function handleSelectionChange(rows: Farmer[]) {
  selectedRows.value = rows
}

function handleDelete(row: Farmer) {
  ElMessageBox.confirm(
    `确定要删除烟农"${row.name}"吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteFarmer(row.id)
      ElMessage.success('删除成功')
      loadStats()
      loadTableData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的${selectedRows.value.length}条记录吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await batchDeleteFarmer(selectedRows.value.map(row => row.id))
      ElMessage.success('删除成功')
      loadStats()
      loadTableData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

// ==================== 导入操作 ====================

function handleImport() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.xlsx,.xls'
  input.onchange = async (e: any) => {
    const file = e.target.files[0]
    if (!file) return

    try {
      ElMessage.info('正在解析文件，请稍候...')
      const reader = new FileReader()
      reader.onload = async (event) => {
        try {
          const data = new Uint8Array(event.target?.result as ArrayBuffer)
          const workbook = XLSX.read(data, { type: 'array' })
          const firstSheetName = workbook.SheetNames[0]
          const worksheet = workbook.Sheets[firstSheetName]
          // 获取所有行数据（以数组形式返回，每行为一个数组）
          const rows: any[][] = XLSX.utils.sheet_to_json(worksheet, { header: 1, defval: '' })
          // 跳过前2行（第1行空行，第2行表头），从第3行开始为数据
          const dataRows = rows.slice(2)
          // 将每行数据转为对象
          const dataArray = dataRows
            .filter(row => row && row.some((cell: any) => cell !== '' && cell != null))
            .map((row: any[]) => ({
              unitName: String(row[0] ?? ''),
              name: String(row[1] ?? ''),
              age: row[2] != null ? Number(row[2]) : null,
              phone: String(row[3] ?? ''),
              area: String(row[4] ?? ''),
              contractNo: String(row[5] ?? ''),
              contractType: String(row[6] ?? ''),
              tobaccoVariety: String(row[7] ?? ''),
              plantingArea: row[8] != null ? Number(row[8]) : null,
              agreedQuantity: row[9] != null ? Number(row[9]) : null
            }))

          if (dataArray.length === 0) {
            ElMessage.warning('未解析到有效数据，请检查文件格式')
            return
          }

          ElMessage.info('正在导入数据，请稍候...')
          const res = await importFarmerJson(dataArray)
          ElMessage.success(`导入成功${res.success ?? dataArray.length}条，跳过${res.skipped ?? 0}条`)
          loadStats()
          loadTableData()
        } catch (error) {
          console.error('解析Excel失败:', error)
          ElMessage.error('解析Excel失败')
        }
      }
      reader.onerror = () => {
        ElMessage.error('读取文件失败')
      }
      reader.readAsArrayBuffer(file)
    } catch (error) {
      console.error('导入失败:', error)
      ElMessage.error('导入失败')
    }
  }
  input.click()
}

// ==================== 导出操作 ====================

function handleExport() {
  try {
    if (tableData.value.length === 0) {
      ElMessage.warning('当前没有可导出的数据')
      return
    }
    ElMessage.info('正在导出，请稍候...')
    // 准备导出数据（包含中文表头）
    const exportData = tableData.value.map(row => ({
      '单位名称': row.area || '',
      '种植者名称': row.name || '',
      '年龄': '',
      '手机号': row.phone || '',
      '行政区划': row.area || '',
      '合同编号': '',
      '合同类型': '',
      '烟叶品种': '',
      '种植面积': row.plantingArea ?? '',
      '约定总收购量': ''
    }))
    const worksheet = XLSX.utils.json_to_sheet(exportData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, '烟农信息')
    XLSX.writeFile(workbook, `烟农信息_${new Date().getTime()}.xlsx`)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// ==================== 查看烟农的烤房分配 ====================

const barnDialogVisible = ref(false)
const barnDialogLoading = ref(false)
const barnDialogTitle = ref('')
const barnList = ref<any[]>([])

async function handleViewBarns(row: Farmer) {
  barnDialogTitle.value = `${row.name} 的烤房分配记录`
  barnDialogVisible.value = true
  barnDialogLoading.value = true
  barnList.value = []
  try {
    const res: any = await get('/barn/assignment/list', { farmerName: row.name, pageSize: 100 })
    barnList.value = res?.rows || res || []
  } catch (error) {
    console.error('查询烤房分配失败:', error)
    ElMessage.error('查询烤房分配失败')
  } finally {
    barnDialogLoading.value = false
  }
}

function getBarnStatusType(status: string) {
  switch (status) {
    case '已分配': return 'info'
    case '使用中': return 'success'
    case '已归还': return ''
    case '已撤销': return 'danger'
    default: return 'info'
  }
}

/** 删除单条分配记录 */
async function handleDeleteAssignment(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除烤房「${row.barnName}」的分配记录吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }

  try {
    await del(`/barn/assignment/${row.id}`)
    ElMessage.success('删除成功')
    // 从列表中移除
    barnList.value = barnList.value.filter((item: any) => item.id !== row.id)
  } catch (error) {
    console.error('删除分配记录失败:', error)
    ElMessage.error('删除失败，请稍后重试')
  }
}

/** 清空当前烟农的所有分配记录 */
async function handleClearAllAssignments() {
  if (!barnList.value.length) {
    ElMessage.warning('当前没有分配记录可清空')
    return
  }

  const farmerName = barnList.value[0]?.farmerName || ''
  try {
    await ElMessageBox.confirm(
      `确定要清空「${farmerName}」的所有 ${barnList.value.length} 条分配记录吗？此操作不可恢复！`,
      '清空确认',
      { confirmButtonText: '确定清空', cancelButtonText: '取消', type: 'error' }
    )
  } catch {
    return // 用户取消
  }

  try {
    let successCount = 0
    let failCount = 0
    for (const item of barnList.value) {
      try {
        await del(`/barn/assignment/${item.id}`)
        successCount++
      } catch {
        failCount++
      }
    }
    if (failCount === 0) {
      ElMessage.success(`成功清空 ${successCount} 条分配记录`)
    } else {
      ElMessage.warning(`成功 ${successCount} 条，失败 ${failCount} 条`)
    }
    barnList.value = []
  } catch (error) {
    console.error('清空分配记录失败:', error)
    ElMessage.error('清空失败，请稍后重试')
  }
}

// ==================== 模板下载 ====================

function handleDownloadTemplate() {
  downloadImportTemplate().then((res: any) => {
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '烟农信息导入模板.xlsx'
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  })
}

// ==================== 分页 ====================

function handlePageChange(page: number) {
  pagination.pageNum = page
  loadTableData()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadTableData()
}
</script>

<template>
  <div class="farmer-manage">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #0A4D3E, #0D5D46);">
          <Users :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">总烟农</div>
          <div class="stat-value">{{ stats.total }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #2E8B6A, #3A9B7A);">
          <UserCheck :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">在用</div>
          <div class="stat-value">{{ stats.active }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #1A6B4F, #2A8B6F);">
          <Award :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">信用A级</div>
          <div class="stat-value">{{ stats.creditA }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #5A8A7A, #6A9A8A);">
          <BadgeCheck :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">信用B级</div>
          <div class="stat-value">{{ stats.creditB }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #8A9A8A, #9AABA0);">
          <ShieldCheck :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">信用C级</div>
          <div class="stat-value">{{ stats.creditC }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #D4A017, #E4B027);">
          <AlertTriangle :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">信用D级</div>
          <div class="stat-value">{{ stats.creditD }}</div>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus" size="large" @click="handleAdd">
        新增
      </el-button>
      <el-button :icon="Upload" @click="handleImport">
        导入Excel
      </el-button>
      <el-button :icon="Download" @click="handleDownloadTemplate">
        下载模板
      </el-button>
      <el-button :icon="Download" @click="handleExport">
        导出
      </el-button>
      <el-button :icon="Delete" @click="handleBatchDelete" :disabled="selectedRows.length === 0">
        批量删除
      </el-button>
    </div>

    <!-- 烟农信息对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body align-center>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="信用等级" prop="creditLevel">
          <el-select v-model="form.creditLevel">
            <el-option label="A级" value="A" />
            <el-option label="B级" value="B" />
            <el-option label="C级" value="C" />
            <el-option label="D级" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item label="信用分数" prop="creditScore">
          <el-input-number v-model="form.creditScore" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="所属区域" prop="area">
          <el-input v-model="form.area" placeholder="请输入区域" />
        </el-form-item>
        <el-form-item label="种植面积(亩)" prop="plantingArea">
          <el-input-number v-model="form.plantingArea" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="种植年限(年)" prop="plantingYears">
          <el-input-number v-model="form.plantingYears" :min="0" :max="50" />
        </el-form-item>
        <el-form-item label="总烘烤次数" prop="totalBakes">
          <el-input-number v-model="form.totalBakes" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查询表单 -->
    <div class="query-form">
      <el-form :model="queryForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="queryForm.name" placeholder="请输入姓名" clearable style="width: 180px;" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryForm.phone" placeholder="请输入手机号" clearable style="width: 180px;" />
        </el-form-item>
        <el-form-item label="信用等级">
          <el-select v-model="queryForm.creditLevel" placeholder="请选择" clearable style="width: 120px;">
            <el-option label="A级" value="A" />
            <el-option label="B级" value="B" />
            <el-option label="C级" value="C" />
            <el-option label="D级" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属区域">
          <el-input v-model="queryForm.area" placeholder="请输入区域" clearable style="width: 180px;" />
        </el-form-item>
      </el-form>
      <div class="filter-actions">
        <el-button type="primary" class="search-btn" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="tableData"
        @selection-change="handleSelectionChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="creditLevel" label="信用等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getCreditLevelType(row.creditLevel)">
              {{ row.creditLevel }}级
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用分数" width="100" />
        <el-table-column prop="area" label="所属区域" min-width="150" show-overflow-tooltip />
        <el-table-column prop="plantingArea" label="种植面积(亩)" width="120" />
        <el-table-column prop="plantingYears" label="种植年限(年)" width="120" />
        <el-table-column prop="totalBakes" label="总烘烤次数" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="farmer-actions">
              <el-button type="primary" size="small" :icon="Edit" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button type="success" size="small" :icon="Connection" @click="handleViewBarns(row)">
                查看
              </el-button>
              <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 烤房分配查看对话框 -->
    <el-dialog v-model="barnDialogVisible" :title="barnDialogTitle" width="900px" append-to-body align-center>
      <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
        <span style="color: #909399; font-size: 13px;">共 {{ barnList.length }} 条记录</span>
        <el-button
          type="danger"
          plain
          size="small"
          :icon="Delete"
          :disabled="!barnList.length"
          @click="handleClearAllAssignments"
        >
          清空所有记录
        </el-button>
      </div>
      <el-table v-loading="barnDialogLoading" :data="barnList" border style="width: 100%" empty-text="暂无烤房分配记录">
        <el-table-column prop="barnName" label="烤房名称" min-width="120" />
        <el-table-column prop="farmerName" label="烟农姓名" width="100" />
        <el-table-column prop="farmerPhone" label="烟农电话" width="130" />
        <el-table-column prop="status" label="分配状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getBarnStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="seasonYear" label="烘烤年份" width="100" align="center" />
        <el-table-column prop="seasonName" label="烘烤季" min-width="120" />
        <el-table-column prop="assignDate" label="分配时间" min-width="160">
          <template #default="{ row }">
            {{ row.assignDate || row.assignedAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              link
              size="small"
              :icon="Delete"
              @click="handleDeleteAssignment(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.farmer-manage {
  padding: 12px;
}

/* 操作按钮组 */
.farmer-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.farmer-actions .el-button {
  margin-left: 0 !important;
}

/* 筛选操作按钮 */
.filter-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}
.filter-actions .search-btn {
  background: #409EFF !important;
  border: none !important;
  color: white !important;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}
.filter-actions .search-btn:hover {
  background: #66b1ff !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}
.filter-actions .search-btn:active {
  transform: translateY(0);
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.72));
  backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.06);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 32px rgba(10, 77, 62, 0.12);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #6B8B80;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #0D3D30;
}

/* 操作栏 */
.action-bar {
  margin-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 查询表单 */
.query-form {
  margin-bottom: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.72));
  backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

/* 表格容器 */
.table-container {
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.72));
  backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  padding: 12px;
}

.pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

/* 信用等级标签颜色 */
:deep(.el-tag--A) {
  background: linear-gradient(135deg, #2E8B6A, #3A9B7A);
  color: #fff;
  border: none;
}

:deep(.el-tag--B) {
  background: linear-gradient(135deg, #5A8A7A, #6A9A8A);
  color: #fff;
  border: none;
}

:deep(.el-tag--C) {
  background: linear-gradient(135deg, #8A9A8A, #9AABA0);
  color: #fff;
  border: none;
}

:deep(.el-tag--D) {
  background: linear-gradient(135deg, #D4A017, #E4B027);
  color: #fff;
  border: none;
}
</style>
