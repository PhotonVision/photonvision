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

  if (useSettingsStore().metrics.npuUsage) {
    stats.push({
      header: "NPU Usage",
      value: useSettingsStore().metrics.npuUsage || "Unknown"
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
  <v-card dark class="mb-3" style="background-color: #006492">
    <v-card-title class="pl-6" style="display: flex; justify-content: space-between">
      <span class="pt-2 pb-2">Stats</span>
      <v-btn text @click="fetchMetrics">
        <v-icon left class="open-icon">mdi-reload</v-icon>
        Last Fetched: {{ metricsLastFetched }}
      </v-btn>
    </v-card-title>
    <v-card-text class="pa-6 pt-0 pb-3">
      <v-card-subtitle class="pa-0" style="font-size: 16px">General Metrics</v-card-subtitle>
      <v-simple-table class="metrics-table mt-3">
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
      </v-simple-table>
    </v-card-text>
    <v-card-text class="pa-6 pt-4">
      <v-card-subtitle class="pa-0 pb-1" style="font-size: 16px">Hardware Metrics</v-card-subtitle>
      <v-simple-table class="metrics-table mt-3">
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
      </v-simple-table>
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

.v-data-table {
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
