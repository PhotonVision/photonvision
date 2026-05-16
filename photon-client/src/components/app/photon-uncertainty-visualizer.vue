<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, type Ref } from "vue";
import * as echarts from "echarts";
import type { CvPoint3 } from "@/types/SettingTypes";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useTheme } from "vuetify";

const theme = useTheme();

const props = defineProps<{
  cameraUniqueName: string;
  resolution: { width: number; height: number };
  title: string;
}>();

let chart: echarts.ECharts | undefined;

const uncertaintyData: Ref<CvPoint3[] | null> = ref(null);
const isLoading: Ref<boolean> = ref(true);
const error: Ref<string | null> = ref(null);
const containerRef = ref<HTMLDivElement | null>(null);

const drawUncertainty = (data: CvPoint3[] | null) => {
  if (!chart || !data || data.length === 0) return;

  // Get theme colors
  const themeName = theme.global.name.value;
  const themeColors = theme.themes.value[themeName].colors;
  const textColor = themeColors.onBackground;

  // Get unique X and Y values as category axes
  const xValues = Array.from(new Set(data.map((p) => p.x))).sort((a, b) => a - b);
  const yValues = Array.from(new Set(data.map((p) => p.y))).sort((a, b) => a - b);

  // Create a map for quick point lookup
  const pointMap = new Map<string, number>();
  data.forEach((point) => {
    pointMap.set(`${point.x},${point.y}`, point.z);
  });

  // Prepare heatmap data: convert to [xIndex, yIndex, value] format
  const heatmapData: [number, number, number][] = [];
  xValues.forEach((x, xi) => {
    yValues.forEach((y, yi) => {
      const value = pointMap.get(`${x},${y}`);
      if (value !== undefined) {
        heatmapData.push([xi, yi, value]);
      }
    });
  });

  // Get the range of values for normalization
  const zValues = data.map((p) => p.z);
  const zMin = 0;
  const zMax = Math.ceil(Math.max(...zValues));

  const option: echarts.EChartsOption = {
    title: {
      text: props.title,
      left: "center",
      textStyle: {
        color: textColor
      }
    },
    tooltip: {
      position: "top",
      backgroundColor: themeColors.background,
      textStyle: {
        color: textColor
      },
      formatter: function (params) {
        if (!Array.isArray(params) && params.value) {
          const [xi, yi, value] = params.value as [number, number, number];
          const x = xValues[xi];
          const y = yValues[yi];
          return `X: ${x}<br/>Y: ${y}<br/>Uncertainty: ${value.toFixed(4)}`;
        }
        return "";
      }
    },
    grid: {
      top: 40,
      bottom: 60,
      left: 60,
      right: 120,
      containLabel: true
    },
    xAxis: {
      type: "category",
      name: "X (pixels)",
      nameTextStyle: {
        color: textColor
      },
      axisLabel: {
        color: textColor
      },
      axisLine: {
        lineStyle: {
          color: textColor
        }
      },
      data: xValues.map((v) => v.toString())
    },
    yAxis: {
      type: "category",
      name: "Y (pixels)",
      nameTextStyle: {
        color: textColor
      },
      axisLabel: {
        color: textColor
      },
      axisLine: {
        lineStyle: {
          color: textColor
        }
      },
      data: yValues.map((v) => v.toString())
    },
    visualMap: {
      min: zMin,
      max: zMax,
      text: [`${zMax} px`, `${zMin} px`],
      realtime: true,
      textStyle: {
        color: textColor
      },
      formatter: (min, max) => {
        if (typeof min === "number") {
          if (typeof max === "number") {
            return `${min.toFixed(2)} - ${max.toFixed(2)} px`;
          }
          return `${min.toFixed(2)} px`;
        }
        return "";
      },
      inRange: {
        color: ["blue", "cyan", "green", "yellow", "red"]
      }
    },
    series: [
      {
        name: "Uncertainty",
        type: "heatmap",
        data: heatmapData
      }
    ]
  };

  chart.setOption(option);
};

const fetchUncertaintyData = async () => {
  isLoading.value = true;
  error.value = null;

  try {
    const response = await axios.get("/settings/camera/getUncertainty", {
      params: {
        cameraUniqueName: props.cameraUniqueName,
        width: props.resolution.width,
        height: props.resolution.height
      }
    });
    uncertaintyData.value = response.data;
  } catch (err) {
    const errorMsg = "Failed to load uncertainty data -- calibration may be too old, please recalibrate the camera. " + err.message;
    console.error("Failed to fetch uncertainty data:", err);
    error.value = errorMsg;
    chart?.clear();
  } finally {
    isLoading.value = false;
  }
};

const onWindowResize = () => {
  const container = containerRef.value;
  if (!container || !chart) return;

  // Update container height based on aspect ratio of camera resolution
  const aspectRatio = props.resolution.width / props.resolution.height;
  const containerWidth = container.clientWidth;
  const containerHeight = containerWidth / aspectRatio;
  container.style.height = `${containerHeight}px`;

  // Resize the chart
  chart.resize();
};

onMounted(async () => {
  // Fetch data
  await fetchUncertaintyData();

  const container = containerRef.value;
  if (!container) return;

  // Set container height based on aspect ratio of camera resolution
  const aspectRatio = props.resolution.width / props.resolution.height;
  const containerWidth = container.clientWidth;
  const containerHeight = containerWidth / aspectRatio;
  container.style.height = `${containerHeight}px`;

  // Initialize ECharts instance
  chart = echarts.init(container);

  // Draw initial data
  drawUncertainty(uncertaintyData.value);

  // Handle window resize
  window.addEventListener("resize", onWindowResize);
});

const cleanup = () => {
  window.removeEventListener("resize", onWindowResize);

  if (chart) {
    chart.dispose();
    chart = undefined;
  }
};

onBeforeUnmount(cleanup);

// If hot-reloading, cleanup on hot reload
if (import.meta.hot) {
  import.meta.hot.dispose(() => {
    cleanup();
  });
}

// Update chart when data changes
watch(
  () => uncertaintyData.value,
  () => {
    void drawUncertainty(uncertaintyData.value);
  }
);

watch(
  () => [
    props.cameraUniqueName,
    props.resolution.width,
    props.resolution.height,
    useCameraSettingsStore().getCalibrationCoeffs(props.resolution)
  ],
  () => {
    console.log("Camera or resolution changed, refetching calibration");
    void fetchUncertaintyData();
  }
);
</script>

<template>
  <div style="width: 100%; min-height: 400px; display: flex; justify-content: center; align-items: center;">
    <div
      v-if="error"
      style="display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; padding: 1rem; max-width: 85%;"
    >
      <v-icon color="red" size="70">mdi-close</v-icon>
      <v-card-text>{{ error }}</v-card-text>
    </div>
    <div v-else-if="isLoading" style="display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; padding: 1rem; width: 100%;">
      <v-progress-circular indeterminate size="70" width="8" color="primary" />
      <v-card-text class="pt-3">Loading uncertainty data...</v-card-text>
    </div>
    <div v-else ref="containerRef" style="width: 100%; min-height: 400px; flex: 1 1 auto;"></div>
  </div>
</template>
