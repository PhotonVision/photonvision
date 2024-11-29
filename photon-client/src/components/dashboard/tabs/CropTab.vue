<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type AprilTagPipelineSettings, PipelineType } from "@/types/PipelineTypes";
import PvSlider from "@/components/common/pv-slider.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";
const currentPipelineSettings = computed<AprilTagPipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings as AprilTagPipelineSettings
);
const frame_width = computed(() => useCameraSettingsStore().currentVideoFormat.resolution.width);
const frame_height = computed(() => useCameraSettingsStore().currentVideoFormat.resolution.height);
const static_x = computed<number>({
  get: () => currentPipelineSettings.value.static_x || 0,
  set: (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ static_x: value }, false)
});
const static_y = computed<number>({
  get: () => currentPipelineSettings.value.static_y || 0,
  set: (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ static_y: value }, false)
});
const static_width = computed<number>({
  get: () => currentPipelineSettings.value.static_width || frame_width.value,
  set: (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ static_width: value }, false)
});
const static_height = computed<number>({
  get: () => currentPipelineSettings.value.static_height || frame_height.value,
  set: (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ static_height: value }, false)
});
const interactiveCols = computed(() =>
  (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) &&
  (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)
    ? 9
    : 8
);
</script>
<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.AprilTag">
    <!-- static crop -->
    <span>Static Crop</span>
    <pv-slider
      v-model="static_x"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="X"
      tooltip="The X coordinate of the top left corner of the statically cropped area"
      :min="0"
      :max="frame_width"
    />
    <pv-slider
      v-model="static_y"
      :slider-cols="interactiveCols"
      label="Y"
      tooltip="The Y coordinate of the top left corner of the statically cropped area"
      :min="0"
      :max="frame_height"
    />
    <pv-slider
      v-model="static_width"
      :slider-cols="interactiveCols"
      label="Width"
      tooltip="The width of the statically cropped area"
      :min="1"
      :max="frame_width"
    />
    <pv-slider
      v-model="static_height"
      :slider-cols="interactiveCols"
      label="Height"
      tooltip="The height of the statically cropped area"
      :min="1"
      :max="frame_height"
    />
  </div>
</template>
