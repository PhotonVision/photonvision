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
    // Only show the input stream in Color Picking Mode
    if (useStateStore().colorPickingMode) return [0];

    // Only show the output stream in Driver Mode or Calibration Mode
    if (useCameraSettingsStore().isDriverMode || useCameraSettingsStore().isCalibrationMode) return [1];

    const ret: number[] = [];
    if (useCameraSettingsStore().currentPipelineSettings.inputShouldShow) {
      ret.push(0);
    }
    if (useCameraSettingsStore().currentPipelineSettings.outputShouldShow) {
      ret.push(1);
    }

    if (ret.length === 0) return [0];

    return ret;
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
