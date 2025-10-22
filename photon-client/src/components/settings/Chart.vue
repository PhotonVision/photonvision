<script setup lang="ts">
import * as echarts from "echarts";
import { onMounted, ref, onBeforeUnmount, watch } from "vue";

const chartRef = ref(null);
let chart: echarts.ECharts | null = null;

const DEFAULT_COLOR = "blue";
const colors = {
  "blue-LightTheme": { r: 255, g: 255, b: 255 },
  "blue-DarkTheme": { r: 92, g: 154, b: 255 },
  "purple-LightTheme": { r: 255, g: 255, b: 255 },
  "purple-DarkTheme": { r: 167, g: 104, b: 196 },
  "red-LightTheme": { r: 255, g: 255, b: 255 },
  "red-DarkTheme": { r: 238, g: 102, b: 102 },
  "green-LightTheme": { r: 255, g: 255, b: 255 },
  "green-DarkTheme": { r: 65, g: 181, b: 127 }
};

const getOptions = (data: ChartData[] = []) => {
  return {
    title: {
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
      right: 50,
      containLabel: false
    },
    xAxis: {
      type: "time",
      splitLine: {
        show: false
      },
      splitNumber: 4,
      axisLine: {
        lineStyle: {
          color: props.theme === "LightTheme" ? "#aaa" : "#777"
        }
      },
      axisLabel: {
        align: "left",
        color: props.theme === "LightTheme" ? "#fff" : "#ddd",
        formatter: (value: number) => {
          const date = new Date(value);
          return date.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false
          });
        }
      }
    },
    yAxis: {
      type: "value",
      position: "right",
      min:
        props.min ??
        function (value) {
          return Math.max(0, (value.min - 10) | 0);
        },
      max:
        props.max ??
        function (value) {
          return (value.max + 10) | 0;
        },
      interval: props.min || props.max ? 50 : 20,
      splitLine: {
        show: false
      },
      axisLabel: {
        color: props.theme === "LightTheme" ? "#fff" : "#ddd"
      }
    },
    series: getSeries(data),
    animation: false
  };
};

const getSeries = (data: ChartData[] = []) => {
  let color = colors[`${props.color ?? DEFAULT_COLOR}-${props.theme}`];
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
// blue 59, 130, 246
// purple 154, 96, 180
// green 65, 181, 127
// red 238, 102, 102
const props = defineProps<{
  theme: string;
  data: ChartData[];
  min?: number;
  max?: number;
  color?: string;
}>();

onMounted(() => {
  chart = echarts.init(chartRef.value);
  chart.setOption(getOptions(props.data));

  // Handle resize
  window.addEventListener("resize", resizeChart);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeChart);
  chart?.dispose();
});

function resizeChart() {
  chart?.resize();
}

// Watch for prop updates (reactive chart updates)
watch(
  () => props.data,
  (data) => {
    chart?.setOption(getOptions(data));
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
  width: calc(100% + 20px);
  height: 100px;
  margin-right: -20px;
}
</style>
