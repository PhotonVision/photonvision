<script setup lang="ts">
import * as echarts from "echarts";
import { onMounted, ref, onBeforeUnmount, watch } from "vue";

const chartRef = ref(null);
let chartInstance: echarts.ECharts | null = null;
const DEFAULT_COLOR: Color = { r: 59, g: 130, b: 246 };

const getOptions = (title?: string, data: ChartData[] = [], color: Color = DEFAULT_COLOR) => {
  color ??= DEFAULT_COLOR;
  return {
    title: title
      ? {
          text: title,
          textStyle: {
            color: "#fff",
            fontWeight: "normal"
          },
          left: 0
        }
      : {
          show: false
        },
    tooltip: {
      trigger: "axis",
      formatter: (params: any) => {
        const p = params[0];
        return `${new Date(p.value[0]).toLocaleTimeString()}<br/>Value: ${p.value[1].toFixed(2)}`;
      },
      axisPointer: {
        animation: false
      }
    },
    grid: {
      top: 0,
      bottom: 10,
      left: 0,
      right: 0,
      containLabel: false
    },
    xAxis: {
      type: "time",
      splitLine: {
        show: false
      },
      boundaryGap: false,
      minInterval: 10 * 1000,
      // min: data.length ? data[0]?.time : undefined,
      // max: data.at(-1)?.time,
      splitNumber: 3,
      axisLabel: {
        align: "left",
        formatter: (value: number) => {
          const date = new Date(value);
          return date.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
          });
        }
      }
    },
    yAxis: {
      type: "value",
      position: "right",
      min: 0,
      max: 100,
      interval: 50,
      splitLine: {
        show: false
      }
    },
    series: getSeries(data, color),
    animation: false
  };
};

const getSeries = (data: ChartData[] = [], color: Color = DEFAULT_COLOR) => {
  return [
    {
      name: "Fake Data",
      type: "line",
      showSymbol: false,
      data: data.map((d) => [d.time, d.value]),
      lineStyle: {
        color: `rgb(${color?.r ?? 84}, ${color?.g ?? 112}, ${color?.b ?? 198})`
      },
      areaStyle: {
        color: {
          type: "linear",
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            {
              offset: 0,
              color: `rgba(${color?.r ?? 84}, ${color?.g ?? 112}, ${color?.b ?? 198}, 0.35)`
            },
            {
              offset: 1,
              color: `rgba(${color?.r ?? 84}, ${color?.g ?? 112}, ${color?.b ?? 198}, 0)`
            }
          ]
        }
      }
    }
  ];
};

const baseOptions: any = ref();

// Example chart data — make it a prop if you want dynamic data
interface ChartData {
  time: number;
  value: number;
}
interface Color {
  r: number;
  g: number;
  b: number;
}
// blue 84, 112, 198 (59, 130, 246)
// purple 154, 96, 180
// green 65, 181, 127
const props = defineProps<{
  title?: string;
  data: ChartData[];
  color?: Color;
}>();

onMounted(() => {
  chartInstance = echarts.init(chartRef.value);
  chartInstance.setOption(getOptions(props.title, props.data, props.color));

  // Handle resize
  window.addEventListener("resize", resizeChart);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeChart);
  chartInstance?.dispose();
});

function resizeChart() {
  chartInstance?.resize();
}

// Watch for prop updates (reactive chart updates)
watch(
  () => props.data,
  (newData) => {
    chartInstance?.setOption(getOptions(props.title, newData, props.color));
  },
  { deep: true }
);
</script>

<template>
  <div ref="chartRef" class="w-full h-64"></div>
</template>

<style scoped>
/* You can size it however you like */
div {
  width: 100%;
  height: 100px;
}
</style>
