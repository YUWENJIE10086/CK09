<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'

/** 部门数据类型 */
interface DeptRecord {
  deptId: number
  parentId: number
  deptName: string
  sort: number
  leader: string
  phone: string
  email: string
  status: '0' | '1'
  createTime: string
  children?: DeptRecord[]
}

/** Mock部门树数据 */
const mockDeptList: DeptRecord[] = [
  {
    deptId: 100, parentId: 0, deptName: '曲靖烟草公司', sort: 0, leader: '张总', phone: '0874-3111111', email: 'qj@yt.cn', status: '0', createTime: '2025-01-01 10:00:00',
    children: [
      {
        deptId: 101, parentId: 100, deptName: '宣威分公司', sort: 1, leader: '王经理', phone: '0874-7122222', email: 'xw@yt.cn', status: '0', createTime: '2025-01-15 09:00:00',
        children: [
          { deptId: 1011, parentId: 101, deptName: '烤房管理部', sort: 1, leader: '李主任', phone: '13800001001', email: 'xwkf@yt.cn', status: '0', createTime: '2025-02-01 10:00:00' },
          { deptId: 1012, parentId: 101, deptName: '技术保障部', sort: 2, leader: '赵主任', phone: '13800001002', email: 'xwjs@yt.cn', status: '0', createTime: '2025-02-01 10:30:00' },
        ]
      },
      {
        deptId: 102, parentId: 100, deptName: '沾益分公司', sort: 2, leader: '刘经理', phone: '0874-3133333', email: 'zy@yt.cn', status: '0', createTime: '2025-01-15 09:30:00',
        children: [
          { deptId: 1021, parentId: 102, deptName: '烤房管理部', sort: 1, leader: '陈主任', phone: '13800001003', email: 'zykf@yt.cn', status: '0', createTime: '2025-02-05 10:00:00' },
          { deptId: 1022, parentId: 102, deptName: '生产指导部', sort: 2, leader: '杨主任', phone: '13800001004', email: 'zysc@yt.cn', status: '0', createTime: '2025-02-05 10:30:00' },
        ]
      },
      {
        deptId: 103, parentId: 100, deptName: '陆良分公司', sort: 3, leader: '吴经理', phone: '0874-6144444', email: 'll@yt.cn', status: '0', createTime: '2025-01-20 14:00:00',
        children: [
          { deptId: 1031, parentId: 103, deptName: '烤房管理部', sort: 1, leader: '周主任', phone: '13800001005', email: 'llkf@yt.cn', status: '0', createTime: '2025-02-10 09:00:00' },
        ]
      },
      {
        deptId: 104, parentId: 100, deptName: '师宗分公司', sort: 4, leader: '孙经理', phone: '0874-5155555', email: 'sz@yt.cn', status: '0', createTime: '2025-01-25 11:00:00',
      },
      {
        deptId: 105, parentId: 100, deptName: '罗平分公司', sort: 5, leader: '马经理', phone: '0874-8166666', email: 'lp@yt.cn', status: '0', createTime: '2025-02-01 08:30:00',
      },
      {
        deptId: 106, parentId: 100, deptName: '富源分公司', sort: 6, leader: '黄经理', phone: '0874-4677777', email: 'fy@yt.cn', status: '1', createTime: '2025-02-05 15:00:00',
      },
    ]
  }
]

/** 筛选表单 */
const filterForm = reactive({
  deptName: '',
  status: '' as '' | '0' | '1',
})

/** 表格数据 */
const tableData = ref<DeptRecord[]>([])
const loading = ref(false)
const expandAll = ref(true)

/** 对话框状态 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const dialogForm = reactive({
  deptId: 0,
  parentId: 100,
  deptName: '',
  sort: 0,
  leader: '',
  phone: '',
  email: '',
  status: '0' as '0' | '1',
})
const dialogFormRef = ref()
const isEdit = ref(false)

/** 上级部门树 */
const parentDeptTree = computed(() => {
  return [{ deptId: 0, deptName: '顶级部门', children: mockDeptList }]
})

/** 表单校验规则 */
const dialogRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }],
}

/** 加载数据 */
function loadData() {
  loading.value = true
  setTimeout(() => {
    let filtered = JSON.parse(JSON.stringify(mockDeptList)) as DeptRecord[]
    if (filterForm.deptName || filterForm.status !== '') {
      filtered = filterDeptTree(filtered, filterForm.deptName, filterForm.status)
    }
    tableData.value = filtered
    loading.value = false
  }, 300)
}

/** 递归过滤部门树 */
function filterDeptTree(list: DeptRecord[], name: string, status: '' | '0' | '1'): DeptRecord[] {
  return list.filter(item => {
    const nameMatch = !name || item.deptName.includes(name)
    const statusMatch = status === '' || item.status === status
    if (item.children && item.children.length > 0) {
      item.children = filterDeptTree(item.children, name, status)
      return nameMatch || item.children.length > 0
    }
    return nameMatch && statusMatch
  })
}

/** 搜索 */
function handleSearch() {
  loadData()
}

/** 重置 */
function handleReset() {
  Object.assign(filterForm, { deptName: '', status: '' as const })
  loadData()
}

/** 展开/折叠切换 */
function toggleExpand() {
  expandAll.value = !expandAll.value
  const data = [...tableData.value]
  tableData.value = []
  setTimeout(() => { tableData.value = data })
}

/** 新增 */
function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增部门'
  Object.assign(dialogForm, { deptId: 0, parentId: 100, deptName: '', sort: 0, leader: '', phone: '', email: '', status: '0' })
  dialogVisible.value = true
}

/** 新增子部门 */
function handleAddChild(row: DeptRecord) {
  isEdit.value = false
  dialogTitle.value = '新增子部门'
  Object.assign(dialogForm, { deptId: 0, parentId: row.deptId, deptName: '', sort: 0, leader: '', phone: '', email: '', status: '0' })
  dialogVisible.value = true
}

/** 编辑 */
function handleEdit(row: DeptRecord) {
  isEdit.value = true
  dialogTitle.value = '编辑部门'
  Object.assign(dialogForm, {
    deptId: row.deptId,
    parentId: row.parentId,
    deptName: row.deptName,
    sort: row.sort,
    leader: row.leader,
    phone: row.phone,
    email: row.email,
    status: row.status,
  })
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  const valid = await dialogFormRef.value?.validate().catch(() => false)
  if (!valid) return
  ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
  dialogVisible.value = false
  loadData()
}

/** 删除 */
async function handleDelete(row: DeptRecord) {
  await ElMessageBox.confirm(`确定删除部门「${row.deptName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="dept-manage">
    <!-- 筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="部门名称">
          <el-input v-model="filterForm.deptName" placeholder="请输入部门名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
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
      <el-button @click="toggleExpand">{{ expandAll ? '全部折叠' : '全部展开' }}</el-button>
    </div>

    <!-- 树形表格 -->
    <div class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        row-key="deptId"
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="leader" label="负责人" width="100" align="center" />
        <el-table-column prop="phone" label="联系电话" width="140" align="center" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <button class="table-action-btn table-action-btn--primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </button>
            <button class="table-action-btn table-action-btn--success" @click="handleAddChild(row)">
              <el-icon><Plus /></el-icon>新增
            </button>
            <span class="action-divider"></span>
            <button class="table-action-btn table-action-btn--danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close append-to-body align-center>
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="80px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="dialogForm.parentId"
            :data="parentDeptTree"
            :props="{ children: 'children', label: 'deptName', value: 'deptId' }"
            placeholder="请选择上级部门"
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="dialogForm.deptName" placeholder="请输入部门名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="dialogForm.sort" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="dialogForm.leader" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="dialogForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="dialogForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="dialogForm.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.dept-manage {
  padding: clamp(16px, 2vw, 24px);
  min-height: 100%;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95) 0%, rgba(241, 245, 249, 0.9) 100%);
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
  .dept-manage {
    padding: 12px;
  }

  .filter-card,
  .table-card {
    padding: 12px;
    border-radius: 8px;
  }
}
</style>
