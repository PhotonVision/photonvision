<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import type { ToggleItem } from "@/components/common/form/pv-toggle-group.vue";
import { useStateStore } from "@/stores/StateStore";
import { PipelineType } from "@/types/PipelineTypes";
import IconSquareOutline from "~icons/mdi/square-outline";
import IconCubeOutline from "~icons/mdi/cube-outline";
import IconImport from "~icons/mdi/import";
import IconExport from "~icons/mdi/export";

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
  { value: "0", label: "2D", icon: IconSquareOutline, disabled: !useCameraSettingsStore().hasConnected },
  {
    value: "1",
    label: "3D",
    icon: IconCubeOutline,
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
  { value: "0", label: "Raw", icon: IconImport },
  { value: "1", label: "Processed", icon: IconExport }
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
      useCameraSettingsStore().isDriverMode || useCameraSettingsStore().isFocusMode || useStateStore().colorPickingMode
        ? 'pointer-events-none opacity-60'
        : ''
    ]"
  >
    <div class="flex flex-wrap items-center text-sm">
      <div class="w-full">
        <p class="pb-1 text-white">Processing Mode</p>
        <pv-toggle-group v-model="processingModeModel" :items="processingModeItems" />
      </div>
    </div>
    <div class="flex flex-wrap items-center pt-1 text-sm">
      <div class="w-full pt-0">
        <p class="pb-1 text-white">Stream Display</p>
        <pv-toggle-group v-model="streamDisplayModel" :items="streamDisplayItems" multiple />
      </div>
    </div>
  </pv-card>
</template>
