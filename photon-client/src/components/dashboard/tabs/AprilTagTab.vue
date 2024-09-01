<script setup lang="ts">
import { PipelineType } from "@/types/PipelineTypes";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import type { ActivePipelineSettings } from "@/types/PipelineTypes";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useDisplay } from "vuetify";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ActivePipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings
);

const { mdAndDown } = useDisplay();
const labelCols = computed(
  () => 12 - (mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8)
);
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.AprilTag">
    <pv-dropdown
      v-model="currentPipelineSettings.tagFamily"
      :items="
        ['AprilTag 36h11 (6.5in)', 'AprilTag 25h9 (6in)', 'AprilTag 16h5 (6in)'].map((v, i) => ({ name: v, value: i }))
      "
      label="Target Family"
      :label-cols="labelCols"
      @update:model-value="
        (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ tagFamily: value }, false)
      "
    />
    <pv-number-slider
      v-model="currentPipelineSettings.decimate"
      label="Decimate"
      :label-cols="labelCols"
      :max="8"
      :min="1"
      :step="1"
      tooltip="Increases FPS at the expense of range by reducing image resolution initially"
      @update:model-value="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ decimate: value }, false)"
    />
    <pv-number-slider
      v-model="currentPipelineSettings.blur"
      label="Blur"
      :label-cols="labelCols"
      :max="5"
      :min="0"
      :step="0.1"
      tooltip="Gaussian blur added to the image, high FPS cost for slightly decreased noise"
      @update:model-value="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ blur: value }, false)"
    />
    <pv-number-slider
      v-model="currentPipelineSettings.threads"
      label="Threads"
      :label-cols="labelCols"
      :max="8"
      :min="1"
      :step="1"
      tooltip="Number of threads spawned by the AprilTag detector"
      @update:model-value="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threads: value }, false)"
    />
    <pv-switch
      v-model="currentPipelineSettings.refineEdges"
      label="Refine Edges"
      :label-cols="labelCols"
      tooltip="Further refines the AprilTag corner position initial estimate, suggested left on"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ refineEdges: value }, false)
      "
    />
    <pv-number-slider
      v-model="currentPipelineSettings.decisionMargin"
      label="Decision Margin Cutoff"
      :label-cols="labelCols"
      :max="250"
      :min="0"
      :step="1"
      tooltip="Tags with a 'margin' (decoding quality score) less than this wil be rejected. Increase this to reduce the number of false positive detections"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ decisionMargin: value }, false)
      "
    />
    <pv-number-slider
      v-model="currentPipelineSettings.numIterations"
      label="Pose Estimation Iterations"
      :label-cols="labelCols"
      :max="500"
      :min="0"
      :step="1"
      tooltip="Number of iterations the pose estimation algorithm will run, 50-100 is a good starting point"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ numIterations: value }, false)
      "
    />
  </div>
</template>
