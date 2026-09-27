/**
 * 字典API - 区域/项目类型 - 对接RuoYi后端
 */
import { get } from './request'

export function listCounty(cityCode?: string): Promise<any[]> {
  return get('/barn/dict/county', { cityCode }) as any
}

export function listTownship(countyCode?: string): Promise<any[]> {
  return get('/barn/dict/township', { countyCode }) as any
}

export function listVillage(townCode?: string): Promise<any[]> {
  return get('/barn/dict/village', { townCode }) as any
}

export function listProjectType(): Promise<any[]> {
  return get('/barn/dict/projectType') as any
}

/** 管护队伍 */
export function listTeam(params: { teamType?: string; countyCode?: string } = {}): Promise<any> {
  return get('/barn/team/list', params) as any
}

export function getTeamInfo(id: number): Promise<any> {
  return get(`/barn/team/${id}`) as any
}

/** 资金管理 */
export function listFund(params: { fundYear?: number; countyCode?: string } = {}): Promise<any> {
  return get('/barn/fund/list', params) as any
}
