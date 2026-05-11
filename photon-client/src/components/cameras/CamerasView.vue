<script setup lang="ts">
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvToggleGroup, { type ToggleItem } from "@/components/common/pv-toggle-group.vue";
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";

const value = defineModel<number[]>({ required: true });

const driverMode = computed<boolean>({
  get: () => useCameraSettingsStore().isDriverMode,
  set: (v) =>
    useCameraSettingsStore().changeCurrentPipelineIndex(
      v ? WebsocketPipelineType.DriverMode : useCameraSettingsStore().currentCameraSettings.lastPipelineIndex || 0,
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

const streamToggleDisabled = computed(
  () =>
    useCameraSettingsStore().isDriverMode ||
    useCameraSettingsStore().isCalibrationMode ||
    useCameraSettingsStore().isFocusMode
);

const streamToggleItems: ToggleItem<string>[] = [
  { value: "0", label: "Raw", icon: "mdi-import" },
  { value: "1", label: "Processed", icon: "mdi-export" }
];

const streamToggleModel = computed<string[]>({
  get: () => value.value.map(String),
  set: (nextValue) => {
    if (nextValue.length === 0) return;
    value.value = nextValue.map(Number).sort((a, b) => a - b);
  }
});
</script>

<template>
  <section
    id="camera-settings-camera-view-card"
    class="rounded-[12px] bg-pv-surface text-white shadow-lg shadow-black/10 min-[960px]:sticky min-[960px]:top-3"
  >
    <div class="flex flex-wrap items-center gap-3 px-5 py-4">
      <span class="mr-1 whitespace-nowrap text-xl font-semibold">Cameras</span>
      <span
        v-if="useCameraSettingsStore().currentCameraSettings.isConnected"
        class="inline-flex rounded-full px-3 py-1 text-sm font-medium"
        :class="fpsTooLow ? 'bg-pv-error/12 text-pv-error' : 'bg-transparent text-pv-primary'"
      >
        {{ Math.round(useStateStore().currentPipelineResults?.fps || 0) }} FPS -
        {{ Math.min(Math.round(useStateStore().currentPipelineResults?.latency || 0), 9999) }} ms latency
      </span>
      <span v-else class="inline-flex rounded-full px-3 py-1 text-sm font-medium text-pv-error">
        Camera not connected
      </span>
      <span
        v-if="useCameraSettingsStore().isFocusMode"
        class="ml-auto inline-flex rounded-full px-3 py-1 text-sm font-medium text-pv-primary"
      >
        Focus: {{ Math.round(useStateStore().currentPipelineResults?.focus || 0) }}
      </span>
      <div class="ml-auto w-full min-w-56 sm:w-auto">
        <pv-switch
          v-model="driverMode"
          :disabled="useCameraSettingsStore().isCalibrationMode || useCameraSettingsStore().pipelineNames.length === 0"
          label="Driver Mode"
        />
      </div>
    </div>
    <div class="flex flex-wrap items-center justify-center gap-3 px-5 pb-5 min-[512px]:max-[960px]:flex-nowrap">
      <div class="flex w-full justify-center min-[512px]:max-[960px]:w-1/2">
        <photon-camera-stream
          v-if="value.includes(0)"
          id="input-camera-stream"
          :camera-settings="useCameraSettingsStore().currentCameraSettings"
          stream-type="Raw"
          style="max-width: 100%"
        />
      </div>
      <div class="flex w-full justify-center min-[512px]:max-[960px]:w-1/2">
        <photon-camera-stream
          v-if="value.includes(1)"
          id="output-camera-stream"
          :camera-settings="useCameraSettingsStore().currentCameraSettings"
          stream-type="Processed"
          style="max-width: 100%"
        />
      </div>
    </div>
    <div class="px-5 pb-5 pt-0">
      <pv-toggle-group v-model="streamToggleModel" :items="streamToggleItems" multiple :disabled="streamToggleDisabled" />
    </div>
  </section>
</template>
