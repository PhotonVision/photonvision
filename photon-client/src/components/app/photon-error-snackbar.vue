<script setup lang="ts">
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { ToastClose, ToastDescription, ToastProvider, ToastRoot, ToastTitle, ToastViewport } from "reka-ui";
import IconAlert from "~icons/mdi/alert-outline";
import IconAlertCircle from "~icons/mdi/alert-circle-outline";
import IconCheckCircle from "~icons/mdi/check-circle-outline";
import IconClose from "~icons/mdi/close";
import IconInformation from "~icons/mdi/information-outline";

import { useThemeColor } from "../../lib/ComponentUtils";

const { solid } = useThemeColor(() => useStateStore().snackbarData.color);

const icon = computed(() => {
  switch (useStateStore().snackbarData.color) {
    case "success":
      return IconCheckCircle;
    case "error":
      return IconAlertCircle;
    case "warning":
      return IconAlert;
    default:
      return IconInformation;
  }
});

const MAX_TIMEOUT_MS = 2 ** 31 - 1;
const duration = computed(() => {
  const timeout = useStateStore().snackbarData.timeout;
  return timeout > 0 ? timeout : MAX_TIMEOUT_MS;
});
</script>

<template>
  <ToastProvider swipe-direction="up">
    <ToastRoot
      v-model:open="useStateStore().snackbarData.show"
      :style="{ backgroundColor: solid, '--snackbar-solid': solid }"
      class="snackbar data-[state=open]:animate-toast-in data-[state=closed]:animate-toast-out data-[swipe=end]:animate-toast-swipe-out pointer-events-auto flex flex-col gap-3 rounded-xl border border-black/10 p-4 shadow-lg shadow-black/30 data-[swipe=cancel]:translate-y-0 data-[swipe=cancel]:transition-transform data-[swipe=move]:translate-y-(--reka-toast-swipe-move-y)"
      :duration="duration"
    >
      <div class="flex items-start gap-3">
        <component :is="icon" class="size-5 shrink-0" aria-hidden="true" />
        <ToastTitle class="snackbar-title min-w-0 flex-1 text-sm leading-5 font-medium wrap-break-word">
          {{ useStateStore().snackbarData.message }}
        </ToastTitle>
        <ToastClose
          class="-m-1 inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full opacity-75 transition hover:bg-current/15 hover:opacity-100 focus-visible:ring-2 focus-visible:ring-current/40 focus-visible:outline-none"
          aria-label="Close notification"
        >
          <IconClose class="size-4" aria-hidden="true" />
        </ToastClose>
      </div>
      <ToastDescription v-if="useStateStore().snackbarData.progressBar !== -1" as-child>
        <div class="flex w-full justify-center">
          <pv-progress
            v-model="useStateStore().snackbarData.progressBar"
            :color="useStateStore().snackbarData.progressBarColor"
          ></pv-progress>
        </div>
      </ToastDescription>
    </ToastRoot>
    <ToastViewport
      class="pointer-events-none fixed top-0 left-1/2 z-2147483647 m-0 flex w-110 max-w-[100vw] -translate-x-1/2 list-none flex-col gap-2.5 p-4 outline-none"
    />
  </ToastProvider>
</template>

<style scoped>
.snackbar {
  color: oklch(from var(--snackbar-solid) round(1.21 - L) 0 0);
}

@supports (color: contrast-color(red)) {
  .snackbar {
    color: contrast-color(var(--snackbar-solid));
  }
}
</style>
