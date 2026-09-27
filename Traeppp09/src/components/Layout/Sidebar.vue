<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineProps<{
  /** 侧边栏是否折叠 */
  collapsed: boolean
}>()

const route = useRoute()

/** 当前激活的菜单路径 */
const activeMenu = computed(() => route.path)
</script>

<template>
  <el-aside
    class="sidebar"
    :width="collapsed ? 'var(--sidebar-collapsed-width)' : 'var(--sidebar-width)'"
  >
    <!-- Logo区域 — 白色底 + 深色渐变装饰条 -->
    <div class="sidebar-logo" :class="{ 'is-collapsed': collapsed }">
      <div class="logo-accent-bar"></div>
      <div class="logo-content">
        <div class="logo-icon-wrapper">
          <el-icon :size="22" color="#FFFFFF">
            <House />
          </el-icon>
        </div>
        <transition name="fade">
          <div v-show="!collapsed" class="logo-text-group">
            <span class="logo-title">烤房管理平台</span>
          </div>
        </transition>
      </div>
    </div>

    <!-- 菜单区域 — 白色背景 -->
    <el-scrollbar class="sidebar-menu-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="true"
        :unique-opened="true"
        text-color="#2A4A42"
        active-text-color="#FFFFFF"
        router
        class="sidebar-menu"
      >
        <!-- 工作台 — 一级菜单，大标题风格 -->
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <!-- 烤房管理 — 子菜单，小标题风格 -->
        <el-sub-menu index="/barn">
          <template #title>
            <el-icon><House /></el-icon>
            <span>烤房管理</span>
          </template>
          <el-menu-item index="/barn/list">烤房列表</el-menu-item>
          <el-menu-item index="/barn/components">烤房部件</el-menu-item>
          <el-menu-item index="/barn/create">新建烤房</el-menu-item>
        </el-sub-menu>

        <!-- 维修管理 -->
        <el-sub-menu index="/repair">
          <template #title>
            <el-icon><SetUp /></el-icon>
            <span>维修管理</span>
          </template>
          <el-menu-item index="/repair/list">维修列表</el-menu-item>
          <el-menu-item index="/repair/submit">维修提报</el-menu-item>
          <el-menu-item index="/repair/component-quote">部件报价</el-menu-item>
          <el-menu-item index="/repair/analysis">维修分析</el-menu-item>
        </el-sub-menu>

        <!-- 烤房分配 -->
        <el-menu-item index="/barn/assign">
          <el-icon><Connection /></el-icon>
          <template #title>烤房分配</template>
        </el-menu-item>

        <!-- 人员管理 -->
        <el-sub-menu index="/farmer">
          <template #title>
            <el-icon><User /></el-icon>
            <span>人员管理</span>
          </template>
          <el-menu-item index="/farmer/list">烟农列表</el-menu-item>
          <el-menu-item index="/farmer/stats">烟农分析</el-menu-item>
          <el-menu-item index="/farmer/baker">烘烤师管理</el-menu-item>
        </el-sub-menu>

        <!-- 评价管理 -->
        <el-sub-menu index="/evaluate">
          <template #title>
            <el-icon><ChatDotSquare /></el-icon>
            <span>评价管理</span>
          </template>
          <el-menu-item index="/evaluate/list">评价列表</el-menu-item>
          <el-menu-item index="/evaluate/analysis">评价分析</el-menu-item>
        </el-sub-menu>

        <!-- 智能分析 -->
        <el-sub-menu index="/analysis">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>智能分析</span>
          </template>
          <el-menu-item index="/analysis/barn-health-score">烤房健康分</el-menu-item>
          <el-menu-item index="/analysis/barn-life-predict">寿命预测算法</el-menu-item>
          <el-menu-item index="/analysis/barn-recommend">智能推荐</el-menu-item>
          <el-menu-item index="/analysis/fund-allocate">资金分配</el-menu-item>
        </el-sub-menu>

        <!-- 烤房新建 -->
        <el-menu-item index="/site-new-build">
          <el-icon><Aim /></el-icon>
          <template #title>烤房新建</template>
        </el-menu-item>

        <!-- 烟农培育 -->
        <el-sub-menu index="/farmer-cultivate">
          <template #title>
            <el-icon><User /></el-icon>
            <span>烟农培育</span>
          </template>
          <el-menu-item index="/farmer-cultivate/answers">答题情况</el-menu-item>
          <el-menu-item index="/farmer-cultivate/profile">烟农画像</el-menu-item>
        </el-sub-menu>

        <!-- BI大屏 -->
        <el-menu-item index="/bi-screen">
          <el-icon><DataBoard /></el-icon>
          <template #title>BI大屏</template>
        </el-menu-item>

        <!-- AI助手 -->
        <el-menu-item index="/ai-assistant">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>AI助手</template>
        </el-menu-item>

        <!-- 管护队伍 -->
        <el-menu-item index="/team">
          <el-icon><UserFilled /></el-icon>
          <template #title>管护队伍</template>
        </el-menu-item>

        <!-- 年度更新 -->
        <el-menu-item index="/annual-update">
          <el-icon><Upload /></el-icon>
          <template #title>年度更新</template>
        </el-menu-item>

        <!-- 系统管理 -->
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/menu">菜单管理</el-menu-item>
          <el-menu-item index="/system/dept">部门管理</el-menu-item>
          <el-menu-item index="/system/dict">字典管理</el-menu-item>
          <el-menu-item index="/system/log">操作日志</el-menu-item>
          <el-menu-item index="/system/database">数据库配置</el-menu-item>
          <el-sub-menu index="/system/hidden">
            <template #title>隐藏菜单</template>
            <el-menu-item index="/digital-twin">烤房分布图</el-menu-item>
            <el-menu-item index="/site-selection">烤房选址</el-menu-item>
          </el-sub-menu>
        </el-sub-menu>
      </el-menu>
    </el-scrollbar>
  </el-aside>
</template>

<style scoped>
/* ===== 侧栏容器 — 纯白底，与右侧深色渐变形成对比 ===== */
.sidebar {
  background: #FFFFFF;
  transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;
}

/* 右侧精致分割线 — 顶部深绿渐变到底部淡灰 */
.sidebar::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(
    180deg,
    rgba(13, 93, 70, 0.4) 0%,
    rgba(13, 93, 70, 0.15) 15%,
    var(--border-color) 40%,
    var(--border-color) 100%
  );
  z-index: 1;
}

/* ===== Logo区域 — 白色底 + 左侧深绿渐变装饰条 ===== */
.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  position: relative;
  background: #FFFFFF;
  border-bottom: 1px solid var(--border-light);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 滚动到顶部时 — Logo区颜色加深 */
.sidebar-logo.is-deep {
  background: linear-gradient(135deg, #E8F5F0 0%, #FFFFFF 100%);
}

/* 左侧深绿渐变装饰条 — 4px宽 */
.logo-accent-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #0A4D3E 0%, #0D5D46 30%, #1A6B4F 60%, #2E8B6A 100%);
  border-radius: 0 2px 2px 0;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 2px 0 8px rgba(13, 93, 70, 0.2);
}

.sidebar-logo.is-deep .logo-accent-bar {
  box-shadow: 3px 0 16px rgba(13, 93, 70, 0.35);
}

.sidebar-logo.is-collapsed {
  justify-content: center;
  padding: 0;
}

.logo-content {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.logo-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0D5D46 0%, #1A6B4F 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 10px rgba(13, 93, 70, 0.25);
  transition: all 0.3s ease;
}

.sidebar-logo.is-deep .logo-icon-wrapper {
  box-shadow: 0 2px 16px rgba(13, 93, 70, 0.4);
}

.logo-text-group {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 大标题 — 深绿色，与整体风格一致 */
.logo-title {
  font-size: 16px;
  font-weight: 800;
  color: #0D5D46;
  letter-spacing: 0.1em;
  white-space: nowrap;
  line-height: 1.3;
  transition: color 0.4s ease;
}

.sidebar-logo.is-deep .logo-title {
  color: #0A4D3E;
}

/* ===== 菜单区域 ===== */
.sidebar-menu-scroll {
  flex: 1;
  overflow: hidden;
}

.sidebar-menu {
  border-right: none;
  background: transparent !important;
  padding: 6px 0;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: var(--sidebar-width);
}

/* ===== 一级菜单项（工作台、数字孪生等）— 大标题风格 ===== */
:deep(.el-menu-item) {
  position: relative;
  margin: 2px 10px;
  border-radius: var(--radius-md);
  height: 46px;
  line-height: 46px;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.06em;
  background-color: transparent !important;
  color: #0F3D34 !important;
}

:deep(.el-menu-item:hover) {
  background: linear-gradient(135deg, rgba(13, 93, 70, 0.12) 0%, rgba(26, 107, 79, 0.08) 100%) !important;
  color: #0D5D46 !important;
}

/* 激活态 — 深色渐变背景 + 白色文字 */
:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 50%, #1A6B4F 100%) !important;
  color: #FFFFFF !important;
  font-weight: 800 !important;
  box-shadow: 0 2px 12px rgba(13, 93, 70, 0.3), 0 0 0 1px rgba(13, 93, 70, 0.1);
}

:deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 28px;
  background: linear-gradient(180deg, #2E8B6A, #5FB292);
  border-radius: 0 4px 4px 0;
  box-shadow: 4px 0 14px rgba(46, 139, 106, 0.5);
}

/* 一级菜单图标 — 深绿色 */
:deep(.el-menu-item .el-icon) {
  color: #0D5D46;
  font-size: 20px;
  transition: all 0.25s ease;
}

:deep(.el-menu-item:hover .el-icon) {
  color: #0A4D3E;
}

:deep(.el-menu-item.is-active .el-icon) {
  color: #FFFFFF;
}

/* ===== 子菜单标题（烤房管理、维修管理等）— 小标题风格 ===== */
:deep(.el-sub-menu__title) {
  margin: 2px 10px;
  border-radius: var(--radius-md);
  height: 42px;
  line-height: 42px;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.05em;
  color: #2A5A4E !important;
}

:deep(.el-sub-menu__title:hover) {
  background: linear-gradient(135deg, rgba(26, 107, 79, 0.08) 0%, rgba(46, 139, 106, 0.05) 100%) !important;
  color: #0D5D46 !important;
}

/* 子菜单标题图标 — 比大标题图标淡的绿色 */
:deep(.el-sub-menu__title .el-icon) {
  color: #2E8B6A;
  font-size: 18px;
}

:deep(.el-sub-menu__title .el-sub-menu__icon-arrow) {
  color: #8CBCAC;
  font-size: 12px;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: #0D5D46 !important;
  background: linear-gradient(135deg, rgba(26, 107, 79, 0.06) 0%, rgba(46, 139, 106, 0.03) 100%) !important;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .el-icon) {
  color: #1A6B4F;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .el-sub-menu__icon-arrow) {
  color: #1A6B4F;
}

/* ===== 子菜单展开区 — 极淡绿底 ===== */
:deep(.el-sub-menu .el-menu) {
  background: #F6FAF8 !important;
  border-radius: var(--radius-md);
  margin: 0 10px 4px;
  padding: 2px 0 !important;
}

/* 子菜单项 — 浅色渐变激活态 */
:deep(.el-sub-menu .el-menu-item) {
  background-color: transparent !important;
  min-width: auto;
  padding-left: 48px !important;
  color: #4A7A6E !important;
  font-weight: 500;
  font-size: 13px;
  letter-spacing: 0.03em;
  height: 36px;
  line-height: 36px;
  margin: 1px 4px;
  border-radius: 6px;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background: linear-gradient(135deg, rgba(26, 107, 79, 0.08) 0%, rgba(46, 139, 106, 0.05) 100%) !important;
  color: #0D5D46 !important;
}

/* 子菜单项激活态 — 深色渐变，与一级菜单一致 */
:deep(.el-sub-menu .el-menu-item.is-active) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 50%, #1A6B4F 100%) !important;
  color: #FFFFFF !important;
  font-weight: 700 !important;
  box-shadow: 0 2px 10px rgba(13, 93, 70, 0.25);
}

:deep(.el-sub-menu .el-menu-item.is-active)::before {
  left: -10px;
  width: 3px;
  height: 18px;
  background: linear-gradient(180deg, #2E8B6A, #5FB292);
  box-shadow: 3px 0 10px rgba(46, 139, 106, 0.35);
}

/* ===== 分组间距 ===== */
:deep(.el-sub-menu) {
  margin-top: 2px;
}

/* ===== 折叠态弹出菜单 ===== */
:deep(.el-menu--popup) {
  background: #FFFFFF !important;
  border-radius: var(--radius-md) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06) !important;
  border: 1px solid var(--border-color) !important;
  padding: 6px !important;
}

:deep(.el-menu--popup .el-menu-item) {
  margin: 2px 4px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 14px;
  color: #1A2F2E !important;
}

:deep(.el-menu--popup .el-menu-item.is-active) {
  background: linear-gradient(135deg, #0A4D3E, #1A6B4F) !important;
  color: #FFFFFF !important;
}

/* ===== Logo标题过渡 ===== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
