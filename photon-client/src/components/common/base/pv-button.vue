<script setup lang="ts">
import { computed, useAttrs } from "vue";
import type { Component } from "vue";
import IconLoading from "~icons/mdi/loading";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    variant?: "primary" | "passive" | "danger" | "ghost" | "text";
    size?: "sm" | "md" | "icon";
    icon?: Component;
    iconTrailing?: boolean;
    block?: boolean;
    disabled?: boolean;
    loading?: boolean;
    type?: "button" | "submit" | "reset";
  }>(),
  {
    variant: "passive",
    size: "md",
    iconTrailing: false,
    block: false,
    disabled: false,
    loading: false,
    type: "button"
  }
);

const attrs = useAttrs();

const baseClass =
  "inline-flex items-center justify-center gap-2 rounded-xl font-semibold  outline-none transition focus-visible:ring-2 focus-visible:ring-pv-primary/50 disabled:cursor-not-allowed disabled:opacity-45";

const variantClass = computed(() => {
  switch (props.variant) {
    case "primary":
      return "shadow-sm bg-pv-button-active text-slate-950 hover:brightness-105";
    case "danger":
      return "shadow-sm border border-pv-error/45 bg-pv-error/25 text-white hover:bg-pv-error/20";
    case "ghost":
      return "border border-white/12 bg-transparent text-white hover:bg-white/6 ";
    case "text":
      return "bg-transparent text-white hover:bg-white/6 ";
    default:
      return "shadow-sm border border-white/12 bg-black/15 text-white hover:bg-white/8";
  }
});

const sizeClass = computed(() => {
  switch (props.size) {
    case "sm":
      return "min-h-9 px-3 py-2 text-sm";
    case "icon":
      return "h-9 w-9 shrink-0 rounded-full px-0 text-base";
    default:
      return "min-h-11 px-4 py-2 text-sm";
  }
});

const widthClass = computed(() => (props.block ? "w-full" : ""));
</script>

<template>
  <button
    v-bind="attrs"
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
    :class="[baseClass, variantClass, sizeClass, widthClass, attrs.class]"
  >
    <IconLoading v-if="loading" class="size-5 animate-spin" aria-hidden="true" />
    <component v-else-if="icon && !iconTrailing" :is="icon" class="size-5 shrink-0" aria-hidden="true" />
    <slot />
    <component v-if="icon && iconTrailing" :is="icon" class="size-5 shrink-0" aria-hidden="true" />
  </button>
</template>
