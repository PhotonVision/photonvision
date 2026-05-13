<script setup lang="ts">
import { ProgressIndicator, ProgressRoot } from "reka-ui";
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const props = defineProps<{
  color?: string;
}>();
const progressValue = defineModel<number>({
  default: 0
});
const themeColor = computed(() => (props.color ? `rgb(var(--v-theme-${props.color}))` : "rgb(var(--v-theme-primary))"));
</script>

<template>
  <ProgressRoot
    v-model="progressValue"
    class="rounded-full relative h-4 w-[300px] overflow-hidden border border-pv-surface-variant bg-pv-surface"
  >
    <ProgressIndicator
      class="indicator rounded-full block relative w-full h-full transition-width overflow-hidden duration-660 ease-[cubic-bezier(0.65, 0, 0.35, 1)] before:animate-progress before:content-[''] before:absolute before:inset-0 before:bg-[linear-gradient(-45deg,rgba(255,255,255,0.2)_25%,transparent_25%,transparent_50%,rgba(255,255,255,0.2)_50%,rgba(255,255,255,0.2)_75%,transparent_75%,transparent)] before:bg-size-[30px_30px] before:-z-10 flex justify-center items-center text-xs  z-1"
      :style="`width: ${progressValue}%; background-color: ${themeColor}; color: contrast-color(${themeColor})`"
    >
      <span></span>{{ Math.ceil(progressValue) }}%
    </ProgressIndicator>
  </ProgressRoot>
</template>
