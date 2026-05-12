<script setup lang="ts">
import PvToggleGroup, { type ToggleItem } from "@/components/common/pv-toggle-group.vue";
import PvCard from "@/components/common/pv-card.vue";
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { PipelineType } from "@/types/PipelineTypes";

const value = defineModel<number[]>();

const processingMode = computed<number>({
  get: () => (useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled ? 1 : 0),
  set: (v) => {
    if (useCameraSettingsStore().isCurrentVideoFormatCalibrated) {
      useCameraSettingsStore().changeCurrentPipelineSetting({ solvePNPEnabled: v === 1 }, true);
    }
  }
});

const processingModeItems = computed<ToggleItem<string>[]>(() => [
  { value: "0", label: "2D", icon: "mdi-square-outline", disabled: !useCameraSettingsStore().hasConnected },
  {
    value: "1",
    label: "3D",
    icon: "mdi-cube-outline",
    disabled:
      !useCameraSettingsStore().hasConnected ||
      !useCameraSettingsStore().isCurrentVideoFormatCalibrated ||
      useCameraSettingsStore().currentPipelineSettings.pipelineType === PipelineType.ObjectDetection ||
      useCameraSettingsStore().currentPipelineSettings.pipelineType === PipelineType.ColoredShape
  }
]);

const processingModeModel = computed<string>({
  get: () => String(processingMode.value),
  set: (nextValue) => {
    if (nextValue === undefined || nextValue === "") return;
    processingMode.value = Number(nextValue);
  }
});

const streamDisplayItems: ToggleItem<string>[] = [
  { value: "0", label: "Raw", icon: "mdi-import" },
  { value: "1", label: "Processed", icon: "mdi-export" }
];

const streamDisplayModel = computed<string[]>({
  get: () => (value.value ?? []).map(String),
  set: (nextValue) => {
    if (!nextValue.length) return;
    value.value = nextValue.map(Number).sort((a, b) => a - b);
  }
});
</script>

<template>
  <pv-card
    :class="[
      'mt-3 flex flex-col',
      useCameraSettingsStore().isDriverMode ||
      useCameraSettingsStore().isFocusMode ||
      useStateStore().colorPickingMode
        ? 'pointer-events-none opacity-60'
        : ''
    ]"
  >
    <div class="flex flex-wrap items-center p-3">
      <div class="w-full ">
        <p style="color: white">Processing Mode</p>
        <pv-toggle-group v-model="processingModeModel" :items="processingModeItems" />
      </div>
    </div>
    <div class="flex flex-wrap items-center p-3 pt-1">
      <div class="w-full pt-0">
        <p style="color: white">Stream Display</p>
        <pv-toggle-group v-model="streamDisplayModel" :items="streamDisplayItems" multiple />
      </div>
    </div>
  </pv-card>
</template>
