<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import PvSelect from "@/components/common/pv-select.vue";
import PvSlider from "@/components/common/pv-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
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
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.AprilTag">
    <pv-select
      v-model="currentPipelineSettings.tagFamily"
      label="Target family"
      :items="['AprilTag Family 36h11', 'AprilTag Family 25h9', 'AprilTag Family 16h5']"
      :select-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ tagFamily: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.decimate"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Decimate"
      tooltip="Increases FPS at the expense of range by reducing image resolution initially"
      :min="1"
      :max="8"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ decimate: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.blur"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Blur"
      tooltip="Gaussian blur added to the image, high FPS cost for slightly decreased noise"
      :min="0"
      :max="5"
      :step="0.1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ blur: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.threads"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Threads"
      tooltip="Number of threads spawned by the AprilTag detector"
      :min="1"
      :max="8"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threads: value }, false)"
    />
    <pv-switch
      v-model="currentPipelineSettings.refineEdges"
      class="pt-2"
      label="Refine Edges"
      tooltip="Further refines the AprilTag corner position initial estimate, suggested left on"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ refineEdges: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.decisionMargin"
      class="pt-2 pb-4"
      :slider-cols="interactiveCols"
      label="Decision Margin Cutoff"
      tooltip="Tags with a 'margin' (decoding quality score) less than this wil be rejected. Increase this to reduce the number of false positive detections"
      :min="0"
      :max="250"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ decisionMargin: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.numIterations"
      class="pt-2 pb-4"
      :slider-cols="interactiveCols"
      label="Pose Estimation Iterations"
      tooltip="Number of iterations the pose estimation algorithm will run, 50-100 is a good starting point"
      :min="0"
      :max="500"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ numIterations: value }, false)"
    />
  </div>
</template>
