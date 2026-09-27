<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Refresh, User } from '@element-plus/icons-vue'
import { UserCog, HardHat, Crown, Gem, Sprout } from 'lucide-vue-next'
import {
  getBakerList,
  getBakerStats,
  getPoundGroups,
  addBaker,
  updateBaker,
  deleteBaker,
} from '@/api/baker'
import type { BakerStats } from '@/api/baker'

// ==================== 统计数据 ====================

const stats = reactive<BakerStats>({
  total: 0,
  active: 0,
  senior: 0,
  mid: 0,
  junior: 0,
})

// ==================== 列表数据 ====================

const tableData = ref<any[]>([])
const loading = ref(false)
const poundGroups = ref<any[]>([])

const queryForm = reactive({
  name: '',
  phone: '',
  status: '' as string | number,
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

// ==================== 生命周期 ====================

onMounted(() => {
  loadPoundGroups()
  loadStats()
  loadTableData()
})

// ==================== 数据加载 ====================

async function loadPoundGroups() {
  try {
    const res: any = await getPoundGroups()
    poundGroups.value = res || []
  } catch (error) {
    console.error('加载磅组列表失败:', error)
  }
}

async function loadStats() {
  try {
    const res = await getBakerStats()
    Object.assign(stats, res)
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

async function loadTableData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    }
    if (queryForm.name) params.name = queryForm.name
    if (queryForm.phone) params.phone = queryForm.phone
    if (queryForm.status !== '') params.status = queryForm.status

    const res: any = await getBakerList(params)
    tableData.value = res?.rows || []
    pagination.total = res?.total || 0
  } catch (error) {
    console.error('加载烘烤师列表失败:', error)
    ElMessage.error('加载烘烤师列表失败')
  } finally {
    loading.value = false
  }
}

// ==================== 查询操作 ====================

function handleSearch() {
  pagination.pageNum = 1
  loadTableData()
}

function handleReset() {
  queryForm.name = ''
  queryForm.phone = ''
  queryForm.status = ''
  handleSearch()
}

// ==================== 新增/编辑 ====================

const dialogVisible = ref(false)
const dialogTitle = ref('新增烘烤师')
const isEdit = ref(false)
const formRef = ref()

const form = reactive({
  id: '' as string,
  name: '',
  phone: '',
  experience: 0,
  rating: 5.0,
  totalOrders: 0,
  poundGroupId: undefined as number | undefined,
  status: 1,
})

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

function handleAdd() {
  dialogTitle.value = '新增烘烤师'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: any) {
  dialogTitle.value = '编辑烘烤师'
  isEdit.value = true
  Object.assign(form, {
    id: row.id || '',
    name: row.name || '',
    phone: row.phone || '',
    experience: row.experience || 0,
    rating: Number(row.rating) || 5.0,
    totalOrders: row.totalOrders || 0,
    poundGroupId: row.poundGroupId,
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    id: '',
    name: '',
    phone: '',
    experience: 0,
    rating: 5.0,
    totalOrders: 0,
    poundGroupId: undefined,
    status: 1,
  })
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    const data = { ...form }
    if (!isEdit.value) {
      delete (data as any).id
    }

    try {
      if (isEdit.value) {
        await updateBaker(data)
        ElMessage.success('更新成功')
      } else {
        await addBaker(data)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      loadStats()
      loadTableData()
    } catch (error) {
      console.error('保存失败:', error)
      ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
    }
  })
}

// ==================== 删除操作 ====================

function handleDelete(row: any) {
  ElMessageBox.confirm(
    `确定要删除烘烤师"${row.name}"吗？`,
    '删除确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await deleteBaker(row.id)
      ElMessage.success('删除成功')
      loadStats()
      loadTableData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
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

// ==================== 工具函数 ====================

function formatTime(time: any): string {
  if (!time) return '—'
  const str = String(time)
  return str.replace('T', ' ').substring(0, 19)
}
</script>

<template>
  <div class="baker-manage">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #0A4D3E, #0D5D46);">
          <UserCog :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">烘烤师总数</div>
          <div class="stat-value">{{ stats.total }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #2E8B6A, #3A9B7A);">
          <HardHat :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">在岗人数</div>
          <div class="stat-value">{{ stats.active }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #1A6B4F, #2A8B6F);">
          <Crown :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">10年以上</div>
          <div class="stat-value">{{ stats.senior }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #5A8A7A, #6A9A8A);">
          <Gem :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">5-10年</div>
          <div class="stat-value">{{ stats.mid }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #8A9A8A, #9AABA0);">
          <Sprout :size="24" color="#FFFFFF" />
        </div>
        <div class="stat-info">
          <div class="stat-label">5年以下</div>
          <div class="stat-value">{{ stats.junior }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="queryForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="queryForm.name" placeholder="请输入姓名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryForm.phone" placeholder="请输入手机号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="在岗" :value="1" />
            <el-option label="离岗" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card class="table-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增烘烤师</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="experience" label="从业年限" width="100" align="center">
          <template #default="{ row }">{{ row.experience }}年</template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="100" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.rating >= 4.5 ? '#2E8B6A' : row.rating >= 3.5 ? '#F39C12' : '#E74C3C', fontWeight: 600 }">
              {{ Number(row.rating || 0).toFixed(1) }}
            </span>
            <span style="color: #999; font-size: 12px;"> / 5</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalOrders" label="累计烘烤次数" width="130" align="center">
          <template #default="{ row }">{{ row.totalOrders }}次</template>
        </el-table-column>
        <el-table-column label="磅组" width="120" align="center">
          <template #default="{ row }">{{ row.poundGroupName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在岗' : '离岗' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btn-group">
              <button class="table-action-btn table-action-btn--primary" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </button>
              <button class="table-action-btn table-action-btn--danger" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close append-to-body align-center>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="从业年限">
          <el-input-number v-model="form.experience" :min="0" :max="50" />
          <span style="margin-left: 8px; color: #999;">年</span>
        </el-form-item>
        <el-form-item label="评分">
          <el-input-number v-model="form.rating" :min="0" :max="5" :precision="1" :step="0.1" />
          <span style="margin-left: 8px; color: #999;">/ 5</span>
        </el-form-item>
        <el-form-item label="累计烘烤">
          <el-input-number v-model="form.totalOrders" :min="0" />
          <span style="margin-left: 8px; color: #999;">次</span>
        </el-form-item>
        <el-form-item label="磅组">
          <el-select v-model="form.poundGroupId" placeholder="请选择磅组" clearable style="width: 200px">
            <el-option v-for="pg in poundGroups" :key="pg.id" :label="pg.name" :value="pg.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在岗</el-radio>
            <el-radio :value="0">离岗</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.baker-manage {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: clamp(12px, 1.5vw, 18px);
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
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
}

.stat-card:hover {
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.06);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #6B8B80;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #0D3D30;
}

/* 筛选卡片 */
.filter-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.06);
}

.filter-card:hover {
  transform: none !important;
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.06) !important;
}

.filter-card :deep(.el-card__body) {
  padding: 12px 16px;
}

.filter-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow: 0 2px 4px rgba(10, 77, 62, 0.2);
}

.filter-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
}

/* 表格卡片 */
.table-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.06);
}

.table-card:hover {
  transform: none !important;
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.06) !important;
}

.table-card :deep(.el-card__body) {
  padding: 12px;
}

/* 去掉表格行悬停高亮 */
.table-card :deep(.el-table__body tr:hover > td) {
  background-color: transparent !important;
}

.toolbar {
  margin-bottom: 14px;
}

.toolbar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow: 0 2px 4px rgba(10, 77, 62, 0.2);
}

.toolbar :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
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
}

.table-action-btn--primary {
  color: #0A4D3E;
}

.table-action-btn--primary:hover {
  color: #0A4D3E;
}

.table-action-btn--danger {
  color: #D4604A;
}

.table-action-btn--danger:hover {
  color: #D4604A;
}

.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  white-space: nowrap;
}

.action-divider {
  display: inline-block;
  width: 1px;
  height: 12px;
  background: #E4E7ED;
  margin: 0 6px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
