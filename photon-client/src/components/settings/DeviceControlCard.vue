<script setup lang="ts">
import { inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import PvSelect from "@/components/common/pv-select.vue";
import axios from "axios";

const restartProgram = () => {
  axios
    .post("/utils/restartProgram")
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Successfully sent program restart request",
        color: "success"
      });
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
      useStateStore().showSnackbarMessage({
        message: response.data.text || response.data,
        color: "success"
      });
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
const importType = ref<ImportType | number>(-1);
const importFile = ref<File | null>(null);
const handleSettingsImport = () => {
  if (importType.value === -1 || importFile.value === null) return;

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
    .post(`/settings${settingsEndpoint}`, formData, {
      headers: { "Content-Type": "multipart/form-data" }
    })
    .then((response) => {
      useStateStore().showSnackbarMessage({
        message: response.data.text || response.data,
        color: "success"
      });
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
  importType.value = -1;
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
          message: "The backend is unable to fulfil the request to reset the device.",
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
</script>

<template>
  <v-card dark class="mb-3" style="background-color: #006492">
    <v-card-title class="pa-6">Device Control</v-card-title>
    <div class="pa-6 pt-0">
      <v-row>
        <v-col cols="12" lg="4" md="6">
          <v-btn color="error" @click="restartProgram">
            <v-icon left class="open-icon"> mdi-restart </v-icon>
            <span class="open-label">Restart PhotonVision</span>
          </v-btn>
        </v-col>
        <v-col cols="12" lg="4" md="6">
          <v-btn color="error" @click="restartDevice">
            <v-icon left class="open-icon"> mdi-restart-alert </v-icon>
            <span class="open-label">Restart Device</span>
          </v-btn>
        </v-col>
        <v-col cols="12" lg="4">
          <v-btn color="secondary" @click="openOfflineUpdatePrompt">
            <v-icon left class="open-icon"> mdi-upload </v-icon>
            <span class="open-label">Offline Update</span>
          </v-btn>
          <input ref="offlineUpdate" type="file" accept=".jar" style="display: none" @change="handleOfflineUpdate" />
        </v-col>
      </v-row>
      <v-divider class="mt-3 pb-3" />
      <v-row>
        <v-col cols="12" sm="6">
          <v-btn color="secondary" @click="() => (showImportDialog = true)">
            <v-icon left class="open-icon"> mdi-import </v-icon>
            <span class="open-label">Import Settings</span>
          </v-btn>
          <v-dialog
            v-model="showImportDialog"
            width="600"
            @input="
              () => {
                importType = -1;
                importFile = null;
              }
            "
          >
            <v-card color="primary" dark>
              <v-card-title>Import Settings</v-card-title>
              <v-card-text>
                Upload and apply previously saved or exported PhotonVision settings to this device
                <v-row class="mt-6 ml-4">
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
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-file-input
                    v-model="importFile"
                    :disabled="importType === -1"
                    :error-messages="importType === -1 ? 'Settings type not selected' : ''"
                    :accept="importType === ImportType.AllSettings ? '.zip' : '.json'"
                  />
                </v-row>
                <v-row
                  class="mt-12 ml-8 mr-8 mb-1"
                  style="display: flex; align-items: center; justify-content: center"
                  align="center"
                >
                  <v-btn color="secondary" :disabled="importFile === null" @click="handleSettingsImport">
                    <v-icon left class="open-icon"> mdi-import </v-icon>
                    <span class="open-label">Import Settings</span>
                  </v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
        <v-col cols="12" sm="6">
          <v-btn color="secondary" @click="openExportSettingsPrompt">
            <v-icon left class="open-icon"> mdi-export </v-icon>
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
          <v-btn color="secondary" @click="openExportLogsPrompt">
            <v-icon left class="open-icon"> mdi-download </v-icon>
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
          <v-btn color="secondary" @click="useStateStore().showLogModal = true">
            <v-icon left class="open-icon"> mdi-eye </v-icon>
            <span class="open-label">View program logs</span>
          </v-btn>
        </v-col>
      </v-row>
      <v-divider class="mt-3 pb-3" />
      <v-row>
        <v-col cols="12">
          <v-btn color="error" @click="() => (showFactoryReset = true)">
            <v-icon left class="open-icon"> mdi-skull-crossbones </v-icon>
            <span class="open-icon">
              {{
                $vuetify.breakpoint.mdAndUp
                  ? "Factory Reset PhotonVision and delete EVERYTHING"
                  : "Factory Reset PhotonVision"
              }}
            </span>
          </v-btn>
        </v-col>
      </v-row>
    </div>

    <v-dialog v-model="showFactoryReset" width="800" dark>
      <v-card dark color="primary" class="pa-3" flat>
        <v-card-title style="justify-content: center" class="pb-6">
          <span class="open-label">
            <v-icon right color="error" class="open-icon ma-1">mdi-nuke</v-icon>
            Factory Reset PhotonVision
            <v-icon right color="error" class="open-icon ma-1">mdi-nuke</v-icon>
          </span>
        </v-card-title>
        <v-card-text class="pt-3">
          <v-row class="align-center white--text">
            <v-col cols="12" md="6">
              <span class="mt-3"> This will delete ALL OF YOUR SETTINGS and restart PhotonVision. </span>
            </v-col>
            <v-col cols="12" md="6">
              <v-btn color="secondary" style="float: right" @click="openExportSettingsPrompt">
                <v-icon left class="open-icon"> mdi-export </v-icon>
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
        <v-card-text>
          <pv-input
            v-model="yesDeleteMySettingsText"
            :label="'Type &quot;' + expected + '&quot;:'"
            :label-cols="6"
            :input-cols="6"
          />
        </v-card-text>
        <v-card-text>
          <v-btn
            color="error"
            :disabled="yesDeleteMySettingsText.toLowerCase() !== expected.toLowerCase()"
            @click="nukePhotonConfigDirectory"
          >
            <v-icon left class="open-icon"> mdi-trash-can-outline </v-icon>
            <span class="open-label">
              {{
                $vuetify.breakpoint.mdAndUp ? "Delete everything, I have backed up what I need" : "Delete Everything"
              }}
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
