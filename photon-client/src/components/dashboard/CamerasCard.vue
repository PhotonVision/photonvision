<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";

defineProps<{
  // TODO fully update v-model usage in custom components on Vue3 update
  value: number[];
}>();

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
  <v-card color="primary" height="100%" style="display: flex; flex-direction: column" dark>
    <v-row>
      <v-col class="align-self-center text-no-wrap">
        <v-card-title>Cameras</v-card-title>
      </v-col>
      <v-col class="align-self-center" style="text-align: right; margin-right: 12px; padding-left: 24px">
        <v-chip
          label
          :color="fpsTooLow ? 'error' : 'transparent'"
          :text-color="fpsTooLow ? '#C7EA46' : '#ff4d00'"
          style="font-size: 1rem; padding: 0; margin: 0"
        >
          <span class="pr-1"
            >Processing @ {{ Math.round(useStateStore().currentPipelineResults?.fps || 0) }}&nbsp;FPS &ndash;</span
          ><span>{{ performanceRecommendation }}</span>
        </v-chip>
      </v-col>
      <v-col
        class="align-self-center"
        style="
          width: min-content;
          flex-grow: 0;
          display: flex;
          justify-content: flex-end;
          margin-right: 24px;
          padding: 0;
        "
      >
        <v-switch
          v-model="driverMode"
          :disabled="useCameraSettingsStore().isCalibrationMode || useCameraSettingsStore().pipelineNames.length === 0"
          label="Driver Mode"
          style="margin: 0; padding: 0; padding-left: 18px; margin-top: 14px"
          color="accent"
        />
      </v-col>
    </v-row>
    <v-divider style="border-color: white" />
    <v-row class="stream-viewer-container pa-3">
      <v-col v-if="value.includes(0)" class="stream-view">
        <photon-camera-stream id="input-camera-stream" stream-type="Raw" style="width: 100%; height: auto" />
      </v-col>
      <v-col v-if="value.includes(1)" class="stream-view">
        <photon-camera-stream id="output-camera-stream" stream-type="Processed" style="width: 100%; height: auto" />
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
