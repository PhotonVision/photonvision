<script setup lang="ts">
import PvSelect from "@/components/common/pv-select.vue";
import PvNumberInput from "@/components/common/pv-number-input.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed, ref, watchEffect } from "vue";
import { type CameraSettingsChangeRequest, ValidQuirks } from "@/types/SettingTypes";

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
    </div>
  </v-card>
</template>
