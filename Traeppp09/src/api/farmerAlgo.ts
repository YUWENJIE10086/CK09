import { get, post } from './request'

/** 问卷批次 */
export const DEFAULT_PAPER = '2026'

/** 五维基础权重 */
export const DIM_COLS = ['k_cult', 'k_pp', 'k_hv', 'k_cur', 'k_syn'] as const
export const DIMS = ['栽培', '植保', '采烤', '烘烤', '综合'] as const
export const DIM_W = [0.18, 0.18, 0.19, 0.25, 0.20] as const

/** 导入答题明细 */
export function importAnswers(rows: unknown[], paperId = DEFAULT_PAPER, replace = true): Promise<any> {
  return post('/farmer-algo/import', rows, { params: { paperId, replace } })
}

/** 生成演示问卷数据 */
export function genDemo(paperId = DEFAULT_PAPER): Promise<any> {
  return post(`/farmer-algo/demo?paperId=${paperId}`)
}

/** 清空该批次 */
export function clearPaper(paperId = DEFAULT_PAPER): Promise<any> {
  return post(`/farmer-algo/clear?paperId=${paperId}`)
}

/** 答题明细分页 */
export function getAnswers(params: Record<string, any>): Promise<any> {
  return get('/farmer-algo/answers', params)
}

/** 答题概况 */
export function getAnswerStats(paperId = DEFAULT_PAPER): Promise<any> {
  return get('/farmer-algo/answer-stats', { paperId })
}

/** 重建画像 */
export function computeProfiles(paperId = DEFAULT_PAPER): Promise<any> {
  return post(`/farmer-algo/compute?paperId=${paperId}`)
}

/** 总览（画像分级分布 + 表格） */
export function getOverview(paperId = DEFAULT_PAPER): Promise<any> {
  return get('/farmer-algo/overview', { paperId })
}

/** 画像分页 */
export function getProfiles(params: Record<string, any>): Promise<any> {
  return get('/farmer-algo/profiles', params)
}

/** 村×五维薄弱矩阵 */
export function getVillageGap(paperId = DEFAULT_PAPER): Promise<any> {
  return get('/farmer-algo/village-gap', { paperId })
}

/** 培育计划 */
export function getTrainPlan(paperId = DEFAULT_PAPER): Promise<any> {
  return get('/farmer-algo/train-plan', { paperId })
}

/** 画像样板（雷达）TOP N */
export function getRadarTop(limit = 8, paperId = DEFAULT_PAPER): Promise<any> {
  return get('/farmer-algo/radar', { limit, paperId })
}