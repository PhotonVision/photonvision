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
    <cv-slider
      v-model="currentPipelineSettings.threshMinSize"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Thresh Min Size"
      tooltip=""
      :min="3"
      :max="128"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshMinSize: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.threshStepSize"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Thresh Step Size"
      tooltip=""
      :min="1"
      :max="128"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshStepSize: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.threshMaxSize"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Thresh Max Size"
      tooltip=""
      :min="3"
      :max="256"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshMaxSize: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.threshConstant"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Thresh Constant"
      tooltip=""
      :min="0"
      :max="128"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshConstant: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.errorCorrectionRate"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Error Correction Ratio"
      tooltip="Bits allowed to be corrected, expressed as a ratio of the tag families theoretical maximum."
      :min="0"
      :max="1.0"
      :step="0.05"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ errorCorrectionRate: value }, false)"
    />
    <cv-switch
      v-model="currentPipelineSettings.useCornerRefinement"
      class="pt-2"
      label="Refine Corners"
      tooltip="Further refine the initial corners with subpixel accuracy. This should be considered mandatory for 3D estimation."
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ useCornerRefinement: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.refineNumIterations"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Refinement Iterations"
      tooltip="Maximum corner refinement iterations before stopping. Higher values can increase accuracy at the cost of performance."
      :disabled="!currentPipelineSettings.useCornerRefinement"
      :min="30"
      :max="1000"
      :step="5"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ refineNumIterations: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.refineMinErrorPx"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Refinement Minimum Error"
      tooltip="Minimum corner refinement error in pixels before stopping. Lower values can increase accuracy at the cost of performance."
      :disabled="!currentPipelineSettings.useCornerRefinement"
      :min="0.01"
      :max="1"
      :step="0.005"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ refineMinErrorPx: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.refineWinSize"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Refinement Window Size"
      tooltip="Ref "
      :disabled="!currentPipelineSettings.useCornerRefinement"
      :min="2"
      :max="20"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ refineWinSize: value }, false)"
    />
    <cv-slider
      v-model="currentPipelineSettings.cornerRefinementStrategy"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Corner refinement strategy"
      tooltip=""
      :min="0"
      :max="3"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ cornerRefinementStrategy: value }, false)"
    />
    <cv-switch
      v-model="currentPipelineSettings.useAruco3"
      class="pt-2"
      label="Use ArUco3 Speedup"
      tooltip="Enables an 'ArUco3' implementation which may increase performance at the cost of accuracy."
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
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ aruco3MinMarkerSideRatio: value }, false)"
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
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ aruco3MinCanonicalImgSide: value }, false)"
    />
  </div>
</template>
