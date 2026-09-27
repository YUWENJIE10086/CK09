<script setup lang="ts">
import { onMounted } from 'vue'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
import TagsView from './TagsView.vue'

const appStore = useAppStore()
const userStore = useUserStore()

// 登录后获取用户信息
onMounted(async () => {
  if (userStore.token && !userStore.userInfo.userId) {
    try {
      await userStore.getUserInfo()
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }
})
</script>

<template>
  <el-container class="app-layout">
    <!-- 左侧侧边栏 -->
    <Sidebar :collapsed="appStore.sidebarCollapsed" />

    <!-- 右侧主区域 -->
    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="navbar-wrapper" :height="'auto'">
        <Navbar />
        <TagsView />
      </el-header>

      <!-- 主内容区域 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  width: 100%;
  height: 100%;
}

.main-container {
  flex-direction: column;
  overflow: hidden;
}

.navbar-wrapper {
  padding: 0;
  height: auto !important;
  z-index: 9;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: transparent !important;
  padding: 0;
}

/* 页面切换动画 */
.fade-transform-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.fade-transform-leave-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
