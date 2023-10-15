<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import CvSlider from "@/components/common/cv-slider.vue";
import CvSwitch from "@/components/common/cv-switch.vue";
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
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.Aruco">
    <cv-switch
      v-model="currentPipelineSettings.useCornerRefinement"
      class="pt-2"
      label="Refine Corners"
      tooltip="Further refine the initial corners with subpixel accuracy."
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ useCornerRefinement: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.threshMinSize"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Thresh Min Size"
      tooltip="The minimum adaptive threshold window size."
      :min="3"
      :max="255"
      :step="2"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshMinSize: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.threshStepSize"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Thresh Step Size"
      tooltip="Smaller values will cause more steps between the min/max sizes. More, varied steps can improve detection robustness to lighting, but may decrease performance."
      :min="2"
      :max="128"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshStepSize: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.threshMaxSize"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Thresh Max Size"
      tooltip="The maximum adaptive threshold window size."
      :min="currentPipelineSettings.threshMinSize"
      :max="255"
      :step="2"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshMaxSize: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.threshConstant"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Thresh Constant"
      tooltip="Affects the threshold window mean value cutoff for all steps. Higher values can improve performance, but may harm detection rate."
      :min="0"
      :max="128"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshConstant: value }, false)"
    />
    <cv-switch
      v-model="currentPipelineSettings.debugThreshold"
      class="pt-2"
      label="Debug Threshold"
      tooltip="Display the first threshold step to the color stream."
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ debugThreshold: value }, false)"
    />
    <cv-switch
      v-model="currentPipelineSettings.useAruco3"
      class="pt-2"
      label="Use ArUco3 Speedup"
      tooltip="Enables an 'ArUco3' implementation which may increase performance at the cost of detection distance. This is similar to AprilTag's 'decimation'."
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ useAruco3: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.aruco3MinMarkerSideRatio"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="ArUco3 Min Marker Ratio"
      tooltip="Minimum side length of markers expressed as a ratio of the largest image dimension."
      :min="0"
      :max="0.1"
      :step="0.001"
      @input="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ aruco3MinMarkerSideRatio: value }, false)
      "
    />
    <cv-slider
      v-model="currentPipelineSettings.aruco3MinCanonicalImgSide"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="ArUco3 Min Canonical Side"
      tooltip="Minimum side length of the canonical image (marker after undoing perspective distortion)."
      :min="16"
      :max="128"
      :step="1"
      @input="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ aruco3MinCanonicalImgSide: value }, false)
      "
    />
  </div>
</template>
