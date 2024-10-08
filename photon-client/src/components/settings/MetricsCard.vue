<script setup lang="ts">
import { computed, onBeforeMount } from "vue";
import PvTooltippedIcon from "@/components/common/pv-tooltipped-icon.vue";
import { useServerStore } from "@/stores/ServerStore";

interface MetricItem {
  header: string;
  value?: string | boolean;
}

const serverStore = useServerStore();

const instanceMetrics = computed<MetricItem[]>(() => [
  {
    header: "Version",
    value: serverStore.instanceConfig?.version
  },
  {
    header: "Hardware Model",
    value: serverStore.instanceConfig?.hardwareModel
  },
  {
    header: "Platform",
    value: serverStore.instanceConfig?.hardwarePlatform
  },
  {
    header: "GPU Acceleration Supported",
    value: serverStore.instanceConfig?.gpuAccelerationSupported
  }
]);
const platformMetrics = computed<MetricItem[]>(() => {
  const stats = [
    {
      header: "CPU Temp",
      value: serverStore.platformMetrics?.cpuTemp === undefined ? "Unknown" : `${serverStore.platformMetrics?.cpuTemp}°C`
    },
    {
      header: "CPU Usage",
      value: serverStore.platformMetrics?.cpuUtil === undefined ? "Unknown" : `${serverStore.platformMetrics?.cpuUtil}%`
    },
    {
      header: "CPU Memory Usage",
      value:
        serverStore.platformMetrics?.ramUtil === undefined || serverStore.platformMetrics?.cpuMem === undefined
          ? "Unknown"
          : `${serverStore.platformMetrics?.ramUtil || "Unknown"}MB of ${serverStore.platformMetrics?.cpuMem}MB`
    },
    {
      header: "GPU Memory Usage",
      value:
        serverStore.platformMetrics?.gpuMemUtil === undefined || serverStore.platformMetrics?.gpuMem === undefined
          ? "Unknown"
          : `${serverStore.platformMetrics?.gpuMemUtil}MB of ${serverStore.platformMetrics?.gpuMem}MB`
    },
    {
      header: "CPU Throttling",
      value: serverStore.platformMetrics?.cpuThr
    },
    {
      header: "CPU Uptime",
      value: serverStore.platformMetrics?.cpuUptime
    },
    {
      header: "Disk Usage",
      value: serverStore.platformMetrics?.diskUtilPct
    }
  ];

  // Don't display NPU usage header if not possible
  if (serverStore.platformMetrics?.npuUsage) {
    stats.push({
      header: "NPU Usage",
      value: serverStore.platformMetrics?.npuUsage
    });
  }

  return stats;
});

const lastUpdatedString = computed<string>(() => {
  const dateOpt = serverStore.platformMetrics?.lastReceived;

  if (!dateOpt) return "Never";

  const pad = (num: number): string => {
    return String(num).padStart(2, "0");
  };

  return `${pad(dateOpt.getHours())}:${pad(dateOpt.getMinutes())}:${pad(dateOpt.getSeconds())}`;
});

onBeforeMount(() => {
  serverStore.publishMetrics();
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
          tooltip="Request Updated Metrics"
          @click="serverStore.publishMetrics()"
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
          <v-card-text>{{ metric.value || "Unknown" }}</v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-card-actions class="d-flex justify-end">
      <span class="pr-4">Metrics Last Fetched: {{ lastUpdatedString }}</span>
    </v-card-actions>
  </v-card>
</template>
