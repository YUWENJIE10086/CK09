<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Key, Lock } from '@element-plus/icons-vue'
import {
  getUserList, addUser, updateUser, deleteUser,
  resetUserPwd, changeUserStatus, updateUserPwd
} from '@/api/user'

/** 用户数据类型 */
interface UserRecord {
  userId: number
  userName: string
  nickName: string
  userType: string
  phone: string
  email: string
  status: string
  createTime: string
  [key: string]: any
}

/** 用户类型映射 */
const userTypeMap: Record<string, string> = {
  '00': '系统管理员',
  '01': '合作社',
  '02': '技术人员',
  '03': '烟农',
  '04': '烟站',
  '05': '县局',
  '06': '市局',
  'F': '烟农',
}
const userTypeOptions = Object.entries(userTypeMap).map(([value, label]) => ({ value, label }))

/** 筛选表单 */
const filterForm = reactive({
  userName: '',
  phone: '',
  status: '' as string,
  userType: '' as string,
})

/** 表格数据 */
const tableData = ref<UserRecord[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
})

/** 新增/编辑对话框 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const dialogForm = reactive({
  userId: 0,
  userName: '',
  nickName: '',
  userType: '03',
  phone: '',
  email: '',
  password: '',
  status: '0',
})
const dialogFormRef = ref()
const isEdit = ref(false)

const dialogRules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度8-64位', trigger: 'blur' },
  ],
}

/** 修改密码对话框 */
const pwdDialogVisible = ref(false)
const pwdForm = reactive({
  userId: 0,
  userName: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const pwdFormRef = ref()

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度8-64位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

/** 加载数据 */
async function loadData() {
  loading.value = true
  try {
    const res: any = await getUserList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      userName: filterForm.userName || undefined,
      phone: filterForm.phone || undefined,
      status: filterForm.status || undefined,
      userType: filterForm.userType || undefined,
    })
    const rows = res?.rows || res?.data?.rows || (Array.isArray(res) ? res : [])
    tableData.value = rows
    total.value = res?.total || rows.length
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
  Object.assign(filterForm, { userName: '', phone: '', status: '', userType: '' })
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
  dialogTitle.value = '新增用户'
  Object.assign(dialogForm, { userId: 0, userName: '', nickName: '', userType: '03', phone: '', email: '', password: '', status: '0' })
  dialogVisible.value = true
}

/** 编辑 */
function handleEdit(row: UserRecord) {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(dialogForm, {
    userId: row.userId,
    userName: row.userName,
    nickName: row.nickName,
    userType: row.userType,
    phone: row.phone || '',
    email: row.email || '',
    password: '',
    status: row.status,
  })
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  const valid = await dialogFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) {
      await updateUser({
        userId: dialogForm.userId,
        nickName: dialogForm.nickName,
        userType: dialogForm.userType,
        phone: dialogForm.phone,
        email: dialogForm.email,
        status: dialogForm.status,
      })
      ElMessage.success('修改成功')
    } else {
      await addUser({
        userName: dialogForm.userName,
        nickName: dialogForm.nickName,
        userType: dialogForm.userType,
        phone: dialogForm.phone,
        email: dialogForm.email,
        password: dialogForm.password,
        status: dialogForm.status,
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

/** 删除 */
async function handleDelete(row: UserRecord) {
  await ElMessageBox.confirm(`确定删除用户「${row.nickName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  try {
    await deleteUser(row.userId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

/** 重置密码 */
async function handleResetPwd(row: UserRecord) {
  const { value } = await ElMessageBox.prompt(`请为用户「${row.nickName}」设置新密码`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    inputType: 'password',
    inputPlaceholder: '请输入8-64位新密码',
    inputValidator: (input) => {
      if (!input || input.length < 8 || input.length > 64) {
        return '密码长度必须为8-64位'
      }
      return true
    },
  })
  try {
    await resetUserPwd(row.userId, value)
    ElMessage.success('密码已重置')
  } catch (e: any) {
    ElMessage.error(e?.message || '重置失败')
  }
}

/** 修改密码 */
function handleChangePwd(row: UserRecord) {
  Object.assign(pwdForm, {
    userId: row.userId,
    userName: row.nickName || row.userName,
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  pwdDialogVisible.value = true
}

/** 提交修改密码 */
async function handleSubmitPwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await updateUserPwd(pwdForm.userId, pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功')
    pwdDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '密码修改失败')
  }
}

/** 状态变更 */
async function handleStatusChange(row: UserRecord) {
  const newStatus = row.status === '0' ? '1' : '0'
  const text = newStatus === '0' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${text}用户「${row.nickName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await changeUserStatus(row.userId, newStatus)
    row.status = newStatus
    ElMessage.success(`${text}成功`)
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><User /></el-icon>
        </div>
        <h2 class="page-title">用户管理</h2>
      </div>
      <p class="page-desc">管理系统用户账号、角色与密码</p>
    </div>

    <!-- 筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="用户名">
          <el-input v-model="filterForm.userName" placeholder="请输入用户名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="filterForm.phone" placeholder="请输入手机号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="filterForm.userType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="opt in userTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
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
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="编号" width="70" align="center" />
        <el-table-column prop="userName" label="用户名" width="160" align="center" />
        <el-table-column prop="nickName" label="昵称" width="120" align="center" />
        <el-table-column prop="userType" label="用户类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.userType === '00' ? 'danger' : row.userType === '03' ? '' : 'info'">
              {{ userTypeMap[row.userType] || row.userType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" align="center" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
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
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <button class="table-action-btn table-action-btn--primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </button>
            <span class="action-divider"></span>
            <button class="table-action-btn table-action-btn--warning" @click="handleChangePwd(row)">
              <el-icon><Lock /></el-icon>改密
            </button>
            <button class="table-action-btn table-action-btn--warning" @click="handleResetPwd(row)">
              <el-icon><Key /></el-icon>重置
            </button>
            <span class="action-divider"></span>
            <button class="table-action-btn table-action-btn--danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close class="stripe-dialog" append-to-body align-center>
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="userName">
              <el-input v-model="dialogForm.userName" placeholder="请输入用户名" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="dialogForm.nickName" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户类型" prop="userType">
              <el-select v-model="dialogForm.userType" placeholder="请选择" style="width: 100%">
                <el-option v-for="opt in userTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="dialogForm.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="dialogForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="dialogForm.status" active-value="0" inactive-value="1" inline-prompt active-text="正常" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="dialogForm.password" type="password" placeholder="请输入8-64位初始密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="460px" destroy-on-close class="stripe-dialog" append-to-body align-center>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="用户">
          <el-input :model-value="pwdForm.userName" disabled />
        </el-form-item>
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码（8-64位）" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmitPwd">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.page-container {
  padding: clamp(16px, 2vw, 24px);
  min-height: 100%;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95) 0%, rgba(241, 245, 249, 0.9) 100%);
}

/* ===== 页面标题区域 ===== */
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

/* ===== 密码提示 ===== */
.pwd-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: clamp(8px, 1vw, 12px) clamp(12px, 1.5vw, 16px);
  background: linear-gradient(135deg, rgba(254, 243, 199, 0.9) 0%, rgba(254, 243, 199, 0.8) 100%);
  backdrop-filter: blur(8px);
  border-radius: 8px;
  font-size: clamp(12px, 1.3vw, 14px);
  color: #92400E;
  margin-top: clamp(8px, 1vw, 12px);
  border: 1px solid rgba(251, 191, 36, 0.3);
}

/* ===== 对话框样式 ===== */
.stripe-dialog :deep(.el-dialog) {
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.stripe-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: clamp(16px, 2vw, 24px) clamp(20px, 2.5vw, 28px) !important;
}

.stripe-dialog :deep(.el-dialog__body) {
  padding: clamp(16px, 2vw, 24px) clamp(20px, 2.5vw, 28px) !important;
}

.stripe-dialog :deep(.el-dialog__footer) {
  padding: clamp(12px, 1.5vw, 16px) clamp(20px, 2.5vw, 28px) !important;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
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

/* ===== 响应式调整 ===== */
@media (max-width: 768px) {
  .page-container {
    padding: 12px;
  }

  .page-title-icon {
    width: 36px;
    height: 36px;
  }

  .filter-card,
  .table-card {
    padding: 12px;
    border-radius: 8px;
  }
}
</style>
