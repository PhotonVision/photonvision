<script setup lang="ts">
import CvSlider from "@/components/common/cv-slider.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import CvSwitch from "@/components/common/cv-switch.vue";
import CvSelect from "@/components/common/cv-select.vue";
import { computed, getCurrentInstance } from "vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useStateStore } from "@/stores/StateStore";

// Due to something with libcamera or something else IDK much about, the 90° rotations need to be disabled if the libcamera drivers are being used.
const cameraRotations = computed(() => ["Normal","90° CW","180°","90° CCW"].map((v, i) => ({ name: v, value: i, disabled: useSettingsStore().gpuAccelerationEnabled ?  [1, 3].includes(i) : false })));

const streamDivisors = [1, 2, 4, 6];
const getFilteredStreamDivisors = (): number[] => {
  const currentResolutionWidth = useCameraSettingsStore().currentVideoFormat.resolution.width;
  return streamDivisors.filter(x =>
      useCameraSettingsStore().isDriverMode
      || !useSettingsStore().gpuAccelerationEnabled
      || currentResolutionWidth / x < 400);
};
const getNumberOfSkippedDivisors = () => streamDivisors.length - getFilteredStreamDivisors().length;

const cameraResolutions = computed(() => useCameraSettingsStore().currentCameraSettings.validVideoFormats.map(f => `${f.resolution.width} X ${f.resolution.height} at ${f.fps} FPS, ${f.pixelFormat}`));
const handleResolutionChange = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraVideoModeIndex: value }, false);

  useCameraSettingsStore().changeCurrentPipelineSetting({ streamingFrameDivisor: getNumberOfSkippedDivisors() }, false);
  useCameraSettingsStore().currentPipelineSettings.streamingFrameDivisor = 0;

  if(!useCameraSettingsStore().isCurrentVideoFormatCalibrated) {
    useCameraSettingsStore().changeCurrentPipelineSetting({ solvePNPEnabled: false }, true);
  }
};

const streamResolutions = computed(() => {
  const streamDivisors = getFilteredStreamDivisors();
  const currentResolution = useCameraSettingsStore().currentVideoFormat.resolution;
  return streamDivisors
      .map(x => `${Math.floor(currentResolution.width / x)} X ${Math.floor(currentResolution.height / x)}`);
});
const handleStreamResolutionChange = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ streamingFrameDivisor: value + getNumberOfSkippedDivisors() }, false);
};

const interactiveCols = computed(() => (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)) ? 9 : 8;
</script>

<template>
  <div>
    <cv-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraExposure"
      :disabled="useCameraSettingsStore().currentCameraSettings.pipelineSettings.cameraAutoExposure"
      label="Exposure"
      tooltip="Directly controls how much light is allowed to fall onto the sensor, which affects apparent brightness"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      :step="0.1"
      @input="args => useCameraSettingsStore().changeCurrentPipelineSetting({cameraExposure: args}, false)"
    />
    <cv-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraBrightness"
      label="Brightness"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      @input="args => useCameraSettingsStore().changeCurrentPipelineSetting({cameraBrightness: args}, false)"
    />
    <cv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraAutoExposure"
      class="pt-2"
      label="Auto Exposure"
      :switch-cols="interactiveCols"
      tooltip="Enables or Disables camera automatic adjustment for current lighting conditions"
      @input="args => useCameraSettingsStore().changeCurrentPipelineSetting({cameraAutoExposure: args}, false)"
    />
    <cv-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraGain >= 0"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraGain"
      label="Camera Gain"
      tooltip="Controls camera gain, similar to brightness"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      @input="args => useCameraSettingsStore().changeCurrentPipelineSetting({cameraGain: args}, false)"
    />
    <cv-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraRedGain !== -1"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraRedGain"
      label="Red AWB Gain"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      tooltip="Controls red automatic white balance gain, which affects how the camera captures colors in different conditions"
      @input="args => useCameraSettingsStore().changeCurrentPipelineSetting({cameraRedGain: args}, false)"
    />
    <cv-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain !== -1"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain"
      label="Blue AWB Gain"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      tooltip="Controls blue automatic white balance gain, which affects how the camera captures colors in different conditions"
      @input="args => useCameraSettingsStore().changeCurrentPipelineSetting({cameraBlueGain: args}, false)"
    />
    <cv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.inputImageRotationMode"
      label="Orientation"
      tooltip="Rotates the camera stream"
      :items="cameraRotations"
      :select-cols="interactiveCols"
      @input="args => useCameraSettingsStore().changeCurrentPipelineSetting({inputImageRotationMode: args}, false)"
    />
    <cv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraVideoModeIndex"
      label="Resolution"
      tooltip="Resolution and FPS the camera should directly capture at"
      :items="cameraResolutions"
      :select-cols="interactiveCols"
      @input="args => handleResolutionChange(args)"
    />
    <cv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.streamingFrameDivisor"
      label="Stream Resolution"
      tooltip="Resolution to which camera frames are downscaled for streaming to the dashboard"
      :items="streamResolutions"
      :select-cols="interactiveCols"
      @input="args => handleStreamResolutionChange(args)"
    />
  </div>
</template>
