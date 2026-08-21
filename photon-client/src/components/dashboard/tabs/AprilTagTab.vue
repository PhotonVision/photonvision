<script setup lang="ts">
import { PipelineType, type AprilTagPipelineSettings, AprilTagFamily } from "@/types/PipelineTypes";
import PvSelect, { type SelectItem } from "@/components/common/pv-select.vue";
import PvSlider from "@/components/common/pv-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useDisplay } from "vuetify";
import type { ObjectDetectionModelProperties } from "@/types/SettingTypes";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<AprilTagPipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings as AprilTagPipelineSettings
);
const { mdAndDown } = useDisplay();
const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 8 : 7
);

// Whether this platform has a supported neural network backend at all
const mlSupported = computed<boolean>(() => useSettingsStore().general.supportedBackends.length > 0);

// Filters out models that are not supported by the current backend, and returns a flattened list.
const supportedModels = computed<ObjectDetectionModelProperties[]>(() => {
  const { availableModels, supportedBackends } = useSettingsStore().general;
  const isSupported = (model: ObjectDetectionModelProperties) => {
    // Check if model's family is in the list of supported backends
    return supportedBackends.some((backend: string) => backend.toLowerCase() === model.family.toLowerCase());
  };

  // Filter models where the family is supported and flatten the list
  return availableModels.filter(isSupported);
});

const modelWrapper = computed<SelectItem<string>[]>(() =>
  supportedModels.value.map((model) => ({
    name: model.nickname,
    value: model.modelPath
  }))
);

const selectedModel = computed<string>({
  get: () => currentPipelineSettings.value.tagModel?.modelPath ?? "",
  set: (value) => {
    const tagModel = supportedModels.value.find((supportedModel) => supportedModel.modelPath === value);
    if (tagModel) {
      useCameraSettingsStore().changeCurrentPipelineSetting({ tagModel }, true);
    }
  }
});
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.AprilTag">
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
      v-if="mlSupported"
      v-model="currentPipelineSettings.mltagEnabled"
      :switch-cols="interactiveCols"
      label="ML Assisted Detection"
      tooltip="Uses an object detection model to find tags in the image, then runs the AprilTag detector on each detected region"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ mltagEnabled: value }, false)
      "
    />
    <template v-if="mlSupported && currentPipelineSettings.mltagEnabled">
      <pv-select
        v-model="selectedModel"
        label="Tag Model"
        tooltip="The object detection model used to find AprilTags in the camera feed"
        :select-cols="interactiveCols"
        :items="modelWrapper"
      />
      <pv-slider
        v-model="currentPipelineSettings.mlConfidence"
        :slider-cols="interactiveCols"
        label="Confidence"
        tooltip="The minimum confidence for a tag detection to be considered valid. Bigger numbers mean fewer but more probable detections are allowed through."
        :min="0"
        :max="1"
        :step="0.01"
        @update:modelValue="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ mlConfidence: value }, false)
        "
      />
      <pv-slider
        v-model="currentPipelineSettings.mlNms"
        :slider-cols="interactiveCols"
        label="NMS Threshold"
        tooltip="The Non-Maximum Suppression threshold used to filter out overlapping detections. Higher values mean more detections are allowed through, but may result in false positives."
        :min="0"
        :max="1"
        :step="0.01"
        @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ mlNms: value }, false)"
      />
    </template>
  </div>
</template>
