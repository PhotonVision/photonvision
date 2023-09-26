<script setup lang="ts">
import CvSelect from "@/components/common/cv-select.vue";
import CvNumberInput from "@/components/common/cv-number-input.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { ref, watchEffect } from "vue";

const currentFov = ref();

const saveCameraSettings = () => {
  useCameraSettingsStore()
    .updateCameraSettings({ fov: currentFov.value }, false)
    .then((response) => {
      useCameraSettingsStore().currentCameraSettings.fov.value = currentFov.value;
      useStateStore().showSnackbarMessage({
        color: "success",
        message: response.data.text || response.data
      });
    })
    .catch((error) => {
      currentFov.value = useCameraSettingsStore().currentCameraSettings.fov.value;
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
  currentFov.value = useCameraSettingsStore().currentCameraSettings.fov.value;
});
</script>

<template>
  <v-card class="mb-3 pr-6 pb-3" color="primary" dark>
    <v-card-title>Camera Settings</v-card-title>
    <div class="ml-5">
      <cv-select
        v-model="useStateStore().currentCameraIndex"
        label="Camera"
        :items="useCameraSettingsStore().cameraNames"
        :select-cols="8"
        @input="
          (args) => {
            currentFov = useCameraSettingsStore().cameras[args].fov.value;
            useCameraSettingsStore().setCurrentCameraIndex(args);
          }
        "
      />
      <cv-number-input
        v-model="currentFov"
        :tooltip="
          !useCameraSettingsStore().currentCameraSettings.fov.managedByVendor
            ? 'Field of view (in degrees) of the camera measured across the diagonal of the frame, in a video mode which covers the whole sensor area.'
            : 'This setting is managed by a vendor'
        "
        label="Maximum Diagonal FOV"
        :disabled="useCameraSettingsStore().currentCameraSettings.fov.managedByVendor"
        :label-cols="4"
      />
      <br />
      <v-btn
        style="margin-top: 10px"
        small
        color="secondary"
        :disabled="currentFov === useCameraSettingsStore().currentCameraSettings.fov.value"
        @click="saveCameraSettings"
      >
        <v-icon left> mdi-content-save </v-icon>
        Save Changes
      </v-btn>
    </div>
  </v-card>
</template>
