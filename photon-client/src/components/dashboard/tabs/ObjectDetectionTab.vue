<script setup lang="ts">
import { ObjectDetectionPipelineSettings } from "@/types/PipelineTypes";
import { computed } from "vue";
import { useDisplay } from "vuetify";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvDropdown from "@/components/common/pv-dropdown.vue";
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

const targetPipelineSettings = computed<ObjectDetectionPipelineSettings>(() => props.cameraSettings.pipelineSettings.find((v) => v.pipelineIndex === props.pipelineIndex) as ObjectDetectionPipelineSettings);

const contourArea = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.contourArea) as [number, number]);
const contourRatio = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.contourRatio) as [number, number]);

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(() => mdAndDown.value && (!clientStore.sidebarFolded || serverStore.isDriverMode) ? 3 : 5);
</script>

<template>
  <div>
    <pv-number-slider
      label="Confidence"
      :label-cols="labelCols"
      :max="1"
      :min="0"
      :model-value="targetPipelineSettings.confidence"
      :step="0.01"
      tooltip="The minimum confidence for a detection to be considered valid. Bigger numbers mean fewer but more probable detections are allowed through."
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { confidence: value }, true, true)
      "
    />
    <pv-range-number-slider
      label="Area"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="contourArea"
      :step="0.01"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourArea: value }, true, true)
      "
    />
    <pv-range-number-slider
      label="Ratio (W/H)"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="contourRatio"
      :step="0.01"
      tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourRatio: value }, true, true)
      "
    />
    <pv-dropdown
      :items="['Portrait', 'Landscape'].map((v, i) => ({ name: v, value: i }))"
      label="Target Orientation"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.contourTargetOrientation"
      tooltip="Used to determine how to calculate target landmarks, as well as aspect ratio"
      @update:model-value="
        (value: number) =>
          serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourTargetOrientation: value }, true, true)
      "
    />
    <pv-dropdown
      :items="
        ['Largest', 'Smallest', 'Highest', 'Lowest', 'Rightmost', 'Leftmost', 'Centermost'].map((v, i) => ({
          name: v,
          value: i
        }))
      "
      label="Target Sort"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.contourSortMode"
      tooltip="Chooses the sorting mode used to determine the 'best' targets to provide to user code"
      @update:model-value="
        (value: number) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourSortMode: value }, true, true)
      "
    />
  </div>
</template>
