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
  if (canAdjustCrop.value) {
    const drag = dragState.value;
    return { cursor: edgesToCursor(drag !== null ? drag.edges : hoverEdges.value, drag !== null) };
  }

  return {};
});

// All crop visualization and interaction lives on the Raw stream: its content is the full frame
// (with the cropped-away area dimmed by the backend), so the region can be shown in context. The
// Processed stream simply shows the cropped image.
const isRawStream = props.streamType === "Raw";

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
  // The Processed stream contains only the cropped pixels, so its card takes the crop's shape.
  const region = cropRegion.value;
  if (!isRawStream && region !== null) {
    // Keep the card size stable even when a processed (cropped-only) stream is shown.
    // Previously the card adopted the crop's aspect ratio which caused the surrounding
    // Cameras view to grow/shrink while adjusting the crop. Use the full-frame
    // aspect ratio so adjusting the crop doesn't change the overall layout.
    return {
      aspectRatio: `${resolution.width}/${resolution.height}`
    };
  }
  return {
    aspectRatio: `${resolution.width}/${resolution.height}`
  };
});

// The static crop region as fractions of the full (rotated) frame, or null when cropping is off.
// Mirrors the backend's clamping: bounds are clamped into the frame (the stored upper bounds
// default to a larger-than-any-frame sentinel meaning "the frame edge") and a reversed range is
// normalized. A whole-frame region still shows, so an enabled crop is always adjustable on the
// stream even before it is narrowed.
const cropRegion = computed<{ left: number; top: number; width: number; height: number } | null>(() => {
  const resolution = rotatedResolution.value;
  const settings = props.cameraSettings.pipelineSettings;
  // A settings object without crop support (driver mode) reads as undefined -> falsy here.
  if (resolution === null || !settings.staticCropEnabled) {
    return null;
  }

  const clampedRange = (range: (typeof settings)["staticCropX"], max: number): [number, number] =>
    (Object.values(range) as number[]).map((bound) => Math.max(0, Math.min(bound, max))).sort((a, b) => a - b) as [
      number,
      number
    ];

  const [x0, x1] = clampedRange(settings.staticCropX, resolution.width);
  const [y0, y1] = clampedRange(settings.staticCropY, resolution.height);

  // A degenerate region means the backend doesn't crop at all.
  if (x1 - x0 <= 0 || y1 - y0 <= 0) return null;

  return {
    left: x0 / resolution.width,
    top: y0 / resolution.height,
    width: (x1 - x0) / resolution.width,
    height: (y1 - y0) / resolution.height
  };
});

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

// On the Raw stream, the frame box is sized to exactly the full frame's on-screen rectangle
// (contain-fit to the measured container) whenever the crop region is shown, so the region
// overlays and pointer positions map linearly to frame pixels. The Processed stream (and the
// no-crop case) just fills the container and lets the stream contain-fit as always.
const frameStyle = computed<StyleValue>(() => {
  const resolution = rotatedResolution.value;
  const container = containerSize.value;
  if (
    !isRawStream ||
    cropRegion.value === null ||
    resolution === null ||
    container === null ||
    container.width <= 0 ||
    container.height <= 0
  ) {
    return { position: "absolute", inset: "0" };
  }
  const scale = Math.min(container.width / resolution.width, container.height / resolution.height);
  return {
    position: "relative",
    width: `${resolution.width * scale}px`,
    height: `${resolution.height * scale}px`
  };
});

const streamFrame = useTemplateRef("streamFrame");

// The crop region geometry currently on screen: while the region is being moved or resized this is
// the dragged geometry (the throttled live commits catch the backend up as the pointer travels).
const previewRegion = computed<{ left: number; top: number; width: number; height: number } | null>(() => {
  const region = cropRegion.value;
  if (region === null) return null;
  return dragState.value?.region ?? region;
});

const regionGeometryStyle = (region: { left: number; top: number; width: number; height: number }) => ({
  left: `${region.left * 100}%`,
  top: `${region.top * 100}%`,
  width: `${region.width * 100}%`,
  height: `${region.height * 100}%`
});

// The crop region outlined in PhotonVision yellow, around the crisp area of the dimmed full frame
// on the Raw stream.
const cropOutlineStyle = computed<StyleValue>(() => {
  const preview = previewRegion.value;
  if (preview === null || !isRawStream) {
    return { display: "none" };
  }
  return {
    ...regionGeometryStyle(preview),
    outline: "2px solid #ffd843",
    outlineOffset: "-2px"
  };
});

// Resize handles drawn on the region's corners and border midpoints. Purely visual (the borders
// themselves are the grab targets), so they never intercept pointer events.
const cropHandlePositions = [
  { left: "0%", top: "0%" },
  { left: "50%", top: "0%" },
  { left: "100%", top: "0%" },
  { left: "0%", top: "50%" },
  { left: "100%", top: "50%" },
  { left: "0%", top: "100%" },
  { left: "50%", top: "100%" },
  { left: "100%", top: "100%" }
];

const handleBoxStyle = computed<StyleValue>(() => {
  const preview = previewRegion.value;
  if (preview === null || !canAdjustCrop.value) {
    return { display: "none" };
  }
  return regionGeometryStyle(preview);
});

// Dragging the visible crop region adjusts the crop: grabbing its interior moves the window around
// the frame, grabbing a border (or corner) resizes it.
const canAdjustCrop = computed(() => isRawStream && cropRegion.value !== null && !useStateStore().colorPickingMode);

// Which crop borders a drag adjusts; none selected means the whole region moves.
type DragEdges = { left: boolean; right: boolean; top: boolean; bottom: boolean };
const isMove = (edges: DragEdges) => !edges.left && !edges.right && !edges.top && !edges.bottom;

const dragState = ref<{
  startX: number;
  startY: number;
  frameWidth: number;
  frameHeight: number;
  base: { left: number; top: number; width: number; height: number };
  edges: DragEdges;
  // The current preview geometry, in fractions of the full frame
  region: { left: number; top: number; width: number; height: number };
} | null>(null);

// Borders under the pointer while hovering (drives the resize cursors)
const hoverEdges = ref<DragEdges | null>(null);

const edgesToCursor = (edges: DragEdges | null, dragging: boolean): string | undefined => {
  if (edges === null) return undefined;
  if (!isMove(edges)) {
    if ((edges.left && edges.top) || (edges.right && edges.bottom)) return "nwse-resize";
    if ((edges.left && edges.bottom) || (edges.right && edges.top)) return "nesw-resize";
    if (edges.left || edges.right) return "ew-resize";
    return "ns-resize";
  }
  return dragging ? "grabbing" : "grab";
};

// How far from a border, in screen pixels, a grab still catches it. The zone extends to both sides
// of the border: nobody lands on a 2px outline exactly, and coming at a corner from the dimmed
// outside has to work as well as from the inside.
const EDGE_GRAB_DISTANCE = 12;

// Hit-test the pointer against the crop region's on-screen rectangle: which borders are within
// grabbing distance (from either side), the interior (none selected -- a move), or null when the
// pointer is outside the region and not near any border. Only the inward reach is capped by the
// region size, so small regions keep a movable middle without shrinking the outside target.
const edgeHitTest = (event: PointerEvent): DragEdges | null => {
  const frameRect = streamFrame.value?.getBoundingClientRect();
  const region = previewRegion.value;
  if (!frameRect || region === null || frameRect.width <= 0 || frameRect.height <= 0) return null;

  const left = frameRect.left + region.left * frameRect.width;
  const top = frameRect.top + region.top * frameRect.height;
  const width = region.width * frameRect.width;
  const height = region.height * frameRect.height;
  if (width <= 0 || height <= 0) return null;
  const right = left + width;
  const bottom = top + height;

  const inwardX = Math.min(EDGE_GRAB_DISTANCE, width / 4);
  const inwardY = Math.min(EDGE_GRAB_DISTANCE, height / 4);
  // Near a border's line and within (or just past) the span it covers on the other axis.
  const alongX = event.clientX >= left - EDGE_GRAB_DISTANCE && event.clientX <= right + EDGE_GRAB_DISTANCE;
  const alongY = event.clientY >= top - EDGE_GRAB_DISTANCE && event.clientY <= bottom + EDGE_GRAB_DISTANCE;

  const edges = {
    left: alongY && event.clientX >= left - EDGE_GRAB_DISTANCE && event.clientX <= left + inwardX,
    right: alongY && event.clientX >= right - inwardX && event.clientX <= right + EDGE_GRAB_DISTANCE,
    top: alongX && event.clientY >= top - EDGE_GRAB_DISTANCE && event.clientY <= top + inwardY,
    bottom: alongX && event.clientY >= bottom - inwardY && event.clientY <= bottom + EDGE_GRAB_DISTANCE
  };
  if (edges.left || edges.right || edges.top || edges.bottom) return edges;

  // Inside the region away from every border: a move.
  if (event.clientX > left && event.clientX < right && event.clientY > top && event.clientY < bottom) {
    return edges;
  }

  return null;
};

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(value, max));

// Never resize below what the backend would grow a crop back to anyway (unless the region already
// started smaller).
const MIN_CROP_PIXELS = 16;

/** The drag's preview region for a pointer offset, clamped into the frame. */
const adjustedRegion = (
  state: NonNullable<typeof dragState.value>,
  dx: number,
  dy: number,
  resolution: { width: number; height: number }
) => {
  const base = state.base;
  if (isMove(state.edges)) {
    return {
      left: clamp(base.left + dx, 0, 1 - base.width),
      top: clamp(base.top + dy, 0, 1 - base.height),
      width: base.width,
      height: base.height
    };
  }

  const right = base.left + base.width;
  const bottom = base.top + base.height;
  const minWidth = Math.min(MIN_CROP_PIXELS / resolution.width, base.width);
  const minHeight = Math.min(MIN_CROP_PIXELS / resolution.height, base.height);

  const newLeft = state.edges.left ? clamp(base.left + dx, 0, right - minWidth) : base.left;
  const newRight = state.edges.right ? clamp(right + dx, base.left + minWidth, 1) : right;
  const newTop = state.edges.top ? clamp(base.top + dy, 0, bottom - minHeight) : base.top;
  const newBottom = state.edges.bottom ? clamp(bottom + dy, base.top + minHeight, 1) : bottom;

  return { left: newLeft, top: newTop, width: newRight - newLeft, height: newBottom - newTop };
};

// The bounds last written during a drag, so mid-drag updates and the final release only send when
// the region actually changed (a sub-pixel drag is a click, not an adjustment).
let lastSentBounds: { x0: number; x1: number; y0: number; y1: number } | null = null;
let lastDragSendMillis = 0;

const commitDrag = (state: NonNullable<typeof dragState.value>) => {
  const resolution = rotatedResolution.value;
  if (resolution === null) return;

  let bounds: { x0: number; x1: number; y0: number; y1: number };
  if (isMove(state.edges)) {
    // A move preserves the region's exact pixel size, whatever the drag rounding did.
    const width = Math.round(state.base.width * resolution.width);
    const height = Math.round(state.base.height * resolution.height);
    const x0 = clamp(Math.round(state.region.left * resolution.width), 0, resolution.width - width);
    const y0 = clamp(Math.round(state.region.top * resolution.height), 0, resolution.height - height);
    bounds = { x0, x1: x0 + width, y0, y1: y0 + height };
  } else {
    bounds = {
      x0: Math.round(state.region.left * resolution.width),
      x1: Math.round((state.region.left + state.region.width) * resolution.width),
      y0: Math.round(state.region.top * resolution.height),
      y1: Math.round((state.region.top + state.region.height) * resolution.height)
    };
  }

  if (
    lastSentBounds !== null &&
    bounds.x0 === lastSentBounds.x0 &&
    bounds.x1 === lastSentBounds.x1 &&
    bounds.y0 === lastSentBounds.y0 &&
    bounds.y1 === lastSentBounds.y1
  ) {
    return;
  }
  lastSentBounds = bounds;
  useCameraSettingsStore().changeCurrentPipelineSetting(
    { staticCropX: [bounds.x0, bounds.x1], staticCropY: [bounds.y0, bounds.y1] },
    true,
    props.cameraSettings.uniqueName
  );
};

const handleRegionPointerDown = (event: PointerEvent) => {
  const base = cropRegion.value;
  const rect = streamFrame.value?.getBoundingClientRect();
  const resolution = rotatedResolution.value;
  const edges = edgeHitTest(event);
  if (
    !canAdjustCrop.value ||
    base === null ||
    resolution === null ||
    edges === null ||
    !rect ||
    rect.width <= 0 ||
    rect.height <= 0
  ) {
    return;
  }
  event.preventDefault();
  (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  // Seed the dedupe with the current bounds so a drag that goes nowhere sends nothing.
  lastSentBounds = {
    x0: Math.round(base.left * resolution.width),
    x1: Math.round((base.left + base.width) * resolution.width),
    y0: Math.round(base.top * resolution.height),
    y1: Math.round((base.top + base.height) * resolution.height)
  };
  lastDragSendMillis = 0;
  dragState.value = {
    startX: event.clientX,
    startY: event.clientY,
    frameWidth: rect.width,
    frameHeight: rect.height,
    base,
    edges,
    region: base
  };
};

const handleRegionPointerMove = (event: PointerEvent) => {
  const state = dragState.value;
  if (state === null) {
    // Not dragging: just track which borders are under the pointer for cursor feedback.
    hoverEdges.value = canAdjustCrop.value ? edgeHitTest(event) : null;
    return;
  }
  const resolution = rotatedResolution.value;
  if (resolution === null) return;

  const dx = (event.clientX - state.startX) / state.frameWidth;
  const dy = (event.clientY - state.startY) / state.frameHeight;
  const updated = { ...state, region: adjustedRegion(state, dx, dy, resolution) };
  dragState.value = updated;

  // Push the region to the backend as it changes (throttled), so the stream live-previews exactly
  // what the new crop will show. The final geometry is always sent on release.
  const now = Date.now();
  if (now - lastDragSendMillis >= 100) {
    lastDragSendMillis = now;
    commitDrag(updated);
  }
};

const handleRegionPointerUp = () => {
  const state = dragState.value;
  dragState.value = null;
  if (state === null) return;
  commitDrag(state);
};

const handleRegionPointerLeave = () => {
  if (dragState.value === null) hoverEdges.value = null;
};

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
    <div ref="streamFrame" class="stream-frame" :style="frameStyle">
      <img
        :id="id"
        ref="mjpgStream"
        class="stream-video"
        crossorigin="anonymous"
        draggable="false"
        :src="streamSrc"
        :alt="streamDesc"
        :style="streamStyle"
        @error="handleStreamError"
        @pointerdown="handleRegionPointerDown"
        @pointermove="handleRegionPointerMove"
        @pointerup="handleRegionPointerUp"
        @pointerleave="handleRegionPointerLeave"
      />
      <div class="crop-outline" :style="cropOutlineStyle" />
      <div class="crop-handles" :style="handleBoxStyle">
        <div
          v-for="(handle, index) in cropHandlePositions"
          :key="index"
          class="crop-handle"
          :style="{ left: handle.left, top: handle.top }"
        />
      </div>
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

.crop-handles,
.crop-outline {
  position: absolute;
  pointer-events: none;
}

.crop-handle {
  position: absolute;
  width: 10px;
  height: 10px;
  background-color: #ffd843;
  border: 1px solid rgba(0, 0, 0, 0.6);
  border-radius: 1px;
  transform: translate(-50%, -50%);
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
