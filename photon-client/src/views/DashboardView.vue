<script setup lang="ts">
import { computed } from "vue";
import CamerasCard from "@/components/dashboard/CamerasCard.vue";
import CameraAndPipelineSelectCard from "@/components/dashboard/CameraAndPipelineSelectCard.vue";
import StreamConfigCard from "@/components/dashboard/StreamConfigCard.vue";
import PipelineConfigCard from "@/components/dashboard/ConfigOptions.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";

const cameraViewType = computed<number[]>({
  get: (): number[] => {
    // Only show the input stream in Color Picking Mode
    if(useStateStore().colorPickingMode) return [0];

    // Only show the output stream in Driver Mode or Calibration Mode
    if(useCameraSettingsStore().isDriverMode || useCameraSettingsStore().isCalibrationMode) return [1];

    const ret: number[] = [];
    if(useCameraSettingsStore().currentPipelineSettings.inputShouldShow) {
      ret.push(0);
    }
    if(useCameraSettingsStore().currentPipelineSettings.outputShouldShow) {
      ret.push(1);
    }

    if(ret.length === 0) return [0];

    return ret;
  },
  set: v => {
    useCameraSettingsStore().currentPipelineSettings.inputShouldShow = v.includes(0);
    useCameraSettingsStore().currentPipelineSettings.outputShouldShow = v.includes(1);
    useCameraSettingsStore().changeCurrentPipelineSetting({ inputShouldShow: v.includes(0) }, false);
  }
});
</script>

<template>
  <v-container
    class="pa-3"
    fluid
  >
    <v-row
      no-gutters
      align="center"
      justify="center"
    >
      <v-col
        cols="12"
        class="pb-3 pr-lg-3"
        lg="8"
        align-self="stretch"
      >
        <CamerasCard v-model="cameraViewType" />
      </v-col>
      <v-col
        cols="12"
        class="pb-3"
        lg="4"
        style="display: flex; flex-direction: column"
        align-self="stretch"
      >
        <CameraAndPipelineSelectCard style="height: 50%" />
        <StreamConfigCard
          v-model="cameraViewType"
          style="height: 50%"
        />
      </v-col>
    </v-row>
    <PipelineConfigCard />
  </v-container>
</template>
