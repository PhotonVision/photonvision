<script setup lang="ts">
import CamerasCard from "@/components/cameras/CameraSettingsCard.vue";
import CalibrationCard from "@/components/cameras/CameraCalibrationCard.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed } from "vue";
import CamerasView from "@/components/cameras/CamerasView.vue";
import { useStateStore } from "@/stores/StateStore";
import CameraControlCard from "@/components/cameras/CameraControlCard.vue";

const cameraViewType = computed<number[]>({
  get: (): number[] => {
    // Only show the raw input stream in Color Picking Mode and prior to calibration
    if (useStateStore().colorPickingMode) return [0];
    if (useCameraSettingsStore().isCalibrationMode) return [1];
    if (useCameraSettingsStore().isDriverMode || useCameraSettingsStore().isFocusMode) return [1];
    return [0];
  },
  set: (v) => {
    useCameraSettingsStore().currentPipelineSettings.inputShouldShow = v.includes(0);
    useCameraSettingsStore().currentPipelineSettings.outputShouldShow = v.includes(1);
    useCameraSettingsStore().changeCurrentPipelineSetting({ inputShouldShow: v.includes(0) }, false);
  }
});
</script>

<template>
  <div>
    <v-row no-gutters class="pa-3">
      <v-col cols="12" md="7">
        <CamerasCard />
        <CalibrationCard />
        <CameraControlCard />
      </v-col>
      <v-col class="pl-md-3 pt-3 pt-md-0" cols="12" md="5">
        <CamerasView v-model="cameraViewType" />
      </v-col>
    </v-row>
  </div>
</template>
