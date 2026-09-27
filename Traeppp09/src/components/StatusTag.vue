<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  /** 状态值 */
  status: string
  /** 类型：烤房/维修/预约/烤房分配/健康/使用状态 */
  type: 'barn' | 'repair' | 'reservation' | 'assignment' | 'health' | 'useStatus'
}>()

/** 自定义样式映射：背景色、文字色、边框色 */
interface CustomStyle {
  bg: string
  text: string
  border: string
  label: string
}

/** 健康等级样式映射 - 柔和色系 */
const healthStyleMap: Record<string, CustomStyle> = {
  excellent: { label: '优良', bg: '#E8F5E9', text: '#2E8B6A', border: '#2E8B6A' },
  good: { label: '良好', bg: '#E0F2EC', text: '#5FB292', border: '#5FB292' },
  fair: { label: '一般', bg: '#FFF8E1', text: '#F39C12', border: '#F39C12' },
  poor: { label: '较差', bg: '#FFEBEE', text: '#E74C3C', border: '#E74C3C' },
  maintenance: { label: '需维护', bg: '#FFF8E1', text: '#F39C12', border: '#F39C12' },
  urgent: { label: '急需修复', bg: '#FFEBEE', text: '#E74C3C', border: '#E74C3C' },
  retired: { label: '退出', bg: '#F1F5F9', text: '#94A3B8', border: '#94A3B8' },
}

/** 使用状态样式映射 - 实心色块（白字深底色，无边框，与设施现状区分）
 *  统一只有两种状态：在烤(绿色) / 空闲(橙色)
 */
const useStatusStyleMap: Record<string, CustomStyle> = {
  baking: { label: '在烤', bg: '#2E8B6A', text: '#FFFFFF', border: 'none' },
  inUse: { label: '在烤', bg: '#2E8B6A', text: '#FFFFFF', border: 'none' },
  idle: { label: '空闲', bg: '#E8A714', text: '#FFFFFF', border: 'none' },
}

/** 烤房状态样式映射 - 统一颜色规范 */
const barnStyleMap: Record<string, CustomStyle> = {
  normal: { label: '正常', bg: '#E8F5E9', text: '#2E8B6A', border: '#2E8B6A' },
  excellent: { label: '优良', bg: '#E8F5E9', text: '#2E8B6A', border: '#2E8B6A' },
  urgent: { label: '急需修复', bg: '#FFEBEE', text: '#E74C3C', border: '#E74C3C' },
  hopeful: { label: '有望修复', bg: '#FFF8E1', text: '#F39C12', border: '#F39C12' },
  worthless: { label: '无修复价值', bg: '#F1F5F9', text: '#94A3B8', border: '#94A3B8' },
  trackable: { label: '可利用跟踪', bg: '#E3F2FD', text: '#3498DB', border: '#3498DB' },
}

/** 维修状态样式映射 - 统一颜色规范 */
const repairStyleMap: Record<string, CustomStyle> = {
  pending: { label: '待审核', bg: '#FFF8E1', text: '#F39C12', border: '#F39C12' },
  approved: { label: '已审核', bg: '#FFF8E1', text: '#F39C12', border: '#F39C12' },
  implementing: { label: '实施中', bg: '#FFF8E1', text: '#F39C12', border: '#F39C12' },
  inspecting: { label: '待验收', bg: '#FFF8E1', text: '#F39C12', border: '#F39C12' },
  completed: { label: '已验收', bg: '#E8F5E9', text: '#2E8B6A', border: '#2E8B6A' },
  archived: { label: '已归档', bg: '#F1F5F9', text: '#94A3B8', border: '#94A3B8' },
}

/** 预约状态样式映射 - 统一颜色规范，已确认和已完成用不同颜色区分 */
const reservationStyleMap: Record<string, CustomStyle> = {
  pending: { label: '待审核', bg: '#FFF8E1', text: '#F39C12', border: '#F39C12' },
  confirmed: { label: '已确认', bg: '#E3F2FD', text: '#3498DB', border: '#3498DB' },
  baking: { label: '使用中', bg: '#E3F2FD', text: '#2980B9', border: '#2980B9' },
  finished: { label: '已完成', bg: '#E8F5E9', text: '#2E8B6A', border: '#2E8B6A' },
  rejected: { label: '已取消', bg: '#F1F5F9', text: '#94A3B8', border: '#94A3B8' },
  timeout: { label: '超时', bg: '#FFEBEE', text: '#E74C3C', border: '#E74C3C' },
}

/** 烤房分配状态样式映射 - 统一颜色规范 */
const assignmentStyleMap: Record<string, CustomStyle> = {
  confirmed: { label: '已分配', bg: '#E3F2FD', text: '#3498DB', border: '#3498DB' },
  baking: { label: '使用中', bg: '#E8F5E9', text: '#2E8B6A', border: '#2E8B6A' },
  archived: { label: '已归还', bg: '#F1F5F9', text: '#94A3B8', border: '#94A3B8' },
  rejected: { label: '已撤销', bg: '#FFEBEE', text: '#E74C3C', border: '#E74C3C' },
}

/** 根据类型获取样式映射 */
const styleMap = computed(() => {
  switch (props.type) {
    case 'barn': return barnStyleMap
    case 'repair': return repairStyleMap
    case 'reservation': return reservationStyleMap
    case 'assignment': return assignmentStyleMap
    case 'health': return healthStyleMap
    case 'useStatus': return useStatusStyleMap
    default: return {}
  }
})

/** 当前标签配置 */
const tagStyle = computed(() => {
  const s = styleMap.value[props.status]
  if (s) return s
  return { label: props.status, bg: '#F3F4F6', text: '#6B7280', border: '#E5E7EB' }
})

/** 是否使用实心色块样式（使用状态用实心色块，其他用柔和标签） */
const isSolidBlock = computed(() => props.type === 'useStatus')
</script>

<template>
  <span
    :class="isSolidBlock ? 'solid-status-block' : 'custom-status-tag'"
    :style="{
      backgroundColor: tagStyle.bg,
      color: tagStyle.text,
      borderColor: tagStyle.border,
    }"
  >
    {{ tagStyle.label }}
  </span>
</template>

<style scoped>
/* 柔和标签样式（健康等级、维修状态、分配状态等） */
.custom-status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  border-radius: 4px;
  border: 1px solid;
  white-space: nowrap;
  letter-spacing: 0.01em;
}

/* 实心色块样式（使用状态专用 - 与设施现状区分） */
.solid-status-block {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  border-radius: 4px;
  border: none;
  white-space: nowrap;
  letter-spacing: 0.03em;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
</style>
