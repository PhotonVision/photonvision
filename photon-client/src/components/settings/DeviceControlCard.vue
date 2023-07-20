<script setup lang="ts">
import { inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import CvSelect from "@/components/common/cv-select.vue";
import axios from "axios";

const restartProgram = () => {
  axios.post("/utils/restartProgram")
      .then(() => {
        useStateStore().showSnackbarMessage({
          message: "Successfully sent program restart request",
          color: "success"
        });
      })
      .catch(error => {
        // This endpoint always return 204 regardless of outcome
        if(error.request) {
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
  axios.post("/utils/restartDevice")
      .then(() => {
        useStateStore().showSnackbarMessage({
          message: "Successfully dispatched the restart command. It isn't confirmed if a device restart will occur.",
          color: "success"
        });
      })
      .catch(error => {
        if(error.response) {
          useStateStore().showSnackbarMessage({
            message: "The backend is unable to fulfil the request to restart the device.",
            color: "error"
          });
        } else if(error.request) {
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

const address = inject<string>("backendAddress");

const offlineUpdate = ref();
const openOfflineUpdatePrompt = () => {
  offlineUpdate.value.click();
};
const handleOfflineUpdate = (event) => {
  useStateStore().showSnackbarMessage({ message: "New Software Upload in Progress...", color: "secondary", timeout: -1 });

  const formData = new FormData();
  formData.append("jarData", event.target.files[0]);

  axios.post("/utils/offlineUpdate", formData, {
    headers: { "Content-Type": "multipart/form-data" },
    onUploadProgress: ({ progress }) => {
      const uploadPercentage = ((progress || 0) * 100.0);
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
      .then(response => {
        useStateStore().showSnackbarMessage({
          message:  response.data.text || response.data,
          color: "success"
        });
      })
      .catch(error => {
        if(error.response) {
          useStateStore().showSnackbarMessage({
            color: "error",
            message: error.response.data.text || error.response.data
          });
        } else if(error.request) {
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
  NetworkConfig
}
const showImportDialog = ref(false);
const importType = ref<ImportType | number>(-1);
const importFile = ref(null);
const handleSettingsImport = () => {
  if(importType.value === -1 || importFile.value === null) return;

  const formData = new FormData();
  formData.append("data", importFile.value);

  let settingsEndpoint;
  switch (importType.value) {
    case ImportType.AllSettings:
      settingsEndpoint = "";
      break;
    case ImportType.HardwareConfig:
      settingsEndpoint = "/hardwareConfig";
      break;
    case ImportType.HardwareSettings:
      settingsEndpoint = "/hardwareSettings";
      break;
    case ImportType.NetworkConfig:
      settingsEndpoint = "/networkConfig";
      break;
  }

  axios.post(`/settings${settingsEndpoint}`, formData, {
    headers: { "Content-Type": "multipart/form-data" }
  })
      .then(response => {
        useStateStore().showSnackbarMessage({
          message:  response.data.text || response.data,
          color: "success"
        });
      })
      .catch(error => {
        if(error.response) {
          useStateStore().showSnackbarMessage({
            color: "error",
            message: error.response.data.text || error.response.data
          });
        } else if(error.request) {
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
</script>

<template>
  <v-card
      dark
      class="mb-3 pr-6 pb-3"
      style="background-color: #006492;"
  >
    <v-card-title>Device Control</v-card-title>
    <div class="ml-5">
      <v-row>
        <v-col
            cols="12"
            lg="4"
            md="6"
        >
          <v-btn
              color="red"
              @click="restartProgram"
          >
            <v-icon left>
              mdi-restart
            </v-icon>
            Restart PhotonVision
          </v-btn>
        </v-col>
        <v-col
            cols="12"
            lg="4"
            md="6"
        >
          <v-btn
              color="red"
              @click="restartDevice"
          >
            <v-icon left>
              mdi-restart-alert
            </v-icon>
            Restart Device
          </v-btn>
        </v-col>
        <v-col
            cols="12"
            lg="4"
        >
          <v-btn
              color="secondary"
              @click="openOfflineUpdatePrompt"
          >
            <v-icon left>
              mdi-upload
            </v-icon>
            Offline Update
          </v-btn>
          <input
              ref="offlineUpdate"
              type="file"
              accept=".jar"
              style="display: none;"
              @change="handleOfflineUpdate"
          >
        </v-col>
      </v-row>
      <v-divider style="margin: 12px 0;"/>
      <v-row>
        <v-col
            cols="12"
            sm="6"
        >
          <v-btn
              color="secondary"
              @click="() => showImportDialog = true"
          >
            <v-icon left>
              mdi-import
            </v-icon>
            Import Settings
          </v-btn>
          <v-dialog
              v-model="showImportDialog"
              width="600"
              @input="() => {
                importType = -1;
                importFile = null;
              }"
          >
            <v-card
                color="primary"
                dark
            >
              <v-card-title>Import Settings</v-card-title>
              <v-card-text>
                Upload and apply previously saved or exported PhotonVision settings to this device
                <v-row
                    class="mt-6 ml-4"
                >
                  <cv-select
                      v-model="importType"
                      label="Type"
                      tooltip="Select the type of settings file you are trying to upload"
                      :items="['All Settings', 'Hardware Config', 'Hardware Settings', 'Network Config']"
                      :select-cols="10"
                  />
                </v-row>
                <v-row
                    class="mt-6 ml-4 mr-8"
                >
                  <v-file-input
                      :disabled="importType === -1"
                      :error-messages="importType === -1 ? 'Settings type not selected' : ''"
                      :accept="importType === ImportType.AllSettings ? '.zip' : '.json'"
                      @change="(file) => importFile = file"
                  />
                </v-row>
                <v-row
                    class="mt-12 ml-8 mr-8 mb-1"
                    style="display: flex; align-items: center; justify-content: center"
                    align="center"
                >
                  <v-btn
                      color="secondary"
                      :disabled="importFile === null"
                      @click="handleSettingsImport"
                  >
                    <v-icon left>
                      mdi-import
                    </v-icon>
                    Import Settings
                  </v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
        <v-col
            cols="12"
            sm="6"
        >
          <v-btn
              color="secondary"
              @click="openExportSettingsPrompt"
          >
            <v-icon left>
              mdi-export
            </v-icon>
            Export Settings
          </v-btn>
          <a
              ref="exportSettings"
              style="color: black; text-decoration: none; display: none"
              :href="`http://${address}/api/settings/photonvision_config.zip`"
              download="photonvision-settings.zip"
              target="_blank"
          />
        </v-col>
        <v-col
            cols="12"
            sm="6"
        >
          <v-btn
              color="secondary"
              @click="openExportLogsPrompt"
          >
            <v-icon left>
              mdi-download
            </v-icon>
            Download Current Log

            <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
            <a
                ref="exportLogFile"
                style="color: black; text-decoration: none; display: none"
                :href="'http://' + address + '/api/utils/logs/photonvision-journalctl.txt'"
                download="photonvision-journalctl.txt"
                target="_blank"
            />
          </v-btn>
        </v-col>
        <v-col
            cols="12"
            sm="6"
        >
          <v-btn
              color="secondary"
              @click="useStateStore().showLogModal = true"
          >
            <v-icon left>
              mdi-eye
            </v-icon>
            Show log viewer
          </v-btn>
        </v-col>
      </v-row>
    </div>
  </v-card>
</template>

<style scoped>
.v-divider {
  border-color: white !important;
}
.v-btn {
  width: 100%;
}
</style>
