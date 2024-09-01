<script setup lang="ts">
import { computed } from "vue";
import CamerasViewCard from "@/components/dashboard/cards/CamerasViewCard.vue";
import CameraAndPipelineConfigCard from "@/components/dashboard/cards/CameraAndPipelineConfigCard.vue";
import StreamConfigCard from "@/components/dashboard/cards/StreamConfigCard.vue";
import PipelineConfigCard from "@/components/dashboard/cards/PipelineConfigOptionsCard.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";

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
    useCameraSettingsStore().changeCurrentPipelineSetting(
      {
        inputShouldShow: v.includes(0),
        outputShouldShow: v.includes(1)
      },
      true
    );
  }
});
</script>

<template>
  <v-container class="pa-3" fluid>
    <v-row align="center" class="pb-3" justify="center" no-gutters>
      <v-col align-self="stretch" class="pr-lg-3 pb-3 pb-lg-0" cols="12" lg="8">
        <CamerasViewCard v-model="cameraViewType" />
      </v-col>
      <v-col align-self="stretch" cols="12" lg="4">
        <v-row class="flex-column pt-0 mt-0 fill-height" no-gutters>
          <v-col class="pb-3">
            <CameraAndPipelineConfigCard />
          </v-col>
          <v-col>
            <StreamConfigCard v-model="cameraViewType" />
          </v-col>
        </v-row>
      </v-col>
    </v-row>
    <PipelineConfigCard />
  </v-container>
</template>
