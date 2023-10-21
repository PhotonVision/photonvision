<script setup lang="ts">
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";

const props = defineProps<{
  // TODO fully update v-model usage in custom components on Vue3 update
  value: number[];
}>();

const emit = defineEmits<{
  (e: "input", value: number[]): void;
}>();

const localValue = computed({
  get: () => props.value,
  set: (v) => emit("input", v)
});

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
  <v-card class="mb-3 pb-3 pa-4" color="primary" dark>
    <v-card-title
      class="pb-0 mb-2 pl-4 pt-1"
      style="min-height: 50px; justify-content: space-between; align-content: center"
    >
      <div style="display: flex; flex-wrap: wrap">
        <div>
          <span class="mr-4" style="white-space: nowrap"> Cameras </span>
        </div>
        <div>
          <v-chip
            label
            :color="fpsTooLow ? 'error' : 'transparent'"
            :text-color="fpsTooLow ? '#C7EA46' : '#ff4d00'"
            style="font-size: 1rem; padding: 0; margin: 0"
          >
            <span class="pr-1">
              {{ Math.round(useStateStore().currentPipelineResults?.fps || 0) }}&nbsp;FPS &ndash;
              {{ Math.min(Math.round(useStateStore().currentPipelineResults?.latency || 0), 9999) }} ms latency
            </span>
          </v-chip>
        </div>
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
    <div class="stream-container pb-4">
      <div class="stream">
        <photon-camera-stream v-show="value.includes(0)" stream-type="Raw" style="max-width: 100%" />
      </div>
      <div class="stream">
        <photon-camera-stream v-show="value.includes(1)" stream-type="Processed" style="max-width: 100%" />
      </div>
    </div>
    <v-divider />
    <div class="pt-4">
      <p style="color: white">Stream Display</p>
      <v-btn-toggle v-model="localValue" :multiple="true" mandatory dark class="fill" style="width: 100%">
        <v-btn
          color="secondary"
          class="fill"
          :disabled="useCameraSettingsStore().isDriverMode || useCameraSettingsStore().isCalibrationMode"
        >
          <v-icon>mdi-import</v-icon>
          <span>Raw</span>
        </v-btn>
        <v-btn
          color="secondary"
          class="fill"
          :disabled="useCameraSettingsStore().isDriverMode || useCameraSettingsStore().isCalibrationMode"
        >
          <v-icon>mdi-export</v-icon>
          <span>Processed</span>
        </v-btn>
      </v-btn-toggle>
    </div>
  </v-card>
</template>

<style scoped>
.v-btn-toggle.fill {
  width: 100%;
  height: 100%;
}
.v-btn-toggle.fill > .v-btn {
  width: 50%;
  height: 100%;
}
th {
  width: 80px;
  text-align: center;
}

.stream-container {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.stream {
  display: flex;
  justify-content: center;
}

@media only screen and (min-width: 512px) and (max-width: 960px) {
  .stream-container {
    flex-wrap: nowrap;
    justify-content: center;
  }

  .stream {
    max-width: 50%;
  }
}
</style>
