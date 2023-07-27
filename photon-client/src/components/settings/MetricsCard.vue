<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { computed, onBeforeMount } from "vue";
import { useStateStore } from "@/stores/StateStore";
import CvIcon from "@/components/common/cv-icon.vue";

interface MetricItem {
  header: string,
  value?: string
}

const generalMetrics = computed<MetricItem[]>(() => [
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
    }]);
const platformMetrics = computed<MetricItem[]>(() => [
  {
    header: "CPU Temp",
    value: `${useSettingsStore().metrics.cpuTemp}°C`
  },
  {
    header: "CPU Usage",
    value: `${useSettingsStore().metrics.cpuUtil}%`
  },
  {
    header: "CPU Memory Usage",
    value: `${useSettingsStore().metrics.ramUtil}MB of ${useSettingsStore().metrics.cpuMem}MB`
  },
  {
    header: "GPU Memory Usage",
    value: `${useSettingsStore().metrics.gpuMemUtil}MB of ${useSettingsStore().metrics.gpuMem}MB`
  },
  {
    header: "CPU Throttling",
    value: useSettingsStore().metrics.cpuThr
  },
  {
    header: "CPU Uptime",
    value: useSettingsStore().metrics.cpuUptime
  },
  {
    header: "Disk Usage",
    value: useSettingsStore().metrics.diskUtilPct
  }
]);

const fetchMetrics = () => {
  useSettingsStore()
      .requestMetricsUpdate()
      .catch(error => {
        if(error.request) {
          useStateStore().showSnackbarMessage({
            color: "error",
            message: "Unable to fetch Metrics! The backend didn't respond."
          });
        } else {
          useStateStore().showSnackbarMessage({
            color: "error",
            message: "An error occurred while trying to fetch Metrics."
          });
        }
      });
};

onBeforeMount(() => {
  fetchMetrics();
});
</script>

<template>
  <v-card
    dark
    class="mb-3 pr-6 pb-3"
    style="background-color: #006492;"
  >
    <v-card-title style="display: flex; justify-content: space-between">
      <span>Stats</span>
      <cv-icon
        icon-name="mdi-reload"
        color="white"
        tooltip="Reload Metrics"
        hover
        @click="fetchMetrics"
      />
    </v-card-title>
    <v-row class="pa-4 ml-5">
      <div style="width: 100%">
        <v-card-subtitle class="ma-0 pa-0 pb-2">
          General Metrics
        </v-card-subtitle>
        <table
          id="general-metrics"
          class="metrics-table"
        >
          <tr>
            <th
              v-for="(item, itemIndex) in generalMetrics"
              :key="itemIndex"
              class="metric-item metric-item-title"
            >
              {{ item.header }}
            </th>
          </tr>
          <tr>
            <td
              v-for="(item, itemIndex) in generalMetrics"
              :key="itemIndex"
              class="metric-item"
            >
              {{ item.value }}
            </td>
          </tr>
        </table>
      </div>
      <div style="width: 100%">
        <v-card-subtitle class="ma-0 pa-0 pb-2">
          Hardware Metrics
        </v-card-subtitle>
        <table
          id="device-metrics"
          class="metrics-table"
        >
          <tr>
            <th
              v-for="(item, itemIndex) in platformMetrics"
              :key="itemIndex"
              class="metric-item metric-item-title"
            >
              {{ item.header }}
            </th>
          </tr>
          <tr>
            <td
              v-for="(item, itemIndex) in platformMetrics"
              :key="itemIndex"
              class="metric-item"
            >
              <span v-if="useSettingsStore().metrics.cpuUtil !== undefined">{{ item.value }}</span>
              <span v-else>---</span>
            </td>
          </tr>
        </table>
      </div>
    </v-row>
  </v-card>
</template>

<style scoped>
.metrics-table{
  border-collapse: separate;
  border-spacing: 0;
  border-radius: 5px;
  margin-bottom: 10px;
  border: 1px solid white;
  width: 100%;
  text-align: center;
}

.metric-item {
  padding: 1px 15px 1px 10px;
  border-right: 1px solid;
  font-weight: normal;
  color: white;
}

.metric-item-title {
  font-size: 18px;
  text-decoration: underline;
  text-decoration-color: #ffd843;
}
</style>
