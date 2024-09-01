<script setup lang="ts">
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed } from "vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { getResolutionString } from "@/lib/PhotonUtils";
import { useDisplay } from "vuetify";
import type { DropdownSelectItem } from "@/types/Components";

// Due to something with libcamera or something else IDK much about, the 90° rotations need to be disabled if the libcamera drivers are being used.
const cameraRotations = computed<DropdownSelectItem<number>[]>(() =>
  ["Normal", "90° CW", "180°", "90° CCW"].map((v, i) => ({
    name: v,
    value: i,
    disabled: useCameraSettingsStore().isCSICamera ? [1, 3].includes(i) : false
  }))
);

const streamDivisors = [1, 2, 4, 6];
const getFilteredStreamDivisors = (): number[] => {
  const currentResolutionWidth = useCameraSettingsStore().currentVideoFormat.resolution.width;
  return streamDivisors.filter(
    (x) =>
      useCameraSettingsStore().isDriverMode ||
      !useSettingsStore().gpuAccelerationEnabled ||
      currentResolutionWidth / x < 400
  );
};
const getNumberOfSkippedDivisors = () => streamDivisors.length - getFilteredStreamDivisors().length;

const cameraResolutions = computed<DropdownSelectItem<number>[]>(() =>
  useCameraSettingsStore().currentCameraSettings.validVideoFormats.map((f, i) => ({
    name: `${getResolutionString(f.resolution)} at ${f.fps} FPS, ${f.pixelFormat}`,
    value: i
  }))
);
const handleResolutionChange = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraVideoModeIndex: value }, false);

  useCameraSettingsStore().changeCurrentPipelineSetting({ streamingFrameDivisor: getNumberOfSkippedDivisors() }, false);
  useCameraSettingsStore().currentPipelineSettings.streamingFrameDivisor = 0;

  if (!useCameraSettingsStore().isCurrentVideoFormatCalibrated) {
    useCameraSettingsStore().changeCurrentPipelineSetting({ solvePNPEnabled: false }, true);
  }
};

const streamResolutions = computed<DropdownSelectItem<number>[]>(() => {
  const streamDivisors = getFilteredStreamDivisors();
  const currentResolution = useCameraSettingsStore().currentVideoFormat.resolution;
  return streamDivisors.map((x, i) => ({
    name: `${getResolutionString({
      width: Math.floor(currentResolution.width / x),
      height: Math.floor(currentResolution.height / x)
    })}`,
    value: i
  }));
});
const handleStreamResolutionChange = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting(
    { streamingFrameDivisor: value + getNumberOfSkippedDivisors() },
    false
  );
};

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(
  () => 12 - (mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8)
);
</script>

<template>
  <div>
    <pv-number-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraExposure"
      :disabled="useCameraSettingsStore().currentCameraSettings.pipelineSettings.cameraAutoExposure"
      label="Exposure"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :step="0.1"
      tooltip="Directly controls how much light is allowed to fall onto the sensor, which affects apparent brightness"
      @update:model-value="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraExposure: args }, false)
      "
    />
    <pv-number-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraBrightness"
      label="Brightness"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      @update:model-value="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraBrightness: args }, false)
      "
    />
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraAutoExposure"
      label="Auto Exposure"
      :label-cols="labelCols"
      tooltip="Enables or Disables camera automatic adjustment for current lighting conditions"
      @update:model-value="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraAutoExposure: args }, false)
      "
    />
    <pv-number-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraGain >= 0"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraGain"
      label="Camera Gain"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      tooltip="Controls camera gain, similar to brightness"
      @update:model-value="(args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraGain: args }, false)"
    />
    <pv-number-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraRedGain !== -1"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraRedGain"
      label="Red AWB Gain"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      tooltip="Controls red automatic white balance gain, which affects how the camera captures colors in different conditions"
      @update:model-value="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraRedGain: args }, false)
      "
    />
    <pv-number-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain !== -1"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain"
      label="Blue AWB Gain"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      tooltip="Controls blue automatic white balance gain, which affects how the camera captures colors in different conditions"
      @update:model-value="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraBlueGain: args }, false)
      "
    />
    <!-- Disable camera orientation as stop gap for Issue 1084 until calibration data gets rotated. https://github.com/PhotonVision/photonvision/issues/1084 -->
    <v-banner
      v-show="
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.inputImageRotationMode != 0
      "
      bg-color="red"
      class="mt-3"
      icon="mdi-alert-circle-outline"
      rounded
      text-color="white"
    >
      Warning! A known bug affects rotation of calibrated camera. Turn off rotation here and rotate using
      cameraToRobotTransform in your robot code.
    </v-banner>
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.inputImageRotationMode"
      :disabled="
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.inputImageRotationMode == 0
      "
      :items="cameraRotations"
      label="Orientation"
      :label-cols="labelCols"
      tooltip="Rotates the camera stream. Rotation not available when camera has been calibrated."
      @update:model-value="
        (args: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ inputImageRotationMode: args }, false)
      "
    />
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraVideoModeIndex"
      :items="cameraResolutions"
      label="Resolution"
      :label-cols="labelCols"
      tooltip="Resolution and FPS the camera should directly capture at"
      @update:model-value="(args: number) => handleResolutionChange(args)"
    />
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.streamingFrameDivisor"
      :items="streamResolutions"
      label="Stream Resolution"
      :label-cols="labelCols"
      tooltip="Resolution to which camera frames are downscaled for streaming to the dashboard"
      @update:model-value="(args: number) => handleStreamResolutionChange(args)"
    />
  </div>
</template>
