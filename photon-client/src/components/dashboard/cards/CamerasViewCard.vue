<script setup lang="ts">
import { computed } from "vue";
import { PipelineType } from "@/types/PipelineTypes";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import PvSwitch from "@/components/common/pv-switch.vue";

const clientStore = useClientStore();
const serverStore = useServerStore();

const fpsTooLow = computed<boolean>(() => {
  const currFPS = clientStore.currentPipelineResults?.fps || 0;
  const targetFPS = serverStore.currentVideoFormat?.fps || 0;
  const driverMode = serverStore.isDriverMode;
  const gpuAccel = serverStore.instanceConfig?.gpuAccelerationSupported || false;
  const isReflective = serverStore.currentPipelineType === PipelineType.Reflective;

  return currFPS - targetFPS < -5 && currFPS !== 0 && !driverMode && gpuAccel && isReflective;
});

const performanceRecommendation = computed<string>(() => {
  if (
    fpsTooLow.value &&
    !serverStore.currentPipelineSettings?.inputShouldShow &&
    serverStore.currentPipelineSettings?.pipelineType === PipelineType.Reflective
  ) {
    return "HSV thresholds are too broad; narrow them for better performance";
  } else if (fpsTooLow.value && serverStore.currentPipelineSettings?.inputShouldShow) {
    return "Stop viewing the raw stream for better performance";
  } else {
    return `${Math.min(Math.round(clientStore.currentPipelineResults?.latency || 0), 9999)} ms latency`;
  }
});

const model = defineModel<number[]>({ required: true });
</script>

<template>
  <v-card class="fill-height d-flex flex-column">
    <v-row>
      <v-col>
        <v-row no-gutters>
          <v-col cols="12" md="3">
            <v-card-title>Cameras</v-card-title>
          </v-col>
          <v-col v-if="clientStore.currentPipelineResults" class="ml-4 ml-md-0 mb-2 mb-md-0 d-flex align-center" cols="12" md="7">
            <span :class="fpsTooLow && 'text-error'">
              Processing @ {{ Math.round(clientStore.currentPipelineResults?.fps || 0) }}&nbsp;FPS &ndash;
              {{ performanceRecommendation }}
            </span>
          </v-col>
        </v-row>
      </v-col>
      <v-col v-if="serverStore.currentPipelineSettings" cols="5" md="3">
        <pv-switch
          v-model="serverStore.driverMode"
          class="pr-3 pr-md-6"
          :disabled="serverStore.isCalibMode"
          label="Driver Mode"
          style="justify-items: right"
          tooltip="Disable processing using this camera and just use it as a driver camera"
        />
      </v-col>
    </v-row>
    <v-divider style="border-color: white" />
    <v-row class="pa-3 fill-height" justify="center">
      <v-col v-show="model.includes(0)" class="d-flex justify-center align-center" cols="12" md="6">
        <photon-camera-stream id="input-camera-stream" class="fill-height stream" stream-type="Raw" />
      </v-col>
      <v-col v-show="model.includes(1)" class="d-flex justify-center align-center" cols="12" md="6">
        <photon-camera-stream id="output-camera-stream" class="fill-height stream" stream-type="Processed" />
      </v-col>
    </v-row>
  </v-card>
</template>

<style scoped lang="scss">
@import "vuetify/settings";

@media #{map-get($display-breakpoints, 'md-and-down')} {
  .stream {
    max-width: 500px;
  }
}
</style>
