<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, CircleCheck } from '@element-plus/icons-vue'

/** 角色数据类型 */
interface RoleRecord {
  roleId: number
  roleName: string
  roleKey: string
  sort: number
  status: '0' | '1'
  createTime: string
  menuIds: number[]
}

/** 菜单树节点 */
interface MenuNode {
  id: number
  label: string
  children?: MenuNode[]
}

/** Mock菜单树数据 */
const menuTreeData: MenuNode[] = [
  {
    id: 1, label: '烤房管理', children: [
      { id: 11, label: '烤房列表' },
      { id: 12, label: '新建烤房' },
      { id: 13, label: '烤房详情' },
    ]
  },
  {
    id: 2, label: '维修管理', children: [
      { id: 21, label: '维修列表' },
      { id: 22, label: '维修提报' },
      { id: 23, label: '维修审批' },
      { id: 24, label: '维修验收' },
      { id: 25, label: '维修资金' },
    ]
  },
  {
    id: 3, label: '预约管理', children: [
      { id: 31, label: '预约审核' },
      { id: 32, label: '预约日历' },
    ]
  },
  {
    id: 4, label: '评价管理', children: [
      { id: 41, label: '评价列表' },
      { id: 42, label: '评价分析' },
    ]
  },
  {
    id: 5, label: '系统管理', children: [
      { id: 51, label: '用户管理' },
      { id: 52, label: '角色管理' },
      { id: 53, label: '菜单管理' },
      { id: 54, label: '部门管理' },
      { id: 55, label: '字典管理' },
      { id: 56, label: '操作日志' },
    ]
  },
]

/** Mock角色数据 */
const mockRoles: RoleRecord[] = [
  { roleId: 1, roleName: '超级管理员', roleKey: 'admin', sort: 1, status: '0', createTime: '2025-01-01 10:00:00', menuIds: [1, 11, 12, 13, 2, 21, 22, 23, 24, 25, 3, 31, 32, 4, 41, 42, 5, 51, 52, 53, 54, 55, 56] },
  { roleId: 2, roleName: '烤房管理员', roleKey: 'barn_admin', sort: 2, status: '0', createTime: '2025-02-10 09:00:00', menuIds: [1, 11, 12, 13, 3, 31, 32] },
  { roleId: 3, roleName: '维修人员', roleKey: 'repair', sort: 3, status: '0', createTime: '2025-03-15 14:30:00', menuIds: [2, 21, 22, 23, 24] },
  { roleId: 4, roleName: '普通用户', roleKey: 'common', sort: 4, status: '0', createTime: '2025-04-20 11:00:00', menuIds: [1, 11, 3, 31] },
  { roleId: 5, roleName: '审核人员', roleKey: 'auditor', sort: 5, status: '0', createTime: '2025-05-10 16:00:00', menuIds: [2, 21, 23, 3, 31] },
  { roleId: 6, roleName: '测试角色', roleKey: 'test', sort: 6, status: '1', createTime: '2025-06-01 08:00:00', menuIds: [] },
]

/** 筛选表单 */
const filterForm = reactive({
  roleName: '',
  roleKey: '',
})

/** 表格数据 */
const tableData = ref<RoleRecord[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
})

/** 对话框状态 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const dialogForm = reactive({
  roleId: 0,
  roleName: '',
  roleKey: '',
  sort: 0,
  status: '0' as '0' | '1',
  menuIds: [] as number[],
})
const dialogFormRef = ref()
const menuTreeRef = ref()
const isEdit = ref(false)

/** 数据权限对话框 */
const dataPermVisible = ref(false)
const dataPermRoleId = ref(0)
const dataPermRoleName = ref('')

/** 表单校验规则 */
const dialogRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
}

/** 加载数据 */
function loadData() {
  loading.value = true
  setTimeout(() => {
    let filtered = [...mockRoles]
    if (filterForm.roleName) {
      filtered = filtered.filter(r => r.roleName.includes(filterForm.roleName))
    }
    if (filterForm.roleKey) {
      filtered = filtered.filter(r => r.roleKey.includes(filterForm.roleKey))
    }
    total.value = filtered.length
    const start = (pagination.pageNum - 1) * pagination.pageSize
    tableData.value = filtered.slice(start, start + pagination.pageSize)
    loading.value = false
  }, 300)
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

/** 重置 */
function handleReset() {
  Object.assign(filterForm, { roleName: '', roleKey: '' })
  pagination.pageNum = 1
  loadData()
}

/** 分页 */
function handlePageChange(page: number) {
  pagination.pageNum = page
  loadData()
}
function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

/** 新增 */
function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  Object.assign(dialogForm, { roleId: 0, roleName: '', roleKey: '', sort: 0, status: '0', menuIds: [] })
  dialogVisible.value = true
}

/** 编辑 */
function handleEdit(row: RoleRecord) {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  Object.assign(dialogForm, {
    roleId: row.roleId,
    roleName: row.roleName,
    roleKey: row.roleKey,
    sort: row.sort,
    status: row.status,
    menuIds: [...row.menuIds],
  })
  dialogVisible.value = true
  // 对话框打开后设置树勾选
  setTimeout(() => {
    menuTreeRef.value?.setCheckedKeys(row.menuIds)
  }, 100)
}

/** 提交表单 */
async function handleSubmit() {
  const valid = await dialogFormRef.value?.validate().catch(() => false)
  if (!valid) return
  // 获取勾选的菜单ID
  const checkedKeys = menuTreeRef.value?.getCheckedKeys() || []
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
  dialogForm.menuIds = [...checkedKeys, ...halfCheckedKeys]

  if (isEdit.value) {
    const idx = mockRoles.findIndex(r => r.roleId === dialogForm.roleId)
    if (idx !== -1) {
      mockRoles[idx] = { ...mockRoles[idx], ...dialogForm }
    }
    ElMessage.success('修改成功')
  } else {
    mockRoles.push({
      roleId: Date.now(),
      roleName: dialogForm.roleName,
      roleKey: dialogForm.roleKey,
      sort: dialogForm.sort,
      status: dialogForm.status,
      createTime: new Date().toLocaleString(),
      menuIds: dialogForm.menuIds,
    })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

/** 删除 */
async function handleDelete(row: RoleRecord) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  const idx = mockRoles.findIndex(r => r.roleId === row.roleId)
  if (idx !== -1) mockRoles.splice(idx, 1)
  ElMessage.success('删除成功')
  loadData()
}

/** 数据权限 */
function handleDataPerm(row: RoleRecord) {
  dataPermRoleId.value = row.roleId
  dataPermRoleName.value = row.roleName
  dataPermVisible.value = true
}

/** 状态变更 */
function handleStatusChange(row: RoleRecord) {
  const text = row.status === '0' ? '停用' : '启用'
  ElMessageBox.confirm(`确定${text}角色「${row.roleName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    row.status = row.status === '0' ? '1' : '0'
    ElMessage.success(`${text}成功`)
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="role-manage">
    <!-- 筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="角色名称">
          <el-input v-model="filterForm.roleName" placeholder="请输入角色名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="权限字符">
          <el-input v-model="filterForm.roleKey" placeholder="请输入权限字符" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="roleId" label="角色编号" width="100" align="center" />
        <el-table-column prop="roleName" label="角色名称" width="150" align="center" />
        <el-table-column prop="roleKey" label="权限字符" width="150" align="center" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === '0'"
              inline-prompt
              active-text="正常"
              inactive-text="停用"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <button class="table-action-btn table-action-btn--primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </button>
            <span class="action-divider"></span>
            <button class="table-action-btn table-action-btn--danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </button>
            <button class="table-action-btn table-action-btn--warning" @click="handleDataPerm(row)">
              <el-icon><CircleCheck /></el-icon>数据权限
            </button>
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
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close append-to-body align-center>
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="dialogForm.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="dialogForm.roleKey" placeholder="请输入权限字符" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dialogForm.sort" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dialogForm.status" active-value="0" inactive-value="1" inline-prompt active-text="正常" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="menuTreeRef"
            :data="menuTreeData"
            show-checkbox
            node-key="id"
            :default-checked-keys="dialogForm.menuIds"
            :props="{ children: 'children', label: 'label' }"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 数据权限对话框 -->
    <el-dialog v-model="dataPermVisible" title="数据权限" width="500px" destroy-on-close append-to-body align-center>
      <el-form label-width="80px">
        <el-form-item label="角色名称">
          <el-input :model-value="dataPermRoleName" disabled />
        </el-form-item>
        <el-form-item label="权限范围">
          <el-select placeholder="请选择权限范围" style="width: 100%">
            <el-option label="全部数据权限" value="1" />
            <el-option label="自定义数据权限" value="2" />
            <el-option label="本部门数据权限" value="3" />
            <el-option label="本部门及以下数据权限" value="4" />
            <el-option label="仅本人数据权限" value="5" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataPermVisible = false">取 消</el-button>
        <el-button type="primary" @click="dataPermVisible = false; ElMessage.success('设置成功')">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.role-manage {
  padding: clamp(16px, 2vw, 24px);
  min-height: 100%;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95) 0%, rgba(241, 245, 249, 0.9) 100%);
}

/* ===== 页面标题 ===== */
.page-header {
  margin-bottom: clamp(16px, 2vw, 24px);
}

.page-title-wrap {
  display: flex;
  align-items: center;
  gap: clamp(10px, 1.5vw, 16px);
  margin-bottom: 8px;
}

.page-title-icon {
  width: clamp(36px, 4vw, 44px);
  height: clamp(36px, 4vw, 44px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.25);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.page-title-icon:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(10, 77, 62, 0.35);
}

.page-title-icon .el-icon {
  color: #FFFFFF;
  font-size: clamp(16px, 1.8vw, 20px);
}

.page-title {
  font-size: clamp(18px, 2.2vw, 24px);
  font-weight: 700;
  color: #1F2937;
  margin: 0;
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.4vw, 14px);
  color: #6B7280;
  margin: 0;
  padding-left: clamp(44px, 5vw, 56px);
}

/* ===== 毛玻璃卡片 ===== */
.filter-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  padding: clamp(16px, 2vw, 24px);
  padding-bottom: clamp(8px, 1vw, 12px);
  margin-bottom: clamp(12px, 1.5vw, 20px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06), inset 0 1px 1px rgba(255, 255, 255, 0.8);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.filter-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(10, 77, 62, 0.12), inset 0 1px 1px rgba(255, 255, 255, 0.8);
}

/* ===== 操作栏 ===== */
.action-bar {
  margin-bottom: clamp(12px, 1.5vw, 20px);
  display: flex;
  gap: clamp(6px, 0.8vw, 10px);
}

/* 轻拟态主按钮 */
.action-bar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.25), 0 1px 2px rgba(255, 255, 255, 0.5) inset;
  transition: all 0.2s ease;
}

.action-bar :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #126B52 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.35), 0 1px 2px rgba(255, 255, 255, 0.5) inset;
  transform: translateY(-2px);
}

.action-bar :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(10, 77, 62, 0.2), inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 轻拟态默认按钮 */
.action-bar :deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(145deg, #ffffff 0%, #f7f8f9 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(255, 255, 255, 0.8) inset;
  transition: all 0.2s ease;
}

.action-bar :deep(.el-button:not(.el-button--primary):hover) {
  background: linear-gradient(145deg, #f7f8f9 0%, #ffffff 100%);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(255, 255, 255, 0.8) inset;
  transform: translateY(-2px);
}

.action-bar :deep(.el-button:not(.el-button--primary):active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08), inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* ===== 毛玻璃表格卡片 ===== */
.table-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  padding: clamp(16px, 2vw, 24px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06), inset 0 1px 1px rgba(255, 255, 255, 0.8);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.table-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(10, 77, 62, 0.12), inset 0 1px 1px rgba(255, 255, 255, 0.8);
}

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: clamp(12px, 1.5vw, 20px);
}

/* ===== 筛选表单按钮 ===== */
.filter-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.25), 0 1px 2px rgba(255, 255, 255, 0.5) inset;
}

.filter-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #126B52 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.35);
  transform: translateY(-2px);
}

.filter-card :deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(145deg, #ffffff 0%, #f7f8f9 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.filter-card :deep(.el-button:not(.el-button--primary):hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

/* ===== 对话框样式 ===== */
:deep(.el-dialog) {
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

/* ===== 响应式调整 ===== */
@media (max-width: 768px) {
  .role-manage {
    padding: 12px;
  }

  .filter-card,
  .table-card {
    padding: 12px;
    border-radius: 8px;
  }
}
</style>
