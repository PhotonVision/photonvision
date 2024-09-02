<script setup lang="ts">
import { inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
// import PvSelect from "@/components/common/pv-select.vue";
import axios from "axios";
import OfflineUpdateModal from "@/components/modals/OfflineUpdateModal.vue";
import SettingsImportModal from "@/components/modals/SettingsImportModal.vue";

const address = inject<string>("backendHost");

// TODO:
// when you click either restart program or device, force the websocket to disconnect and then wait a set period, then restart the websocket connection

const restartProgram = () => {
  axios
    .post("/utils/restartProgram")
    .then(() => {
      // TODO handle this
      // useStateStore().showSnackbarMessage({
      //   message: "Successfully sent program restart request",
      //   color: "success"
      // });
    })
    .catch((error) => {
      // TODO handle this
      // // This endpoint always return 204 regardless of outcome
      // if (error.request) {
      //   useStateStore().showSnackbarMessage({
      //     message: "Error while trying to process the request! The backend didn't respond.",
      //     color: "error"
      //   });
      // } else {
      //   useStateStore().showSnackbarMessage({
      //     message: "An error occurred while trying to process the request.",
      //     color: "error"
      //   });
      // }
    });
};
const restartDevice = () => {
  axios
    .post("/utils/restartDevice")
    .then(() => {
      // TODO handle this
      // useStateStore().showSnackbarMessage({
      //   message: "Successfully dispatched the restart command. It isn't confirmed if a device restart will occur.",
      //   color: "success"
      // });
    })
    .catch((error) => {
      // TODO handle this
      // if (error.response) {
      //   useStateStore().showSnackbarMessage({
      //     message: "The backend is unable to fulfil the request to restart the device.",
      //     color: "error"
      //   });
      // } else if (error.request) {
      //   useStateStore().showSnackbarMessage({
      //     message: "Error while trying to process the request! The backend didn't respond.",
      //     color: "error"
      //   });
      // } else {
      //   useStateStore().showSnackbarMessage({
      //     message: "An error occurred while trying to process the request.",
      //     color: "error"
      //   });
      // }
    });
};

const exportLogFile = ref();
const exportSettings = ref();

// enum ImportType {
//   AllSettings,
//   HardwareConfig,
//   HardwareSettings,
//   NetworkConfig,
//   ApriltagFieldLayout
// }
// const showImportDialog = ref(false);
// const importType = ref<ImportType | number>(-1);
// const importFile = ref<File | null>(null);
// const handleSettingsImport = () => {
//   if (importType.value === -1 || importFile.value === null) return;
//
//   const formData = new FormData();
//   formData.append("data", importFile.value);
//
//   let settingsEndpoint: string;
//   switch (importType.value) {
//     case ImportType.HardwareConfig:
//       settingsEndpoint = "/hardwareConfig";
//       break;
//     case ImportType.HardwareSettings:
//       settingsEndpoint = "/hardwareSettings";
//       break;
//     case ImportType.NetworkConfig:
//       settingsEndpoint = "/networkConfig";
//       break;
//     case ImportType.ApriltagFieldLayout:
//       settingsEndpoint = "/aprilTagFieldLayout";
//       break;
//     default:
//     case ImportType.AllSettings:
//       settingsEndpoint = "";
//       break;
//   }
//
//   axios
//     .post(`/settings${settingsEndpoint}`, formData, {
//       headers: { "Content-Type": "multipart/form-data" }
//     })
//     .then((response) => {
//       useStateStore().showSnackbarMessage({
//         message: response.data.text || response.data,
//         color: "success"
//       });
//     })
//     .catch((error) => {
//       if (error.response) {
//         useStateStore().showSnackbarMessage({
//           color: "error",
//           message: error.response.data.text || error.response.data
//         });
//       } else if (error.request) {
//         useStateStore().showSnackbarMessage({
//           color: "error",
//           message: "Error while trying to process the request! The backend didn't respond."
//         });
//       } else {
//         useStateStore().showSnackbarMessage({
//           color: "error",
//           message: "An error occurred while trying to process the request."
//         });
//       }
//     });
//
//   showImportDialog.value = false;
//   importType.value = -1;
//   importFile.value = null;
// };
</script>

<template>
  <v-card>
    <v-card-title class="mb-3 mt-2">Device Control</v-card-title>
    <v-row class="pl-4 pr-4" no-gutters>
      <v-col class="pb-3 pb-md-0 pr-md-1" cols="12" md="4">
        <v-btn
          class="w-100"
          color="red"
          prepend-icon="mdi-restart"
          text="Restart PhotonVision"
          @click="restartProgram"
        />
      </v-col>
      <v-col class="pb-3 pb-md-0 pr-md-1 pl-md-1" cols="12" md="4">
        <v-btn
          class="w-100"
          color="red"
          prepend-icon="mdi-restart-alert"
          text="Restart Device"
          @click="restartDevice"
        />
      </v-col>
      <v-col class="pb-3 pb-md-0 pl-md-1" cols="12" md="4">
        <OfflineUpdateModal>
          <template #activator="{ props }">
            <v-btn
              class="w-100"
              color="secondary"
              prepend-icon="mdi-upload"
              text="Offline Update"
              v-bind="props"
            />
          </template>
        </OfflineUpdateModal>
      </v-col>
    </v-row>
    <v-divider class="mt-2 mb-2 ml-4 mr-4" />
    <v-row class="pl-4 pr-4 pt-2 mb-4" no-gutters>
      <v-col class="pb-3 pb-sm-0 pr-sm-1 pb-sm-1" cols="12" sm="6">
        <SettingsImportModal>
          <template #activator="{ props }">
            <v-btn
              class="w-100"
              color="secondary"
              prepend-icon="mdi-import"
              text="Import Settings"
              v-bind="props"
            />
          </template>
        </SettingsImportModal>
      </v-col>
      <v-col class="pb-3 pb-sm-0 pl-sm-1 pb-sm-1" cols="12" sm="6">
        <v-btn
          class="w-100"
          color="secondary"
          prepend-icon="mdi-export"
          text="Export Settings"
          @click="exportSettings.click()"
        />
        <a
          ref="exportSettings"
          class="d-none"
          download="photonvision-settings.zip"
          :href="`http://${address}/api/settings/photonvision_config.zip`"
          target="_blank"
        />
      </v-col>
      <v-col class="pb-3 pb-sm-0 pr-sm-1 pt-sm-1" cols="12" sm="6">
        <v-btn
          class="w-100"
          color="secondary"
          prepend-icon="mdi-download"
          text="Download Current Log"
          @click="exportLogFile.click()"
        />
        <a
          ref="exportLogFile"
          class="d-none"
          download="photonvision-journalctl.txt"
          :href="`http://${address}/api/utils/photonvision-journalctl.txt`"
          target="_blank"
        />
      </v-col>
      <v-col class="pb-3 pb-sm-0 pl-sm-1 pt-sm-1" cols="12" sm="6">
        <v-btn
          class="w-100"
          color="secondary"
          prepend-icon="mdi-eye"
          text="Show log viewer"
          @click="useStateStore().showLogModal = true"
        />
      </v-col>
    </v-row>

    <!--          <v-btn-->
    <!--            color="secondary"-->
    <!--            prepend-icon="mdi-import"-->
    <!--            text="Import Settings"-->
    <!--            @click="() => (showImportDialog = true)"-->
    <!--          />-->
    <!--          <v-dialog-->
    <!--            v-model="showImportDialog"-->
    <!--            width="600"-->
    <!--            @input="-->
    <!--              () => {-->
    <!--                importType = -1;-->
    <!--                importFile = null;-->
    <!--              }-->
    <!--            "-->
    <!--          >-->
    <!--            <v-card>-->
    <!--              <v-card-title>Import Settings</v-card-title>-->
    <!--              <v-card-text>-->
    <!--                Upload and apply previously saved or exported PhotonVision settings to this device-->
    <!--                <v-row class="mt-6 ml-4">-->
    <!--                  <pv-select-->
    <!--                    v-model="importType"-->
    <!--                    :items="[-->
    <!--                      'All Settings',-->
    <!--                      'Hardware Config',-->
    <!--                      'Hardware Settings',-->
    <!--                      'Network Config',-->
    <!--                      'Apriltag Layout'-->
    <!--                    ]"-->
    <!--                    label="Type"-->
    <!--                    :select-cols="10"-->
    <!--                    class="w-100"-->
    <!--                    tooltip="Select the type of settings file you are trying to upload"-->
    <!--                  />-->
    <!--                </v-row>-->
    <!--                <v-row class="mt-6 ml-4 mr-8">-->
    <!--                  <v-file-input-->
    <!--                    v-model="importFile"-->
    <!--                    :accept="importType === ImportType.AllSettings ? '.zip' : '.json'"-->
    <!--                    :disabled="importType === -1"-->
    <!--                    :error-messages="importType === -1 ? 'Settings type not selected' : ''"-->
    <!--                  />-->
    <!--                </v-row>-->
    <!--                <v-row-->
    <!--                  align="center"-->
    <!--                  class="d-flex align-center justify-center mt-12 ml-8 mr-8 mb-1"-->
    <!--                >-->
    <!--                  <v-btn-->
    <!--                    color="secondary"-->
    <!--                    :disabled="importFile === null"-->
    <!--                    prepend-icon="mdi-import"-->
    <!--                    text="Import Settings"-->
    <!--                    @click="handleSettingsImport"-->
    <!--                  />-->
    <!--                </v-row>-->
    <!--              </v-card-text>-->
    <!--            </v-card>-->
    <!--          </v-dialog>-->
  </v-card>
</template>
