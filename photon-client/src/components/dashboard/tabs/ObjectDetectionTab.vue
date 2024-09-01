<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ActivePipelineSettings, PipelineType } from "@/types/PipelineTypes";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useDisplay } from "vuetify";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvRangeNumberSlider from "@/components/common/pv-range-number-slider.vue";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ActivePipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings
);

// TODO fix pv-range-slider so that store access doesn't need to be deferred
const contourArea = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.contourArea) as [number, number],
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.contourArea = v)
});
const contourRatio = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.contourRatio) as [number, number],
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.contourRatio = v)
});

const { mdAndDown } = useDisplay();
const labelCols = computed(
  () => 12 - (mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8)
);
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.ObjectDetection">
    <pv-number-slider
      v-model="currentPipelineSettings.confidence"
      label="Confidence"
      :label-cols="labelCols"
      :max="1"
      :min="0"
      :step="0.01"
      tooltip="The minimum confidence for a detection to be considered valid. Bigger numbers mean fewer but more probable detections are allowed through."
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ confidence: value }, false)
      "
    />
    <pv-range-number-slider
      v-model="contourArea"
      label="Area"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :step="0.01"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourArea: value }, false)
      "
    />
    <pv-range-number-slider
      v-model="contourRatio"
      label="Ratio (W/H)"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :step="0.01"
      tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourRatio: value }, false)
      "
    />
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOrientation"
      :items="['Portrait', 'Landscape'].map((v, i) => ({ name: v, value: i }))"
      label="Target Orientation"
      :label-cols="labelCols"
      tooltip="Used to determine how to calculate target landmarks, as well as aspect ratio"
      @update:model-value="
        (value: number) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOrientation: value }, false)
      "
    />
    <pv-dropdown
      v-model="currentPipelineSettings.contourSortMode"
      :items="
        ['Largest', 'Smallest', 'Highest', 'Lowest', 'Rightmost', 'Leftmost', 'Centermost'].map((v, i) => ({
          name: v,
          value: i
        }))
      "
      label="Target Sort"
      :label-cols="labelCols"
      tooltip="Chooses the sorting mode used to determine the 'best' targets to provide to user code"
      @update:model-value="
        (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourSortMode: value }, false)
      "
    />
  </div>
</template>
