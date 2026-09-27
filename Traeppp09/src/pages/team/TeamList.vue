<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { listTeam, addTeam, updateTeam, deleteTeam } from '@/api/team'
import { listCounty } from '@/api/dict'
import type { TeamInfo } from '@/api/team'

/** 队伍类型选项 */
const teamTypeOptions = [
  { label: '烘烤技术服务队', value: '烘烤技术服务队' },
  { label: '简易维修服务队', value: '简易维修服务队' },
  { label: '常规管护服务队', value: '常规管护服务队' },
  { label: '综合管理服务队', value: '综合管理服务队' },
]

/** 队伍类型Tag颜色映射 */
const teamTypeTagMap: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
  '烘烤技术服务队': '',
  '简易维修服务队': 'warning',
  '常规管护服务队': 'success',
  '综合管理服务队': 'danger',
}

/** 县区选项（从数据库加载） */
const countyOptions = ref<{ label: string; value: string }[]>([])

/** 加载县区数据 */
async function loadCounties() {
  try {
    const res: any = await listCounty()
    const list = Array.isArray(res) ? res : (res?.rows || res?.data || [])
    countyOptions.value = list.map((c: any) => ({
      label: c.countyName || c.county_name,
      value: c.countyCode || c.county_code,
    }))
  } catch (e) {
    console.error('加载县区数据失败:', e)
  }
}

/** 队伍列表数据 */
const allTeams = ref<TeamInfo[]>([])

/** 加载中 */
const loading = ref(false)

/** 加载队伍列表 */
async function loadTeams() {
  loading.value = true
  try {
    const params: any = {}
    if (filterForm.teamType) params.teamType = filterForm.teamType
    if (filterForm.countyCode) params.countyCode = filterForm.countyCode
    const res: any = await listTeam(params)
    // 后端返回 TableDataInfo 结构 {total, rows}，已自动解包
    const list = Array.isArray(res) ? res : (res?.rows || [])
    allTeams.value = list || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载队伍数据失败')
    allTeams.value = []
  } finally {
    loading.value = false
  }
}

/** 筛选表单 */
const filterForm = reactive({
  teamType: '',
  countyCode: '',
  keyword: '',
})

/** 筛选后的数据（前端关键词过滤） */
const filteredTeams = computed(() => {
  let list = allTeams.value
  if (filterForm.keyword) {
    const kw = filterForm.keyword.toLowerCase()
    list = list.filter(t =>
      t.teamName.toLowerCase().includes(kw) ||
      t.leaderName?.toLowerCase().includes(kw)
    )
  }
  return list
})

/** 分页 */
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
})

/** 当前页数据 */
const pagedData = computed(() => {
  const start = (pagination.pageNum - 1) * pagination.pageSize
  return filteredTeams.value.slice(start, start + pagination.pageSize)
})

/** 总数 */
const total = computed(() => filteredTeams.value.length)

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  loadTeams()
}

/** 重置 */
function handleReset() {
  filterForm.teamType = ''
  filterForm.countyCode = ''
  filterForm.keyword = ''
  pagination.pageNum = 1
  loadTeams()
}

/** 对话框状态 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增队伍')
const isEdit = ref(false)
const editId = ref(0)

/** 表单数据 */
const formData = reactive({
  teamName: '',
  teamType: '',
  countyCode: '',
  leaderName: '',
  leaderPhone: '',
  memberCount: 5,
  serviceArea: '',
  remark: '',
})

/** 表单引用 */
const formRef = ref()

/** 表单校验规则 */
const formRules = {
  teamName: [{ required: true, message: '请输入队伍名称', trigger: 'blur' }],
  teamType: [{ required: true, message: '请选择队伍类型', trigger: 'change' }],
  countyCode: [{ required: true, message: '请选择所属县区', trigger: 'change' }],
  leaderName: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  leaderPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
}

/** 选中的县区名称 */
const selectedCountyName = computed(() => {
  const opt = countyOptions.value.find(o => o.value === formData.countyCode)
  return opt?.label || ''
})

/** 新增 */
function handleAdd() {
  dialogTitle.value = '新增队伍'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

/** 编辑 */
function handleEdit(row: TeamInfo) {
  dialogTitle.value = '编辑队伍'
  isEdit.value = true
  editId.value = row.teamId
  Object.assign(formData, {
    teamName: row.teamName,
    teamType: row.teamType,
    countyCode: row.countyCode || '',
    leaderName: row.leaderName,
    leaderPhone: row.leaderPhone,
    memberCount: row.memberCount || 5,
    serviceArea: row.serviceArea || '',
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

/** 删除 */
async function handleDelete(row: TeamInfo) {
  await ElMessageBox.confirm(`确定删除队伍「${row.teamName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  try {
    await deleteTeam(row.teamId)
    ElMessage.success('删除成功')
    await loadTeams()
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()

  const data = {
    teamName: formData.teamName,
    teamType: formData.teamType,
    countyCode: formData.countyCode,
    countyName: selectedCountyName.value,
    leaderName: formData.leaderName,
    leaderPhone: formData.leaderPhone,
    memberCount: formData.memberCount,
    serviceArea: formData.serviceArea,
    remark: formData.remark,
  }

  try {
    if (isEdit.value) {
      await updateTeam({ ...data, teamId: editId.value })
      ElMessage.success('编辑成功')
    } else {
      await addTeam(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadTeams()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

/** 重置表单 */
function resetForm() {
  Object.assign(formData, {
    teamName: '',
    teamType: '',
    countyCode: '',
    leaderName: '',
    leaderPhone: '',
    memberCount: 5,
    serviceArea: '',
    remark: '',
  })
}

/** 分页变化 */
function handlePageChange(page: number) {
  pagination.pageNum = page
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
}

onMounted(() => {
  loadCounties()
  loadTeams()
})
</script>

<template>
  <div class="team-list-page">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filterForm" inline>
        <el-form-item label="队伍类型">
          <el-select v-model="filterForm.teamType" placeholder="全部类型" clearable style="width: 180px">
            <el-option v-for="opt in teamTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属县区">
          <el-select v-model="filterForm.countyCode" placeholder="全部县区" clearable style="width: 200px">
            <el-option v-for="opt in countyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="filterForm.keyword" placeholder="队伍名称/负责人" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="toolbar-card" shadow="never">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增队伍</el-button>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="pagedData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="teamName" label="队伍名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="teamTypeTagMap[row.teamType] || 'info'" size="default">{{ row.teamType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="countyName" label="所属县区" width="150" align="center" />
        <el-table-column prop="leaderName" label="负责人" width="100" align="center" />
        <el-table-column prop="leaderPhone" label="联系电话" width="140" align="center" />
        <el-table-column prop="memberCount" label="人数" width="80" align="center" />
        <el-table-column prop="repairCount" label="维修次数" width="100" align="center" />
        <el-table-column prop="serviceArea" label="服务区域" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <button class="table-action-btn table-action-btn--primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
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
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close append-to-body align-center>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="队伍名称" prop="teamName">
          <el-input v-model="formData.teamName" placeholder="请输入队伍名称" />
        </el-form-item>
        <el-form-item label="队伍类型" prop="teamType">
          <el-select v-model="formData.teamType" placeholder="请选择类型" style="width: 100%">
            <el-option v-for="opt in teamTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属县区" prop="countyCode">
          <el-select v-model="formData.countyCode" placeholder="请选择县区" style="width: 100%">
            <el-option v-for="opt in countyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="leaderName">
          <el-input v-model="formData.leaderName" placeholder="请输入负责人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="leaderPhone">
          <el-input v-model="formData.leaderPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="人数" prop="memberCount">
          <el-input-number v-model="formData.memberCount" :min="1" :max="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="服务区域">
          <el-input v-model="formData.serviceArea" type="textarea" :rows="3" placeholder="请输入服务区域" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ==================== 毛玻璃质感设计系统 ==================== */
.team-list-page {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: clamp(12px, 1.5vw, 18px);
}

/* 毛玻璃筛选卡片 */
.filter-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.filter-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(10, 77, 62, 0.2), transparent);
}

.filter-card:hover {
  box-shadow:
    0 6px 24px rgba(10, 77, 62, 0.08),
    0 12px 40px rgba(10, 77, 62, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
}

.filter-card :deep(.el-card__body) {
  padding: clamp(14px, 1.8vw, 20px) clamp(18px, 2.2vw, 26px);
}

/* 轻拟态按钮 */
.filter-card :deep(.el-button--primary),
.toolbar-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 2px 4px rgba(10, 77, 62, 0.2),
    0 4px 8px rgba(10, 77, 62, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transition: all 0.2s ease;
}

.filter-card :deep(.el-button--primary:hover),
.toolbar-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
  box-shadow:
    0 4px 8px rgba(10, 77, 62, 0.25),
    0 8px 16px rgba(10, 77, 62, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.filter-card :deep(.el-button--primary:active),
.toolbar-card :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow:
    0 1px 2px rgba(10, 77, 62, 0.2),
    inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 工具栏卡片 */
.toolbar-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.toolbar-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(10, 77, 62, 0.2), transparent);
}

.toolbar-card :deep(.el-card__body) {
  padding: clamp(10px, 1.2vw, 14px) clamp(18px, 2.2vw, 26px);
}

/* 毛玻璃表格卡片 */
.table-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.table-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(10, 77, 62, 0.2), transparent);
}

.table-card:hover {
  box-shadow:
    0 8px 24px rgba(10, 77, 62, 0.1),
    0 16px 48px rgba(10, 77, 62, 0.08),
    0 0 0 1px rgba(10, 77, 62, 0.1);
  transform: translateY(-3px);
}

.table-card :deep(.el-card__body) {
  padding: clamp(14px, 1.8vw, 20px);
}

/* 表格操作按钮 */
.table-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
}

.table-action-btn--primary {
  color: #0A4D3E;
}

.table-action-btn--primary:hover {
  color: #0D5D46;
  background: rgba(10, 77, 62, 0.06);
  border-radius: 4px;
}

.table-action-btn--danger {
  color: #D4604A;
}

.table-action-btn--danger:hover {
  color: #E74C3C;
  background: rgba(212, 96, 74, 0.06);
  border-radius: 4px;
}

.action-divider {
  display: inline-block;
  width: 1px;
  height: 12px;
  background: #E4E7ED;
  margin: 0 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: clamp(12px, 1.5vw, 18px);
}
</style>