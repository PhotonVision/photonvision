<script setup lang="ts">
import {
  PipelineType,
  type VkAprilTagPipelineSettings,
  AprilTagFamily,
  VkAprilTagPoseEstimatorBackend
} from "@/types/PipelineTypes";
import PvSelect from "@/components/common/pv-select.vue";
import PvSlider from "@/components/common/pv-slider.vue";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useDisplay } from "vuetify";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<VkAprilTagPipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings as VkAprilTagPipelineSettings
);
const { mdAndDown } = useDisplay();
const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 8 : 7
);

// "Automatic" (-1) first, then every device the backend enumerated - device.description already
// comes from vkapriltag's own Context::DescribeDevice(), so the formatting stays owned by that
// library, not duplicated here.
const vulkanDeviceItems = computed(() => [
  { value: -1, name: "Automatic" },
  ...useSettingsStore().general.vulkanDevices.map((device) => ({
    value: device.index,
    name: device.description
  }))
]);
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.AprilTagVulkan">
    <v-alert
      density="compact"
      variant="tonal"
      type="warning"
      class="mb-3"
      text="BETA: this backend has no blur or edge-refinement tuning (not supported by the underlying
        library), and falls back to the CPU detector automatically if Vulkan isn't usable on this
        device or the chosen decimation doesn't evenly divide the camera's resolution."
    />
    <pv-select
      v-model="currentPipelineSettings.tagFamily"
      label="Target family"
      :items="[
        { value: AprilTagFamily.Family36h11, name: 'AprilTag 36h11 (6.5in)' },
        { value: AprilTagFamily.Family16h5, name: 'AprilTag 16h5 (6in)' }
      ]"
      :select-cols="interactiveCols"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ tagFamily: value }, false)"
    />
    <pv-select
      v-model="currentPipelineSettings.vulkanDeviceIndex"
      label="Vulkan Device"
      tooltip="Which GPU (or vkapriltag's own automatic scoring) to run detection on"
      :items="vulkanDeviceItems"
      :select-cols="interactiveCols"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ vulkanDeviceIndex: value }, false)
      "
    />
    <pv-select
      v-model="currentPipelineSettings.decimation"
      label="Decimation"
      tooltip="Downsampling factor before detection; must evenly divide the camera's resolution or
        this pipeline falls back to CPU. 1 = full resolution (slowest, most accurate), 2 = this
        pipeline's original behavior, 4 = fastest, least precise."
      :items="[
        { value: 1, name: '1x (full resolution)' },
        { value: 2, name: '2x (default)' },
        { value: 4, name: '4x (fastest)' }
      ]"
      :select-cols="interactiveCols"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ decimation: value }, false)"
    />
    <pv-select
      v-model="currentPipelineSettings.poseEstimatorBackend"
      label="Pose Estimator"
      tooltip="CPU (WPILib) is the well-tested default. Vulkan-native pose estimation is new and
        faster per-tag, but has not yet been benchmarked against WPILib's implementation on real
        hardware/data - compare results carefully before relying on it."
      :items="[
        { value: VkAprilTagPoseEstimatorBackend.CPU, name: 'CPU (WPILib)' },
        { value: VkAprilTagPoseEstimatorBackend.VULKAN, name: 'Vulkan-native (experimental)' }
      ]"
      :select-cols="interactiveCols"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ poseEstimatorBackend: value }, false)
      "
    />
    <pv-slider
      v-model="currentPipelineSettings.cpuThreads"
      :slider-cols="interactiveCols"
      label="CPU Threads"
      tooltip="Threads for the CPU tail (per-blob quad fitting); 0 uses all available cores"
      :min="0"
      :max="8"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ cpuThreads: value }, false)"
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
  </div>
</template>
