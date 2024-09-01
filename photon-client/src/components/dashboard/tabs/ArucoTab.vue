<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ActivePipelineSettings, ArucoPipelineSettings, PipelineType } from "@/types/PipelineTypes";
import { computed } from "vue";
import { useDisplay } from "vuetify";
import { useStateStore } from "@/stores/StateStore";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvRangeNumberSlider from "@/components/common/pv-range-number-slider.vue";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ActivePipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings
);

const threshWinSizes = computed<[number, number]>({
  get: () =>
    Object.values((useCameraSettingsStore().currentPipelineSettings as ArucoPipelineSettings).threshWinSizes) as [
      number,
      number
    ],
  set: (v) => ((useCameraSettingsStore().currentPipelineSettings as ArucoPipelineSettings).threshWinSizes = v)
});

const { mdAndDown } = useDisplay();
const labelCols = computed(
  () => 12 - (mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8)
);
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.Aruco">
    <pv-dropdown
      v-model="currentPipelineSettings.tagFamily"
      :items="
        ['AprilTag Family 36h11', 'AprilTag Family 25h9', 'AprilTag Family 16h5'].map((v, i) => ({ name: v, value: i }))
      "
      label="Target Family"
      :label-cols="labelCols"
      @update:model-value="
        (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ tagFamily: value }, false)
      "
    />
    <pv-switch
      v-model="currentPipelineSettings.useCornerRefinement"
      label="Refine Corners"
      :label-cols="labelCols"
      tooltip="Further refine the initial corners with subpixel accuracy."
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ useCornerRefinement: value }, false)
      "
    />
    <pv-range-number-slider
      v-model="threshWinSizes"
      label="Thresh Min/Max Size"
      :label-cols="labelCols"
      :max="255"
      :min="3"
      :step="2"
      tooltip="The minimum and maximum adaptive threshold window size. Larger windows tend more towards global thresholding, but small windows can be weak to noise."
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshWinSizes: value }, false)
      "
    />
    <pv-number-slider
      v-model="currentPipelineSettings.threshStepSize"
      label="Thresh Step Size"
      :label-cols="labelCols"
      :max="128"
      :min="2"
      :step="1"
      tooltip="Smaller values will cause more steps between the min/max sizes. More, varied steps can improve detection robustness to lighting, but may decrease performance."
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshStepSize: value }, false)
      "
    />
    <pv-number-slider
      v-model="currentPipelineSettings.threshConstant"
      label="Thresh Constant"
      :label-cols="labelCols"
      :max="128"
      :min="0"
      :step="1"
      tooltip="Affects the threshold window mean value cutoff for all steps. Higher values can improve performance, but may harm detection rate."
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshConstant: value }, false)
      "
    />
    <pv-switch
      v-model="currentPipelineSettings.debugThreshold"
      label="Debug Threshold"
      :label-cols="labelCols"
      tooltip="Display the first threshold step to the color stream."
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ debugThreshold: value }, false)
      "
    />
  </div>
</template>
