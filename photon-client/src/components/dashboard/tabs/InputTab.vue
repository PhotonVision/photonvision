<script setup lang="ts">
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { computed } from "vue";
import { getResolutionString, resolutionsAreEqual } from "@/lib/PhotonUtils";
import { useDisplay } from "vuetify";
import type { DropdownSelectItem } from "@/types/Components";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import { CameraConfig, VideoFormat } from "@/types/SettingTypes";
import { PossiblePipelineSettings } from "@/types/PipelineTypes";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
  pipelineIndex: number
}>();

const targetPipelineSettings = computed<PossiblePipelineSettings>(() => props.cameraSettings.pipelineSettings.find((v) => v.pipelineIndex === props.pipelineIndex) as PossiblePipelineSettings);
const targetVideoFormat = computed<VideoFormat>(() => {
  const targetIndex = targetPipelineSettings.value.cameraVideoModeIndex;
  return props.cameraSettings.videoFormats.find((v) => v.sourceIndex === targetIndex) as VideoFormat;
});

// Due to something with libcamera or something else IDK much about, the 90° rotations need to be disabled if the libcamera drivers are being used.
const cameraRotations = computed<DropdownSelectItem<number>[]>(() =>
  ["Normal", "90° CW", "180°", "90° CCW"].map((v, i) => ({
    name: v,
    value: i,
    disabled: props.cameraSettings.isCSICamera ? [1, 3].includes(i) : false
  }))
);

const streamDivisors = [1, 2, 4, 6];
const getFilteredStreamDivisors = (): number[] => {
  const currentResolutionWidth = targetVideoFormat.value.resolution.width;
  return streamDivisors.filter(
    (x) =>
      serverStore.isDriverMode ||
      !serverStore.instanceConfig?.gpuAccelerationSupported ||
      currentResolutionWidth / x < 400
  );
};
const getNumberOfSkippedDivisors = () => streamDivisors.length - getFilteredStreamDivisors().length;

const cameraResolutions = computed<DropdownSelectItem<number>[]>(() => props.cameraSettings.videoFormats.map<DropdownSelectItem<number>>((f, i) => ({
    name: `${getResolutionString(f.resolution)} at ${f.fps} FPS, ${f.pixelFormat}`,
    value: i
  }))
);
const handleResolutionChange = (value: number) => {
  serverStore.updatePipelineSettings(props.cameraSettings.cameraIndex, props.pipelineIndex, { cameraVideoModeIndex: value }, true, true);

  serverStore.updatePipelineSettings(props.cameraSettings.cameraIndex, props.pipelineIndex, { streamingFrameDivisor: getNumberOfSkippedDivisors() }, true, true);

  const targetFormat = targetVideoFormat.value;
  if (!props.cameraSettings.calibrations.some((v) => resolutionsAreEqual(v.resolution, targetFormat.resolution))) {
    serverStore.updatePipelineSettings(props.cameraSettings.cameraIndex, props.pipelineIndex, { solvePNPEnabled: false }, true, true);
  }
};

const streamResolutions = computed<DropdownSelectItem<number>[]>(() => {
  const streamDivisors = getFilteredStreamDivisors();
  const currentResolution = targetVideoFormat.value.resolution;
  return streamDivisors.map((x, i) => ({
    name: `${getResolutionString({
      width: Math.floor(currentResolution.width / x),
      height: Math.floor(currentResolution.height / x)
    })}`,
    value: i
  }));
});
const handleStreamResolutionChange = (value: number) => {
  serverStore.updatePipelineSettings(props.cameraSettings.cameraIndex, props.pipelineIndex,
    { streamingFrameDivisor: value + getNumberOfSkippedDivisors() },
    true, true
  );
};

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(() => mdAndDown.value && (!clientStore.sidebarFolded || serverStore.isDriverMode) ? 3 : 5);
</script>

<template>
  <div>
    <pv-number-slider
      :disabled="targetPipelineSettings.cameraAutoExposure"
      label="Exposure"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="targetPipelineSettings.cameraExposure"
      :step="0.1"
      tooltip="Directly controls how much light is allowed to fall onto the sensor, which affects apparent brightness"
      @update:model-value="
        (args) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { cameraExposure: args }, true, true)
      "
    />
    <pv-number-slider
      label="Brightness"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="targetPipelineSettings.cameraBrightness"
      @update:model-value="
        (args) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex,{ cameraBrightness: args }, true, true)
      "
    />
    <pv-switch
      label="Auto Exposure"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.cameraAutoExposure"
      tooltip="Enables or Disables camera automatic adjustment for current lighting conditions"
      @update:model-value="
        (args) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { cameraAutoExposure: args }, true, true)
      "
    />
    <pv-number-slider
      v-if="targetPipelineSettings.cameraGain >= 0"
      label="Camera Gain"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="targetPipelineSettings.cameraGain"
      tooltip="Controls camera gain, similar to brightness"
      @update:model-value="(args) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { cameraGain: args }, true, true)"
    />
    <pv-number-slider
      v-if="targetPipelineSettings.cameraRedGain !== -1"
      label="Red AWB Gain"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="targetPipelineSettings.cameraRedGain"
      tooltip="Controls red automatic white balance gain, which affects how the camera captures colors in different conditions"
      @update:model-value="
        (args) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { cameraRedGain: args }, true, true)
      "
    />
    <pv-number-slider
      v-if="targetPipelineSettings.cameraBlueGain !== -1"
      label="Blue AWB Gain"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="targetPipelineSettings.cameraBlueGain"
      tooltip="Controls blue automatic white balance gain, which affects how the camera captures colors in different conditions"
      @update:model-value="
        (args) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { cameraBlueGain: args }, true, true)
      "
    />
    <!-- Disable camera orientation as stop gap for Issue 1084 until calibration data gets rotated. https://github.com/PhotonVision/photonvision/issues/1084 -->
    <v-alert
      v-show="
        serverStore.isCurrentVideoFormatCalibrated &&
          targetPipelineSettings.inputImageRotationMode != 0
      "
      class="mt-3"
      density="compact"
      rounded
      text="A known bug affects rotation of calibrated camera. Turn off rotation here and rotate using cameraToRobotTransform in your robot code."
      type="warning"
    />
    <pv-dropdown
      :disabled="
        serverStore.isCurrentVideoFormatCalibrated &&
          targetPipelineSettings.inputImageRotationMode == 0
      "
      :items="cameraRotations"
      label="Orientation"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.inputImageRotationMode"
      tooltip="Rotates the camera stream. Rotation not available when camera has been calibrated."
      @update:model-value="
        (args: number) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { inputImageRotationMode: args }, true, true)
      "
    />
    <pv-dropdown
      :items="cameraResolutions"
      label="Resolution"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.cameraVideoModeIndex"
      tooltip="Resolution and FPS the camera should directly capture at"
      @update:model-value="(args: number) => handleResolutionChange(args)"
    />
    <pv-dropdown
      :items="streamResolutions"
      label="Stream Resolution"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.streamingFrameDivisor"
      tooltip="Resolution to which camera frames are downscaled for streaming to the dashboard"
      @update:model-value="(args: number) => handleStreamResolutionChange(args)"
    />
  </div>
</template>
