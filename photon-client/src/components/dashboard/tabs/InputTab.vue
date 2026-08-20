<script setup lang="ts">
import PvSlider from "@/components/common/pv-slider.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvSelect from "@/components/common/pv-select.vue";
import PvRangeSlider from "@/components/common/pv-range-slider.vue";
import { computed, watch } from "vue";
import type { WebsocketNumberPair } from "@/types/WebsocketDataTypes";
import { FrameEdgeCropBound, type ConfigurablePipelineSettings } from "@/types/PipelineTypes";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { getResolutionString } from "@/lib/PhotonUtils";
import { useDisplay } from "vuetify";

// Due to something with libcamera or something else IDK much about, the 90° rotations need to be disabled if the libcamera drivers are being used.
const cameraRotations = computed(() =>
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

const cameraResolutions = (): { name: string; value: number }[] =>
  useCameraSettingsStore().currentCameraSettings.validVideoFormats.map<{ name: string; value: number }>((f) => ({
    name: `${getResolutionString(f.resolution)} at ${f.fps} FPS, ${f.pixelFormat}`,
    value: f.index || 0 // Index won't ever be undefined
  }));
const handleResolutionChange = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraVideoModeIndex: value }, false);

  useCameraSettingsStore().changeCurrentPipelineSetting({ streamingFrameDivisor: getNumberOfSkippedDivisors() }, false);
  useCameraSettingsStore().currentPipelineSettings.streamingFrameDivisor = 0;

  if (!useCameraSettingsStore().isCurrentVideoFormatCalibrated && !useCameraSettingsStore().isDriverMode) {
    useCameraSettingsStore().changeCurrentPipelineSetting({ solvePNPEnabled: false }, true);
  }
};

const streamResolutions = computed(() => {
  const streamDivisors = getFilteredStreamDivisors();
  const currentResolution = useCameraSettingsStore().currentVideoFormat.resolution;
  return streamDivisors.map((x, i) => ({
    name: `${Math.floor(currentResolution.width / x)}x${Math.floor(currentResolution.height / x)}`,
    value: i
  }));
});
const currentStreamResolutionIndex = computed<number>({
  get: () => {
    const stored = useCameraSettingsStore().currentPipelineSettings.streamingFrameDivisor;
    const skipped = getNumberOfSkippedDivisors();
    return stored - skipped;
  },
  set: (index) => {
    useCameraSettingsStore().changeCurrentPipelineSetting({
      streamingFrameDivisor: index + getNumberOfSkippedDivisors()
    });
  }
});

const showStaticCrop = computed(
  () =>
    !useCameraSettingsStore().isDriverMode &&
    !useCameraSettingsStore().isCalibrationMode &&
    !useCameraSettingsStore().isFocusMode
);

// The crop is applied after rotation, so its bounds are the rotated frame dimensions. 90° rotations
// (rotation modes 1 and 3) swap the width and height.
const croppableResolution = computed<{ width: number; height: number }>(() => {
  const resolution = useCameraSettingsStore().currentVideoFormat.resolution;
  const rotation = useCameraSettingsStore().currentPipelineSettings.inputImageRotationMode;
  return rotation === 1 || rotation === 3
    ? { width: resolution.height, height: resolution.width }
    : { width: resolution.width, height: resolution.height };
});

const cropBounds = (range: WebsocketNumberPair | [number, number] | undefined): [number, number] =>
  Object.values(range || { first: 0, second: FrameEdgeCropBound }) as [number, number];

// The stored upper bounds default to a sentinel larger than any frame (the backend clamps the crop to
// the frame edge), so clamp into the frame here: an unconfigured crop reads as the full 0-to-res
// range instead of a value the slider can't display.
const clampCropRange = (range: WebsocketNumberPair | [number, number] | undefined, max: number): [number, number] =>
  cropBounds(range).map((bound) => Math.max(0, Math.min(bound, max))) as [number, number];

const staticCropX = computed<[number, number]>({
  get: () =>
    clampCropRange(useCameraSettingsStore().currentPipelineSettings.staticCropX, croppableResolution.value.width),
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.staticCropX = v)
});
const staticCropY = computed<[number, number]>({
  get: () =>
    clampCropRange(useCameraSettingsStore().currentPipelineSettings.staticCropY, croppableResolution.value.height),
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.staticCropY = v)
});

// The stored crop doesn't know the frame size, so changing the resolution (or rotating the frame) can
// leave bounds outside it -- and clamping them for display isn't enough, because the stored setting is
// what the next frame size is applied to. An upper bound that was sitting on the frame edge means "the
// whole frame", so store it as the frame-edge sentinel: it then shrinks and grows with the frame
// instead of freezing at the pixel count of whichever resolution was selected when it was last set.
// Interior bounds are the ones the user actually picked, so they are left alone.
watch(
  [() => croppableResolution.value.width, () => croppableResolution.value.height],
  ([width, height], [oldWidth, oldHeight] = [width, height]) => {
    // No camera or no video mode yet -- there's nothing meaningful to clamp against.
    if (width <= 0 || height <= 0) return;

    const settings = useCameraSettingsStore().currentPipelineSettings;
    const changes: ConfigurablePipelineSettings = {};

    for (const [axis, max, oldMax] of [
      ["staticCropX", width, oldWidth],
      ["staticCropY", height, oldHeight]
    ] as ["staticCropX" | "staticCropY", number, number][]) {
      const [first, second] = cropBounds(settings[axis]);
      const updated: [number, number] = [
        // A start point past the end of the frame is meaningless, so pull it inside.
        Math.max(0, Math.min(first, max)),
        // At or past the smaller of the two frames, the bound either covered the old frame's edge or
        // no longer fits in the new one. Either way it belongs on the edge from here on.
        second >= Math.min(oldMax, max) ? FrameEdgeCropBound : second
      ];

      if (updated[0] !== first || updated[1] !== second) changes[axis] = updated;
    }

    if (Object.keys(changes).length) useCameraSettingsStore().changeCurrentPipelineSetting(changes);
  },
  { immediate: true }
);

const { mdAndDown } = useDisplay();

const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 8 : 7
);
</script>

<template>
  <div>
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraAutoExposure"
      label="Auto Exposure"
      :switch-cols="interactiveCols"
      tooltip="Enables or Disables camera automatic adjustment for current lighting conditions"
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraAutoExposure: args }, false)
      "
    />
    <pv-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraExposureRaw"
      :disabled="useCameraSettingsStore().currentCameraSettings.pipelineSettings.cameraAutoExposure"
      label="Exposure"
      tooltip="Directly controls how long the camera shutter remains open. Units are dependant on the underlying driver."
      :min="useCameraSettingsStore().minExposureRaw"
      :max="useCameraSettingsStore().maxExposureRaw"
      :slider-cols="interactiveCols"
      :step="1"
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraExposureRaw: args }, false)
      "
    />
    <pv-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraBrightness"
      label="Brightness"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraBrightness: args }, false)
      "
    />
    <pv-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraGain >= 0"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraGain"
      label="Camera Gain"
      tooltip="Controls camera gain, similar to brightness"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      @update:modelValue="(args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraGain: args }, false)"
    />
    <pv-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraRedGain !== -1"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraRedGain"
      label="Red AWB Gain"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      tooltip="Controls red automatic white balance gain, which affects how the camera captures colors in different conditions"
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraRedGain: args }, false)
      "
    />
    <pv-slider
      v-if="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain !== -1"
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain"
      label="Blue AWB Gain"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      tooltip="Controls blue automatic white balance gain, which affects how the camera captures colors in different conditions"
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraBlueGain: args }, false)
      "
    />
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraAutoWhiteBalance"
      label="Auto White Balance"
      :switch-cols="interactiveCols"
      tooltip="Enables or Disables camera automatic adjustment for current lighting conditions"
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraAutoWhiteBalance: args }, false)
      "
    />
    <pv-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraWhiteBalanceTemp"
      :disabled="useCameraSettingsStore().currentPipelineSettings.cameraAutoWhiteBalance"
      label="White Balance Temperature"
      :min="useCameraSettingsStore().minWhiteBalanceTemp"
      :max="useCameraSettingsStore().maxWhiteBalanceTemp"
      :slider-cols="interactiveCols"
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraWhiteBalanceTemp: args }, false)
      "
    />
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.blockForFrames"
      :disabled="useCameraSettingsStore().currentCameraSettings.matchedCameraInfo.type !== 'PVUsbCameraInfo'"
      label="Low Latency Mode"
      :switch-cols="interactiveCols"
      tooltip="When enabled, USB cameras wait for the next camera frame for lowest latency. When disabled, uses the most recent available frame for higher FPS."
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ blockForFrames: args }, false)
      "
    />
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.inputImageRotationMode"
      label="Orientation"
      tooltip="Rotates the camera stream"
      :items="cameraRotations"
      :select-cols="interactiveCols"
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ inputImageRotationMode: args }, false)
      "
    />
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.cameraVideoModeIndex"
      label="Resolution"
      tooltip="Resolution and FPS the camera should directly capture at"
      :items="cameraResolutions()"
      :select-cols="interactiveCols"
      @update:modelValue="(args) => handleResolutionChange(args)"
    />
    <pv-select
      v-model="currentStreamResolutionIndex"
      label="Stream Resolution"
      tooltip="Resolution to which camera frames are downscaled for streaming to the dashboard"
      :items="streamResolutions"
      :select-cols="interactiveCols"
    />
    <pv-switch
      v-if="showStaticCrop"
      v-model="useCameraSettingsStore().currentPipelineSettings.staticCropEnabled"
      label="Static Crop"
      :switch-cols="interactiveCols"
      tooltip="Crops the camera frame to a fixed region before processing. Camera calibration is adjusted so 3D pose estimation stays accurate."
      @update:modelValue="
        (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ staticCropEnabled: args }, false)
      "
    />
    <pv-range-slider
      v-if="showStaticCrop"
      v-model="staticCropX"
      label="Crop X Range"
      tooltip="Left and right pixel bounds of the crop region (after rotation)"
      :disabled="!useCameraSettingsStore().currentPipelineSettings.staticCropEnabled"
      :min="0"
      :max="croppableResolution.width"
      :step="1"
      :slider-cols="interactiveCols"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ staticCropX: value }, false)
      "
    />
    <pv-range-slider
      v-if="showStaticCrop"
      v-model="staticCropY"
      label="Crop Y Range"
      tooltip="Top and bottom pixel bounds of the crop region (after rotation)"
      :disabled="!useCameraSettingsStore().currentPipelineSettings.staticCropEnabled"
      :min="0"
      :max="croppableResolution.height"
      :step="1"
      :slider-cols="interactiveCols"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ staticCropY: value }, false)
      "
    />
    <pv-switch
      v-if="useCameraSettingsStore().isDriverMode"
      v-model="useCameraSettingsStore().currentPipelineSettings.crosshair"
      label="Crosshair"
      :switch-cols="interactiveCols"
      tooltip="Enables or disables a crosshair overlay on the camera stream"
      @update:modelValue="(args) => useCameraSettingsStore().changeCurrentPipelineSetting({ crosshair: args }, false)"
    />
  </div>
</template>
