/**
 * 评价记录API - 对接RuoYi后端
 */
import { get, post, put, del } from './request'

export interface EvaluationListParams {
  pageNum?: number
  pageSize?: number
  ovenId?: string
  userId?: string | number
  evaluatorPhone?: string
  county?: string
  minRating?: number
  maxRating?: number
  startDate?: string
  endDate?: string
}

export function getEvaluationList(params: EvaluationListParams = {}): Promise<any> {
  return get('/barn/evaluation/list', params) as any
}

export function getEvaluationInfo(id: number): Promise<any> {
  return get(`/barn/evaluation/${id}`) as any
}

export function addEvaluation(data: any): Promise<any> {
  return post('/barn/evaluation', data)
}

export function updateEvaluation(data: any): Promise<any> {
  return put('/barn/evaluation', data)
}

export function deleteEvaluation(id: number): Promise<any> {
  return del(`/barn/evaluation/${id}`)
}

export function getEvaluationStats(): Promise<any> {
  return get('/barn/evaluation/stats') as any
}

/** 别名 - 兼容旧引用 */
export const getEvaluationStatistics = getEvaluationStats
