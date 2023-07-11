<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import CvSlider from "@/components/common/cv-slider.vue";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = useCameraSettingsStore().currentPipelineSettings;
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.Aruco">
    <cv-slider
        v-model="currentPipelineSettings.decimate"
        class="pt-2"
        :slider-cols="10"
        label="Decimate"
        tooltip="Increases FPS at the expense of range by reducing image resolution initially"
        :min="1"
        :max="8"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({decimate: value}, false)"
    />
    <cv-slider
        v-model="currentPipelineSettings.numIterations"
        class="pt-2"
        :slider-cols="10"
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
        :slider-cols="10"
        label="Corner Accuracy"
        tooltip="Minimum accuracy for the corners, lower is better but more performance intensive "
        :min="0.01"
        :max="100"
        :step="0.01"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({cornerAccuracy: value}, false)"
    />
  </div>
</template>
