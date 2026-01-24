<script setup lang="ts">
import { PipelineType } from "@/types/PipelineTypes";
import PvSelect from "@/components/common/pv-select.vue";
import PvSlider from "@/components/common/pv-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import type { ActivePipelineSettings } from "@/types/PipelineTypes";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useDisplay } from "vuetify";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ActivePipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings
);
const { mdAndDown } = useDisplay();
const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 8 : 7
);

// Check if ML detection is available on this platform
const mlDetectionAvailable = computed(() => useSettingsStore().general.supportedBackends.length > 0);
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.AprilTag">
    <pv-select
      v-model="currentPipelineSettings.tagFamily"
      label="Target family"
      :items="['AprilTag 36h11 (6.5in)', 'AprilTag 16h5 (6in)']"
      :select-cols="interactiveCols"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ tagFamily: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.decimate"
      :slider-cols="interactiveCols"
      label="Decimate"
      tooltip="Increases FPS at the expense of range by reducing image resolution initially"
      :min="1"
      :max="8"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ decimate: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.blur"
      :slider-cols="interactiveCols"
      label="Blur"
      tooltip="Gaussian blur added to the image, high FPS cost for slightly decreased noise"
      :min="0"
      :max="5"
      :step="0.1"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ blur: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.threads"
      :slider-cols="interactiveCols"
      label="Threads"
      tooltip="Number of threads spawned by the AprilTag detector"
      :min="1"
      :max="8"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ threads: value }, false)"
    />
    <pv-slider
      v-model="currentPipelineSettings.decisionMargin"
      :slider-cols="interactiveCols"
      label="Decision Margin Cutoff"
      tooltip="Tags with a 'margin' (decoding quality score) less than this wil be rejected. Increase this to reduce the number of false positive detections"
      :min="0"
      :max="250"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ decisionMargin: value }, false)
      "
    />
    <pv-slider
      v-model="currentPipelineSettings.numIterations"
      :slider-cols="interactiveCols"
      label="Pose Estimation Iterations"
      tooltip="Number of iterations the pose estimation algorithm will run, 50-100 is a good starting point"
      :min="0"
      :max="500"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ numIterations: value }, false)
      "
    />
    <pv-switch
      v-model="currentPipelineSettings.refineEdges"
      :switch-cols="interactiveCols"
      label="Refine Edges"
      tooltip="Further refines the AprilTag corner position initial estimate, suggested left on"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ refineEdges: value }, false)
      "
    />
    <pv-switch
      v-model="currentPipelineSettings.atrCornerRefinementEnabled"
      :switch-cols="interactiveCols"
      label="ATR Corner Refinement"
      tooltip="Enables adaptive tag resizing corner refinement for improved accuracy when tags are scaled"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ atrCornerRefinementEnabled: value }, false)
      "
    />

    <!-- ML-Assisted Detection Section -->
    <v-divider class="mt-3 mb-2" v-if="mlDetectionAvailable" />
    <div v-if="mlDetectionAvailable" class="ml-settings-section">
      <p class="text-subtitle-2 mb-2">AI-Assisted Detection (NPU)</p>
      <pv-switch
        v-model="currentPipelineSettings.useMLDetection"
        :switch-cols="interactiveCols"
        label="Enable AI Detection"
        tooltip="Use NPU-accelerated ML model for faster AprilTag detection. Requires compatible hardware (RK3588 or QCS6490)"
        @update:modelValue="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ useMLDetection: value }, false)
        "
      />
      <div v-if="currentPipelineSettings.useMLDetection">
        <pv-slider
          v-model="currentPipelineSettings.mlConfidenceThreshold"
          :slider-cols="interactiveCols"
          label="Confidence Threshold"
          tooltip="Minimum confidence score for ML detection (0-1). Higher values reduce false positives"
          :min="0.1"
          :max="1.0"
          :step="0.05"
          @update:modelValue="
            (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ mlConfidenceThreshold: value }, false)
          "
        />
        <pv-slider
          v-model="currentPipelineSettings.mlNmsThreshold"
          :slider-cols="interactiveCols"
          label="NMS Threshold"
          tooltip="Non-maximum suppression threshold for overlapping detections (0-1)"
          :min="0.1"
          :max="1.0"
          :step="0.05"
          @update:modelValue="
            (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ mlNmsThreshold: value }, false)
          "
        />
        <pv-slider
          v-model="currentPipelineSettings.mlRoiExpansionFactor"
          :slider-cols="interactiveCols"
          label="ROI Expansion"
          tooltip="Factor to expand detected regions for traditional decoding (1.0-2.0). Larger values help with edge cases"
          :min="1.0"
          :max="2.0"
          :step="0.1"
          @update:modelValue="
            (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ mlRoiExpansionFactor: value }, false)
          "
        />
        <pv-switch
          v-model="currentPipelineSettings.mlFallbackToTraditional"
          :switch-cols="interactiveCols"
          label="Fallback to Traditional"
          tooltip="If ML detection finds no tags, fall back to traditional full-frame detection"
          @update:modelValue="
            (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ mlFallbackToTraditional: value }, false)
          "
        />
      </div>
    </div>
  </div>
</template>
