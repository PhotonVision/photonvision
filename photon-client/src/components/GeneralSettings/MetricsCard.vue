<script setup lang="ts">
import {useSettingsStore} from "@/stores/settings/GeneralSettingsStore";
import { onMounted } from "vue";
import {useStateStore} from "@/stores/StateStore";

onMounted(() => {
  // TODO should this be silent?
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
});
</script>

<template>
  <v-card
      dark
      class="mb-3 pr-6 pb-3"
      style="background-color: #006492;"
  >
    <v-card-title>Stats</v-card-title>
    <v-row class="pa-4 ml-5">
      <table id="general-metrics" class="infoTable">
        <tr>
          <th class="infoElem infoElemTitle">
            Version
          </th>
          <th class="infoElem infoElemTitle">
            Hardware Model
          </th>
          <th class="infoElem infoElemTitle">
            Platform
          </th>
          <th class="infoElem infoElemTitle">
            GPU Acceleration
          </th>
        </tr>
        <tr>
          <td class="infoElem">
            {{useSettingsStore().general.version || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().general.hardwareModel || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().general.hardwarePlatform || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().general.gpuAcceleration || "Unknown"}}
          </td>
        </tr>
      </table>
      <table id="device-metrics" class="infoTable">
        <tr>
          <th class="infoElem infoElemTitle">
            CPU Temp
          </th>
          <th class="infoElem infoElemTitle">
            CPU Usage
          </th>
          <th class="infoElem infoElemTitle">
            CPU Memory
          </th>
          <th class="infoElem infoElemTitle">
            GPU Memory
          </th>
          <th class="infoElem infoElemTitle">
            Memory Usage
          </th>
          <th class="infoElem infoElemTitle">
            GPU Mem Usage
          </th>
          <th class="infoElem infoElemTitle">
            CPU Throttling
          </th>
          <th class="infoElem infoElemTitle">
            CPU Uptime
          </th>
          <th class="infoElem infoElemTitle">
            Disk Usage
          </th>
        </tr>
        <tr>
          <td class="infoElem">
            {{useSettingsStore().metrics.cpuTemp || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().metrics.cpuUtil || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().metrics.cpuMem || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().metrics.gpuMem || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().metrics.ramUtil || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().metrics.gpuMemUtil || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().metrics.cpuThr || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().metrics.cpuUptime || "Unknown"}}
          </td>
          <td class="infoElem">
            {{useSettingsStore().metrics.diskUtilPct || "Unknown"}}
          </td>
        </tr>
      </table>
    </v-row>
  </v-card>
</template>

<style scoped>
.infoTable{
  border: 1px solid;
  border-collapse: separate;
  border-spacing: 0;
  border-radius: 5px;
  text-align: left;
  margin-bottom: 10px;
  width: 100%;
  display: block;
  overflow-x: auto;
}

.infoElem {
  padding: 1px 15px 1px 10px;
  border-right: 1px solid;
  font-weight: normal;
}

.infoElemTitle {
  font-size: 18px;
  text-decoration: underline;
  text-decoration-color: #ffd843;
}
</style>
