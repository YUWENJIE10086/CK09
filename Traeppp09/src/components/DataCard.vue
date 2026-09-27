<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  /** 卡片标题 */
  title: string
  /** 数值 */
  value: number | string
  /** 图标名称 */
  icon: string
  /** 图标背景颜色 */
  color: string
  /** 趋势方向 */
  trend?: 'up' | 'down' | ''
  /** 趋势值 */
  trendValue?: string
}>()

/** 趋势颜色类 */
const trendClass = computed(() => {
  if (props.trend === 'up') return 'trend-up'
  if (props.trend === 'down') return 'trend-down'
  return ''
})

/** 图标区域渐变背景 - 统一森林绿系配色 */
const iconBgStyle = computed(() => {
  // 根据传入颜色映射到统一的森林绿系渐变
  const colorMap: Record<string, { start: string; end: string }> = {
    // 主色系 - 深森林绿
    '#1A6B4F': { start: '#0A4D3E', end: '#1A6B4F' },
    '#0A4D3E': { start: '#083D32', end: '#0A4D3E' },
    
    // 正常/良好 - 中绿
    '#2E8B6A': { start: '#1A6B4F', end: '#3A8B70' },
    '#2ECC71': { start: '#27AE60', end: '#2ECC71' },
    
    // 资金/金额 - 金绿
    '#D4944A': { start: '#8B6914', end: '#B8860B' },
    
    // 警示/需关注 - 深青绿
    '#F39C12': { start: '#5A6A5A', end: '#6A7A6A' },
    
    // 紧急/异常 - 深红褐
    '#E74C3C': { start: '#9B4A3A', end: '#A85646' },
    '#D4604A': { start: '#9B4A3A', end: '#A85646' },
    
    // 信息/辅助 - 灰绿
    '#3a6cb8': { start: '#3A5A5A', end: '#4A6A6A' },
  }
  
  const c = props.color || '#1A6B4F'
  const gradient = colorMap[c] || { start: '#0A4D3E', end: '#1A6B4F' }
  
  return {
    background: `linear-gradient(135deg, ${gradient.start}, ${gradient.end})`,
  }
})
</script>

<template>
  <div class="data-card">
    <div class="data-card-body">
      <div class="data-card-icon" :style="iconBgStyle">
        <el-icon :size="22" color="#fff">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="data-card-info">
        <div class="data-card-title">{{ title }}</div>
        <div class="data-card-value">{{ value }}</div>
        <div v-if="trend && trendValue" class="data-card-trend" :class="trendClass">
          <el-icon :size="12">
            <Top v-if="trend === 'up'" />
            <Bottom v-if="trend === 'down'" />
          </el-icon>
          <span>{{ trendValue }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== 毛玻璃卡片 ========== */
.data-card {
  position: relative;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 2px 8px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  cursor: default;
}

.data-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 8px 32px rgba(10, 77, 62, 0.12),
    0 4px 16px rgba(10, 77, 62, 0.08),
    0 0 48px rgba(26, 107, 79, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.data-card-body {
  display: flex;
  align-items: center;
  gap: clamp(12px, 1.5vw, 18px);
  padding: clamp(16px, 2vw, 22px);
}

.data-card-icon {
  width: clamp(42px, 4vw, 56px);
  height: clamp(42px, 4vw, 56px);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  box-shadow:
    0 6px 20px rgba(10, 77, 62, 0.28),
    0 2px 8px rgba(10, 77, 62, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.35),
    inset 0 -2px 0 rgba(0, 0, 0, 0.08);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.data-card:hover .data-card-icon {
  transform: scale(1.05);
  box-shadow:
    0 8px 28px rgba(10, 77, 62, 0.35),
    0 3px 10px rgba(10, 77, 62, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.4),
    inset 0 -2px 0 rgba(0, 0, 0, 0.1);
}

.data-card-icon :deep(.el-icon) {
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
}

.data-card-info {
  flex: 1;
  min-width: 0;
}

.data-card-title {
  font-size: clamp(12px, 1.1vw, 14px);
  color: var(--text-muted, #6B8B80);
  margin-bottom: 4px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.data-card-value {
  font-size: clamp(26px, 3vw, 36px);
  font-weight: 800;
  color: var(--text-primary, #0D3D30);
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.data-card-trend {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: clamp(11px, 1vw, 13px);
  margin-top: 6px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.data-card-trend.trend-up {
  color: var(--success-color, #2E8B6A);
  background: rgba(46, 139, 106, 0.1);
}

.data-card-trend.trend-down {
  color: var(--danger-color, #D4604A);
  background: rgba(212, 96, 74, 0.1);
}
</style>