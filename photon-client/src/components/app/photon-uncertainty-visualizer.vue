<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, useTemplateRef, watch, type Ref } from "vue";
import type { CvPoint3 } from "@/types/SettingTypes";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useTheme } from "vuetify";

const theme = useTheme();

const props = defineProps<{
  cameraUniqueName: string;
  resolution: { width: number; height: number };
  title: string;
}>();

const uncertaintyData: Ref<CvPoint3[] | null> = ref(null);
const isLoading: Ref<boolean> = ref(true);
const error: Ref<string | null> = ref(null);
const containerRef = useTemplateRef<HTMLDivElement | null>("container");

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let plotly: any = null;

const getThemeTextColor = (): string => {
  const styles = getComputedStyle(document.documentElement);
  const onBackground = styles.getPropertyValue("--v-theme-on-background").trim();
  const onSurface = styles.getPropertyValue("--v-theme-on-surface").trim();
  const onSurfaceVariant = styles.getPropertyValue("--v-theme-on-surface-variant").trim();
  const raw = onBackground || onSurface || onSurfaceVariant;

  if (!raw) {
    return theme.global.current.value.dark ? "#ffffff" : "#000000";
  }

  if (raw.startsWith("#") || raw.startsWith("rgb") || raw.startsWith("hsl")) {
    return raw;
  }

  return `rgb(${raw})`;
};

const getThemeSurfaceColor = (): string => {
  const styles = getComputedStyle(document.documentElement);
  const surface = styles.getPropertyValue("--v-theme-surface").trim();

  if (!surface) {
    return theme.global.current.value.colors.surface ?? (theme.global.current.value.dark ? "#1e1e1e" : "#ffffff");
  }

  if (surface.startsWith("#") || surface.startsWith("rgb") || surface.startsWith("hsl")) {
    return surface;
  }

  return `rgb(${surface})`;
};

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

  const zValues = data.map((p) => p.z);
  const zMin = 0;
  const zMax = Math.ceil(Math.max(...zValues));

  const trace = {
    type: "contour" as const,
    x: xValues,
    y: yValues,
    z: zMatrix,
    colorscale: [
      [0, "blue"],
      [0.25, "cyan"],
      [0.5, "green"],
      [0.75, "yellow"],
      [1, "red"]
    ] as [number, string][],
    contours: {
      coloring: "heatmap" as const,
      showlabels: false,
      labelfont: { color: textColor }
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
    zmin: zMin,
    zmax: zMax,
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
    const response = await axios.get("/settings/camera/getUncertainty", {
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

watch(
  () => uncertaintyData.value,
  () => {
    void drawUncertainty(uncertaintyData.value);
  }
);
</script>

<template>
  <div style="width: 100%; min-height: 400px; display: flex; justify-content: center; align-items: center">
    <div
      v-if="error"
      style="
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        text-align: center;
        padding: 1rem;
        max-width: 85%;
      "
    >
      <v-icon color="red" size="70">mdi-close</v-icon>
      <v-card-text>{{ error }}</v-card-text>
    </div>
    <div
      v-else-if="isLoading"
      style="
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        text-align: center;
        padding: 1rem;
        width: 100%;
      "
    >
      <v-progress-circular indeterminate size="70" width="8" color="primary" />
      <v-card-text class="pt-3">Loading uncertainty data...</v-card-text>
    </div>
    <div v-else ref="containerRef" style="width: 100%; min-height: 400px; flex: 1 1 auto"></div>
  </div>
</template>
