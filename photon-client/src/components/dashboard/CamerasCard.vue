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
  set: (v) =>
    useCameraSettingsStore().changeCurrentPipelineIndex(
      v ? -1 : useCameraSettingsStore().currentCameraSettings.lastPipelineIndex || 0,
      true
    )
});

const fpsTooLow = computed<boolean>(() => {
  const currFPS = useStateStore().currentPipelineResults?.fps || 0;
  const targetFPS = useCameraSettingsStore().currentVideoFormat.fps;
  const driverMode = useCameraSettingsStore().isDriverMode;
  const gpuAccel = useSettingsStore().general.gpuAcceleration !== undefined;
  const isReflective = useCameraSettingsStore().currentPipelineSettings.pipelineType === PipelineType.Reflective;

  return currFPS - targetFPS < -5 && currFPS !== 0 && !driverMode && gpuAccel && isReflective;
});
</script>

<template>
  <v-card color="primary" height="100%" style="display: flex; flex-direction: column" dark>
    <v-card-title
      class="pb-0 mb-0 pl-4 pt-1"
      style="min-height: 50px; justify-content: space-between; align-content: center"
    >
      <div class="pt-2">
        <span class="mr-4">Cameras</span>
        <v-chip
          label
          :color="fpsTooLow ? 'error' : 'transparent'"
          :text-color="fpsTooLow ? '#C7EA46' : '#ff4d00'"
          style="font-size: 1rem; padding: 0; margin: 0"
        >
          <span class="pr-1">
            Processing @ {{ Math.round(useStateStore().currentPipelineResults?.fps || 0) }}&nbsp;FPS &ndash;
          </span>
          <span
            v-if="
              fpsTooLow &&
              !useCameraSettingsStore().currentPipelineSettings.inputShouldShow &&
              useCameraSettingsStore().currentPipelineSettings.pipelineType === PipelineType.Reflective
            "
          >
            HSV thresholds are too broad; narrow them for better performance
          </span>
          <span v-else-if="fpsTooLow && useCameraSettingsStore().currentPipelineSettings.inputShouldShow">
            stop viewing the raw stream for better performance
          </span>
          <span v-else>
            {{ Math.min(Math.round(useStateStore().currentPipelineResults?.latency || 0), 9999) }} ms latency
          </span>
        </v-chip>
      </div>
      <div>
        <v-switch
            v-model="driverMode"
            :disabled="useCameraSettingsStore().isCalibrationMode || useCameraSettingsStore().pipelineNames.length === 0"
            label="Driver Mode"
            style="margin-left: auto"
            color="accent"
            class="pt-2"
        />
      </div>
    </v-card-title>
    <v-divider style="border-color: white" />
    <v-row class="pl-3 pr-3 pt-3 pb-3" style="flex-wrap: nowrap; justify-content: center">
      <v-col v-show="value.includes(0)" style="max-width: 500px; display: flex; align-items: center">
        <photon-camera-stream id="input-camera-stream" stream-type="Raw" style="width: 100%; height: auto" />
      </v-col>
      <v-col v-show="value.includes(1)" style="max-width: 500px; display: flex; align-items: center">
        <photon-camera-stream id="output-camera-stream" stream-type="Processed" style="width: 100%; height: auto" />
      </v-col>
    </v-row>
  </v-card>
</template>
