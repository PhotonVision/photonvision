<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import CvSlider from "@/components/common/cv-slider.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = useCameraSettingsStore().currentPipelineSettings;

const interactiveCols = computed(() => (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)) ? 9 : 8;
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.Aruco">
    <cv-slider
      v-model="currentPipelineSettings.decimate"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Decimate"
      tooltip="Increases FPS at the expense of range by reducing image resolution initially"
      :min="1"
      :max="8"
      @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({decimate: value}, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.numIterations"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Corner Iterations"
      tooltip="How many iterations are going to be used in order to refine corners. Higher values are lead to more accuracy at the cost of performance"
      :min="30"
      :max="1000"
      :step="5"
      @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({numIterations: value}, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.cornerAccuracy"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Corner Accuracy"
      tooltip="Minimum accuracy for the corners, lower is better but more performance intensive "
      :min="0.01"
      :max="100"
      :step="0.01"
      @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({cornerAccuracy: value}, false)"
    />
  </div>
</template>
