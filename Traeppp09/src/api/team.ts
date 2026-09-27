/**
 * 管护队伍API
 */
import { get, post, put, del } from './request'

export interface TeamListParams {
  teamType?: string
  countyCode?: string
}

export interface TeamInfo {
  teamId: number
  teamName: string
  teamType: string
  countyCode: string
  countyName: string
  leaderName: string
  leaderPhone: string
  memberCount: number
  serviceArea: string
  repairCount: number
  status: string
  remark: string
  createTime: string
}

/** 获取队伍列表 */
export function listTeam(params: TeamListParams = {}): Promise<TeamInfo[]> {
  return get('/barn/team/list', params) as any
}

/** 获取单个队伍 */
export function getTeamInfo(id: number): Promise<TeamInfo> {
  return get(`/barn/team/${id}`) as any
}

/** 新增队伍 */
export function addTeam(data: Partial<TeamInfo>): Promise<void> {
  return post('/barn/team', data) as any
}

/** 更新队伍 */
export function updateTeam(data: Partial<TeamInfo>): Promise<void> {
  return put('/barn/team', data) as any
}

/** 删除队伍 */
export function deleteTeam(id: number): Promise<void> {
  return del(`/barn/team/${id}`) as any
}