@ -0,0 +1,565 @@
<script setup lang="ts">
import { inject, computed, ref, watch } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import PvSelect from "@/components/common/pv-select.vue";
import PvDeleteModal from "@/components/common/pv-delete-modal.vue";
import MetricsChart from "./MetricsChart.vue";
import { useTheme } from "vuetify";
import { axiosPost, forceReloadPage } from "@/lib/PhotonUtils";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { metricsHistorySnapshot } from "@/stores/settings/GeneralSettingsStore";

const theme = useTheme();

const restartProgram = () => {
  axiosPost("/utils/restartProgram", "restart PhotonVision");
  forceReloadPage();
};
const restartDevice = () => {
  axiosPost("/utils/restartDevice", "restart the device");
  forceReloadPage();
};

const address = inject<string>("backendHost");

const offlineUpdate = ref();
const openOfflineUpdatePrompt = () => {
  offlineUpdate.value.click();
};
const handleOfflineUpdate = async () => {
  const files = offlineUpdate.value.files;
  if (files.length === 0) return;
  const formData = new FormData();
  formData.append("jarData", files[0]);
  useStateStore().showSnackbarMessage({
    message: "New Software Upload in Progress...",
    color: "secondary",
    timeout: -1
  });
  await axiosPost("/utils/offlineUpdate", "upload new software", formData, {
    headers: { "Content-Type": "multipart/form-data" },
    onUploadProgress: ({ progress }) => {
      const uploadPercentage = (progress || 0) * 100.0;
      if (uploadPercentage < 99.5) {
        useStateStore().showSnackbarMessage({
          message: "New Software Upload in Progress",
          color: "secondary",
          timeout: -1,
          progressBar: uploadPercentage,
          progressBarColor: "primary"
        });
      } else {
        useStateStore().showSnackbarMessage({
          message: "Installing uploaded software...",
          color: "secondary",
          timeout: -1
        });
      }
    }
  });
  forceReloadPage();
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
  axiosPost(`/settings${settingsEndpoint}`, "import settings", formData, {
    headers: { "Content-Type": "multipart/form-data" }
  });
  showImportDialog.value = false;
  importType.value = undefined;
  importFile.value = null;
};

const showFactoryReset = ref(false);
const nukePhotonConfigDirectory = () => {
  axiosPost("/utils/nukeConfigDirectory", "delete the config directory");
  forceReloadPage();
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
    { header: "GPU Acceleration", value: useSettingsStore().general.gpuAcceleration || "None detected" }
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

  if (metrics.recvBitRate && metrics.recvBitRate !== -1) {
    stats.push({
      header: "Received Bit Rate",
      value: `${(metrics.recvBitRate / 1e6).toFixed(5)} Mb/s`
    });
  }

  return stats;
});

const cpuUsageData = ref<{ time: number; value: number }[]>([]);
const cpuMemoryUsageData = ref<{ time: number; value: number }[]>([]);
const cpuTempData = ref<{ time: number; value: number }[]>([]);
const networkUsageData = ref<{ time: number; value: number }[]>([]);

watch(metricsHistorySnapshot, () => {
  cpuUsageData.value = metricsHistorySnapshot.value.map((entry) => ({
    time: entry.time,
    value: entry.metrics.cpuUtil ?? 0
  }));
  cpuMemoryUsageData.value = metricsHistorySnapshot.value.map((entry) => ({
    time: entry.time,
    value: entry.metrics.ramUtil === -1 ? -1 : ((entry.metrics.ramUtil ?? 0) / (entry.metrics.ramMem ?? -1.0)) * 100
  }));
  cpuTempData.value = metricsHistorySnapshot.value.map((entry) => ({
    time: entry.time,
    value: entry.metrics.cpuTemp ?? 0
  }));
  networkUsageData.value = metricsHistorySnapshot.value.map((entry) => ({
    time: entry.time,
    value: entry.metrics.sentBitRate === -1 ? -1 : (entry.metrics.sentBitRate ?? 0) / 1e6
  }));
});
</script>

<template>
  <v-row no-gutters>
    <!-- Device control card -->
    <v-col class="pr-3">
      <v-card class="mb-3 rounded-12 fill-height d-flex flex-column justify-space-between" color="surface">
        <v-card-title class="d-flex justify-space-between">
          <span>Device Control</span>
        </v-card-title>
        <v-card-text class="flex-0-0">
          <v-table>
            <tbody>
              <tr v-for="(item, itemIndex) in generalMetrics.concat(platformMetrics)" :key="itemIndex">
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
        <v-card-text class="pt-0 flex-0-0">
          <v-row>
            <v-col>
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="useStateStore().showLogModal = true"
              >
                <v-icon start class="open-icon" size="large"> mdi-eye </v-icon>
                <span class="open-label">View Logs</span>
              </v-btn>
            </v-col>
            <v-col>
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="openExportLogsPrompt"
              >
                <v-icon start class="open-icon" size="large"> mdi-download </v-icon>
                <span class="open-label">Download Logs</span>

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
        </v-card-text>
        <v-card-text class="pt-0 flex-0-0">
          <v-row>
            <v-col>
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="() => (showImportDialog = true)"
              >
                <v-icon start class="open-icon" size="large"> mdi-import </v-icon>
                <span class="open-label">Import Settings</span>
              </v-btn>
            </v-col>
            <v-col>
              <v-btn
                color="buttonPassive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="openExportSettingsPrompt"
              >
                <v-icon start class="open-icon" size="large"> mdi-export </v-icon>
                <span class="open-label">Export Settings</span>
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text class="pt-0 flex-0-0">
          <v-row>
            <v-col cols="12" sm="6"
              ><v-btn
                color="buttonActive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="restartProgram"
              >
                <v-icon start class="open-icon" size="large"> mdi-restart </v-icon>
                <span class="open-label">Restart Software</span>
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
        </v-card-text>
        <v-card-text class="pt-0 flex-0-0">
          <v-row>
            <v-col cols="12" sm="6">
              <v-btn
                color="buttonActive"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="restartDevice"
              >
                <v-icon start class="open-icon" size="large"> mdi-restart-alert </v-icon>
                <span class="open-label">Reboot Device</span>
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

    <!-- Device metrics card -->
    <v-col>
      <v-card class="mb-3 rounded-12 fill-height d-flex flex-column justify-space-between" color="surface">
        <v-card-title class="d-flex justify-space-between">
          <span>Device Metrics</span>
        </v-card-title>
        <v-card-text class="pt-0 flex-0-0 pb-2">
          <div class="d-flex justify-space-between pb-3">
            <span>CPU Usage</span>
            <span>{{ Math.round(cpuUsageData.at(-1)?.value ?? 0) }}%</span>
          </div>
          <MetricsChart id="chart" :data="cpuUsageData" type="percentage" :min="0" :max="100" color="blue" />
        </v-card-text>
        <v-card-text class="pt-0 flex-0-0 pb-2">
          <div class="d-flex justify-space-between pb-3 pt-3">
            <span>CPU Memory Usage</span>
            <span>{{ Math.round(cpuMemoryUsageData.at(-1)?.value ?? 0) }}%</span>
          </div>
          <MetricsChart id="chart" :data="cpuMemoryUsageData" type="percentage" :min="0" :max="100" color="purple" />
        </v-card-text>
        <v-card-text class="pt-0 flex-0-0 pb-2">
          <div class="d-flex justify-space-between pb-3 pt-3">
            <span>CPU Temperature</span>
            <span>{{ cpuTempData.at(-1)?.value == -1 ? "--- " : Math.round(cpuTempData.at(-1)?.value ?? 0) }}°C</span>
          </div>
          <MetricsChart id="chart" :data="cpuTempData" type="temperature" color="red" />
        </v-card-text>
        <v-card-text class="pt-0 flex-0-0">
          <div class="d-flex justify-space-between pb-3 pt-3">
            <tooltipped-label
              label="Network Usage"
              icon="mdi-information"
              location="top"
              tooltip="Measured rate for this coprocessor ONLY. This FMS limit is for ALL robot communication. If you are experiencing bandwidth issues while under this limit, check other sources."
            />
            <span
              >{{ networkUsageData.at(-1)?.value == -1 ? "---" : networkUsageData.at(-1)?.value.toFixed(3) }} Mb/s</span
            >
          </div>
          <MetricsChart id="chart" :data="networkUsageData" type="mb" :min="0" :max="10" color="green" />
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>

  <!-- Factory reset modal -->
  <pv-delete-modal
    v-model="showFactoryReset"
    title="Factory Reset PhotonVision"
    description="This will delete all settings and configurations stored on this device, including network settings. This action cannot be undone."
    expected-confirmation-text="Delete Everything"
    :on-confirm="nukePhotonConfigDirectory"
    :on-backup="openExportSettingsPrompt"
    delete-text="Factory reset"
  />

  <!-- Import settings modal -->
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
    <v-card color="surface" dark>
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
            v-model="importFile"
            class="pb-5"
            variant="underlined"
            :disabled="importType === undefined"
            :error-messages="importType === undefined ? 'Settings type not selected' : ''"
            :accept="importType === ImportType.AllSettings ? '.zip' : '.json'"
          />
          <v-btn
            color="primary"
            :disabled="importFile === null"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="handleSettingsImport"
          >
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

<style scoped lang="scss">
.v-btn:not(.refresh) {
  width: 100%;
}
.fill-height {
  height: calc(100% - 12px) !important;
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
