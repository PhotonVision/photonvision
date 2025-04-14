<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType, type ActivePipelineSettings } from "@/types/PipelineTypes";
import PvSlider from "@/components/common/pv-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvRangeSlider from "@/components/common/pv-range-slider.vue";
import PvSelect from "@/components/common/pv-select.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ActivePipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings
);

const interactiveCols = computed(() =>
  (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) &&
  (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)
    ? 8
    : 7
);
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.Aruco">
    <pv-select
      v-model="currentPipelineSettings.tagFamily"
      label="Target family"
      :items="['AprilTag Family 36h11', 'AprilTag Family 16h5']"
      :select-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ tagFamily: value }, false)"
    />
    <pv-range-slider
      v-model="currentPipelineSettings.threshWinSizes"
      label="Thresh Min/Max Size"
      tooltip="The minimum and maximum adaptive threshold window size. Larger windows tend more towards global thresholding, but small windows can be weak to noise."
      :min="3"
      :max="255"
      :slider-cols="interactiveCols"
      :step="2"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshWinSizes: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.threshStepSize"
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
      :slider-cols="interactiveCols"
      label="Thresh Constant"
      tooltip="Affects the threshold window mean value cutoff for all steps. Higher values can improve performance, but may harm detection rate."
      :min="0"
      :max="128"
      :step="1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshConstant: value }, false)"
    />
    <pv-switch
      v-model="currentPipelineSettings.useCornerRefinement"
      label="Refine Corners"
      tooltip="Further refine the initial corners with subpixel accuracy."
      :switch-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ useCornerRefinement: value }, false)"
    />
    <pv-switch
      v-model="currentPipelineSettings.debugThreshold"
      label="Debug Threshold"
      tooltip="Display the first threshold step to the color stream."
      :switch-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ debugThreshold: value }, false)"
    />
  </div>
</template>
