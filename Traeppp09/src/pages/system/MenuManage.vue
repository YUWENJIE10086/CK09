<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

/** 菜单类型 */
type MenuType = 'M' | 'C' | 'F' // M目录 C菜单 F按钮

/** 菜单数据类型 */
interface MenuRecord {
  menuId: number
  menuName: string
  parentId: number
  icon: string
  sort: number
  path: string
  component: string
  perms: string
  menuType: MenuType
  isFrame: '0' | '1' // 0否 1是
  isCache: '0' | '1' // 0缓存 1不缓存
  visible: '0' | '1' // 0显示 1隐藏
  status: '0' | '1' // 0正常 1停用
  children?: MenuRecord[]
}

/** 图标选项 */
const iconOptions = [
  { label: '系统管理', value: 'system' },
  { label: '用户', value: 'user' },
  { label: '角色', value: 'peoples' },
  { label: '菜单', value: 'tree-table' },
  { label: '部门', value: 'tree' },
  { label: '字典', value: 'dict' },
  { label: '日志', value: 'log' },
  { label: '烤房', value: 'house' },
  { label: '维修', value: 'tool' },
  { label: '预约', value: 'date' },
  { label: '评价', value: 'star' },
  { label: '仪表盘', value: 'dashboard' },
  { label: '无图标', value: '' },
]

/** Mock菜单树数据 */
const mockMenuList: MenuRecord[] = [
  {
    menuId: 1, menuName: '烤房管理', parentId: 0, icon: 'house', sort: 1, path: 'barn', component: '', perms: '', menuType: 'M', isFrame: '1', isCache: '0', visible: '0', status: '0',
    children: [
      { menuId: 11, menuName: '烤房列表', parentId: 1, icon: '', sort: 1, path: 'list', component: 'barn/BarnList', perms: 'barn:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0',
        children: [
          { menuId: 111, menuName: '烤房查询', parentId: 11, icon: '', sort: 1, path: '', component: '', perms: 'barn:query', menuType: 'F', isFrame: '1', isCache: '0', visible: '0', status: '0' },
          { menuId: 112, menuName: '烤房新增', parentId: 11, icon: '', sort: 2, path: '', component: '', perms: 'barn:add', menuType: 'F', isFrame: '1', isCache: '0', visible: '0', status: '0' },
          { menuId: 113, menuName: '烤房修改', parentId: 11, icon: '', sort: 3, path: '', component: '', perms: 'barn:edit', menuType: 'F', isFrame: '1', isCache: '0', visible: '0', status: '0' },
          { menuId: 114, menuName: '烤房删除', parentId: 11, icon: '', sort: 4, path: '', component: '', perms: 'barn:remove', menuType: 'F', isFrame: '1', isCache: '0', visible: '0', status: '0' },
        ]
      },
      { menuId: 12, menuName: '新建烤房', parentId: 1, icon: '', sort: 2, path: 'create', component: 'barn/BarnAdd', perms: 'barn:add', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
    ]
  },
  {
    menuId: 2, menuName: '维修管理', parentId: 0, icon: 'tool', sort: 2, path: 'repair', component: '', perms: '', menuType: 'M', isFrame: '1', isCache: '0', visible: '0', status: '0',
    children: [
      { menuId: 21, menuName: '维修列表', parentId: 2, icon: '', sort: 1, path: 'list', component: 'repair/RepairList', perms: 'repair:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 22, menuName: '维修提报', parentId: 2, icon: '', sort: 2, path: 'submit', component: 'repair/RepairApply', perms: 'repair:add', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 23, menuName: '维修审批', parentId: 2, icon: '', sort: 3, path: 'approve/:id', component: 'repair/RepairApprove', perms: 'repair:approve', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 24, menuName: '维修验收', parentId: 2, icon: '', sort: 4, path: 'accept/:id', component: 'repair/RepairAccept', perms: 'repair:accept', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 25, menuName: '维修资金', parentId: 2, icon: '', sort: 5, path: 'fund', component: 'repair/RepairFund', perms: 'repair:fund', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
    ]
  },
  {
    menuId: 3, menuName: '预约管理', parentId: 0, icon: 'date', sort: 3, path: 'reservation', component: '', perms: '', menuType: 'M', isFrame: '1', isCache: '0', visible: '0', status: '0',
    children: [
      { menuId: 31, menuName: '预约审核', parentId: 3, icon: '', sort: 1, path: 'audit', component: 'reservation/ReservationList', perms: 'reservation:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 32, menuName: '预约日历', parentId: 3, icon: '', sort: 2, path: 'calendar', component: 'reservation/ReservationCalendar', perms: 'reservation:calendar', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
    ]
  },
  {
    menuId: 4, menuName: '评价管理', parentId: 0, icon: 'star', sort: 4, path: 'evaluate', component: '', perms: '', menuType: 'M', isFrame: '1', isCache: '0', visible: '0', status: '0',
    children: [
      { menuId: 41, menuName: '评价列表', parentId: 4, icon: '', sort: 1, path: 'list', component: 'evaluation/EvaluationList', perms: 'evaluate:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 42, menuName: '评价分析', parentId: 4, icon: '', sort: 2, path: 'analysis', component: 'evaluation/EvaluationAnalysis', perms: 'evaluate:analysis', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
    ]
  },
  {
    menuId: 5, menuName: '系统管理', parentId: 0, icon: 'system', sort: 5, path: 'system', component: '', perms: '', menuType: 'M', isFrame: '1', isCache: '0', visible: '0', status: '0',
    children: [
      { menuId: 51, menuName: '用户管理', parentId: 5, icon: 'user', sort: 1, path: 'user', component: 'system/UserManage', perms: 'system:user:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 52, menuName: '角色管理', parentId: 5, icon: 'peoples', sort: 2, path: 'role', component: 'system/RoleManage', perms: 'system:role:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 53, menuName: '菜单管理', parentId: 5, icon: 'tree-table', sort: 3, path: 'menu', component: 'system/MenuManage', perms: 'system:menu:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 54, menuName: '部门管理', parentId: 5, icon: 'tree', sort: 4, path: 'dept', component: 'system/DeptManage', perms: 'system:dept:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 55, menuName: '字典管理', parentId: 5, icon: 'dict', sort: 5, path: 'dict', component: 'system/DictManage', perms: 'system:dict:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
      { menuId: 56, menuName: '操作日志', parentId: 5, icon: 'log', sort: 6, path: 'log', component: 'system/LogManage', perms: 'system:log:list', menuType: 'C', isFrame: '1', isCache: '0', visible: '0', status: '0' },
    ]
  },
]

/** 表格数据（树形） */
const tableData = ref<MenuRecord[]>([])
const loading = ref(false)
const expandAll = ref(true)

/** 对话框状态 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const dialogForm = reactive({
  menuId: 0,
  parentId: 0,
  menuName: '',
  menuType: 'M' as MenuType,
  icon: '',
  sort: 0,
  path: '',
  component: '',
  perms: '',
  isFrame: '1',
  isCache: '0',
  visible: '0',
  status: '0',
})
const dialogFormRef = ref()
const isEdit = ref(false)

/** 上级菜单树（包含顶级选项） */
const parentMenuTree = computed(() => {
  return [{ menuId: 0, menuName: '顶级菜单', children: mockMenuList }]
})

import { computed } from 'vue'

/** 表单校验规则 */
const dialogRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }],
}

/** 菜单类型标签颜色 */
function menuTypeTag(type: MenuType) {
  const map: Record<MenuType, { label: string; type: '' | 'success' | 'warning' }> = {
    M: { label: '目录', type: '' },
    C: { label: '菜单', type: 'success' },
    F: { label: '按钮', type: 'warning' },
  }
  return map[type]
}

/** 加载数据 */
function loadData() {
  loading.value = true
  setTimeout(() => {
    tableData.value = mockMenuList
    loading.value = false
  }, 300)
}

/** 展开/折叠切换 */
function toggleExpand() {
  expandAll.value = !expandAll.value
  // 重新加载数据以触发展开状态变化
  const data = [...tableData.value]
  tableData.value = []
  setTimeout(() => { tableData.value = data })
}

/** 新增顶级菜单 */
function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增菜单'
  Object.assign(dialogForm, {
    menuId: 0, parentId: 0, menuName: '', menuType: 'M', icon: '', sort: 0, path: '', component: '', perms: '', isFrame: '1', isCache: '0', visible: '0', status: '0',
  })
  dialogVisible.value = true
}

/** 新增子菜单 */
function handleAddChild(row: MenuRecord) {
  isEdit.value = false
  dialogTitle.value = '新增子菜单'
  Object.assign(dialogForm, {
    menuId: 0, parentId: row.menuId, menuName: '', menuType: 'C', icon: '', sort: 0, path: '', component: '', perms: '', isFrame: '1', isCache: '0', visible: '0', status: '0',
  })
  dialogVisible.value = true
}

/** 编辑 */
function handleEdit(row: MenuRecord) {
  isEdit.value = true
  dialogTitle.value = '编辑菜单'
  Object.assign(dialogForm, {
    menuId: row.menuId,
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    icon: row.icon,
    sort: row.sort,
    path: row.path,
    component: row.component,
    perms: row.perms,
    isFrame: row.isFrame,
    isCache: row.isCache,
    visible: row.visible,
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
async function handleDelete(row: MenuRecord) {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '提示', {
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
  <div class="menu-manage">
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增顶级菜单</el-button>
      <el-button @click="toggleExpand">{{ expandAll ? '全部折叠' : '全部展开' }}</el-button>
    </div>

    <!-- 树形表格 -->
    <div class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        row-key="menuId"
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="icon" label="图标" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.icon">{{ row.icon }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="perms" label="权限标识" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.perms || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="component" label="组件路径" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.component || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.menuType).type" size="small">{{ menuTypeTag(row.menuType).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <button class="table-action-btn table-action-btn--primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </button>
            <button v-if="row.menuType !== 'F'" class="table-action-btn table-action-btn--success" @click="handleAddChild(row)">
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close append-to-body align-center>
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="dialogForm.parentId"
            :data="parentMenuTree"
            :props="{ children: 'children', label: 'menuName', value: 'menuId' }"
            placeholder="请选择上级菜单"
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="dialogForm.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="dialogForm.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标">
              <el-select v-model="dialogForm.icon" placeholder="请选择图标" clearable style="width: 100%">
                <el-option v-for="opt in iconOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="dialogForm.sort" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路由地址" v-if="dialogForm.menuType !== 'F'">
              <el-input v-model="dialogForm.path" placeholder="请输入路由地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="dialogForm.menuType === 'C'">
          <el-col :span="12">
            <el-form-item label="组件路径">
              <el-input v-model="dialogForm.component" placeholder="请输入组件路径" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限标识">
              <el-input v-model="dialogForm.perms" placeholder="请输入权限标识" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="dialogForm.menuType === 'F'">
          <el-col :span="12">
            <el-form-item label="权限标识">
              <el-input v-model="dialogForm.perms" placeholder="请输入权限标识" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="dialogForm.menuType !== 'F'">
          <el-col :span="12">
            <el-form-item label="是否外链">
              <el-radio-group v-model="dialogForm.isFrame">
                <el-radio value="0">是</el-radio>
                <el-radio value="1">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否缓存">
              <el-radio-group v-model="dialogForm.isCache">
                <el-radio value="0">缓存</el-radio>
                <el-radio value="1">不缓存</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="dialogForm.menuType !== 'F'">
          <el-col :span="12">
            <el-form-item label="显示状态">
              <el-radio-group v-model="dialogForm.visible">
                <el-radio value="0">显示</el-radio>
                <el-radio value="1">隐藏</el-radio>
              </el-radio-group>
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
.menu-manage {
  padding: clamp(16px, 2vw, 24px);
  min-height: 100%;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95) 0%, rgba(241, 245, 249, 0.9) 100%);
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
  .menu-manage {
    padding: 12px;
  }

  .table-card {
    padding: 12px;
    border-radius: 8px;
  }
}
</style>
