<script setup lang="ts">
import { computed, inject, onBeforeUnmount, useTemplateRef } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import type { StyleValue } from "vue";
import IconCameraImage from "~icons/mdi/camera-image";
import IconFullscreen from "~icons/mdi/fullscreen";
import IconOpenInNew from "~icons/mdi/open-in-new";

import type { UiCameraConfiguration } from "@/types/SettingTypes";

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
  <div
    class="stream-container group relative flex h-full max-h-full w-full max-w-full items-center justify-center"
    :style="containerStyle"
  >
    <pv-loading class="absolute h-1/4 w-1/4 object-contain" />
    <img
      :id="id"
      ref="mjpgStream"
      class="absolute h-full w-full object-contain"
      crossorigin="anonymous"
      :src="streamSrc"
      :alt="streamDesc"
      :style="streamStyle"
      @error="handleStreamError"
    />
    <div
      class="absolute top-0 right-0 flex opacity-0 transition duration-100 group-hover:opacity-100"
      :style="overlayStyle"
    >
      <pv-tooltipped-icon
        color="primary"
        :icon="IconCameraImage"
        tooltip="Capture and save a frame of this stream"
        class="m-1 mr-2 cursor-pointer"
        @click="handleCaptureClick"
      />
      <pv-tooltipped-icon
        color="primary"
        :icon="IconFullscreen"
        tooltip="Open this stream in fullscreen"
        class="m-1 mr-2 cursor-pointer"
        @click="handleFullscreenRequest"
      />
      <pv-tooltipped-icon
        color="primary"
        :icon="IconOpenInNew"
        tooltip="Open this stream in a new window"
        class="m-1 mr-2 cursor-pointer"
        @click="handlePopoutClick"
      />
    </div>
  </div>
</template>
