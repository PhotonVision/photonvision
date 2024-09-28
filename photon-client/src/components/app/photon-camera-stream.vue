<script setup lang="ts">
import { computed, inject, ref, onBeforeUnmount } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import loadingImage from "@/assets/images/loading.svg";
import type { StyleValue } from "vue/types/jsx";
import PvIcon from "@/components/common/pv-icon.vue";

const props = defineProps<{
  streamType: "Raw" | "Processed";
  id: string;
}>();

const emptyStreamSrc = "//:0";
const streamSrc = computed<string>(() => {
  const port =
    useCameraSettingsStore().currentCameraSettings.stream[props.streamType === "Raw" ? "inputPort" : "outputPort"];

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

  return { };
});

const containerStyle = computed<StyleValue>(() => {
  const resolution = useCameraSettingsStore().currentVideoFormat.resolution;
  const rotation = useCameraSettingsStore().currentPipelineSettings.inputImageRotationMode;
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

const mjpgStream: any = ref(null);
onBeforeUnmount(() => {
  if (!mjpgStream.value) return;
  mjpgStream.value["src"] = emptyStreamSrc;
});


</script>

<template>
  <div class="stream-container" :style="containerStyle">
    <img :src="loadingImage" class="stream-loading" />
    <img :id="id" class="stream-video" ref="mjpgStream" v-show="streamSrc !== emptyStreamSrc" crossorigin="anonymous" :src="streamSrc" :alt="streamDesc" :style="streamStyle" />
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
}

.stream-loading {
  position: absolute;
  width: 100%;
  height: 100%;
}

.stream-video {
  position: absolute;
  width: 100%;
  height: 100%;
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
