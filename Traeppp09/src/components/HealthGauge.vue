<script setup lang="ts">
import { computed } from 'vue'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'

const props = withDefaults(defineProps<{
  /** 健康评分 0-100 */
  score: number
}>(), {
  score: 0
})

/** 仪表盘配置 */
const chartOption = computed(() => ({
  series: [
    {
      type: 'gauge',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      splitNumber: 10,
      itemStyle: {
        color: '#1A3C6E',
      },
      progress: {
        show: true,
        width: 16,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#E74C3C' },
              { offset: 0.4, color: '#F39C12' },
              { offset: 0.7, color: '#F1C40F' },
              { offset: 1, color: '#2ECC71' },
            ],
          },
        },
      },
      pointer: {
        show: false,
      },
      axisLine: {
        lineStyle: {
          width: 16,
          color: [[1, '#e8e8e8']],
        },
      },
      axisTick: {
        show: false,
      },
      splitLine: {
        show: false,
      },
      axisLabel: {
        show: false,
      },
      detail: {
        valueAnimation: true,
        fontSize: 28,
        fontWeight: 'bold',
        offsetCenter: [0, '10%'],
        formatter: '{value}',
        color: (() => {
          if (props.score < 40) return '#E74C3C'
          if (props.score < 70) return '#F39C12'
          if (props.score < 90) return '#F1C40F'
          return '#2ECC71'
        })(),
      },
      title: {
        offsetCenter: [0, '40%'],
        fontSize: 13,
        color: '#909399',
      },
      data: [
        {
          value: props.score,
          name: '健康评分',
        },
      ],
    },
  ],
}))
</script>

<template>
  <div class="health-gauge">
    <VChart v-if="chartOption.series[0].data[0]?.value !== undefined" :option="chartOption" autoresize class="gauge-chart" />
  </div>
</template>

<style scoped>
.health-gauge {
  width: 100%;
  height: 100%;
  min-height: 180px;
}

.gauge-chart {
  width: 100%;
  height: 100%;
}
</style>
