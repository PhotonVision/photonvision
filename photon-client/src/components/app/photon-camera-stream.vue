<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import type { StyleValue } from "vue";
import PvIcon from "@/components/common/pv-icon.vue";
import type { UiCameraConfiguration } from "@/types/SettingTypes";
import PvLoading from "@/components/common/pv-loading.vue";

const props = defineProps<{
  streamType: "Raw" | "Processed";
  id: string;
  cameraSettings: UiCameraConfiguration;
}>();

const backendHostname = inject<string>("backendHostname");
const emptyStreamSrc = "//:0";
const streamSrc = computed<string>(() => {
  const port = props.cameraSettings.stream[props.streamType === "Raw" ? "inputPort" : "outputPort"];

  if (!useStateStore().backendConnected || port === 0) {
    return emptyStreamSrc;
  }

  return `http://${backendHostname}:${port}/stream.mjpg`;
});
const streamDesc = computed<string>(() => `${props.streamType} Stream View`);
const streamStyle = computed<StyleValue>(() => {
  if (useStateStore().colorPickingMode) {
    return { cursor: "crosshair" };
  }

  return {};
});

// The full frame dimensions in the coordinate space the static crop applies in: after rotation, so
// 90° rotations (modes 1 and 3) swap width and height.
const rotatedResolution = computed<{ width: number; height: number } | null>(() => {
  if (props.cameraSettings.validVideoFormats.length === 0) {
    return null;
  }
  const resolution =
    props.cameraSettings.validVideoFormats[props.cameraSettings.pipelineSettings.cameraVideoModeIndex].resolution;
  const rotation = props.cameraSettings.pipelineSettings.inputImageRotationMode;
  return rotation === 1 || rotation === 3
    ? { width: resolution.height, height: resolution.width }
    : { width: resolution.width, height: resolution.height };
});

const containerStyle = computed<StyleValue>(() => {
  const resolution = rotatedResolution.value;
  if (resolution === null) {
    return { aspectRatio: "1/1" };
  }
  return {
    aspectRatio: `${resolution.width}/${resolution.height}`
  };
});

// The static crop region as fractions of the full (rotated) frame, or null when cropping is off or
// covers the whole frame. Mirrors the backend's clamping: bounds are clamped into the frame (the
// stored upper bounds default to a larger-than-any-frame sentinel meaning "the frame edge") and a
// reversed range is normalized.
const cropRegion = computed<{ left: number; top: number; width: number; height: number } | null>(() => {
  const resolution = rotatedResolution.value;
  const settings = props.cameraSettings.pipelineSettings;
  if (resolution === null || !("staticCropEnabled" in settings) || !settings.staticCropEnabled) {
    return null;
  }

  const clampedRange = (range: (typeof settings)["staticCropX"], max: number): [number, number] =>
    (Object.values(range) as number[]).map((bound) => Math.max(0, Math.min(bound, max))).sort((a, b) => a - b) as [
      number,
      number
    ];

  const [x0, x1] = clampedRange(settings.staticCropX, resolution.width);
  const [y0, y1] = clampedRange(settings.staticCropY, resolution.height);

  // A degenerate or whole-frame region means the backend doesn't crop at all.
  if (x1 - x0 <= 0 || y1 - y0 <= 0) return null;
  if (x0 === 0 && y0 === 0 && x1 === resolution.width && y1 === resolution.height) return null;

  return {
    left: x0 / resolution.width,
    top: y0 / resolution.height,
    width: (x1 - x0) / resolution.width,
    height: (y1 - y0) / resolution.height
  };
});

// The container's own box can be letterboxed by its parent, so to place the black frame box we need
// its real dimensions -- measured with a ResizeObserver rather than derived in CSS.
const streamContainer = useTemplateRef("streamContainer");
const containerSize = ref<{ width: number; height: number } | null>(null);
let containerResizeObserver: ResizeObserver | undefined;
onMounted(() => {
  containerResizeObserver = new ResizeObserver((entries) => {
    const rect = entries[0]?.contentRect;
    if (rect) containerSize.value = { width: rect.width, height: rect.height };
  });
  if (streamContainer.value) containerResizeObserver.observe(streamContainer.value);
});
onBeforeUnmount(() => containerResizeObserver?.disconnect());

// With a crop active, the streamed image only contains the cropped region. Show it at its true
// position inside a black, full-frame-shaped box so the cropped-away area stays visible. The box is
// sized to contain-fit the measured container, exactly like object-fit would letterbox a full frame,
// and the (flex-centering) container centers it.
const frameStyle = computed<StyleValue>(() => {
  const resolution = rotatedResolution.value;
  const container = containerSize.value;
  if (
    cropRegion.value === null ||
    resolution === null ||
    container === null ||
    container.width <= 0 ||
    container.height <= 0
  ) {
    // No crop: fill the container and let the stream contain-fit as always.
    return { position: "absolute", inset: "0" };
  }
  const scale = Math.min(container.width / resolution.width, container.height / resolution.height);
  return {
    position: "relative",
    width: `${resolution.width * scale}px`,
    height: `${resolution.height * scale}px`,
    backgroundColor: "black"
  };
});

const cropPositionStyle = computed<StyleValue>(() => {
  const region = cropRegion.value;
  if (region === null) {
    return {};
  }
  return {
    left: `${region.left * 100}%`,
    top: `${region.top * 100}%`,
    width: `${region.width * 100}%`,
    height: `${region.height * 100}%`
  };
});

const overlayStyle = computed<StyleValue>(() => {
  if (useStateStore().colorPickingMode || streamSrc.value === emptyStreamSrc) {
    return { display: "none" };
  } else {
    return {};
  }
});

const handleCaptureClick = () => {
  if (props.streamType === "Raw") {
    useCameraSettingsStore().saveInputSnapshot();
  } else {
    useCameraSettingsStore().saveOutputSnapshot();
  }
};
const handlePopoutClick = () => {
  window.open(streamSrc.value);
};
const handleFullscreenRequest = async () => {
  const stream = document.getElementById(props.id);
  if (!stream) return;
  await stream.requestFullscreen();
};

const mjpgStream = useTemplateRef("mjpgStream");

const handleStreamError = () => {
  if (streamSrc.value && streamSrc.value !== emptyStreamSrc) {
    console.error("Error loading stream:", streamSrc.value, " Trying again.");
    setTimeout(() => {
      mjpgStream.value!.src = streamSrc.value;
    }, 100);
  }
};

onBeforeUnmount(() => {
  if (!mjpgStream.value) return;
  mjpgStream.value.src = emptyStreamSrc;
});
</script>

<template>
  <div ref="streamContainer" class="stream-container" :style="containerStyle">
    <pv-loading class="stream-loading" />
    <div class="stream-frame" :style="frameStyle">
      <img
        :id="id"
        ref="mjpgStream"
        class="stream-video"
        crossorigin="anonymous"
        :src="streamSrc"
        :alt="streamDesc"
        :style="[streamStyle, cropPositionStyle]"
        @error="handleStreamError"
      />
    </div>
    <div class="stream-overlay" :style="overlayStyle">
      <pv-icon
        color="primary"
        icon-name="mdi-camera-image"
        tooltip="Capture and save a frame of this stream"
        class="ma-1 mr-2"
        @click="handleCaptureClick"
      />
      <pv-icon
        color="primary"
        icon-name="mdi-fullscreen"
        tooltip="Open this stream in fullscreen"
        class="ma-1 mr-2"
        @click="handleFullscreenRequest"
      />
      <pv-icon
        color="primary"
        icon-name="mdi-open-in-new"
        tooltip="Open this stream in a new window"
        class="ma-1 mr-2"
        @click="handlePopoutClick"
      />
    </div>
  </div>
</template>

<style scoped>
.stream-container {
  display: flex;
  position: relative;
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  justify-content: center;
  align-items: center;
}

.stream-loading {
  position: absolute;
  width: 25%;
  height: 25%;
  object-fit: contain;
}

.stream-video {
  position: absolute;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.stream-overlay {
  display: flex;
  opacity: 0;
  transition: 0.1s ease;
  position: absolute;
  top: 0;
  right: 0;
}

.stream-container:hover .stream-overlay {
  opacity: 1;
}
</style>
