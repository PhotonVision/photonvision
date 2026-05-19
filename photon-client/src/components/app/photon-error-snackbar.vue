<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { ToastProvider, ToastRoot, ToastTitle, ToastDescription, ToastViewport } from "reka-ui";

import { computed } from "vue";

const toThemeVar = (color: string) => {
  const normalized = color.includes("-") ? color : color.replace(/[A-Z]/g, (match) => `-${match.toLowerCase()}`);
  return `--color-pv-${normalized}`;
};

const themeColor = computed(() => `var(${toThemeVar(useStateStore().snackbarData.color)})`);
const borderThemeColor = computed(() => `color-mix(in srgb, ${themeColor.value} 45%, transparent)`);
</script>

<template>
  <ToastProvider>
    <ToastRoot
      v-model:open="useStateStore().snackbarData.show"
      :style="{ backgroundColor: themeColor, border: `1px solid ${borderThemeColor}` }"
      class="rounded-lg shadow-sm border p-4 grid [grid-template-areas:'title_action'_'description_action'] grid-cols-[auto_max-content] gap-x-4 items-center data-[state=open]:animate-slideIn data-[state=closed]:animate-hide data-[swipe=move]:translate-x-(--reka-toast-swipe-move-x) data-[swipe=cancel]:translate-x-0 data-[swipe=cancel]:transition-[transform_200ms_ease-out] data-[swipe=end]:animate-swipeOut"
      :duration="useStateStore().snackbarData.timeout"
    >
      <ToastTitle
        class="[grid-area:title] mb-1.25 font-medium text-sm"
        :style="{ color: `contrast-color(${themeColor})` }"
      >
        {{ useStateStore().snackbarData.message }}
      </ToastTitle>
      <ToastDescription as-child>
        <div class="w-full flex [grid-area:description]">
          <pv-progress
            v-model="useStateStore().snackbarData.progressBar"
            v-if="useStateStore().snackbarData.progressBar !== -1"
            :color="useStateStore().snackbarData.progressBarColor"
          ></pv-progress>
        </div>
      </ToastDescription>
    </ToastRoot>
    <ToastViewport
      class="[--viewport-padding:25px] fixed bottom-0 right-0 flex flex-col p-(--viewport-padding) gap-2.5 w-97.5 max-w-[100vw] m-0 list-none z-2147483647 outline-none"
    />
  </ToastProvider>
</template>
