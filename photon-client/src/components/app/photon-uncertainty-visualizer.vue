<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, useTemplateRef, watch, type Ref } from "vue";
import type { CvPoint3 } from "@/types/SettingTypes";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useTheme } from "@/composables/useTheme";
import IconClose from "~icons/mdi/close";

const theme = useTheme();

const props = defineProps<{
  cameraUniqueName: string;
  resolution: { width: number; height: number };
  title: string;
}>();

const uncertaintyData: Ref<CvPoint3[] | null> = ref(null);
const isLoading: Ref<boolean> = ref(true);
const error: Ref<string | null> = ref(null);
const containerRef = useTemplateRef<HTMLDivElement | null>("containerRef");

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let plotly: any = null;

// Plotly can't parse CSS variables or modern color functions, so resolve theme tokens to plain rgb()
const resolveThemeColor = (cssVar: string, fallback: string): string => {
  const probe = document.createElement("span");
  probe.style.color = `var(${cssVar})`;
  document.body.appendChild(probe);
  const computed = getComputedStyle(probe).color;
  probe.remove();

  const ctx = document.createElement("canvas").getContext("2d", { willReadFrequently: true });
  if (!computed || !ctx) return fallback;
  ctx.fillStyle = computed;
  ctx.fillRect(0, 0, 1, 1);
  const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
  return `rgb(${r}, ${g}, ${b})`;
};

const getThemeTextColor = (): string => resolveThemeColor("--pv-on-surface", "#ffffff");

const getThemeSurfaceColor = (): string => resolveThemeColor("--pv-surface", theme.colors.value.surface);

const drawUncertainty = (data: CvPoint3[] | null) => {
  const container = containerRef.value;
  if (!container || !data || data.length === 0 || !plotly) return;

  const textColor = getThemeTextColor();
  const backgroundColor = getThemeSurfaceColor();

  const xValues = Array.from(new Set(data.map((p) => p.x))).sort((a, b) => a - b);
  const yValues = Array.from(new Set(data.map((p) => p.y))).sort((a, b) => a - b);
  const pointMap = new Map<string, number>();

  data.forEach((point) => {
    pointMap.set(`${point.x},${point.y}`, point.z);
  });

  const zMatrix = yValues.map((y) =>
    xValues.map((x) => {
      const value = pointMap.get(`${x},${y}`);
      return value !== undefined ? value : NaN;
    })
  );

  const zMax = 4.0; // sane max

  const trace = {
    type: "contour" as const,
    x: xValues,
    y: yValues,
    z: zMatrix,

    // gnuplot pm3d
    // Stops are at (2^i-1)/(2^n-1) for i=0..n, n=5
    colorscale: [
      [0.0, "#000000"],
      [0.03, "#2b0066"],
      [0.1, "#7f00ff"],
      [0.23, "#ff0000"],
      [0.48, "#ff7f00"],
      [1.0, "#ffff00"]
    ],

    zmin: 0.0,
    zmax: zMax,

    contours: {
      coloring: "heatmap" as const,
      showlabels: false,
      labelfont: { color: textColor },
      start: 0.0,
      end: zMax,
      size: 0.2
    },

    colorbar: {
      title: {
        text: "px",
        font: { color: textColor }
      },
      tickfont: { color: textColor },
      outlinecolor: textColor,
      bordercolor: textColor
    },
    hovertemplate: "X: %{x}<br>Y: %{y}<br>Uncertainty: %{z:.4f}<extra></extra>",
    line: {
      smoothing: 0.7,
      width: 1,
      color: textColor
    }
  };

  const layout = {
    title: {
      text: props.title,
      x: 0.5,
      font: { color: textColor }
    },
    margin: { t: 60, b: 60, l: 60, r: 120 },
    paper_bgcolor: backgroundColor,
    plot_bgcolor: backgroundColor,
    xaxis: {
      title: {
        text: "X (pixels)",
        font: { color: textColor }
      },
      showticklabels: false,
      showgrid: false,
      zeroline: false,
      color: textColor
    },
    yaxis: {
      title: {
        text: "Y (pixels)",
        font: { color: textColor }
      },
      showticklabels: false,
      showgrid: false,
      zeroline: false,
      color: textColor
    },
    font: {
      color: textColor
    },
    hoverlabel: {
      font: {
        color: textColor
      },
      bgcolor: backgroundColor
    }
  };

  const config = {
    responsive: true,
    displaylogo: false,
    modeBarButtonsToRemove: ["zoomIn2d", "zoomOut2d", "select2d", "lasso2d", "autoScale2d", "resetScale2d"]
  };

  plotly.react(container, [trace], layout, config);
};

const fetchUncertaintyData = async () => {
  isLoading.value = true;
  error.value = null;

  try {
    const response = await axios.get("settings/camera/getUncertainty", {
      params: {
        cameraUniqueName: props.cameraUniqueName,
        width: props.resolution.width,
        height: props.resolution.height
      }
    });
    uncertaintyData.value = response.data;
  } catch (err) {
    let errorMsg = "Failed to load uncertainty data";

    if (axios.isAxiosError(err)) {
      if (err.response) {
        const statusText = err.response.statusText ? ` ${err.response.statusText}` : "";
        errorMsg += `: HTTP ${err.response.status}${statusText}.`;
      } else if (err.request) {
        errorMsg += ": network error. Please check your connection and try again.";
      } else {
        errorMsg += `: ${err.message}`;
      }
    } else if (err instanceof Error) {
      errorMsg += `: ${err.message}`;
    }

    error.value = `${errorMsg} Calibration may be too old, please recalibrate the camera.`;
    console.error("Failed to fetch uncertainty data:", err);

    const container = containerRef.value;
    if (container && plotly) {
      plotly.purge(container);
    }
  } finally {
    isLoading.value = false;
  }
};

const onWindowResize = () => {
  const container = containerRef.value;
  if (!container || !plotly) return;

  const aspectRatio = props.resolution.width / props.resolution.height;
  const containerWidth = container.clientWidth;
  const containerHeight = containerWidth / aspectRatio;
  container.style.height = `${containerHeight}px`;

  plotly.Plots.resize(container);
};

onMounted(async () => {
  if (!useStateStore().backendConnected) {
    isLoading.value = false;
    return;
  }

  plotly = await import("plotly.js-dist-min");

  await fetchUncertaintyData();

  const container = containerRef.value;
  if (!container) return;

  const aspectRatio = props.resolution.width / props.resolution.height;
  const containerWidth = container.clientWidth;
  const containerHeight = containerWidth / aspectRatio;
  container.style.height = `${containerHeight}px`;

  drawUncertainty(uncertaintyData.value);

  window.addEventListener("resize", onWindowResize);
});

const cleanup = () => {
  window.removeEventListener("resize", onWindowResize);

  const container = containerRef.value;
  if (container && plotly) {
    plotly.purge(container);
  }
};

onBeforeUnmount(cleanup);

if (import.meta.hot) {
  import.meta.hot.dispose(() => {
    cleanup();
  });
}

watch([() => uncertaintyData.value, () => theme.colors.value], () => {
  void drawUncertainty(uncertaintyData.value);
});
</script>

<template>
  <div class="flex min-h-100 w-full items-center justify-center">
    <div v-if="error" class="flex max-w-[85%] flex-col items-center justify-center p-4 text-center">
      <IconClose class="text-pv-error size-[70px]" aria-hidden="true" />
      <div class="p-4">{{ error }}</div>
    </div>
    <div v-else-if="isLoading" class="flex w-full flex-col items-center justify-center p-4 text-center">
      <pv-loading class="size-[70px]" />
      <div class="p-4 pt-3">Loading uncertainty data...</div>
    </div>
    <div v-else ref="containerRef" class="min-h-100 w-full flex-auto"></div>
  </div>
</template>
