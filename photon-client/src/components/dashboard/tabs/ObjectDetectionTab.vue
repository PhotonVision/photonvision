<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import PvSlider from "@/components/common/pv-slider.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = useCameraSettingsStore().currentPipelineSettings;

const interactiveCols = computed(
  () =>
    (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) &&
    (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)
)
  ? 9
  : 8;
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.ObjectDetection">
    <pv-slider
      v-model="currentPipelineSettings.confidence"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Confidence"
      tooltip="The minimum confidence for a detection to be considered valid. Bigger numbers mean fewer but more probable detections are allowed through."
      :min="0"
      :max="1"
      :step="0.01"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ confidence: value }, false)"
    />
  </div>
</template>
