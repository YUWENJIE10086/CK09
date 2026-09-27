<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Connection, Check, Close, Plus, Edit, Delete } from '@element-plus/icons-vue'

/** 数据库配置数据类型 */
interface DatabaseConfig {
  id: number
  name: string
  dbType: string
  host: string
  port: number
  dbName: string
  username: string
  password: string
  connectionPool: string
  maxActive: number
  maxWait: number
  minIdle: number
  initialSize: number
  charset: string
  timezone: string
  sslEnabled: boolean
  remark: string
  status: '0' | '1' // 0正常 1停用
  isDefault: boolean
  createTime: string
  updateTime: string
}

/** 表单数据类型 */
interface DatabaseForm {
  name: string
  dbType: string
  host: string
  port: number
  dbName: string
  username: string
  password: string
  connectionPool: string
  maxActive: number
  maxWait: number
  minIdle: number
  initialSize: number
  charset: string
  timezone: string
  sslEnabled: boolean
  remark: string
  status: '0' | '1'
  isDefault: boolean
}

/** 数据库类型选项 */
const dbTypeOptions = [
  { label: 'MySQL', value: 'mysql' },
  { label: 'PostgreSQL', value: 'postgresql' },
  { label: 'Oracle', value: 'oracle' },
  { label: 'SQL Server', value: 'sqlserver' },
  { label: 'DM（达梦）', value: 'dm' },
  { label: 'KingbaseES（人大金仓）', value: 'kingbase' },
]

/** 连接池类型选项 */
const poolTypeOptions = [
  { label: 'Druid', value: 'druid' },
  { label: 'HikariCP', value: 'hikari' },
  { label: 'DBCP2', value: 'dbcp2' },
]

/** 字符集选项 */
const charsetOptions = [
  { label: 'utf8mb4', value: 'utf8mb4' },
  { label: 'utf8', value: 'utf8' },
  { label: 'gbk', value: 'gbk' },
  { label: 'latin1', value: 'latin1' },
]

/** 时区选项 */
const timezoneOptions = [
  { label: 'Asia/Shanghai (东八区)', value: 'Asia/Shanghai' },
  { label: 'UTC (标准时间)', value: 'UTC' },
  { label: 'America/New_York', value: 'America/New_York' },
  { label: 'Europe/London', value: 'Europe/London' },
]

/** Mock数据库配置数据 */
const mockConfigs: DatabaseConfig[] = [
  {
    id: 1,
    name: '主数据库（生产环境）',
    dbType: 'mysql',
    host: '192.168.1.200',
    port: 3306,
    dbName: 'ry-vue',
    username: 'root',
    password: '********',
    connectionPool: 'druid',
    maxActive: 20,
    maxWait: 60000,
    minIdle: 5,
    initialSize: 5,
    charset: 'utf8mb4',
    timezone: 'Asia/Shanghai',
    sslEnabled: false,
    remark: 'RuoYi主数据库，烤房管理系统核心业务数据',
    status: '0',
    isDefault: true,
    createTime: '2026-01-15 09:00:00',
    updateTime: '2026-06-01 14:30:00',
  },
  {
    id: 2,
    name: '从数据库（只读副本）',
    dbType: 'mysql',
    host: '192.168.1.201',
    port: 3306,
    dbName: 'ry-vue',
    username: 'readonly',
    password: '********',
    connectionPool: 'druid',
    maxActive: 10,
    maxWait: 60000,
    minIdle: 3,
    initialSize: 3,
    charset: 'utf8mb4',
    timezone: 'Asia/Shanghai',
    sslEnabled: false,
    remark: '只读副本，用于报表查询和BI分析',
    status: '0',
    isDefault: false,
    createTime: '2026-02-20 10:00:00',
    updateTime: '2026-05-15 11:20:00',
  },
  {
    id: 3,
    name: '测试数据库',
    dbType: 'mysql',
    host: '192.168.1.100',
    port: 3306,
    dbName: 'ry-vue-test',
    username: 'test',
    password: '********',
    connectionPool: 'hikari',
    maxActive: 5,
    maxWait: 30000,
    minIdle: 2,
    initialSize: 2,
    charset: 'utf8mb4',
    timezone: 'Asia/Shanghai',
    sslEnabled: false,
    remark: '开发测试环境数据库',
    status: '1',
    isDefault: false,
    createTime: '2026-03-10 08:30:00',
    updateTime: '2026-03-10 08:30:00',
  },
]

/** 数据库配置列表 */
const configList = ref<DatabaseConfig[]>([...mockConfigs])

/** 筛选条件 */
const queryParams = reactive({
  name: '',
  dbType: '',
  status: '',
})

/** 当前激活的Tab */
const activeTab = ref('list')

/** 对话框显示控制 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增数据库配置')

/** 是否编辑模式 */
const isEdit = ref(false)
const editId = ref(0)

/** 表单数据 */
const form = reactive<DatabaseForm>({
  name: '',
  dbType: 'mysql',
  host: '',
  port: 3306,
  dbName: '',
  username: '',
  password: '',
  connectionPool: 'druid',
  maxActive: 20,
  maxWait: 60000,
  minIdle: 5,
  initialSize: 5,
  charset: 'utf8mb4',
  timezone: 'Asia/Shanghai',
  sslEnabled: false,
  remark: '',
  status: '0',
  isDefault: false,
})

/** 表单校验规则 */
const formRules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口号', trigger: 'blur' }],
  dbName: [{ required: true, message: '请输入数据库名称', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  connectionPool: [{ required: true, message: '请选择连接池类型', trigger: 'change' }],
}

const formRef = ref()

/** 测试连接状态 */
const testLoading = ref(false)
const testResult = ref<{ success: boolean; message: string } | null>(null)

/** 筛选后的列表 */
const filteredList = computed(() => {
  return configList.value.filter(item => {
    if (queryParams.name && !item.name.includes(queryParams.name)) return false
    if (queryParams.dbType && item.dbType !== queryParams.dbType) return false
    if (queryParams.status && item.status !== queryParams.status) return false
    return true
  })
})

/** 重置筛选 */
const handleReset = () => {
  queryParams.name = ''
  queryParams.dbType = ''
  queryParams.status = ''
}

/** 新增配置 */
const handleAdd = () => {
  isEdit.value = false
  editId.value = 0
  dialogTitle.value = '新增数据库配置'
  resetForm()
  dialogVisible.value = true
}

/** 编辑配置 */
const handleEdit = (row: DatabaseConfig) => {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑数据库配置'
  Object.assign(form, {
    name: row.name,
    dbType: row.dbType,
    host: row.host,
    port: row.port,
    dbName: row.dbName,
    username: row.username,
    password: '',
    connectionPool: row.connectionPool,
    maxActive: row.maxActive,
    maxWait: row.maxWait,
    minIdle: row.minIdle,
    initialSize: row.initialSize,
    charset: row.charset,
    timezone: row.timezone,
    sslEnabled: row.sslEnabled,
    remark: row.remark,
    status: row.status,
    isDefault: row.isDefault,
  })
  testResult.value = null
  dialogVisible.value = true
}

/** 删除配置 */
const handleDelete = (row: DatabaseConfig) => {
  if (row.isDefault) {
    ElMessage.warning('默认数据库配置不可删除')
    return
  }
  ElMessageBox.confirm(`确认删除数据库配置"${row.name}"？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    configList.value = configList.value.filter(item => item.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 重置表单 */
const resetForm = () => {
  Object.assign(form, {
    name: '',
    dbType: 'mysql',
    host: '',
    port: 3306,
    dbName: '',
    username: '',
    password: '',
    connectionPool: 'druid',
    maxActive: 20,
    maxWait: 60000,
    minIdle: 5,
    initialSize: 5,
    charset: 'utf8mb4',
    timezone: 'Asia/Shanghai',
    sslEnabled: false,
    remark: '',
    status: '0',
    isDefault: false,
  })
  testResult.value = null
}

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (isEdit.value) {
    const idx = configList.value.findIndex(item => item.id === editId.value)
    if (idx !== -1) {
      configList.value[idx] = {
        ...configList.value[idx],
        ...form,
        password: form.password || configList.value[idx].password,
        updateTime: new Date().toLocaleString(),
      }
    }
    ElMessage.success('修改成功')
  } else {
    configList.value.push({
      id: Date.now(),
      ...form,
      password: '********',
      createTime: new Date().toLocaleString(),
      updateTime: new Date().toLocaleString(),
    })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
}

/** 测试连接 */
const handleTestConnection = async () => {
  testLoading.value = true
  testResult.value = null
  // 模拟测试连接
  await new Promise(resolve => setTimeout(resolve, 1500))
  // 根据表单填写情况模拟结果
  if (form.host && form.port && form.dbName && form.username) {
    testResult.value = { success: true, message: '连接成功！数据库版本：MySQL 8.0.33，连接耗时：23ms' }
  } else {
    testResult.value = { success: false, message: '连接失败：请填写完整的连接信息' }
  }
  testLoading.value = false
}

/** 设为默认 */
const handleSetDefault = (row: DatabaseConfig) => {
  configList.value.forEach(item => {
    item.isDefault = item.id === row.id
  })
  ElMessage.success(`已将"${row.name}"设为默认数据库`)
}

/** 复制JDBC URL */
const copyJdbcUrl = () => {
  if (jdbcUrl.value) {
    navigator.clipboard.writeText(jdbcUrl.value)
    ElMessage.success('已复制到剪贴板')
  }
}

/** 数据库类型标签颜色 */
const dbTypeColor = (type: string) => {
  const map: Record<string, string> = {
    mysql: '#00758f',
    postgresql: '#336791',
    oracle: '#f80000',
    sqlserver: '#cc2927',
    dm: '#e60012',
    kingbase: '#003366',
  }
  return map[type] || '#909399'
}

/** 获取数据库类型标签 */
const dbTypeLabel = (type: string) => {
  const found = dbTypeOptions.find(o => o.value === type)
  return found ? found.label : type
}

/** JDBC URL预览 */
const jdbcUrl = computed(() => {
  if (!form.dbType || !form.host || !form.port || !form.dbName) return ''
  const params: string[] = []
  if (form.charset) params.push(`characterEncoding=${form.charset}`)
  if (form.timezone) params.push(`serverTimezone=${form.timezone}`)
  if (form.sslEnabled) params.push('useSSL=true')
  else params.push('useSSL=false')
  params.push('allowPublicKeyRetrieval=true')

  switch (form.dbType) {
    case 'mysql':
      return `jdbc:mysql://${form.host}:${form.port}/${form.dbName}?${params.join('&')}`
    case 'postgresql':
      return `jdbc:postgresql://${form.host}:${form.port}/${form.dbName}`
    case 'oracle':
      return `jdbc:oracle:thin:@${form.host}:${form.port}:${form.dbName}`
    case 'sqlserver':
      return `jdbc:sqlserver://${form.host}:${form.port};DatabaseName=${form.dbName}`
    case 'dm':
      return `jdbc:dm://${form.host}:${form.port}/${form.dbName}`
    case 'kingbase':
      return `jdbc:kingbase8://${form.host}:${form.port}/${form.dbName}`
    default:
      return ''
  }
})
</script>

<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 数据库配置列表 -->
      <el-tab-pane label="数据库配置" name="list">
        <!-- 筛选区 -->
        <el-form :model="queryParams" inline class="filter-container" style="padding-bottom: 16px;">
          <el-form-item label="配置名称">
            <el-input v-model="queryParams.name" placeholder="请输入配置名称" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="数据库类型">
            <el-select v-model="queryParams.dbType" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="item in dbTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="正常" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 操作栏 -->
        <div style="margin-bottom: 12px;">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增配置</el-button>
        </div>

        <!-- 配置列表 -->
        <el-table :data="filteredList" border stripe>
          <el-table-column label="配置名称" prop="name" min-width="180" show-overflow-tooltip />
          <el-table-column label="数据库类型" prop="dbType" width="140" align="center">
            <template #default="{ row }">
              <el-tag :color="dbTypeColor(row.dbType)" effect="dark" size="small" style="border: none;">
                {{ dbTypeLabel(row.dbType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="主机地址" min-width="160">
            <template #default="{ row }">
              {{ row.host }}:{{ row.port }}
            </template>
          </el-table-column>
          <el-table-column label="数据库名" prop="dbName" min-width="130" show-overflow-tooltip />
          <el-table-column label="用户名" prop="username" width="120" />
          <el-table-column label="连接池" prop="connectionPool" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.connectionPool.toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="默认" prop="isDefault" width="70" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isDefault" type="success" size="small">默认</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                {{ row.status === '0' ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" prop="updateTime" width="170" />
          <el-table-column label="操作" width="240" fixed="right" align="center">
            <template #default="{ row }">
              <button class="table-action-btn table-action-btn--primary" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </button>
              <button class="table-action-btn table-action-btn--success" @click="handleTestConnection">
                <el-icon><Connection /></el-icon>测试
              </button>
              <button v-if="!row.isDefault" class="table-action-btn table-action-btn--warning" @click="handleSetDefault(row)">
                <el-icon><Check /></el-icon>设为默认
              </button>
              <span class="action-divider"></span>
              <button class="table-action-btn table-action-btn--danger" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 连接参数说明 -->
      <el-tab-pane label="连接参数说明" name="params">
        <el-descriptions title="RuoYi 数据库配置参数说明" :column="1" border>
          <el-descriptions-item label="spring.datasource.druid.master.url">
            JDBC连接URL，格式：jdbc:mysql://主机:端口/数据库名?参数
          </el-descriptions-item>
          <el-descriptions-item label="spring.datasource.druid.master.username">
            数据库用户名，建议使用专用账号，避免使用root
          </el-descriptions-item>
          <el-descriptions-item label="spring.datasource.druid.master.password">
            数据库密码，生产环境建议加密存储
          </el-descriptions-item>
          <el-descriptions-item label="spring.datasource.druid.master.driver-class-name">
            数据库驱动类名，MySQL8: com.mysql.cj.jdbc.Driver
          </el-descriptions-item>
          <el-descriptions-item label="spring.datasource.druid.initial-size">
            初始化连接数，建议5，根据并发量调整
          </el-descriptions-item>
          <el-descriptions-item label="spring.datasource.druid.min-idle">
            最小空闲连接数，建议5，保持最低可用连接
          </el-descriptions-item>
          <el-descriptions-item label="spring.datasource.druid.max-active">
            最大活跃连接数，建议20，高并发场景可调至50-100
          </el-descriptions-item>
          <el-descriptions-item label="spring.datasource.druid.max-wait">
            获取连接最大等待时间(ms)，建议60000(60秒)
          </el-descriptions-item>
          <el-descriptions-item label="characterEncoding">
            字符编码，推荐utf8mb4，支持emoji和特殊字符
          </el-descriptions-item>
          <el-descriptions-item label="serverTimezone">
            服务器时区，国内推荐Asia/Shanghai
          </el-descriptions-item>
          <el-descriptions-item label="useSSL">
            SSL加密连接，生产环境建议true，开发环境可false
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <el-descriptions title="RuoYi application-druid.yml 配置示例" :column="1" border>
          <el-descriptions-item label="配置文件路径">
            src/main/resources/application-druid.yml
          </el-descriptions-item>
        </el-descriptions>

        <div style="background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 6px; font-family: Consolas, monospace; font-size: 13px; line-height: 1.6; margin-top: 12px; overflow-x: auto;">
          <pre style="margin:0;"># 主数据源
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    druid:
      master:
        url: jdbc:mysql://192.168.1.200:3306/ry-vue?useUnicode=true&amp;characterEncoding=utf8mb4&amp;serverTimezone=Asia/Shanghai&amp;useSSL=false&amp;allowPublicKeyRetrieval=true
        username: root
        password: your_password
      # 从数据源（读写分离）
      slave:
        enabled: true
        url: jdbc:mysql://192.168.1.201:3306/ry-vue?useUnicode=true&amp;characterEncoding=utf8mb4&amp;serverTimezone=Asia/Shanghai&amp;useSSL=false
        username: readonly
        password: your_password
      # 连接池配置
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false</pre>
        </div>
      </el-tab-pane>

      <!-- 数据库初始化 -->
      <el-tab-pane label="数据库初始化" name="init">
        <el-alert
          title="数据库初始化说明"
          description="首次部署系统时，需要执行以下SQL脚本初始化数据库。请按顺序执行，确保数据完整性。"
          type="info"
          show-icon
          :closable="false"
          style="margin-bottom: 20px;"
        />

        <el-timeline>
          <el-timeline-item timestamp="步骤1" placement="top" color="#1A3C6E">
            <el-card>
              <template #header>
                <span style="font-weight: 600;">创建数据库</span>
              </template>
              <div style="background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 4px; font-family: Consolas, monospace; font-size: 13px; white-space: pre-wrap;">CREATE DATABASE IF NOT EXISTS &#96;ry-vue&#96; DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE &#96;ry-vue&#96;;</div>
            </el-card>
          </el-timeline-item>

          <el-timeline-item timestamp="步骤2" placement="top" color="#1A3C6E">
            <el-card>
              <template #header>
                <span style="font-weight: 600;">执行RuoYi基础表结构脚本</span>
              </template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="脚本文件">sql/ry_20xxxxxxxx.sql</el-descriptions-item>
                <el-descriptions-item label="说明">包含系统管理、权限、字典等基础表</el-descriptions-item>
                <el-descriptions-item label="核心表">sys_user, sys_role, sys_menu, sys_dept, sys_dict_type, sys_dict_data, sys_oper_log</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-timeline-item>

          <el-timeline-item timestamp="步骤3" placement="top" color="#2ECC71">
            <el-card>
              <template #header>
                <span style="font-weight: 600;">执行烤房业务表结构脚本</span>
              </template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="脚本文件">merge_sql/init.sql</el-descriptions-item>
                <el-descriptions-item label="说明">包含烤房管理、维修、预约、评价等业务表</el-descriptions-item>
                <el-descriptions-item label="核心表">t_barn_info, t_barn_component, t_repair_record, t_reservation, t_evaluation, t_fund_management, t_maintenance_team, t_user_ext</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-timeline-item>

          <el-timeline-item timestamp="步骤4" placement="top" color="#F39C12">
            <el-card>
              <template #header>
                <span style="font-weight: 600;">执行初始数据脚本</span>
              </template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="脚本文件">sql/barn_init_data.sql</el-descriptions-item>
                <el-descriptions-item label="说明">包含字典数据、角色权限、菜单、初始管理员账号等</el-descriptions-item>
                <el-descriptions-item label="管理员密码">由部署管理员单独设置</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-timeline-item>

          <el-timeline-item timestamp="步骤5" placement="top" color="#E74C3C">
            <el-card>
              <template #header>
                <span style="font-weight: 600;">验证数据库</span>
              </template>
              <el-button type="primary" :icon="Connection" @click="handleTestConnection" :loading="testLoading">
                测试数据库连接
              </el-button>
              <div v-if="testResult" style="margin-top: 12px;">
                <el-alert
                  :title="testResult.message"
                  :type="testResult.success ? 'success' : 'error'"
                  :closable="false"
                  show-icon
                />
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close append-to-body align-center>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <!-- 基本信息 -->
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="配置名称" prop="name">
              <el-input v-model="form.name" placeholder="如：主数据库（生产环境）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据库类型" prop="dbType">
              <el-select v-model="form.dbType" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in dbTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 连接信息 -->
        <el-divider content-position="left">连接信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="主机地址" prop="host">
              <el-input v-model="form.host" placeholder="如：192.168.1.200 或 db.example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="端口号" prop="port">
              <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据库名称" prop="dbName">
              <el-input v-model="form.dbName" placeholder="如：ry-vue" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="连接池类型" prop="connectionPool">
              <el-select v-model="form.connectionPool" style="width: 100%">
                <el-option v-for="item in poolTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="数据库登录用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '不修改请留空' : '请输入密码'" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- JDBC URL预览 -->
        <el-form-item label="JDBC URL">
          <el-input :model-value="jdbcUrl" readonly>
            <template #append>
              <el-button @click="copyJdbcUrl">复制</el-button>
            </template>
          </el-input>
        </el-form-item>

        <!-- 字符与时区 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="字符编码">
              <el-select v-model="form.charset" style="width: 100%">
                <el-option v-for="item in charsetOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="服务器时区">
              <el-select v-model="form.timezone" style="width: 100%">
                <el-option v-for="item in timezoneOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="SSL加密">
              <el-switch v-model="form.sslEnabled" active-text="开启" inactive-text="关闭" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 连接池参数 -->
        <el-divider content-position="left">连接池参数</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="初始连接数">
              <el-input-number v-model="form.initialSize" :min="1" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最小空闲连接">
              <el-input-number v-model="form.minIdle" :min="1" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大活跃连接">
              <el-input-number v-model="form.maxActive" :min="1" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大等待(ms)">
              <el-input-number v-model="form.maxWait" :min="1000" :max="300000" :step="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 其他 -->
        <el-divider content-position="left">其他设置</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="设为默认">
              <el-switch v-model="form.isDefault" active-text="是" inactive-text="否" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>

      <!-- 测试连接结果 -->
      <div v-if="testResult" style="margin: 0 0 16px 120px;">
        <el-alert
          :title="testResult.message"
          :type="testResult.success ? 'success' : 'error'"
          :closable="true"
          show-icon
          @close="testResult = null"
        />
      </div>

      <template #footer>
        <el-button @click="handleTestConnection" :loading="testLoading" :icon="Connection">
          测试连接
        </el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
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

/* ===== Tab样式 ===== */
:deep(.el-tabs--border-card) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06), inset 0 1px 1px rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

:deep(.el-tabs__header) {
  background: linear-gradient(135deg, rgba(249, 250, 251, 0.9) 0%, rgba(243, 244, 246, 0.85) 100%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

:deep(.el-tabs__item) {
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.el-tabs__item.is-active) {
  color: #0A4D3E;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
}

:deep(.el-tabs__item:hover) {
  color: #0D5D46;
}

:deep(.el-tabs__content) {
  padding: clamp(16px, 2vw, 24px);
}

/* ===== 筛选容器 ===== */
.filter-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.6) 0%, rgba(255, 255, 255, 0.5) 100%);
  backdrop-filter: blur(8px);
  border-radius: 8px;
  padding: clamp(12px, 1.5vw, 16px);
  margin-bottom: clamp(12px, 1.5vw, 16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

/* ===== 轻拟态按钮 ===== */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.25), 0 1px 2px rgba(255, 255, 255, 0.5) inset;
  transition: all 0.2s ease;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #126B52 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.35), 0 1px 2px rgba(255, 255, 255, 0.5) inset;
  transform: translateY(-2px);
}

:deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(10, 77, 62, 0.2), inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

:deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(145deg, #ffffff 0%, #f7f8f9 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(255, 255, 255, 0.8) inset;
  transition: all 0.2s ease;
}

:deep(.el-button:not(.el-button--primary):hover) {
  background: linear-gradient(145deg, #f7f8f9 0%, #ffffff 100%);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(255, 255, 255, 0.8) inset;
  transform: translateY(-2px);
}

:deep(.el-button:not(.el-button--primary):active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08), inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* ===== 表格样式 ===== */
:deep(.el-table) {
  background: transparent;
}

:deep(.el-table th.el-table__cell) {
  background: linear-gradient(135deg, rgba(249, 250, 251, 0.9) 0%, rgba(243, 244, 246, 0.85) 100%);
}

/* ===== 描述列表样式 ===== */
:deep(.el-descriptions) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.6) 0%, rgba(255, 255, 255, 0.5) 100%);
  backdrop-filter: blur(8px);
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-descriptions__label) {
  width: clamp(200px, 25vw, 280px);
  font-weight: 500;
  background: linear-gradient(135deg, rgba(249, 250, 251, 0.95) 0%, rgba(243, 244, 246, 0.9) 100%);
}

/* ===== 时间线样式 ===== */
:deep(.el-timeline-item__timestamp) {
  font-weight: 600;
  color: #0A4D3E;
}

:deep(.el-timeline-item__tail) {
  border-left-color: rgba(10, 77, 62, 0.2);
}

:deep(.el-card) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

:deep(.el-card:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.1);
}

/* ===== Alert样式 ===== */
:deep(.el-alert) {
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.9) 0%, rgba(219, 234, 254, 0.85) 100%);
  backdrop-filter: blur(8px);
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.2);
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

:deep(.el-dialog__body) {
  max-height: 65vh;
  overflow-y: auto;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #0A4D3E;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
}

/* ===== 代码块样式 ===== */
pre {
  background: linear-gradient(135deg, rgba(30, 30, 30, 0.98) 0%, rgba(40, 40, 40, 0.95) 100%);
  backdrop-filter: blur(4px);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* ===== 响应式调整 ===== */
@media (max-width: 768px) {
  .page-container {
    padding: 12px;
  }

  :deep(.el-tabs--border-card) {
    border-radius: 8px;
  }

  :deep(.el-tabs__content) {
    padding: 12px;
  }

  :deep(.el-descriptions__label) {
    width: 140px;
  }
}
</style>
