<script setup lang="ts">
import { onMounted, onBeforeUnmount, watch, useTemplateRef } from "vue";
import { useTheme } from "vuetify";

// Color  -  original        (adjusted)
// blue   -   59, 130, 246   (r:  92, g: 154, b: 255)
// purple -  154, 100, 180   (r: 167, g: 104, b: 196)
// green  -   65, 181, 127   (r:  75, g: 209, b: 147)
// red    -  238, 102, 102   (r: 238, g: 102, b: 102)
const colors = {
  "blue-LightTheme": { r: 255, g: 216, b: 67 },
  "blue-DarkTheme": { r: 92, g: 154, b: 255 },
  "purple-LightTheme": { r: 255, g: 216, b: 67 },
  "purple-DarkTheme": { r: 167, g: 104, b: 196 },
  "red-LightTheme": { r: 255, g: 216, b: 67 },
  "red-DarkTheme": { r: 238, g: 102, b: 102 },
  "green-LightTheme": { r: 255, g: 216, b: 67 },
  "green-DarkTheme": { r: 75, g: 209, b: 147 }
};
const DEFAULT_COLOR = "blue";

const typeLabels = {
  percentage: "%",
  temperature: "°C",
  mb: " Mb/s"
};

const theme = useTheme();
const chartRef = useTemplateRef("chartRef");
let chart: echarts.ECharts | null = null;

const getOptions = (data: ChartData[] = []) => {
  const now = Date.now();
  return {
    title: {
      show: false
    },
    tooltip: {
      trigger: "axis",
      formatter: (params: any) => {
        const p = params[0];
        const append = typeLabels[props.type as keyof typeof typeLabels];
        const fmsLimitLabel = "FMS Limit - 7.000 Mb/s";

        // prettier-ignore
        let tooltip = "<div style=\"text-align: right;\">";
        const seriesData = `${new Date(p.value[0]).toLocaleTimeString([], { hour12: false })} - ${p.value[1].toFixed(props.type === "mb" ? 3 : 2)}${append}`;

        if (props.type === "mb") {
          if (p.value[1] >= 7) tooltip += seriesData + `<br/>${fmsLimitLabel}`;
          else tooltip += fmsLimitLabel + `<br/>${seriesData}`;
        } else tooltip += seriesData;

        return `${tooltip}</div>`;
      },
      backgroundColor: theme.global.current.value.colors.background,
      textStyle: {
        color: theme.global.current.value.colors.onBackground
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
        show: true,
        lineStyle: {
          color: "#ffffff18"
        }
      },
      splitNumber: 4,
      min: now - 55 * 1000,
      axisLine: {
        lineStyle: {
          color: theme.global.name.value === "LightTheme" ? "#aaa" : "#777"
        }
      },
      axisLabel: {
        align: "left",
        color: theme.global.name.value === "LightTheme" ? "#fff" : "#ddd",
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
        function (value: { min: number; max: number }) {
          return Math.max(0, (value.min - 10) | 0);
        },
      max:
        props.max ??
        function (value: { min: number; max: number }) {
          return (value.max + 10) | 0;
        },
      splitNumber: 2,
      splitLine: {
        show: true,
        lineStyle: {
          color: "#ffffff18"
        }
      },
      axisLabel: {
        color: theme.global.name.value === "LightTheme" ? "#fff" : "#ddd"
      }
    },
    series: getSeries(data),
    animation: false
  };
};

const getSeries = (data: ChartData[] = []) => {
  const color = colors[`${props.color ?? DEFAULT_COLOR}-${theme.global.name.value}` as keyof typeof colors];
  return [
    {
      type: "line",
      showSymbol: false,
      data: data.map((d) => [d.time, d.value]),
      markLine:
        props.type === "mb"
          ? {
              symbol: "none",
              lineStyle: {
                color: "red",
                width: 1
              },
              label: {
                show: false
              },
              data: [{ yAxis: 7 }]
            }
          : null,
      lineStyle: {
        width: 1.5,
        color: theme.global.current.value.dark
          ? `rgb(${color.r}, ${color.g}, ${color.b})`
          : theme.global.current.value.colors.primary
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
              color: theme.global.current.value.dark
                ? `rgba(${color.r}, ${color.g}, ${color.b}, 0.15)`
                : `${theme.global.current.value.colors.primary}40`
            },
            {
              offset: 1,
              color: theme.global.current.value.dark
                ? `rgba(${color.r}, ${color.g}, ${color.b}, 0.15)`
                : `${theme.global.current.value.colors.primary}40`
            }
          ]
        }
      }
    }
  ];
};

interface ChartData {
  time: number;
  value: number;
}

// Type options: "percentage", "temperature", "mb"
const props = defineProps<{
  data: ChartData[];
  type: string;
  min?: number;
  max?: number;
  color?: string;
}>();

onMounted(async () => {
  const echarts = await import("echarts");
  chart = echarts.init(chartRef.value);
  chart.setOption(getOptions(props.data));

  window.addEventListener("resize", resizeChart);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeChart);
  chart?.dispose();
});

function resizeChart() {
  chart?.resize();
}

watch(
  () => props.data,
  (data) => {
    chart?.setOption(getOptions(data));
  },
  { deep: true }
);
</script>

<template>
  <div ref="chartRef"></div>
</template>

<style scoped>
div {
  width: calc(100% + 20px);
  height: 100px;
  margin-right: -20px;
}
</style>
