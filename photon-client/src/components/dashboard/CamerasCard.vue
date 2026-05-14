<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import PvCard from "@/components/common/pv-card.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvChip from "@/components/common/pv-chip.vue";

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
  <pv-card class="flex h-full flex-col">
    <div class="flex items-center justify-between gap-3  pb-2">
      <span class="text-lg">Cameras</span>
      <pv-chip
        v-if="useCameraSettingsStore().currentCameraSettings.isConnected"
        label
        :color="fpsTooLow ? 'error' : 'primary'"
        class="p-0 m-0 text-lg"
        variant="text"
      >
        <span class="pr-1 tabular-nums">{{ Math.round(useStateStore().currentPipelineResults?.fps || 0) }}&nbsp;FPS &middot;</span
        ><span class="tabular-nums">{{ performanceRecommendation }}</span>
      </pv-chip>
      <pv-chip v-else label variant="text" color="red" style="font-size: 1rem; padding: 0; margin: 0">
        <span class="pr-1"> Camera not connected </span>
      </pv-chip>
      <pv-switch
        v-model="driverMode"
        :disabled="useCameraSettingsStore().isCalibrationMode || useCameraSettingsStore().pipelineNames.length === 0"
        label="Driver Mode"
        color="primary"
        hide-details="auto"
        class="!py-0"
      />
    </div>
    <hr class="w-full border-t border-white/10" />
    <div class="stream-viewer-container flex flex-wrap items-center p-2 flex-1 justify-between gap-2">
      <div v-if="value?.includes(0)" class="stream-view flex-1">
        <photon-camera-stream
          id="input-camera-stream"
          :camera-settings="useCameraSettingsStore().currentCameraSettings"
          stream-type="Raw"
          style="width: 100%; height: auto"
        />
      </div>
      <div v-if="value?.includes(1)" class="stream-view flex-1">
        <photon-camera-stream
          id="output-camera-stream"
          :camera-settings="useCameraSettingsStore().currentCameraSettings"
          stream-type="Processed"
          style="width: 100%; height: auto"
        />
      </div>
    </div>
  </pv-card>
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
