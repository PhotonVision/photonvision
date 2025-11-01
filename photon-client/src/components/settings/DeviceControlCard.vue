<script setup lang="ts">
import { inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import PvSelect from "@/components/common/pv-select.vue";
import PvInput from "@/components/common/pv-input.vue";
import { useTheme } from "vuetify";
import { axiosPost } from "@/lib/PhotonUtils";

const theme = useTheme();

const restartProgram = () => {
  axiosPost("/utils/restartProgram", "restart PhotonVision");
};
const restartDevice = () => {
  axiosPost("/utils/restartDevice", "restart the device");
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

  axiosPost("/utils/offlineUpdate", "upload new software", formData, {
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
const expected = "Delete Everything";
const yesDeleteMySettingsText = ref("");
const nukePhotonConfigDirectory = () => {
  axiosPost("/utils/nukeConfigDirectory", "delete the config directory");

  showFactoryReset.value = false;
};
</script>

<template>
  <v-card class="mb-3 rounded-12" color="surface">
    <v-card-title>Device Control</v-card-title>
    <div class="pa-5 pt-0">
      <v-row>
        <v-col cols="12" lg="4" md="6">
          <v-btn
            color="buttonActive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="restartProgram"
          >
            <v-icon start class="open-icon" size="large"> mdi-restart </v-icon>
            <span class="open-label">Restart PhotonVision</span>
          </v-btn>
        </v-col>
        <v-col cols="12" lg="4" md="6">
          <v-btn
            color="buttonActive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="restartDevice"
          >
            <v-icon start class="open-icon" size="large"> mdi-restart-alert </v-icon>
            <span class="open-label">Restart Device</span>
          </v-btn>
        </v-col>
        <v-col cols="12" lg="4">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="openOfflineUpdatePrompt"
          >
            <v-icon start class="open-icon" size="large"> mdi-upload </v-icon>
            <span class="open-label">Offline Update</span>
          </v-btn>
          <input ref="offlineUpdate" type="file" accept=".jar" style="display: none" @change="handleOfflineUpdate" />
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12" sm="6">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="() => (showImportDialog = true)"
          >
            <v-icon start class="open-icon" size="large"> mdi-import </v-icon>
            <span class="open-label">Import Settings</span>
          </v-btn>
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
                    :items="[
                      'All Settings',
                      'Hardware Config',
                      'Hardware Settings',
                      'Network Config',
                      'Apriltag Layout'
                    ]"
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
        </v-col>
        <v-col cols="12" sm="6">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="openExportSettingsPrompt"
          >
            <v-icon start class="open-icon" size="large"> mdi-export </v-icon>
            <span class="open-label">Export Settings</span>
          </v-btn>
          <a
            ref="exportSettings"
            style="color: black; text-decoration: none; display: none"
            :href="`http://${address}/api/settings/photonvision_config.zip`"
            download="photonvision-settings.zip"
            target="_blank"
          />
        </v-col>
        <v-col cols="12" sm="6">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="openExportLogsPrompt"
          >
            <v-icon start class="open-icon" size="large"> mdi-download </v-icon>
            <span class="open-label">Download logs</span>

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
        <v-col cols="12" sm="6">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="useStateStore().showLogModal = true"
          >
            <v-icon start class="open-icon" size="large"> mdi-eye </v-icon>
            <span class="open-label">View logs</span>
          </v-btn>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-btn
            color="error"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="() => (showFactoryReset = true)"
          >
            <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
            <span class="open-icon"> Factory Reset PhotonVision </span>
          </v-btn>
        </v-col>
      </v-row>
    </div>
    <v-dialog v-model="showFactoryReset" width="800" dark>
      <v-card color="surface" flat>
        <v-card-title style="display: flex; justify-content: center">
          <span class="open-label">
            <v-icon end color="red" class="open-icon ma-1" size="large">mdi-alert-outline</v-icon>
            Factory Reset PhotonVision
            <v-icon end color="red" class="open-icon ma-1" size="large">mdi-alert-outline</v-icon>
          </span>
        </v-card-title>
        <v-card-text class="pt-0 pb-10px">
          <v-row class="align-center text-white">
            <v-col cols="12" md="6">
              <span> This will delete ALL OF YOUR SETTINGS and restart PhotonVision. </span>
            </v-col>
            <v-col cols="12" md="6">
              <v-btn
                color="primary"
                style="float: right"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="openExportSettingsPrompt"
              >
                <v-icon start class="open-icon" size="large"> mdi-export </v-icon>
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
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            :disabled="yesDeleteMySettingsText.toLowerCase() !== expected.toLowerCase()"
            @click="nukePhotonConfigDirectory"
          >
            <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
            <span class="open-label">
              {{ $vuetify.display.mdAndUp ? "Delete everything, I have backed up what I need" : "Delete Everything" }}
            </span>
          </v-btn>
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<style scoped>
.v-btn {
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
