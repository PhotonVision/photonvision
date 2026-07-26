<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { ToastProvider, ToastRoot, ToastTitle, ToastDescription, ToastViewport } from "reka-ui";

import { useThemeColor } from "../../lib/ComponentUtils";

const { solid, border } = useThemeColor(useStateStore().snackbarData.color);
</script>

<template>
  <ToastProvider>
    <ToastRoot
      v-model:open="useStateStore().snackbarData.show"
      :style="{ backgroundColor: solid, border: `1px solid ${border}`, '--snackbar-solid': solid }"
      class="data-[state=open]:animate-slideIn data-[state=closed]:animate-hide data-[swipe=end]:animate-swipeOut grid grid-cols-[auto_max-content] items-center gap-x-4 rounded-lg border p-4 shadow-sm [grid-template-areas:'title_action'_'description_action'] data-[swipe=cancel]:translate-x-0 data-[swipe=cancel]:transition-[transform_200ms_ease-out] data-[swipe=move]:translate-x-(--reka-toast-swipe-move-x)"
      :duration="useStateStore().snackbarData.timeout"
    >
      <ToastTitle class="snackbar-title mb-1.25 text-sm font-medium [grid-area:title]">
        {{ useStateStore().snackbarData.message }}
      </ToastTitle>
      <ToastDescription as-child>
        <div class="flex w-full [grid-area:description]">
          <pv-progress
            v-if="useStateStore().snackbarData.progressBar !== -1"
            v-model="useStateStore().snackbarData.progressBar"
            :color="useStateStore().snackbarData.progressBarColor"
          ></pv-progress>
        </div>
      </ToastDescription>
    </ToastRoot>
    <ToastViewport
      class="fixed right-0 bottom-0 z-2147483647 m-0 flex w-97.5 max-w-[100vw] list-none flex-col gap-2.5 p-(--viewport-padding) outline-none [--viewport-padding:25px]"
    />
  </ToastProvider>
</template>

<style scoped>
.snackbar-title {
  color: oklch(from var(--snackbar-solid) round(1.21 - L) 0 0);
}

@supports (color: contrast-color(red)) {
  .snackbar-title {
    color: contrast-color(var(--snackbar-solid));
  }
}
</style>
