<script setup lang="ts">
import PvTooltippedIcon from "@/components/common/pv-tooltipped-icon.vue";
import { computed, inject, onBeforeUnmount, ref } from "vue";
import loadingImage from "@/assets/images/loading.svg";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = withDefaults(
  defineProps<{
    streamType: "Raw" | "Processed";
    id?: string;
  }>(),
  {
    id: "photon-camera-stream"
  }
);

const streamSrc = computed<string>(() => {
  const port = serverStore.currentCameraSettings?.stream[props.streamType === "Raw" ? "inputPort" : "outputPort"];

  if (!clientStore.backendConnected || port === 0) {
    return loadingImage;
  }

  return `http://${inject("backendHostname")}:${port}/stream.mjpg`;
});

const handleCaptureClick = () => {
  if (props.streamType === "Raw") {
    serverStore.saveInputSnapshot();
  } else {
    serverStore.saveOutputSnapshot();
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
  mjpgStream.value.src = null;
});
</script>

<template>
  <div class="stream-container">
    <img
      :id="id"
      ref="mjpgStream"
      :alt="streamType + ' Stream View'"
      class="w-100"
      crossorigin="anonymous"
      :src="streamSrc"
    />
    <div
      class="stream-overlay"
      :style="(clientStore.colorPickingFromCameraStream || streamSrc === loadingImage) && { display: 'none' }"
    >
      <pv-tooltipped-icon
        class="ma-1 mr-2"
        clickable
        icon-name="mdi-camera-image"
        tooltip="Capture and save a frame of this stream"
        @click="handleCaptureClick"
      />
      <pv-tooltipped-icon
        class="ma-1 mr-2"
        clickable
        icon-name="mdi-fullscreen"
        tooltip="Open this stream in fullscreen"
        @click="handleFullscreenRequest"
      />
      <pv-tooltipped-icon
        class="ma-1 mr-2"
        clickable
        icon-name="mdi-open-in-new"
        tooltip="Open this stream in a new window"
        @click="handlePopoutClick"
      />
    </div>
  </div>
</template>

<style scoped>
.stream-container {
  position: relative;
  display: flex;
  align-items: center;
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
