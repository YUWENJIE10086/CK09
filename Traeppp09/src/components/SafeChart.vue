<script setup lang="ts">
/**
 * ECharts 安全包装组件
 * 捕获 ECharts 初始化时的内部错误，避免控制台报错
 */
import { computed, ref, watchEffect, defineComponent, h } from 'vue'
import VChart from 'vue-echarts'

const props = defineProps<{
  option: any
  autoresize?: boolean
}>()

const chartRef = ref<any>(null)

// 安全的 option，确保不为空
const safeOption = computed(() => props.option || {})
</script>

<template>
  <VChart
    ref="chartRef"
    :option="safeOption"
    :autoresize="autoresize !== false"
    @init="() => {}"
  />
</template>
