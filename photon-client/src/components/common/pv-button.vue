<script setup lang="ts">
import { computed, useAttrs } from "vue";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    variant?: "primary" | "passive" | "danger" | "ghost" | "text";
    size?: "sm" | "md" | "icon";
    icon?: string;
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
  "inline-flex items-center justify-center gap-2 rounded-xl font-semibold shadow-sm outline-none transition focus-visible:ring-2 focus-visible:ring-pv-primary/50 disabled:cursor-not-allowed disabled:opacity-45";

const variantClass = computed(() => {
  switch (props.variant) {
    case "primary":
      return "bg-pv-button-active text-slate-950 hover:brightness-105";
    case "danger":
      return "border border-pv-error/45 bg-pv-error/25 text-white hover:bg-pv-error/20";
    case "ghost":
      return "border border-white/12 bg-transparent text-white hover:bg-white/6 shadow-none";
    case "text":
      return "bg-transparent text-white hover:bg-white/6 shadow-none";
    default:
      return "border border-white/12 bg-black/15 text-white hover:bg-white/8";
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
    <span
      v-if="loading"
      class="mdi mdi-loading animate-spin text-lg leading-none"
      aria-hidden="true"
    ></span>
    <span
      v-else-if="icon && !iconTrailing"
      :class="['mdi text-lg leading-none', icon]"
      aria-hidden="true"
    ></span>
    <slot />
    <span
      v-if="icon && iconTrailing"
      :class="['mdi text-lg leading-none', icon]"
      aria-hidden="true"
    ></span>
  </button>
</template>
