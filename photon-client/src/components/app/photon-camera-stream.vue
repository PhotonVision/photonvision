<script setup lang="ts">
import { computed, inject, ref, onBeforeUnmount } from "vue";
import { useStateStore } from "@/stores/StateStore";
import loadingImage from "@/assets/images/loading-transparent.svg";
import type { StyleValue } from "vue/types/jsx";
import PvIcon from "@/components/common/pv-icon.vue";
import type { UiCameraConfiguration } from "@/types/SettingTypes";

const props = defineProps<{
  streamType: "Raw" | "Processed";
  id: string;
  cameraSettings: UiCameraConfiguration;
}>();

const emptyStreamSrc = "//:0";
const streamSrc = computed<string>(() => {
  const port = props.cameraSettings.stream[props.streamType === "Raw" ? "inputPort" : "outputPort"];

  if (!useStateStore().backendConnected || port === 0) {
    return emptyStreamSrc;
  }

  return `http://${inject("backendHostname")}:${port}/stream.mjpg`;
});
const streamDesc = computed<string>(() => `${props.streamType} Stream View`);
const streamStyle = computed<StyleValue>(() => {
  if (useStateStore().colorPickingMode) {
    return { cursor: "crosshair" };
  }

  return {};
});

const containerStyle = computed<StyleValue>(() => {
  if (props.cameraSettings.validVideoFormats.length === 0) {
    return { aspectRatio: "1/1" };
  }
  const resolution =
    props.cameraSettings.validVideoFormats[props.cameraSettings.pipelineSettings.cameraVideoModeIndex].resolution;
  const rotation = props.cameraSettings.pipelineSettings.inputImageRotationMode;
  if (rotation === 1 || rotation === 3) {
    return {
      aspectRatio: `${resolution.height}/${resolution.width}`
    };
  }
  return {
    aspectRatio: `${resolution.width}/${resolution.height}`
  };
});

const overlayStyle = computed<StyleValue>(() => {
  if (useStateStore().colorPickingMode || streamSrc.value == emptyStreamSrc) {
    return { display: "none" };
  } else {
    return {};
  }
});

const handleCaptureClick = () => {
  if (props.streamType === "Raw") {
    props.cameraSettings.pipelineSettings[props.cameraSettings.currentPipelineIndex].saveInputSnapshot();
  } else {
    props.cameraSettings.pipelineSettings[props.cameraSettings.currentPipelineIndex].saveOutputSnapshot();
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

const mjpgStream: any = ref(null);

const handleStreamError = () => {
  if (streamSrc.value && streamSrc.value !== emptyStreamSrc) {
    console.error("Error loading stream:", streamSrc.value, " Trying again.");
    setTimeout(() => {
      mjpgStream.value.src = streamSrc.value;
    }, 100);
  }
};

onBeforeUnmount(() => {
  if (!mjpgStream.value) return;
  mjpgStream.value["src"] = emptyStreamSrc;
});
</script>

<template>
  <div class="stream-container" :style="containerStyle">
    <img :src="loadingImage" class="stream-loading" />
    <img
      :id="id"
      ref="mjpgStream"
      class="stream-video"
      crossorigin="anonymous"
      :src="streamSrc"
      :alt="streamDesc"
      :style="streamStyle"
      @error="handleStreamError"
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
