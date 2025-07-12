<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { computed, onBeforeMount, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";

interface MetricItem {
  header: string;
  value?: string;
}

const generalMetrics = computed<MetricItem[]>(() => {
  const stats = [
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
  ];

  if (!useSettingsStore().network.networkingDisabled) {
    stats.push({
      header: "IP Address",
      value: useSettingsStore().metrics.ipAddress || "Unknown"
    });
  }

  return stats;
});

const platformMetrics = computed<MetricItem[]>(() => {
  const metrics = useSettingsStore().metrics;
  const stats = [
    {
      header: "CPU Temp",
      value: metrics.cpuTemp === undefined || metrics.cpuTemp == -1 ? "Unknown" : `${metrics.cpuTemp}°C`
    },
    {
      header: "CPU Usage",
      value: metrics.cpuUtil === undefined ? "Unknown" : `${metrics.cpuUtil}%`
    },
    {
      header: "CPU Memory Usage",
      value:
        metrics.ramUtil && metrics.ramMem && metrics.ramUtil >= 0 && metrics.ramMem >= 0
          ? `${metrics.ramUtil}MB of ${metrics.ramMem}MB`
          : "Unknown"
    },
    {
      header: "CPU Throttling",
      value: metrics.cpuThr?.toString() || "Unknown"
    },
    {
      header: "Uptime",
      value: (() => {
        const seconds = metrics.uptime;
        if (seconds === undefined) return "Unknown";

        const days = Math.floor(seconds / 86400);
        const hours = Math.floor((seconds % 86400) / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = Math.floor(seconds % 60);

        var result = "";
        if (days > 0) result += `${days}d `;
        if (hours > 0) result += `${hours}h `;
        if (minutes > 0) result += `${minutes}m `;
        return (result += `${secs}s`);
      })()
    },
    {
      header: "Disk Usage",
      value: metrics.diskUtilPct === undefined ? "Unknown" : `${metrics.diskUtilPct}%`
    }
  ];

  if (metrics.npuUsage && metrics.npuUsage.length > 0) {
    stats.push({
      header: "NPU Usage",
      value: metrics.npuUsage?.map((usage, index) => `Core${index} ${usage}%`).join(", ") || "Unknown"
    });
  }

  if (metrics.gpuMem && metrics.gpuMemUtil && metrics.gpuMem > 0 && metrics.gpuMemUtil > 0) {
    stats.push({
      header: "GPU Memory Usage",
      value: `${metrics.gpuMemUtil}MB of ${metrics.gpuMem}MB`
    });
  }

  return stats;
});

const metricsLastFetched = ref("Never");
const fetchMetrics = () => {
  useSettingsStore()
    .requestMetricsUpdate()
    .catch((error) => {
      if (error.request) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "Unable to fetch metrics! The backend didn't respond."
        });
      } else {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "An error occurred while trying to fetch metrics."
        });
      }
    })
    .finally(() => {
      const pad = (num: number): string => {
        return String(num).padStart(2, "0");
      };

      const date = new Date();
      metricsLastFetched.value = `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    });
};

onBeforeMount(() => {
  fetchMetrics();
});
</script>

<template>
  <v-card class="mb-3" style="background-color: #006492">
    <v-card-title class="pl-6" style="display: flex; justify-content: space-between">
      <span class="pt-2 pb-2">Stats</span>
      <v-btn variant="text" @click="fetchMetrics">
        <v-icon start class="open-icon">mdi-reload</v-icon>
        Last Fetched: {{ metricsLastFetched }}
      </v-btn>
    </v-card-title>
    <v-card-text class="pa-6 pt-0 pb-3">
      <v-card-subtitle class="pa-0" style="font-size: 16px">General Metrics</v-card-subtitle>
      <v-table class="metrics-table mt-3">
        <thead>
          <tr>
            <th
              v-for="(item, itemIndex) in generalMetrics"
              :key="itemIndex"
              class="metric-item metric-item-title"
              :class="{
                tl: itemIndex === 0,
                tr: itemIndex === generalMetrics.length - 1,
                t: 0 < itemIndex && itemIndex < generalMetrics.length - 1
              }"
            >
              {{ item.header }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td
              v-for="(item, itemIndex) in generalMetrics"
              :key="itemIndex"
              class="metric-item"
              :class="{
                bl: itemIndex === 0,
                br: itemIndex === generalMetrics.length - 1,
                b: 0 < itemIndex && itemIndex < generalMetrics.length - 1
              }"
            >
              {{ item.value }}
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card-text>
    <v-card-text class="pa-6 pt-4">
      <v-card-subtitle class="pa-0 pb-1" style="font-size: 16px">Hardware Metrics</v-card-subtitle>
      <v-table class="metrics-table mt-3">
        <thead>
          <tr>
            <th
              v-for="(item, itemIndex) in platformMetrics"
              :key="itemIndex"
              class="metric-item metric-item-title"
              :class="{
                tl: itemIndex === 0,
                tr: itemIndex === platformMetrics.length - 1,
                t: 0 < itemIndex && itemIndex < platformMetrics.length - 1
              }"
            >
              {{ item.header }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td
              v-for="(item, itemIndex) in platformMetrics"
              :key="itemIndex"
              class="metric-item"
              :class="{
                bl: itemIndex === 0,
                br: itemIndex === platformMetrics.length - 1,
                b: 0 < itemIndex && itemIndex < platformMetrics.length - 1
              }"
            >
              <span v-if="useSettingsStore().metrics.cpuUtil !== undefined">{{ item.value }}</span>
              <span v-else>---</span>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss">
.metrics-table {
  width: 100%;
  text-align: center;
}

.t {
  border-top: 1px solid white;
  border-right: 1px solid white;
}

.b {
  border-bottom: 1px solid white;
  border-right: 1px solid white;
}

.tl {
  border-top: 1px solid white;
  border-left: 1px solid white;
  border-right: 1px solid white;
  border-top-left-radius: 5px;
}

.tr {
  border-top: 1px solid white;
  border-right: 1px solid white;
  border-top-right-radius: 5px;
}

.bl {
  border-bottom: 1px solid white;
  border-left: 1px solid white;
  border-right: 1px solid white;
  border-bottom-left-radius: 5px;
}

.br {
  border-bottom: 1px solid white;
  border-right: 1px solid white;
  border-bottom-right-radius: 5px;
}

.metric-item {
  font-size: 16px !important;
  padding: 1px 15px 1px 10px;
  border-right: 1px solid;
  font-weight: normal;
  color: white !important;
  text-align: center !important;
}

.metric-item-title {
  font-size: 18px !important;
  text-decoration: underline;
  text-decoration-color: #ffd843;
}

.v-table {
  thead,
  tbody {
    background-color: #006492;
  }

  :hover {
    tbody > tr {
      background-color: #005281 !important;
    }
  }

  ::-webkit-scrollbar {
    width: 0;
    height: 0.55em;
    border-radius: 5px;
  }

  ::-webkit-scrollbar-track {
    -webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
    border-radius: 10px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: #ffd843;
    border-radius: 10px;
  }
}
</style>
