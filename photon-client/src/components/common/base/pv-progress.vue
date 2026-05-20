<script setup lang="ts">
import { ProgressIndicator, ProgressRoot } from "reka-ui";
import { useThemeColor } from "../lib";

const props = withDefaults(
  defineProps<{
    color?: string;
    showPercentage?: boolean;
  }>(),
  { color: "primary", showPercentage: true }
);
const progressValue = defineModel<number>({
  default: 0
});
const { solid: themeColor } = useThemeColor(() => props.color);
</script>

<template>
  <ProgressRoot
    v-model="progressValue"
    class="border-pv-secondary bg-pv-surface relative h-4 w-75 overflow-hidden rounded-full border"
  >
    <ProgressIndicator
      class="indicator transition-width ease-[cubic-bezier(0.65, 0, 0.35, 1)] before:animate-progress relative z-1 flex h-full w-full items-center justify-center overflow-hidden rounded-full text-xs duration-660 before:absolute before:inset-0 before:-z-10 before:bg-[linear-gradient(-45deg,rgba(255,255,255,0.2)_25%,transparent_25%,transparent_50%,rgba(255,255,255,0.2)_50%,rgba(255,255,255,0.2)_75%,transparent_75%,transparent)] before:bg-size-[30px_30px] before:content-['']"
      :style="`width: ${progressValue}%; background-color: ${themeColor}; color: contrast-color(${themeColor})`"
    >
      <span v-if="showPercentage">{{ Math.ceil(progressValue) }}%</span>
    </ProgressIndicator>
  </ProgressRoot>
</template>
