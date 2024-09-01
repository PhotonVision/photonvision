<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";

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

const model = defineModel<number[]>({ required: true });
</script>

<template>
  <v-card class="fill-height" style="display: flex; flex-direction: column">
    <v-row>
      <v-col>
        <v-row no-gutters>
          <v-col cols="12" md="3">
            <v-card-title>Cameras</v-card-title>
          </v-col>
          <v-col align-self="center" class="ml-4 ml-md-0 mb-2 mb-md-0" cols="12" md="7">
            <span :class="fpsTooLow && 'text-error'">
              Processing @ {{ Math.round(useStateStore().currentPipelineResults?.fps || 0) }}&nbsp;FPS &ndash;
              {{ performanceRecommendation }}
            </span>
          </v-col>
        </v-row>
      </v-col>
      <v-col cols="5" md="3">
        <v-switch
          v-model="driverMode"
          class="pr-3 pr-md-6"
          color="accent"
          :disabled="useCameraSettingsStore().isCalibrationMode || useCameraSettingsStore().pipelineNames.length === 0"
          hide-details
          style="justify-items: right"
        >
          <template #label>
            <span style="word-break: normal">Driver Mode</span>
          </template>
        </v-switch>
      </v-col>
    </v-row>
    <v-divider style="border-color: white" />
    <v-row class="pa-3 fill-height" justify="center">
      <v-col v-if="(model || []).includes(0)" class="stream-wrapper" cols="12" md="6">
        <photon-camera-stream id="input-camera-stream" class="stream" stream-type="Raw" />
      </v-col>
      <v-col v-if="(model || []).includes(1)" class="stream-wrapper" cols="12" md="6">
        <photon-camera-stream id="output-camera-stream" class="stream" stream-type="Processed" />
      </v-col>
    </v-row>
  </v-card>
</template>

<style scoped lang="scss">
@import "vuetify/settings";

.stream-wrapper {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
.stream {
  height: 100%;
}

@media #{map-get($display-breakpoints, 'md-and-down')} {
  .stream {
    max-width: 500px;
  }
}
</style>
