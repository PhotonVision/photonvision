<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";

const value = defineModel<number[]>();

const driverMode = computed<boolean>({
  get: () => useCameraSettingsStore().isDriverMode,
  set: (v) => useCameraSettingsStore().setDriverMode(v)
});

const fpsTooLow = computed<boolean>(() => {
  const currFPS = useStateStore().currentPipelineResults?.fps || 0;
  const targetFPS = useCameraSettingsStore().currentVideoFormat.fps;
  const driverMode = useCameraSettingsStore().isDriverMode;
  const gpuAccel = useSettingsStore().general.gpuAcceleration !== undefined;
  const isReflective = useCameraSettingsStore().currentPipelineSettings.pipelineType === PipelineType.Reflective;

  return currFPS - targetFPS < -5 && currFPS !== 0 && !driverMode && gpuAccel && isReflective;
});

const performanceRecommendation = computed<string>(() => {
  if (
    fpsTooLow.value &&
    !useCameraSettingsStore().currentPipelineSettings.inputShouldShow &&
    useCameraSettingsStore().currentPipelineSettings.pipelineType === PipelineType.Reflective
  ) {
    return "HSV thresholds are too broad; narrow them for better performance";
  } else if (fpsTooLow.value && useCameraSettingsStore().currentPipelineSettings.inputShouldShow) {
    return "Stop viewing the raw stream for better performance";
  } else {
    return `${Math.min(Math.round(useStateStore().currentPipelineResults?.latency || 0), 9999)} ms latency`;
  }
});
</script>

<template>
  <v-card color="surface" height="100%" class="d-flex flex-column rounded-12" dark>
    <v-card-title class="justify-space-between align-center pt-1 pb-1 d-flex">
      <span>Cameras</span>
      <v-chip
        v-if="useCameraSettingsStore().currentCameraSettings.isConnected"
        label
        :color="fpsTooLow ? 'error' : 'primary'"
        style="font-size: 1.1rem; padding: 0; margin: 0"
        variant="text"
      >
        <span class="pr-1">{{ Math.round(useStateStore().currentPipelineResults?.fps || 0) }}&nbsp;FPS &ndash;</span
        ><span>{{ performanceRecommendation }}</span>
      </v-chip>
      <v-chip v-else label variant="text" color="red" style="font-size: 1rem; padding: 0; margin: 0">
        <span class="pr-1"> Camera not connected </span>
      </v-chip>
      <v-switch
        v-model="driverMode"
        :disabled="useCameraSettingsStore().isCalibrationMode || useCameraSettingsStore().pipelineNames.length === 0"
        label="Driver Mode"
        color="primary"
        hide-details="auto"
      />
    </v-card-title>
    <v-divider class="ml-3 mr-3" />
    <v-row class="stream-viewer-container pa-3 align-center">
      <v-col v-if="value?.includes(0)" class="stream-view">
        <photon-camera-stream
          id="input-camera-stream"
          :camera-settings="useCameraSettingsStore().currentCameraSettings"
          stream-type="Raw"
          style="width: 100%; height: auto"
        />
      </v-col>
      <v-col v-if="value?.includes(1)" class="stream-view">
        <photon-camera-stream
          id="output-camera-stream"
          :camera-settings="useCameraSettingsStore().currentCameraSettings"
          stream-type="Processed"
          style="width: 100%; height: auto"
        />
      </v-col>
    </v-row>
  </v-card>
</template>

<style scoped>
.stream-viewer-container {
  display: flex;
  justify-content: center;
}
.stream-view {
  max-width: 500px;
}
@media only screen and (max-width: 512px) {
  .stream-view {
    min-width: 80%;
  }
}
</style>
