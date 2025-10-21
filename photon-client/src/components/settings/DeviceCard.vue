@ -0,0 +1,565 @@
<!-- WORK IN PROGRESS -->

<script setup lang="ts">
import { inject, computed, onBeforeMount, ref, onMounted, watch } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import PvSelect from "@/components/common/pv-select.vue";
import PvInput from "@/components/common/pv-input.vue";
import axios from "axios";
import Chart from "./Chart.vue";
import { useTheme } from "vuetify";

const theme = useTheme();

const restartProgram = () => {
  axios
    .post("/utils/restartProgram")
    .then(() => {
      useStateStore().showSnackbarMessage({ message: "Successfully sent program restart request", color: "success" });
    })
    .catch((error) => {
      // This endpoint always return 204 regardless of outcome
      if (error.request) {
        useStateStore().showSnackbarMessage({
          message: "Error while trying to process the request! The backend didn't respond.",
          color: "error"
        });
      } else {
        useStateStore().showSnackbarMessage({
          message: "An error occurred while trying to process the request.",
          color: "error"
        });
      }
    });
};
const restartDevice = () => {
  axios
    .post("/utils/restartDevice")
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Successfully dispatched the restart command. It isn't confirmed if a device restart will occur.",
        color: "success"
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          message: "The backend is unable to fulfil the request to restart the device.",
          color: "error"
        });
      } else if (error.request) {
        useStateStore().showSnackbarMessage({
          message: "Error while trying to process the request! The backend didn't respond.",
          color: "error"
        });
      } else {
        useStateStore().showSnackbarMessage({
          message: "An error occurred while trying to process the request.",
          color: "error"
        });
      }
    });
};

const address = inject<string>("backendHost");

const offlineUpdate = ref();
const openOfflineUpdatePrompt = () => {
  offlineUpdate.value.click();
};
const handleOfflineUpdate = () => {
  const files = offlineUpdate.value.files;
  if (files.length === 0) return;

  const formData = new FormData();
  formData.append("jarData", files[0]);

  useStateStore().showSnackbarMessage({
    message: "New Software Upload in Progress...",
    color: "secondary",
    timeout: -1
  });

  axios
    .post("/utils/offlineUpdate", formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: ({ progress }) => {
        const uploadPercentage = (progress || 0) * 100.0;
        if (uploadPercentage < 99.5) {
          useStateStore().showSnackbarMessage({
            message: "New Software Upload in Process, " + uploadPercentage.toFixed(2) + "% complete",
            color: "secondary",
            timeout: -1
          });
        } else {
          useStateStore().showSnackbarMessage({
            message: "Installing uploaded software...",
            color: "secondary",
            timeout: -1
          });
        }
      }
    })
    .then((response) => {
      useStateStore().showSnackbarMessage({ message: response.data.text || response.data, color: "success" });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: error.response.data.text || error.response.data
        });
      } else if (error.request) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "Error while trying to process the request! The backend didn't respond."
        });
      } else {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "An error occurred while trying to process the request."
        });
      }
    });
};

const exportLogFile = ref();
const openExportLogsPrompt = () => {
  exportLogFile.value.click();
};

const exportSettings = ref();
const openExportSettingsPrompt = () => {
  exportSettings.value.click();
};

enum ImportType {
  AllSettings,
  HardwareConfig,
  HardwareSettings,
  NetworkConfig,
  ApriltagFieldLayout
}
const showImportDialog = ref(false);
const importType = ref<ImportType | undefined>(undefined);
const importFile = ref<File | null>(null);
const handleSettingsImport = () => {
  if (importType.value === undefined || importFile.value === null) return;

  const formData = new FormData();
  formData.append("data", importFile.value);

  let settingsEndpoint: string;
  switch (importType.value) {
    case ImportType.HardwareConfig:
      settingsEndpoint = "/hardwareConfig";
      break;
    case ImportType.HardwareSettings:
      settingsEndpoint = "/hardwareSettings";
      break;
    case ImportType.NetworkConfig:
      settingsEndpoint = "/networkConfig";
      break;
    case ImportType.ApriltagFieldLayout:
      settingsEndpoint = "/aprilTagFieldLayout";
      break;
    default:
    case ImportType.AllSettings:
      settingsEndpoint = "";
      break;
  }

  axios
    .post(`/settings${settingsEndpoint}`, formData, { headers: { "Content-Type": "multipart/form-data" } })
    .then((response) => {
      useStateStore().showSnackbarMessage({ message: response.data.text || response.data, color: "success" });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: error.response.data.text || error.response.data
        });
      } else if (error.request) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "Error while trying to process the request! The backend didn't respond."
        });
      } else {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "An error occurred while trying to process the request."
        });
      }
    });

  showImportDialog.value = false;
  importType.value = undefined;
  importFile.value = null;
};

const showFactoryReset = ref(false);
const expected = "Delete Everything";
const yesDeleteMySettingsText = ref("");
const nukePhotonConfigDirectory = () => {
  axios
    .post("/utils/nukeConfigDirectory")
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Successfully dispatched the reset command. Waiting for backend to start back up",
        color: "success"
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          message: "The backend is unable to fulfill the request to reset the device.",
          color: "error"
        });
      } else if (error.request) {
        useStateStore().showSnackbarMessage({
          message: "Error while trying to process the request! The backend didn't respond.",
          color: "error"
        });
      } else {
        useStateStore().showSnackbarMessage({
          message: "An error occurred while trying to process the request.",
          color: "error"
        });
      }
    });
  showFactoryReset.value = false;
};

interface MetricItem {
  header: string;
  value?: string;
}

const generalMetrics = computed<MetricItem[]>(() => {
  const stats = [
    { header: "Version", value: useSettingsStore().general.version || "Unknown" },
    { header: "Hardware Model", value: useSettingsStore().general.hardwareModel || "Unknown" },
    { header: "Platform", value: useSettingsStore().general.hardwarePlatform || "Unknown" },

    { header: "GPU Acceleration", value: useSettingsStore().general.gpuAcceleration || "Unknown" }
  ];

  if (!useSettingsStore().network.networkingDisabled) {
    stats.push({ header: "IP Address", value: useSettingsStore().metrics.ipAddress || "Unknown" });
  }

  return stats;
});

// @ts-expect-error This uses Intl.DurationFormat which is newly implemented and not available in TS.
const durationFormatter = new Intl.DurationFormat("en", { style: "narrow" });
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
      header: "Uptime",
      value: (() => {
        const seconds = metrics.uptime;
        if (seconds === undefined) return "Unknown";

        const days = Math.floor(seconds / 86400);
        const hours = Math.floor((seconds % 86400) / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = Math.floor(seconds % 60);

        return durationFormatter.format({
          days: days,
          hours: hours,
          minutes: minutes,
          seconds: secs
        });
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

  if (metrics.cpuThr) {
    stats.push({
      header: "CPU Throttling",
      value: metrics.cpuThr.toString()
    });
  }

  return stats;
});

// const metricsLastFetched = ref("Never");
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
      // const pad = (num: number): string => {
      //   return String(num).padStart(2, "0");
      // };
      // const date = new Date();
      // metricsLastFetched.value = `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    });
};

onBeforeMount(() => {
  fetchMetrics();
});

const graphedMetrics = ["CPU Usage", "CPU Memory Usage", "Disk Usage", "GPU Memory Usage", "CPU Temp"];

const cpuUsageData = ref<{ time: number; value: number }[]>([]);
const cpuMemoryUsageData = ref<{ time: number; value: number }[]>([]);
const diskUsageData = ref<{ time: number; value: number }[]>([]);
const cpuTempData = ref<{ time: number; value: number }[]>([]);
const gpuMemoryUsageData = ref<{ time: number; value: number }[]>([]);

watch(useSettingsStore().metricsHistory, () => {
  cpuUsageData.value = useSettingsStore().metricsHistory.map((entry) => ({
    time: entry.time,
    value: entry.metrics.cpuUtil ?? 0
  }));
  cpuMemoryUsageData.value = useSettingsStore().metricsHistory.map((entry) => ({
    time: entry.time,
    value: entry.metrics.ramUtil ?? 0
  }));
  diskUsageData.value = useSettingsStore().metricsHistory.map((entry) => ({
    time: entry.time,
    value: entry.metrics.diskUtilPct ?? 0
  }));
  cpuTempData.value = useSettingsStore().metricsHistory.map((entry) => ({
    time: entry.time,
    value: entry.metrics.cpuTemp ?? 0
  }));
  gpuMemoryUsageData.value = useSettingsStore().metricsHistory.map((entry) => ({
    time: entry.time,
    value: entry.metrics.gpuMemUtil ?? 0
  }));
});
</script>

<template>
  <v-row no-gutters>
    <v-col class="pr-3">
      <v-card class="mb-3 rounded-12" color="surface">
        <v-card-title style="display: flex; justify-content: space-between">
          <span>PhotonVision</span>
        </v-card-title>
        <v-card-text>
          <v-table density="compact">
            <tbody>
              <tr
                v-for="(item, itemIndex) in generalMetrics.concat(
                  platformMetrics.filter((item) => !graphedMetrics.includes(item.header))
                )"
              >
                <td :key="itemIndex">
                  {{ item.header }}
                </td>
                <td :key="itemIndex">
                  {{ item.value }}
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-card-text>
        <v-card-text>
          <v-row>
            <v-col cols="3">Program Logs</v-col>
            <v-col>
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="useStateStore().showLogModal = true"
              >
                <v-icon start class="open-icon" size="large"> mdi-eye </v-icon>
                <span class="open-label">View</span>
              </v-btn>
            </v-col>
            <v-col>
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="openExportLogsPrompt"
              >
                <v-icon start class="open-icon" size="large"> mdi-download </v-icon>
                <span class="open-label">Download</span>

                <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
                <a
                  ref="exportLogFile"
                  style="color: black; text-decoration: none; display: none"
                  :href="`http://${address}/api/utils/photonvision-journalctl.txt`"
                  download="photonvision-journalctl.txt"
                  target="_blank"
                />
              </v-btn>
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="3">Settings</v-col>
            <v-col>
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="() => (showImportDialog = true)"
              >
                <v-icon start class="open-icon" size="large"> mdi-import </v-icon>
                <span class="open-label">Import</span>
              </v-btn>
            </v-col>
            <v-col>
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="openExportSettingsPrompt"
              >
                <v-icon start class="open-icon" size="large"> mdi-export </v-icon>
                <span class="open-label">Export</span>
              </v-btn>
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="12" sm="6"
              ><v-btn
                color="buttonActive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="restartProgram"
              >
                <v-icon start class="open-icon" size="large"> mdi-restart </v-icon>
                <span class="open-label">Restart PhotonVision</span>
              </v-btn>
            </v-col>
            <v-col cols="12" sm="6">
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="openOfflineUpdatePrompt"
              >
                <v-icon start class="open-icon" size="large"> mdi-upload </v-icon>
                <span class="open-label">Offline Update</span>
              </v-btn>
              <input
                ref="offlineUpdate"
                type="file"
                accept=".jar"
                style="display: none"
                @change="handleOfflineUpdate"
              />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="12" sm="6">
              <v-btn
                color="buttonActive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="restartDevice"
              >
                <v-icon start class="open-icon" size="large"> mdi-restart-alert </v-icon>
                <span class="open-label">Restart Device</span>
              </v-btn>
            </v-col>
            <v-col cols="12" sm="6">
              <v-btn
                color="error"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="() => (showFactoryReset = true)"
              >
                <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
                <span class="open-icon"> Factory Reset </span>
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </v-col>

    <v-col>
      <v-card class="mb-3 rounded-12" color="surface">
        <v-card-title style="display: flex; justify-content: space-between">
          <span>Device Metrics</span>
          <v-btn variant="text" @click="fetchMetrics" class="refresh">
            <v-icon start class="open-icon">mdi-reload</v-icon>
            Force Refresh
          </v-btn>
        </v-card-title>
        <v-card-text class="pt-0">
          <div class="d-flex justify-space-between pb-3">
            <span>CPU Usage</span>
            <span>{{ (cpuUsageData.at(-1)?.value ?? 0) | 0 }}%</span>
          </div>
          <Chart :data="cpuUsageData" id="chart" />
          <div class="d-flex justify-space-between pb-3 pt-3">
            <span>CPU Memory Usage</span>
            <span>{{ (cpuMemoryUsageData.at(-1)?.value ?? 0) | 0 }}%</span>
          </div>
          <Chart :data="cpuMemoryUsageData" :color="{ r: 154, g: 96, b: 180 }" id="chart" />
          <div class="d-flex justify-space-between pb-3 pt-3">
            <span>CPU Temperature</span>
            <span>{{ (cpuTempData.at(-1)?.value ?? 0) | 0 }}%</span>
          </div>
          <Chart :data="cpuTempData" :color="{ r: 238, g: 102, b: 102 }" id="chart" />
          <div class="d-flex justify-space-between pb-3 pt-3">
            <span>Disk Usage</span>
            <span>{{ (diskUsageData.at(-1)?.value ?? 0) | 0 }}%</span>
          </div>
          <Chart :data="diskUsageData" :color="{ r: 65, g: 181, b: 127 }" id="chart" />
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>

  <v-dialog v-model="showFactoryReset" width="800" dark>
    <v-card color="primary" flat>
      <v-card-title style="display: flex; justify-content: center">
        <span class="open-label">
          <v-icon end color="error" class="open-icon ma-1">mdi-nuke</v-icon>
          Factory Reset PhotonVision
          <v-icon end color="error" class="open-icon ma-1">mdi-nuke</v-icon>
        </span>
      </v-card-title>
      <v-card-text class="pt-0 pb-10px">
        <v-row class="align-center text-white">
          <v-col cols="12" md="6">
            <span> This will delete ALL OF YOUR SETTINGS and restart PhotonVision. </span>
          </v-col>
          <v-col cols="12" md="6">
            <v-btn color="secondary" style="float: right" @click="openExportSettingsPrompt">
              <v-icon start class="open-icon"> mdi-export </v-icon>
              <span class="open-label">Backup Settings</span>
              <a
                ref="exportSettings"
                style="color: black; text-decoration: none; display: none"
                :href="`http://${address}/api/settings/photonvision_config.zip`"
                download="photonvision-settings.zip"
                target="_blank"
              />
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-text class="pt-0 pb-0">
        <pv-input
          v-model="yesDeleteMySettingsText"
          :label="'Type &quot;' + expected + '&quot;:'"
          :label-cols="6"
          :input-cols="6"
        />
      </v-card-text>
      <v-card-text class="pt-10px">
        <v-btn
          color="error"
          :disabled="yesDeleteMySettingsText.toLowerCase() !== expected.toLowerCase()"
          @click="nukePhotonConfigDirectory"
        >
          <v-icon start class="open-icon"> mdi-trash-can-outline </v-icon>
          <span class="open-label">
            {{ $vuetify.display.mdAndUp ? "Delete everything, I have backed up what I need" : "Delete Everything" }}
          </span>
        </v-btn>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog
    v-model="showImportDialog"
    width="600"
    @update:modelValue="
      () => {
        importType = undefined;
        importFile = null;
      }
    "
  >
    <v-card color="primary" dark>
      <v-card-title class="pb-0">Import Settings</v-card-title>
      <v-card-text>
        Upload and apply previously saved or exported PhotonVision settings to this device
        <div class="pa-5 pb-0">
          <pv-select
            v-model="importType"
            label="Type"
            tooltip="Select the type of settings file you are trying to upload"
            :items="['All Settings', 'Hardware Config', 'Hardware Settings', 'Network Config', 'Apriltag Layout']"
            :select-cols="10"
            style="width: 100%"
          />
          <v-file-input
            class="pb-5"
            v-model="importFile"
            variant="underlined"
            :disabled="importType === undefined"
            :error-messages="importType === undefined ? 'Settings type not selected' : ''"
            :accept="importType === ImportType.AllSettings ? '.zip' : '.json'"
          />
          <v-btn color="accent" :disabled="importFile === null" @click="handleSettingsImport">
            <v-icon start class="open-icon"> mdi-import </v-icon>
            <span class="open-label">Import Settings</span>
          </v-btn>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>

  <a
    ref="exportSettings"
    style="color: black; text-decoration: none; display: none"
    :href="`http://${address}/api/settings/photonvision_config.zip`"
    download="photonvision-settings.zip"
    target="_blank"
  />
</template>

<style scoped>
.v-btn:not(.refresh) {
  width: 100%;
}
@media only screen and (max-width: 351px) {
  .open-icon {
    margin: 0 !important;
  }
  .open-label {
    display: none;
  }
}
</style>
