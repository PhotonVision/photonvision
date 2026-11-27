<script setup lang="ts">
import {
  FiducialAlgorithm,
  PipelineType,
  type AprilTagPipelineSettings,
  AprilTagFamily
} from "@/types/PipelineTypes";
import { WebsocketPipelineType, type WebsocketNumberPair } from "@/types/WebsocketDataTypes";

import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useCustomBreakpoints } from "@/lib/Breakpoints";
import { normalizeNumberPair } from "@/lib/PhotonUtils";

const currentPipelineSettings = computed<AprilTagPipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings as AprilTagPipelineSettings
);
const apriltagSettings = computed<AprilTagPipelineSettings>(
  () => currentPipelineSettings.value as AprilTagPipelineSettings
);
const isAruco = computed(() => currentPipelineSettings.value.algorithm === FiducialAlgorithm.Aruco);

const algorithm = computed<FiducialAlgorithm>(() =>
  currentPipelineSettings.value.algorithm
);
const setAlgorithm = (value: FiducialAlgorithm) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ algorithm: value }, false);
};
const breakpoints = useCustomBreakpoints();
const mdAndDown = breakpoints.smallerOrEqual("md");
const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 8 : 7
);
</script>

<template>

<div
  v-if="
      currentPipelineSettings.pipelineType === PipelineType.AprilTag
    "
>
    <pv-select
      :model-value="algorithm"
      label="Algorithm"
      tooltip="Fiducial detection algorithm used by this pipeline. Switching algorithms overwrites this pipeline's settings with their defaults."
      :items="[
        { name: 'AprilTag', value: FiducialAlgorithm.AprilTag },
        { name: 'ArUco', value: FiducialAlgorithm.Aruco }
      ]"
      :select-cols="interactiveCols"
      @update:modelValue="(value: FiducialAlgorithm) => setAlgorithm(value)"
    />
    <pv-select
      v-model="currentPipelineSettings.tagFamily"
      label="Target family"
      :items="[
        { value: AprilTagFamily.Family36h11, name: 'AprilTag 36h11 (6.5in)' },
        { value: AprilTagFamily.Family16h5, name: 'AprilTag 16h5 (6in)' }
      ]"
      :select-cols="interactiveCols"
      @update:modelValue="
        (value: AprilTagFamily) => useCameraSettingsStore().changeCurrentPipelineSetting({ tagFamily: value }, false)
      "
    />
    <template v-if="!isAruco">
      <pv-slider
        v-model="apriltagSettings.decimate"
        :slider-cols="interactiveCols"
        label="Decimate"
        tooltip="Increases FPS at the expense of range by reducing image resolution initially"
        :min="1"
        :max="8"
        @update:modelValue="
          (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ decimate: value }, false)
        "
      />
      <pv-slider
        v-model="apriltagSettings.blur"
        :slider-cols="interactiveCols"
        label="Blur"
        tooltip="Gaussian blur added to the image, high FPS cost for slightly decreased noise"
        :min="0"
        :max="5"
        :step="0.1"
        @update:modelValue="
          (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ blur: value }, false)
        "
      />
      <pv-slider
        v-model="apriltagSettings.threads"
        :slider-cols="interactiveCols"
        label="Threads"
        tooltip="Number of threads spawned by the AprilTag detector"
        :min="1"
        :max="8"
        @update:modelValue="
          (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ threads: value }, false)
        "
      />
      <pv-slider
        v-model="apriltagSettings.decisionMargin"
        :slider-cols="interactiveCols"
        label="Decision Margin Cutoff"
        tooltip="Tags with a 'margin' (decoding quality score) less than this wil be rejected. Increase this to reduce the number of false positive detections"
        :min="0"
        :max="250"
        @update:modelValue="
          (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ decisionMargin: value }, false)
        "
      />
      <pv-slider
        v-model="apriltagSettings.numIterations"
        :slider-cols="interactiveCols"
        label="Pose Estimation Iterations"
        tooltip="Number of iterations the pose estimation algorithm will run, 50-100 is a good starting point"
        :min="0"
        :max="500"
        @update:modelValue="
          (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ numIterations: value }, false)
        "
      />
      <pv-switch
        v-model="apriltagSettings.refineEdges"
        :switch-cols="interactiveCols"
        label="Refine Edges"
        tooltip="Further refines the AprilTag corner position initial estimate, suggested left on"
        @update:modelValue="
          (value: boolean | undefined) =>
            value !== undefined && useCameraSettingsStore().changeCurrentPipelineSetting({ refineEdges: value }, false)
        "
      />
    </template>
    <template v-if="isAruco">
      <pv-range-slider
        v-model="apriltagSettings.threshWinSizes"
        label="Thresh Min/Max Size"
        tooltip="The minimum and maximum adaptive threshold window size. Larger windows tend more towards global thresholding, but small windows can be weak to noise."
        :min="3"
        :max="255"
        :slider-cols="interactiveCols"
        :step="2"
        @update:modelValue="
          (value: WebsocketNumberPair | [number, number]) =>
            useCameraSettingsStore().changeCurrentPipelineSetting({ threshWinSizes: normalizeNumberPair(value) }, false)
        "
      />
      <pv-slider
        v-model="apriltagSettings.threshStepSize"
        :slider-cols="interactiveCols"
        label="Thresh Step Size"
        tooltip="Smaller values will cause more steps between the min/max sizes. More, varied steps can improve detection robustness to lighting, but may decrease performance."
        :min="2"
        :max="128"
        :step="1"
        @update:modelValue="
          (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshStepSize: value }, false)
        "
      />
      <pv-slider
        v-model="apriltagSettings.threshConstant"
        :slider-cols="interactiveCols"
        label="Thresh Constant"
        tooltip="Affects the threshold window mean value cutoff for all steps. Higher values can improve performance, but may harm detection rate."
        :min="0"
        :max="128"
        :step="1"
        @update:modelValue="
          (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ threshConstant: value }, false)
        "
      />
      <pv-switch
        v-model="apriltagSettings.useCornerRefinement"
        label="Refine Corners"
        tooltip="Further refine the initial corners with subpixel accuracy."
        :switch-cols="interactiveCols"
        @update:modelValue="
          (value: boolean | undefined) =>
            value !== undefined &&
            useCameraSettingsStore().changeCurrentPipelineSetting({ useCornerRefinement: value }, false)
        "
      />
      <pv-switch
        v-model="apriltagSettings.debugThreshold"
        label="Debug Threshold"
        tooltip="Display the first threshold step to the color stream."
        :switch-cols="interactiveCols"
        @update:modelValue="
          (value: boolean | undefined) =>
            value !== undefined &&
            useCameraSettingsStore().changeCurrentPipelineSetting({ debugThreshold: value }, false)
        "
      />
      <pv-switch
        v-model="apriltagSettings.useAruco3"
        label="ArUco3"
        tooltip="Use ArUco3 markers."
        :switch-cols="interactiveCols"
        @update:modelValue="
          (value: boolean | undefined) =>
            value !== undefined && useCameraSettingsStore().changeCurrentPipelineSetting({ useAruco3: value }, false)
        "
      />
      <pv-slider
        v-model="apriltagSettings.aruco3MinMarkerSideRatio"
        :slider-cols="interactiveCols"
        label="ArUco3 Min Marker Side Ratio"
        :min="0.01"
        :max="0.5"
        :step="0.01"
        @update:modelValue="
          (value: number) =>
            useCameraSettingsStore().changeCurrentPipelineSetting({ aruco3MinMarkerSideRatio: value }, false)
        "
      />
      <pv-slider
        v-model="apriltagSettings.aruco3MinCanonicalImgSide"
        :slider-cols="interactiveCols"
        label="ArUco3 Min Canonical Image Side"
        :min="16"
        :max="256"
        :step="1"
        @update:modelValue="
          (value: number) =>
            useCameraSettingsStore().changeCurrentPipelineSetting({ aruco3MinCanonicalImgSide: value }, false)
        "
      />
    </template>
  </div>
</template>
