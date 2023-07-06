<script setup lang="ts">
import CvSelect from "@/components/common/cv-select.vue";
import CvNumberInput from "@/components/common/cv-number-input.vue";
import {useCameraSettingsStore} from "@/stores/settings/CameraSettingsStore";
import {useStateStore} from "@/stores/StateStore";
import {ref} from "vue";

const fov = ref(useCameraSettingsStore().cameras[useStateStore().currentCameraIndex].fov.value);

const saveCameraSettings = () => {
  useCameraSettingsStore().updateCameraSettings({fov: fov.value})
      .then((response) => {
        useStateStore().showSnackbarMessage({
          color: "success",
          message: response.data.text || response.data
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
      })
      .finally(() => {
        // TODO, does this conflict with WS data exchange
        useCameraSettingsStore().cameras[useStateStore().currentCameraIndex].fov.value = fov.value;
      });
};
</script>

<template>
  <v-card
      class="mb-3 pr-6 pb-3"
      color="primary"
      dark
  >
    <v-card-title>Camera Settings</v-card-title>
    <div class="ml-5">
      <cv-select
          v-model="useStateStore().currentCameraIndex"
          label="Camera"
          :items="useCameraSettingsStore().cameraNames"
          :select-cols="8"
          @input="args => useCameraSettingsStore().setCurrentCameraIndex(args)"
      />
      <cv-number-input
          v-model="fov"
          :tooltip="!useCameraSettingsStore().cameras[useStateStore().currentCameraIndex].fov.managedByVendor ? 'Field of view (in degrees) of the camera measured across the diagonal of the frame, in a video mode which covers the whole sensor area.' : 'This setting is managed by a vendor'"
          label="Maximum Diagonal FOV"
          :disabled="useCameraSettingsStore().cameras[useStateStore().currentCameraIndex].fov.managedByVendor"
          :label-cols="4"
      />
      <br>
      <v-btn
          style="margin-top:10px"
          small
          color="secondary"
          @click="saveCameraSettings"
          :disabled="fov === useCameraSettingsStore().cameras[useStateStore().currentCameraIndex].fov.value"
      >
        <v-icon left>
          mdi-content-save
        </v-icon>
        Save Changes
      </v-btn>
    </div>
  </v-card>
</template>
