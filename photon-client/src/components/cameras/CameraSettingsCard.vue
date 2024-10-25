<script setup lang="ts">
import PvSelect from "@/components/common/pv-select.vue";
import PvNumberInput from "@/components/common/pv-number-input.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed, inject, ref, watchEffect } from "vue";
import { type CameraSettingsChangeRequest, ValidQuirks } from "@/types/SettingTypes";
import axios from "axios";

const tempSettingsStruct = ref<CameraSettingsChangeRequest>({
  fov: useCameraSettingsStore().currentCameraSettings.fov.value,
  quirksToChange: Object.assign({}, useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks)
});

const arducamSelectWrapper = computed<number>({
  get: () => {
    if (tempSettingsStruct.value.quirksToChange.ArduOV9281Controls) return 1;
    else if (tempSettingsStruct.value.quirksToChange.ArduOV2311Controls) return 2;
    else if (tempSettingsStruct.value.quirksToChange.ArduOV9782Controls) return 3;
    else return 0;
  },
  set: (v) => {
    switch (v) {
      case 1:
        tempSettingsStruct.value.quirksToChange.ArduOV9281Controls = true;
        tempSettingsStruct.value.quirksToChange.ArduOV2311Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV9782Controls = false;
        break;
      case 2:
        tempSettingsStruct.value.quirksToChange.ArduOV9281Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV2311Controls = true;
        tempSettingsStruct.value.quirksToChange.ArduOV9782Controls = false;
        break;
      case 3:
        tempSettingsStruct.value.quirksToChange.ArduOV9281Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV2311Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV9782Controls = true;
        break;
      default:
        tempSettingsStruct.value.quirksToChange.ArduOV9281Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV2311Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV9782Controls = false;
        break;
    }
  }
});

const currentCameraIsArducam = computed<boolean>(
  () => useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks.ArduCamCamera
);

const settingsHaveChanged = (): boolean => {
  const a = tempSettingsStruct.value;
  const b = useCameraSettingsStore().currentCameraSettings;

  for (const q in ValidQuirks) {
    if (a.quirksToChange[q] != b.cameraQuirks.quirks[q]) return true;
  }

  return a.fov != b.fov.value;
};

const resetTempSettingsStruct = () => {
  tempSettingsStruct.value.fov = useCameraSettingsStore().currentCameraSettings.fov.value;
  tempSettingsStruct.value.quirksToChange = Object.assign(
    {},
    useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks
  );
};

const saveCameraSettings = () => {
  useCameraSettingsStore()
    .updateCameraSettings(tempSettingsStruct.value)
    .then((response) => {
      useStateStore().showSnackbarMessage({
        color: "success",
        message: response.data.text || response.data
      });

      // Update the local settings cause the backend checked their validity. Assign is to deref value
      useCameraSettingsStore().currentCameraSettings.fov.value = tempSettingsStruct.value.fov;
      useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks = Object.assign(
        {},
        tempSettingsStruct.value.quirksToChange
      );
    })
    .catch((error) => {
      resetTempSettingsStruct();
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

watchEffect(() => {
  // Reset temp settings on remote camera settings change
  resetTempSettingsStruct();
});

const showDeleteCamera = ref(false);

const address = inject<string>("backendHost");
const exportSettings = ref();
const openExportSettingsPrompt = () => {
  exportSettings.value.click();
};

const yesDeleteMySettingsText = ref("");
const deleteThisCamera = () => {
  const payload = {
    cameraUniqueName: useCameraSettingsStore().cameraUniqueNames[useStateStore().currentCameraIndex]
  };

  axios
    .post("/utils/nukeOneCamera", payload)
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Successfully dispatched the delete command. Waiting for backend to start back up",
        color: "success"
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          message: "The backend is unable to fulfil the request to delete this camera.",
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
  showDeleteCamera.value = false;
};
</script>

<template>
  <v-card class="mb-3 pr-6 pb-3" color="primary" dark>
    <v-card-title>Camera Settings</v-card-title>
    <div class="ml-5">
      <pv-select
        v-model="useStateStore().currentCameraIndex"
        label="Camera"
        :items="useCameraSettingsStore().cameraNames"
        :select-cols="8"
      />
      <pv-number-input
        v-model="tempSettingsStruct.fov"
        :tooltip="
          !useCameraSettingsStore().currentCameraSettings.fov.managedByVendor
            ? 'Field of view (in degrees) of the camera measured across the diagonal of the frame, in a video mode which covers the whole sensor area.'
            : 'This setting is managed by a vendor'
        "
        label="Maximum Diagonal FOV"
        :disabled="useCameraSettingsStore().currentCameraSettings.fov.managedByVendor"
        :label-cols="4"
      />
      <pv-select
        v-show="currentCameraIsArducam"
        v-model="arducamSelectWrapper"
        label="Arducam Model"
        :items="[
          { name: 'None', value: 0, disabled: true },
          { name: 'OV9281', value: 1 },
          { name: 'OV2311', value: 2 },
          { name: 'OV9782', value: 3 }
        ]"
        :select-cols="8"
      />
      <br />
      <v-row>
        <v-col cols="6">
          <v-btn
            class="mt-2 mb-3"
            style="width: 100%"
            small
            color="secondary"
            :disabled="!settingsHaveChanged()"
            @click="saveCameraSettings"
          >
            <v-icon left> mdi-content-save </v-icon>
            Save Changes
          </v-btn>
        </v-col>
        <v-col cols="6">
          <v-btn class="mt-2 mb-3" style="width: 100%" small color="red" @click="() => (showDeleteCamera = true)">
            <v-icon left> mdi-bomb </v-icon>
            Delete Camera
          </v-btn>
        </v-col>
      </v-row>
    </div>

    <v-dialog v-model="showDeleteCamera" dark width="1500">
      <v-card dark class="dialog-container pa-6" color="primary" flat>
        <v-card-title
          >Delete camera "{{ useCameraSettingsStore().cameraNames[useStateStore().currentCameraIndex] }}"</v-card-title
        >
        <v-row class="pl-3 align-center pa-6">
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

        <v-divider class="mt-4 mb-4" />
        <v-row class="pl-3 align-center pa-6">
          <v-col>
            <pv-input
              v-model="yesDeleteMySettingsText"
              :label="'Type &quot;' + useCameraSettingsStore().currentCameraName + '&quot;:'"
              :label-cols="12"
              :input-cols="12"
            />
          </v-col>

          <v-btn
            color="red"
            :disabled="
              yesDeleteMySettingsText.toLowerCase() !== useCameraSettingsStore().currentCameraName.toLowerCase()
            "
            @click="deleteThisCamera"
          >
            <v-icon left class="open-icon"> mdi-skull </v-icon>
            <span class="open-label">DELETE (UNRECOVERABLE)</span>
          </v-btn>
        </v-row>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<style scoped>
.v-divider {
  border-color: white !important;
}
</style>
