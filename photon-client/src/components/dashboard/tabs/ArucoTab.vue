<script setup lang="ts">
import {
  ArucoPipelineSettings
} from "@/types/PipelineTypes";
import { computed } from "vue";
import { useDisplay } from "vuetify";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvRangeNumberSlider from "@/components/common/pv-range-number-slider.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import { CameraConfig } from "@/types/SettingTypes";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
  pipelineIndex: number
}>();

const targetPipelineSettings = computed<ArucoPipelineSettings>(() => props.cameraSettings.pipelineSettings.find((v) => v.pipelineIndex === props.pipelineIndex) as ArucoPipelineSettings);

const threshWinSizes = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.threshWinSizes) as [number, number]);

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(() => mdAndDown.value && (!clientStore.sidebarFolded || serverStore.isDriverMode) ? 3 : 5);
</script>

<template>
  <div>
    <pv-dropdown
      :items="
        ['AprilTag Family 36h11', 'AprilTag Family 25h9', 'AprilTag Family 16h5'].map((v, i) => ({ name: v, value: i }))
      "
      label="Target Family"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.tagFamily"
      @update:model-value="
        (value: number) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { tagFamily: value }, true, true)
      "
    />
    <pv-switch
      label="Refine Corners"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.useCornerRefinement"
      tooltip="Further refine the initial corners with subpixel accuracy."
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { useCornerRefinement: value }, true, true)
      "
    />
    <pv-range-number-slider
      label="Thresh Min/Max Size"
      :label-cols="labelCols"
      :max="255"
      :min="3"
      :model-value="threshWinSizes"
      :step="2"
      tooltip="The minimum and maximum adaptive threshold window size. Larger windows tend more towards global thresholding, but small windows can be weak to noise."
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { threshWinSizes: value }, true, true)
      "
    />
    <pv-number-slider
      label="Thresh Step Size"
      :label-cols="labelCols"
      :max="128"
      :min="2"
      :model-value="targetPipelineSettings.threshStepSize"
      :step="1"
      tooltip="Smaller values will cause more steps between the min/max sizes. More, varied steps can improve detection robustness to lighting, but may decrease performance."
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { threshStepSize: value }, true, true)
      "
    />
    <pv-number-slider
      label="Thresh Constant"
      :label-cols="labelCols"
      :max="128"
      :min="0"
      :model-value="targetPipelineSettings.threshConstant"
      :step="1"
      tooltip="Affects the threshold window mean value cutoff for all steps. Higher values can improve performance, but may harm detection rate."
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { threshConstant: value }, true, true)
      "
    />
    <pv-switch
      label="Debug Threshold"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.debugThreshold"
      tooltip="Display the first threshold step to the color stream."
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { debugThreshold: value }, true, true)
      "
    />
  </div>
</template>
