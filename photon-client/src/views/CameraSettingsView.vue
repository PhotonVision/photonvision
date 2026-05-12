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

    // Only show the output stream in Driver Mode or Calibration Mode or Focus Mode
    if (
      useCameraSettingsStore().isDriverMode ||
      useCameraSettingsStore().isCalibrationMode ||
      useCameraSettingsStore().isFocusMode
    )
      return [1];

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
    <div class="flex flex-wrap p-3">
      <div class="w-full md:w-7/12">
        <CamerasCard />
        <CalibrationCard />
        <CameraControlCard />
      </div>
      <div class="w-full pt-3 md:w-5/12 md:pl-3 md:pt-0">
        <CamerasView v-model="cameraViewType" />
      </div>
    </div>
  </div>
</template>
