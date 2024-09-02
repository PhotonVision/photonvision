<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { computed, onBeforeMount, ref } from "vue";
import PvTooltippedIcon from "@/components/common/pv-tooltipped-icon.vue";

interface MetricItem {
  header: string;
  value?: string;
}

const instanceMetrics = computed<MetricItem[]>(() => [
  {
    header: "Version",
    value: useSettingsStore().general.version || "Unknown"
  },
  {
    header: "Hardware Model",
    value: useSettingsStore().general.hardwareModel || "Unknown"
  },
  {
    header: "Platform",
    value: useSettingsStore().general.hardwarePlatform || "Unknown"
  },
  {
    header: "GPU Acceleration",
    value: useSettingsStore().general.gpuAcceleration || "Unknown"
  }
]);
const platformMetrics = computed<MetricItem[]>(() => {
  const stats = [
    {
      header: "CPU Temp",
      value: useSettingsStore().metrics.cpuTemp === undefined ? "Unknown" : `${useSettingsStore().metrics.cpuTemp}°C`
    },
    {
      header: "CPU Usage",
      value: useSettingsStore().metrics.cpuUtil === undefined ? "Unknown" : `${useSettingsStore().metrics.cpuUtil}%`
    },
    {
      header: "CPU Memory Usage",
      value:
        useSettingsStore().metrics.ramUtil === undefined || useSettingsStore().metrics.cpuMem === undefined
          ? "Unknown"
          : `${useSettingsStore().metrics.ramUtil || "Unknown"}MB of ${useSettingsStore().metrics.cpuMem}MB`
    },
    {
      header: "GPU Memory Usage",
      value:
        useSettingsStore().metrics.gpuMemUtil === undefined || useSettingsStore().metrics.gpuMem === undefined
          ? "Unknown"
          : `${useSettingsStore().metrics.gpuMemUtil}MB of ${useSettingsStore().metrics.gpuMem}MB`
    },
    {
      header: "CPU Throttling",
      value: useSettingsStore().metrics.cpuThr || "Unknown"
    },
    {
      header: "CPU Uptime",
      value: useSettingsStore().metrics.cpuUptime || "Unknown"
    },
    {
      header: "Disk Usage",
      value: useSettingsStore().metrics.diskUtilPct || "Unknown"
    }
  ];

  // Don't display NPU usage header if not possible
  if (useSettingsStore().metrics.npuUsage) {
    stats.push({
      header: "NPU Usage",
      value: useSettingsStore().metrics.npuUsage || "Unknown"
    });
  }

  return stats;
});

const lastUpdatedString = computed<string>(() => {
  const dateOpt = useSettingsStore().metrics.lastReceived;

  if (!dateOpt) return "Never";

  const pad = (num: number): string => {
    return String(num).padStart(2, "0");
  };

  return `${pad(dateOpt.getHours())}:${pad(dateOpt.getMinutes())}:${pad(dateOpt.getSeconds())}`;
});

const fetchingMetrics = ref(false);
const fetchMetrics = () => {
  fetchingMetrics.value = true;
  useSettingsStore()
    .requestMetricsUpdate()
    .catch((error) => {
      // TODO handle reporting HTTP errors
      // if (error.request) {
      //   useStateStore().showSnackbarMessage({
      //     color: "error",
      //     message: "Unable to fetch metrics! The backend didn't respond."
      //   });
      // } else {
      //   useStateStore().showSnackbarMessage({
      //     color: "error",
      //     message: "An error occurred while trying to fetch metrics."
      //   });
      // }
    })
    .finally(() => (fetchingMetrics.value = false));
};

onBeforeMount(() => {
  fetchMetrics();
});
</script>

<template>
  <v-card>
    <v-row class="pb-3 pt-2" no-gutters>
      <v-col>
        <v-card-title>Metrics</v-card-title>
      </v-col>
      <v-col class="d-flex align-center justify-end" cols="1">
        <pv-tooltipped-icon
          class="pr-4"
          clickable
          color="white"
          hover
          icon-name="mdi-reload"
          :loading="fetchingMetrics"
          tooltip="Request Updated Metrics"
          @click="fetchMetrics"
        />
      </v-col>
    </v-row>
    <v-row
      v-for="(metricGroup, metricGroupIndex) in [instanceMetrics, platformMetrics]"
      :key="metricGroupIndex"
      class="pl-4 pr-4 pb-4"
      no-gutters
      style="gap: 8px"
    >
      <v-col v-for="(metric, metricIndex) in metricGroup" :key="metricIndex">
        <v-card color="surface-variant">
          <v-card-title>{{ metric.header }}</v-card-title>
          <v-card-text>{{ metric.value }}</v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-card-actions class="d-flex justify-end">
      <span class="pr-4">Metrics Last Fetched: {{ lastUpdatedString }}</span>
    </v-card-actions>
  </v-card>
</template>
