<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ObjectDetectionPipelineSettings, PipelineType } from "@/types/PipelineTypes";
import PvSlider from "@/components/common/pv-slider.vue";
import PvSelect from "@/components/common/pv-select.vue";
import PvRangeSlider from "@/components/common/pv-range-slider.vue";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useDisplay } from "vuetify";
import type { ObjectDetectionModelProperties } from "@/types/SettingTypes";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ObjectDetectionPipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings as ObjectDetectionPipelineSettings
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

const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8
);

// Filters out models that are not supported by the current backend, and returns a flattened list.
const supportedModels = computed<ObjectDetectionModelProperties[]>(() => {
  const { availableModels, supportedBackends } = useSettingsStore().general;
  const isSupported = (model: ObjectDetectionModelProperties) => {
    // Check if model's family is in the list of supported backends
    return supportedBackends.some((backend: string) => backend.toLowerCase() === model.family.toLowerCase());
  };

  // Filter models where the family is supported and flatten the list
  return availableModels.filter(isSupported);
});

// const selectedModel = computed({
//   get: () => {
//     const index = supportedModels.value.findIndex(
//       (model) => model.nickname === currentPipelineSettings.value.model.nickname
//     );
//     // Return -1 if not found, which will be handled properly by the select component
//     return index >= 0 ? index : undefined;
//   },
//   set: (v) => {
//     // Only proceed if v is a valid number and within bounds
//     if (typeof v === 'number' && v >= 0 && v < supportedModels.value.length) {
//       useCameraSettingsStore().changeCurrentPipelineSetting({ 
//         model: supportedModels.value[v] 
//       }, false);
//     }
//   }
// });

// Alternative approach - using the model object directly instead of index
const selectedModelDirect = computed({
  get: () => {
    return currentPipelineSettings.value.model.nickname;
  },
  set: (nickname) => {
    const model = supportedModels.value.find(m => m.nickname === nickname);
    if (model) {
      useCameraSettingsStore().changeCurrentPipelineSetting({ model }, false);
    }
  }
});
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.ObjectDetection">
    <!-- Option 1: Using index-based approach (fixed version) -->
    <!-- <pv-select
      v-model="selectedModel"
      label="Model"
      tooltip="The model used to detect objects in the camera feed"
      :select-cols="interactiveCols"
      :items="supportedModels.map((model, index) => ({ 
        name: model.nickname, 
        value: index 
      }))"
    /> -->

    <!-- Option 2: Using direct nickname approach (simpler) -->
    <pv-select
      v-model="selectedModelDirect"
      label="Model"
      tooltip="The model used to detect objects in the camera feed"
      :select-cols="interactiveCols"
      :items="supportedModels.map((model) => model.nickname)"
    />
  
    <pv-slider
      v-model="currentPipelineSettings.confidence"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Confidence"
      tooltip="The minimum confidence for a detection to be considered valid. Bigger numbers mean fewer but more probable detections are allowed through."
      :min="0"
      :max="1"
      :step="0.01"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ confidence: value }, false)
      "
    />
    <pv-range-slider
      v-model="contourArea"
      label="Area"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      :step="0.01"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourArea: value }, false)
      "
    />
    <pv-range-slider
      v-model="contourRatio"
      label="Ratio (W/H)"
      tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      :step="0.01"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourRatio: value }, false)
      "
    />
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOrientation"
      label="Target Orientation"
      tooltip="Used to determine how to calculate target landmarks, as well as aspect ratio"
      :items="['Portrait', 'Landscape']"
      :select-cols="interactiveCols"
      @update:modelValue="
        (value) =>
          useCameraSettingsStore().changeCurrentPipelineSetting(
            { contourTargetOrientation: typeof value === 'string' ? Number(value) : value },
            false
          )
      "
    />
    <pv-select
      v-model="currentPipelineSettings.contourSortMode"
      label="Target Sort"
      tooltip="Chooses the sorting mode used to determine the 'best' targets to provide to user code"
      :select-cols="interactiveCols"
      :items="['Largest', 'Smallest', 'Highest', 'Lowest', 'Rightmost', 'Leftmost', 'Centermost']"
      @update:modelValue="
        (value) =>
          useCameraSettingsStore().changeCurrentPipelineSetting(
            { contourSortMode: typeof value === 'string' ? Number(value) : value },
            false
          )
      "
    />
  </div>
</template>
