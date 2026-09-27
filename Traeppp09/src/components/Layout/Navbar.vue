<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

/** 面包屑列表 */
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title)
  return matched.map(item => ({
    title: item.meta.title as string,
    path: item.path,
  }))
})

/** 当前用户信息 */
const userName = computed(() => userStore.userInfo.nickName || userStore.userInfo.userName || '用户')

/** 切换侧边栏 */
const toggleSidebar = () => {
  appStore.toggleSidebar()
}

/** 全屏切换 */
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

/** 处理下拉菜单命令 */
const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 取消退出
    }
  } else if (command === 'profile') {
    ElMessage.info('个人中心功能开发中')
  } else if (command === 'password') {
    ElMessage.info('修改密码功能开发中')
  }
}

/** 消息通知抽屉 */
const noticeVisible = ref(false)
const notifications = ref([
  { id: 1, title: '系统升级通知', content: '烤房全周期信息化管理平台已升级至最新版本，新增BI大屏、烘烤师管理等功能模块。', time: '2026-08-05 09:00', read: false },
  { id: 2, title: '数据备份完成', content: '系统数据库已自动完成备份，备份文件已存储至服务器。', time: '2026-08-04 23:00', read: false },
  { id: 3, title: '新用户注册提醒', content: '有新用户通过小程序端注册，请及时查看用户管理页面进行审核。', time: '2026-08-04 16:19', read: false },
])
const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

function openNotice() {
  noticeVisible.value = true
}

function markAsRead(item: any) {
  item.read = true
}

function markAllRead() {
  notifications.value.forEach(n => n.read = true)
}
</script>

<template>
  <div class="navbar">
    <!-- 左侧区域 -->
    <div class="navbar-left">
      <!-- 折叠按钮 -->
      <div class="hamburger-btn" @click="toggleSidebar">
        <el-icon :size="20">
          <Fold v-if="!appStore.sidebarCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>

      <!-- 面包屑导航 -->
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item
          v-for="item in breadcrumbs"
          :key="item.path"
        >
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右侧区域 -->
    <div class="navbar-right">
      <!-- 全屏按钮 -->
      <div class="navbar-action" @click="toggleFullscreen">
        <el-icon :size="18">
          <FullScreen />
        </el-icon>
      </div>

      <!-- 消息提醒 -->
      <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0" class="navbar-badge">
        <div class="navbar-action" @click="openNotice">
          <el-icon :size="18">
            <Bell />
          </el-icon>
        </div>
      </el-badge>

      <!-- 用户头像与下拉菜单 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="navbar-user">
          <el-avatar :size="30" class="user-avatar">
            <el-icon :size="16"><UserFilled /></el-icon>
          </el-avatar>
          <span class="user-name">{{ userName }}</span>
          <el-icon :size="12"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon>修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 消息通知抽屉 -->
    <el-drawer v-model="noticeVisible" title="消息通知" size="380px" direction="rtl">
      <div class="notice-header">
        <span class="notice-count">{{ unreadCount }} 条未读</span>
        <el-button v-if="unreadCount > 0" type="primary" link size="small" @click="markAllRead">全部已读</el-button>
      </div>
      <div class="notice-list">
        <div v-for="item in notifications" :key="item.id" class="notice-item" :class="{ 'notice-item--unread': !item.read }" @click="markAsRead(item)">
          <div class="notice-item-header">
            <span class="notice-dot" v-if="!item.read"></span>
            <span class="notice-title">{{ item.title }}</span>
            <span class="notice-time">{{ item.time }}</span>
          </div>
          <div class="notice-content">{{ item.content }}</div>
        </div>
        <div v-if="notifications.length === 0" class="notice-empty">暂无消息</div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.navbar {
  height: var(--navbar-height, 56px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: none;
  position: relative;
  z-index: 10;
}

/* 底部渐变线 */
.navbar::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border-color) 20%, #1A6B4F 50%, var(--border-color) 80%, transparent);
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hamburger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  cursor: pointer;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.hamburger-btn:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

.breadcrumb {
  line-height: var(--navbar-height, 56px);
}

.breadcrumb :deep(.el-breadcrumb__inner) {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 500;
}

.breadcrumb :deep(.el-breadcrumb__inner.is-link) {
  color: var(--text-muted);
  font-weight: 500;
}

.breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: var(--el-color-primary);
}

.breadcrumb :deep(.el-breadcrumb__separator) {
  color: var(--text-placeholder);
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.navbar-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  cursor: pointer;
  border-radius: var(--radius-md);
  color: var(--text-muted);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.navbar-action:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

.navbar-badge {
  line-height: 1;
}

.navbar-badge :deep(.el-badge__content) {
  border: none;
}

.navbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: var(--radius-lg);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  margin-left: 4px;
}

.navbar-user:hover {
  background: var(--surface-hover);
}

.user-avatar {
  background: var(--primary-gradient);
  box-shadow: 0 2px 6px rgba(26, 107, 79, 0.25);
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 消息通知抽屉 */
.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.notice-count {
  font-size: 14px;
  color: #999;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  padding: 14px;
  border-radius: 8px;
  background: #f8f9fa;
  cursor: pointer;
  transition: background 0.2s;
}

.notice-item:hover {
  background: #f0f2f5;
}

.notice-item--unread {
  background: #e8f5e9;
}

.notice-item--unread:hover {
  background: #dceae0;
}

.notice-item-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.notice-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
}

.notice-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  flex: 1;
}

.notice-time {
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
}

.notice-content {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.notice-empty {
  text-align: center;
  color: #ccc;
  padding: 40px 0;
  font-size: 14px;
}
</style>
