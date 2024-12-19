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
      v-model="currentPipelineSettings.static_x"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="X"
      tooltip="The X coordinate of the top left corner of the statically cropped area"
      :min="0"
      :max="frame_width"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ static_x: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.static_y"
      :slider-cols="interactiveCols"
      label="Y"
      tooltip="The Y coordinate of the top left corner of the statically cropped area"
      :min="0"
      :max="frame_height"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ static_y: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.static_width"
      :slider-cols="interactiveCols"
      label="Width"
      tooltip="The width of the statically cropped area"
      :min="1"
      :max="frame_width"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ static_width: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.static_height"
      :slider-cols="interactiveCols"
      label="Height"
      tooltip="The height of the statically cropped area"
      :min="1"
      :max="frame_height"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ static_height: value }, false)"
    />
  </div>
</template>
