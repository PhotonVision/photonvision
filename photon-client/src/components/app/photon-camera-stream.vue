<script setup lang="ts">
import {computed, defineProps, inject} from "vue";
import {useSettingsStore} from "@/stores/SettingsStore";
import {useStateStore} from "@/stores/StateStore";
import loadingImage from "@/assets/images/loading.svg";

const props = defineProps<{
  streamType: "Raw" | "Processed"
}>();

const src = computed<string>(() => {
  const currentCameraSettings = useSettingsStore().currentCameraSettings;

  if(!useStateStore().backendConnected || currentCameraSettings === null) {
    return loadingImage;
  }

  const port = currentCameraSettings.stream[props.streamType === "Raw" ? "inputPort" : "outputPort"];

  return `http://${inject("backendAddress")}:${port}/stream.mjpg`;
});
const alt = computed<string>(() => `${props.streamType} Stream View`);

const style = computed<object>(() => {
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