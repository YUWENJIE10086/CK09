/**
 * 健康分析API - 对接RuoYi后端
 */
import { get } from './request'

/** 健康等级统计 - 使用合并的stats接口 */
export function getHealthStats(): Promise<any> {
  return get('/barn/project/stats').then((res: any) => ({
    excellent: res.healthExcellent,
    maintenance: res.healthMaintenance,
    urgent: res.healthUrgent,
    retired: res.healthRetired
  })) as any
}

/** 县区分布 - 使用合并的stats接口 */
export function getCountyStats(): Promise<any[]> {
  return get('/barn/project/stats').then((res: any) => res.countyStats || []) as any
}

/** 健康评分列表（复用烤房列表） */
export function getHealthScoreList(params: any = {}): Promise<any> {
  return get('/barn/project/list', { pageSize: 500, ...params }) as any
}

/** 健康预测（使用合并的stats接口） */
export function getHealthPrediction(): Promise<any> {
  return get('/barn/project/stats').then((res: any) => ({
    excellent: res.healthExcellent,
    maintenance: res.healthMaintenance,
    urgent: res.healthUrgent,
    retired: res.healthRetired
  })) as any
}

/** 类型别名 */
export type HealthScoreRecord = any
export type HealthLevel = '优良' | '需维护' | '急需修复' | '退出'
