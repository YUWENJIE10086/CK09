<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  /** 烤房编号，如 "42052720230001" */
  code: string
  /** 是否暗色主题 */
  dark?: boolean
  /** 是否显示为紧凑模式（无前缀拆分） */
  compact?: boolean
}>()

/** 解析编号：提取前缀和序号 */
const parsed = computed(() => {
  const raw = props.code || ''
  if (!raw || props.compact) {
    return { prefix: '', num: raw, display: raw }
  }
  // 尝试识别常见格式：
  // 42052720230001 → 420527-2023-0001
  // KF-2024-0001 → KF-2024-0001
  if (/^\d{12,}$/.test(raw)) {
    // 纯数字长编号：6位区划 + 4位年份 + 序号
    const area = raw.slice(0, 6)
    const year = raw.slice(6, 10)
    const seq = raw.slice(10)
    return { prefix: `${area}`, num: `${year}-${seq}`, display: `${area}-${year}-${seq}` }
  }
  if (/^\d{6}/.test(raw)) {
    // 以6位数字开头
    const match = raw.match(/^(\d{6})(.*)$/)
    if (match) return { prefix: match[1], num: match[2], display: raw }
  }
  // 含横线的编号：直接显示
  if (raw.includes('-')) {
    const parts = raw.split('-')
    return { prefix: parts.slice(0, -1).join('-'), num: parts[parts.length - 1], display: raw }
  }
  return { prefix: '', num: raw, display: raw }
})
</script>

<template>
  <span class="barn-code" :class="{ 'barn-code--dark': dark }">
    <span v-if="parsed.prefix && !compact" class="code-prefix">{{ parsed.prefix }}-</span><span class="code-num">{{ compact ? parsed.display : parsed.num }}</span>
  </span>
</template>

<style scoped>
.barn-code {
  font-family: 'Courier New', Consolas, Monaco, monospace;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.code-prefix,
.code-num {
  font-family: inherit;
  font-size: inherit;
  font-weight: inherit;
  letter-spacing: inherit;
}
</style>
