<script setup lang="ts">
import { computed, inject } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import loadingImage from "@/assets/images/loading.svg";
import type { StyleValue } from "vue/types/jsx";
import PvIcon from "@/components/common/pv-icon.vue";

const props = defineProps<{
  streamType: "Raw" | "Processed";
  id: string;
}>();

const streamSrc = computed<string>(() => {
  const port =
    useCameraSettingsStore().currentCameraSettings.stream[props.streamType === "Raw" ? "inputPort" : "outputPort"];

  if (!useStateStore().backendConnected || port === 0) {
    return loadingImage;
  }

  return `http://${inject("backendHostname")}:${port}/stream.mjpg`;
});
const streamDesc = computed<string>(() => `${props.streamType} Stream View`);
const streamStyle = computed<StyleValue>(() => {
  if (useStateStore().colorPickingMode) {
    return { width: "100%", cursor: "crosshair" };
  }

  return { width: "100%" };
});

const overlayStyle = computed<StyleValue>(() => {
  if (useStateStore().colorPickingMode || streamSrc.value == loadingImage) {
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
const handleFullscreenRequest = () => {
  const stream = document.getElementById(props.id);
  if (!stream) return;
  stream.requestFullscreen();
};
</script>

<template>
  <div class="stream-container">
    <img
      :id="id"
      crossorigin="anonymous"
      :src="streamSrc"
      :alt="streamDesc"
      :style="streamStyle"
      onclick="this.requestFullscreen()"
    />
    <div class="stream-overlay" :style="overlayStyle">
      <pv-icon
        icon-name="mdi-camera-image"
        tooltip="Capture and save a frame of this stream"
        class="ma-1 mr-2"
        @click="handleCaptureClick"
      />
      <pv-icon
        icon-name="mdi-fullscreen"
        tooltip="Open this stream in fullscreen"
        class="ma-1 mr-2"
        @click="handleFullscreenRequest"
      />
      <pv-icon
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
  position: relative;
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
