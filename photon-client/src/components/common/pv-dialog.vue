<script setup lang="ts">
import { computed } from "vue";
import { DialogContent, DialogOverlay, DialogPortal, DialogRoot } from "reka-ui";

const model = defineModel<boolean | undefined>({ required: true });

const props = withDefaults(
  defineProps<{
    width?: number | string;
    maxWidth?: number | string;
    persistent?: boolean;
    contentClass?: string;
  }>(),
  {
    persistent: false,
    contentClass: ""
  }
);

const normalizeSize = (value?: number | string) => {
  if (value === undefined) return undefined;
  return typeof value === "number" ? `${value}px` : value;
};

const contentStyle = computed(() => ({
  width: normalizeSize(props.width),
  maxWidth: normalizeSize(props.maxWidth)
}));

const preventDismiss = (event: Event) => {
  if (props.persistent) {
    event.preventDefault();
  }
};
</script>

<template>
  <dialog-root v-model:open="model">
    <dialog-portal>
      <dialog-overlay class="fixed inset-0 z-2400 bg-black/70 backdrop-blur-[1px]" />
      <dialog-content
        :style="contentStyle"
        :class="[
          'fixed top-1/2 left-1/2 z-2401 max-h-[calc(100vh-2rem)]  -translate-x-1/2 -translate-y-1/2 overflow-auto rounded-2xl border border-white/10 bg-pv-surface text-white shadow-2xl shadow-black/50 outline-none w-full max-w-[90%]',
          contentClass
        ]"
        @escape-key-down="preventDismiss"
        @pointer-down-outside="preventDismiss"
        @interact-outside="preventDismiss"
      >
        <slot />
      </dialog-content>
    </dialog-portal>
  </dialog-root>
</template>
