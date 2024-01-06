<script setup lang="ts">
import PvSelect from "@/components/common/pv-select.vue";
import PvNumberInput from "@/components/common/pv-number-input.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed, ref, watchEffect } from "vue";
import { CameraSettings } from "@/types/SettingTypes";

const currentFov = ref();

const saveCameraSettings = () => {
  useCameraSettingsStore()
    .updateCameraSettings({ fov: currentFov.value, quirksToChange: quirksToChange.value }, false)
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

const quirksToChange = ref({
  ArduOV9281: false,
  ArduOV2311: false,
})

let arducams = ["N/A", "OV9281", "OV2311"]

const arducamModel = computed({
  get() {
    const quirks = useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks;

    if (quirks.ArduOV9281) {
      return 1;
    } else if (quirks.ArduOV2311) {
      return 2;
    }
    return 0;
  },
  set(value) {
    console.log("hi")
    console.log(value)

    if (value === 1) {
      quirksToChange.value.ArduOV9281 = true;
      quirksToChange.value.ArduOV2311 = false;
    } else if (value === 2) {
      quirksToChange.value.ArduOV9281 = false;
      quirksToChange.value.ArduOV2311 = true;
    } else {
      quirksToChange.value.ArduOV9281 = false;
      quirksToChange.value.ArduOV2311 = false;
    }
  }
})


const isArducam = () => {
  const settings = useCameraSettingsStore().currentCameraSettings;
  // console.log("Is arducam?")
  // console.log(settings.cameraQuirks.quirks.ArudcamCamera)
  return settings.cameraQuirks.quirks.ArudcamCamera;
}

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
        @input="
          (args) => {
            currentFov = useCameraSettingsStore().cameras[args].fov.value;
            useCameraSettingsStore().setCurrentCameraIndex(args);
          }
        "
      />
      <pv-number-input
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
      <pv-select
        v-model="arducamModel"
        label="Arducam Model"
        :disabled="!isArducam()"
        :items="arducams"
        :select-cols="8"
      />
      <br />
      <v-btn
        style="margin-top: 10px"
        small
        color="secondary"
        @click="saveCameraSettings"
      >
        <v-icon left> mdi-content-save </v-icon>
        Save Changes
      </v-btn>
    </div>
  </v-card>
</template>
