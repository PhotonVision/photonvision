<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import PvSlider from "@/components/common/pv-slider.vue";
import pvSwitch from "@/components/common/pv-switch.vue";
import pvRangeSlider from "@/components/common/pv-range-slider.vue";
import pvSelect from "@/components/common/pv-select.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = useCameraSettingsStore().currentPipelineSettings;

// TODO fix pv-range-slider so that store access doesn't need to be deferred
const threshWinSizes = computed<[number, number]>({
  get: () => {
    if (currentPipelineSettings.pipelineType === PipelineType.Aruco) {
      return Object.values(currentPipelineSettings.threshWinSizes) as [number, number];
    } else {
      return [0, 0] as [number, number];
    }
  },
  set: (v) => {
    if (currentPipelineSettings.pipelineType === PipelineType.Aruco) {
      currentPipelineSettings.threshWinSizes = v;
    }
  }
});

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
    <pv-select
      v-model="currentPipelineSettings.tagFamily"
      label="Target family"
      :items="['AprilTag Family 36h11', 'AprilTag Family 25h9', 'AprilTag Family 16h5']"
      :select-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ tagFamily: value }, false)"
    />
    <pv-switch
      v-model="currentPipelineSettings.useCornerRefinement"
      class="pt-2"
      label="Refine Corners"
      tooltip="Further refine the initial corners with subpixel accuracy."
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ useCornerRefinement: value }, false)"
    />
    <pv-range-slider
      v-model="threshWinSizes"
      label="Thresh Min/Max Size"
      tooltip="The minimum and maximum adaptive threshold window size."
      :min="3"
      :max="255"
      :slider-cols="interactiveCols"
      :step="2"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshWinSizes: value }, false)"
    />
    <pv-slider
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
    <pv-slider
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
    <pv-switch
      v-model="currentPipelineSettings.debugThreshold"
      class="pt-2"
      label="Debug Threshold"
      tooltip="Display the first threshold step to the color stream."
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ debugThreshold: value }, false)"
    />
    <!-- <pv-switch
      v-model="currentPipelineSettings.useAruco3"
      class="pt-2"
      label="Use ArUco3 Speedup"
      tooltip="Enables an 'ArUco3' implementation which may increase performance at the cost of detection distance. This is similar to AprilTag's 'decimation'."
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ useAruco3: value }, false)"
    />
    <pv-slider
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
    <pv-slider
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
    /> -->
  </div>
</template>
