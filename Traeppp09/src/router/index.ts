import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/components/Layout/AppLayout.vue'

/** 登录页面 */
const Login = () => import('@/pages/login/LoginView.vue')
/** 工作台页面 */
const Dashboard = () => import('@/pages/dashboard/DashboardView.vue')

/**
 * 常量路由（不需要权限即可访问）
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '工作台' },
      },
    ],
  },
]

/**
 * 异步路由（需要权限控制）
 */
export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AppLayout,
    children: [
      // 烤房管理
      {
        path: 'barn/list',
        name: 'BarnList',
        component: () => import('@/pages/barn/BarnList.vue'),
        meta: { title: '烤房列表' },
      },
      {
        path: 'barn/create',
        name: 'BarnCreate',
        component: () => import('@/pages/barn/BarnAdd.vue'),
        meta: { title: '新建烤房' },
      },
      {
        path: 'barn/detail/:id',
        name: 'BarnDetail',
        component: () => import('@/pages/barn/BarnDetail.vue'),
        meta: { title: '烤房详情' },
      },
      {
        path: 'barn/edit/:id',
        name: 'BarnEdit',
        component: () => import('@/pages/barn/BarnEdit.vue'),
        meta: { title: '编辑烤房' },
      },
      // 维修管理
      {
        path: 'repair/list',
        name: 'RepairList',
        component: () => import('@/pages/repair/RepairList.vue'),
        meta: { title: '维修列表' },
      },
      {
        path: 'repair/submit',
        name: 'RepairSubmit',
        component: () => import('@/pages/repair/RepairApply.vue'),
        meta: { title: '维修提报' },
      },
      {
        path: 'repair/approve/:id',
        name: 'RepairApprove',
        component: () => import('@/pages/repair/RepairApprove.vue'),
        meta: { title: '维修审批' },
      },
      {
        path: 'repair/accept/:id',
        name: 'RepairAccept',
        component: () => import('@/pages/repair/RepairAccept.vue'),
        meta: { title: '维修验收' },
      },
      {
        path: 'repair/analysis',
        name: 'RepairAnalysis',
        component: () => import('@/pages/repair/RepairAnalysis.vue'),
        meta: { title: '维修分析' },
      },
      {
        path: 'repair/component-quote',
        name: 'ComponentQuote',
        component: () => import('@/pages/repair/ComponentQuote.vue'),
        meta: { title: '部件报价' },
      },
      // 烤房分配
      {
        path: 'barn/assign',
        name: 'BarnAssign',
        component: () => import('@/pages/barn/BarnAssign.vue'),
        meta: { title: '烤房分配' },
      },
      // 烤房部件
      {
        path: 'barn/components',
        name: 'BarnComponents',
        component: () => import('@/pages/barn/BarnComponents.vue'),
        meta: { title: '烤房部件', icon: 'Setting' },
      },
      // 评价管理
      {
        path: 'evaluate/list',
        name: 'EvaluateList',
        component: () => import('@/pages/evaluation/EvaluationList.vue'),
        meta: { title: '评价列表' },
      },
      {
        path: 'evaluate/analysis',
        name: 'EvaluateAnalysis',
        component: () => import('@/pages/evaluation/EvaluationAnalysis.vue'),
        meta: { title: '评价分析' },
      },
      // 智能分析
      {
        path: 'analysis/health-analysis',
        name: 'AnalysisHealthAnalysis',
        component: () => import('@/pages/health/HealthAnalysis.vue'),
        meta: { title: '健康分析' },
      },
      {
        path: 'analysis/health-predict',
        name: 'AnalysisHealthPredict',
        component: () => import('@/pages/ai/HealthPredict.vue'),
        meta: { title: '健康状态预测' },
      },
      {
        path: 'analysis/barn-recommend',
        name: 'AnalysisBarnRecommend',
        component: () => import('@/pages/ai/BarnRecommend.vue'),
        meta: { title: '智能推荐' },
      },
      {
        path: 'analysis/fund-allocate',
        name: 'AnalysisFundAllocate',
        component: () => import('@/pages/ai/FundAllocate.vue'),
        meta: { title: '资金分配' },
      },
      {
        path: 'analysis/health-algo',
        name: 'AnalysisHealthAlgo',
        component: () => import('@/pages/ai/HealthAlgo.vue'),
        meta: { title: '健康分算法' },
      },
      {
        path: 'analysis/barn-health-score',
        name: 'AnalysisBarnHealthScore',
        component: () => import('@/pages/ai/BarnHealthScore.vue'),
        meta: { title: '烤房健康分' },
      },
      {
        path: 'analysis/life-predict',
        name: 'AnalysisLifePredict',
        component: () => import('@/pages/ai/LifePredict.vue'),
        meta: { title: '烤房寿命预测' },
      },
      {
        path: 'analysis/barn-life-predict',
        name: 'AnalysisBarnLifePredict',
        component: () => import('@/pages/ai/BarnLifePredict.vue'),
        meta: { title: '寿命预测算法' },
      },
      {
        path: 'analysis/recommend',
        name: 'AnalysisRecommend',
        component: () => import('@/pages/recommend/RecommendView.vue'),
        meta: { title: '智能推荐' },
      },
      // 烤房分布图
      {
        path: 'digital-twin',
        name: 'DigitalTwin',
        component: () => import('@/pages/digital-twin/DigitalTwin.vue'),
        meta: { title: '烤房分布图' },
      },
      // 烤房选址
      {
        path: 'site-selection',
        name: 'SiteSelection',
        component: () => import('@/pages/site-selection/SiteSelection.vue'),
        meta: { title: '烤房选址' },
      },
      // 烤房新建
      {
        path: 'site-new-build',
        name: 'SiteNewBuild',
        component: () => import('@/pages/site-new-build/SiteNewBuild.vue'),
        meta: { title: '烤房新建' },
      },
      // 烟农培育（答题情况 / 烟农画像）
      {
        path: 'farmer-cultivate/answers',
        name: 'FarmerAnswer',
        component: () => import('@/pages/farmer/FarmerAnswer.vue'),
        meta: { title: '答题情况' },
      },
      {
        path: 'farmer-cultivate/profile',
        name: 'FarmerProfile',
        component: () => import('@/pages/farmer/FarmerProfile.vue'),
        meta: { title: '烟农画像' },
      },
      // 管护队伍
      {
        path: 'team',
        name: 'Team',
        component: () => import('@/pages/team/TeamList.vue'),
        meta: { title: '管护队伍' },
      },
      // 年度更新
      {
        path: 'annual-update',
        name: 'AnnualUpdate',
        component: () => import('@/pages/annual/AnnualUpdate.vue'),
        meta: { title: '年度更新' },
      },
      // 烟农管理
      {
        path: 'farmer/list',
        name: 'FarmerList',
        component: () => import('@/pages/system/FarmerManage.vue'),
        meta: { title: '烟农列表' },
      },
      {
        path: 'farmer/stats',
        name: 'FarmerStats',
        component: () => import('@/pages/system/FarmerStats.vue'),
        meta: { title: '烟农分析' },
      },
      {
        path: 'farmer/baker',
        name: 'BakerManage',
        component: () => import('@/pages/system/BakerManage.vue'),
        meta: { title: '烘烤师管理' },
      },
      // 烤房管理
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/pages/system/MenuManage.vue'),
        meta: { title: '菜单管理' },
      },
      {
        path: 'system/dept',
        name: 'SystemDept',
        component: () => import('@/pages/system/DeptManage.vue'),
        meta: { title: '部门管理' },
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/pages/system/DictManage.vue'),
        meta: { title: '字典管理' },
      },
      {
        path: 'system/log',
        name: 'SystemLog',
        component: () => import('@/pages/system/LogManage.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'system/database',
        name: 'SystemDatabase',
        component: () => import('@/pages/system/DatabaseConfig.vue'),
        meta: { title: '数据库配置' },
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/pages/system/UserManage.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/pages/system/RoleManage.vue'),
        meta: { title: '角色管理' },
      },
      // BI大屏 - 嵌入主页面框架
      {
        path: 'bi-screen',
        name: 'BIScreen',
        component: () => import('@/pages/bi/BiDashboard.vue'),
        meta: { title: 'BI大屏' },
      },
      // AI助手
      {
        path: 'ai-assistant',
        name: 'AiAssistant',
        component: () => import('@/pages/ai/AiAssistant.vue'),
        meta: { title: 'AI助手' },
      },
    ],
  },
]

/** 合并所有路由用于创建路由实例 */
const allRoutes: RouteRecordRaw[] = [
  ...constantRoutes,
  ...asyncRoutes,
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes: allRoutes,
})

// 路由守卫：未登录时跳转到登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    // 已登录时访问登录页，跳转到首页
    if (token) {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    // 未登录时访问其他页面，跳转到登录页
    if (token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
