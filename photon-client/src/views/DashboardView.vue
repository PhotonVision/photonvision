<script setup lang="ts">
import { computed } from "vue";
import CamerasCard from "@/components/dashboard/CamerasCard.vue";
import CameraAndPipelineSelectCard from "@/components/dashboard/CameraAndPipelineSelectCard.vue";
import StreamConfigCard from "@/components/dashboard/StreamConfigCard.vue";
import PipelineConfigCard from "@/components/dashboard/ConfigOptions.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { PlaceholderCameraSettings } from "@/types/SettingTypes";

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

// TODO - deduplicate with needsCamerasConfigured
const warningShown = computed<boolean>(() => {
  return (
    useCameraSettingsStore().cameras.length === 0 || useCameraSettingsStore().cameras[0] === PlaceholderCameraSettings
  );
});
</script>

<template>
  <v-container class="pa-3" fluid>
    <v-row no-gutters align="center" justify="center">
      <v-col cols="12" class="pb-3 pr-lg-3" lg="8" align-self="stretch">
        <CamerasCard v-model="cameraViewType" />
      </v-col>
      <v-col cols="12" class="pb-3" lg="4" style="display: flex; flex-direction: column" align-self="stretch">
        <CameraAndPipelineSelectCard />
        <StreamConfigCard v-model="cameraViewType" />
      </v-col>
    </v-row>
    <PipelineConfigCard />

    <!-- TODO - not sure this belongs here -->
    <v-dialog :persistent="false" v-model="warningShown" v-if="warningShown" max-width="1500" dark>
      <v-card dark flat color="primary">
        <v-card-title>Setup some cameras to get started!</v-card-title>
        <v-card-text>
          No cameras activated - head to the <a href="#/cameraConfigs">Camera matching tab</a> to set some up!
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
a:link {
  color: #ffd843;
  background-color: transparent;
  text-decoration: none;
}
a:visited {
  color: #ffd843;
  background-color: transparent;
  text-decoration: none;
}
a:hover {
  color: pink;
  background-color: transparent;
  text-decoration: underline;
}

a:active {
  color: yellow;
  background-color: transparent;
  text-decoration: none;
}
</style>
