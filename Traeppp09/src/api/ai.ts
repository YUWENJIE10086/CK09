/**
 * AI智能分析API - 对接RuoYi后端
 */
import { post, get } from './request'

/** 自动健康评分 */
export function calculateHealthScore(barnId: number): Promise<any> {
  return post('/barn/ai/health-score', { barnId })
}

/** 健康状态预测 */
export function predictHealth(barnId: number): Promise<any> {
  return post('/barn/ai/predict', { barnId })
}

/** 智能烤房推荐 */
export function recommendBarns(data: any): Promise<any> {
  return post('/barn/ai/recommend', data)
}

/** 维修资金分配 */
export function allocateFund(data: any): Promise<any> {
  return post('/barn/ai/fund-allocate', data)
}

// ======================== 健康分算法 ========================

/** 获取健康分算法描述 */
export function getHealthAlgoDesc(algo: string): Promise<any> {
  return get(`/barn/health-algo/desc/${algo}`)
}

/** 应用健康分算法到所有烤房 */
export function applyHealthAlgo(data: any): Promise<any> {
  return post('/barn/health-algo/apply', data)
}

/** 健康分算法列表 */
export function getHealthAlgoList(params: any): Promise<any> {
  return get('/barn/health-algo/list', params)
}

/** 健康分统计 */
export function getHealthAlgoStats(algo: string): Promise<any> {
  return get('/barn/health-algo/stats', { algo })
}

// ======================== 健康分最终版算法 ========================

/** 应用健康分最终版算法到所有烤房 */
export function applyHealthScoreFinal(data: any): Promise<any> {
  return post('/barn/health-algo/final-apply', data)
}

/** 健康分最终版列表 */
export function getHealthScoreFinalList(params: any): Promise<any> {
  return get('/barn/health-algo/final-list', params)
}

/** 健康分最终版统计 */
export function getHealthScoreFinalStats(algo: string): Promise<any> {
  return get('/barn/health-algo/final-stats', { algo })
}

// ======================== 寿命预测算法 ========================

/** 获取寿命算法描述 */
export function getLifeAlgoDesc(algo: string): Promise<any> {
  return get(`/barn/health-algo/life-desc/${algo}`)
}

/** 应用寿命算法到所有烤房 */
export function applyLifeAlgo(data: any): Promise<any> {
  return post('/barn/health-algo/life-apply', data)
}

/** 寿命预测列表 */
export function getLifeAlgoList(params: any): Promise<any> {
  return get('/barn/health-algo/life-list', params)
}

/** 寿命统计 */
export function getLifeAlgoStats(algo: string): Promise<any> {
  return get('/barn/health-algo/life-stats', { algo })
}

// ======================== 寿命预测最终版（BDRL） ========================

/** 应用寿命预测最终版算法(BDRL)到所有烤房 */
export function applyLifePredictFinal(data: any): Promise<any> {
  return post('/barn/health-algo/life-final-apply', data)
}

/** 寿命预测最终版列表 */
export function getLifePredictFinalList(params: any): Promise<any> {
  return get('/barn/health-algo/life-final-list', params)
}

/** 寿命预测最终版统计 */
export function getLifePredictFinalStats(): Promise<any> {
  return get('/barn/health-algo/life-final-stats')
}

// ======================== 健康分析 ========================

/** 健康分析总览 - 全部烤房统计概览 */
export function getAnalysisOverview(): Promise<any> {
  return get('/barn/health-algo/analysis-overview')
}

/** 健康分析详情 - 某烤房的所有算法健康分、寿命预测、部件详情 */
export function getHealthAnalysis(projectId: string): Promise<any> {
  return get(`/barn/health-algo/analysis/${projectId}`)
}

// ======================== 维修驱动更新 ========================

/** 根据维修记录更新全部健康分与剩余寿命（续期/加分） */
export function repairSyncHealthLife(data: any = {}): Promise<any> {
  return post('/barn/health-algo/repair-sync', data)
}
