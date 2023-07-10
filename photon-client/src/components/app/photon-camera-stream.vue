<script setup lang="ts">
import {computed, inject} from "vue";
import {useCameraSettingsStore} from "@/stores/settings/CameraSettingsStore";
import {useStateStore} from "@/stores/StateStore";
import loadingImage from "@/assets/images/loading.svg";
import type {StyleValue} from "vue/types/jsx";

const props = defineProps<{
  streamType: "Raw" | "Processed"
}>();

const src = computed<string>(() => {
  const currentCameraSettings = useCameraSettingsStore().currentCameraSettings;

  if(!useStateStore().backendConnected || currentCameraSettings === undefined) {
    return loadingImage;
  }

  const port = currentCameraSettings.stream[props.streamType === "Raw" ? "inputPort" : "outputPort"];

  return `http://${inject("backendAddress")}:${port}/stream.mjpg`;
});
const alt = computed<string>(() => `${props.streamType} Stream View`);

const style = computed<StyleValue>(() => {
  if(src.value !== loadingImage) {
    return {
      cursor: "pointer"
    };
  }

  return {};
});

const handleClick = () => {
  if(src.value !== loadingImage) {
    window.open(src.value);
  }
};
</script>


<template>
  <img
      crossorigin="anonymous"
      :src="src"
      :alt="alt"
      :style="style"
      @click="handleClick"
  />
</template>
