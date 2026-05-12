<script setup lang="ts">
import { computed, ref, useAttrs } from "vue";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    color?: string;
    density?: "default" | "compact" | "comfortable";
    icon?: string;
    text?: string;
    variant?: "tonal" | "elevated" | "flat" | "outlined";
    closable?: boolean;
  }>(),
  {
    color: "info",
    density: "default",
    variant: "tonal",
    closable: false
  }
);

const attrs = useAttrs();
const shown = ref(true);

const themeColor = computed(() => `rgb(var(--v-theme-${props.color}))`);
const translucentThemeColor = computed(() => `rgba(var(--v-theme-${props.color}),  0.65)`);
const borderThemeColor = computed(() => `rgba(var(--v-theme-${props.color}), 0.45)`);

const isLightTone = computed(() =>
  ["buttonActive", "primary", "secondary", "warning", "success"].includes(props.color)
);

const alertStyle = computed(() => {
  if (props.color.startsWith("#") || props.color.startsWith("rgb") || props.color.startsWith("var(")) {
    return {
      "--pv-alert-color": props.color,
      backgroundColor: props.variant === "tonal" || props.variant === "outlined" ? "transparent" : props.color,
      borderColor: props.color
    };
  }

  return {
    "--pv-alert-color": themeColor.value,
    backgroundColor:
      props.variant === "tonal"
        ? translucentThemeColor.value
        : props.variant === "outlined"
          ? "transparent"
          : themeColor.value,
    borderColor: borderThemeColor.value
  };
});

const paddingClass = computed(() => {
  switch (props.density) {
    case "compact":
      return "px-3 py-2";
    case "comfortable":
      return "px-4 py-3";
    default:
      return "px-4 py-3.5";
  }
});

const textClass = computed(() => {
  if (props.variant === "tonal" || props.variant === "outlined") return "text-white";
  return isLightTone.value ? "text-slate-950" : "text-white";
});
</script>

<template>
  <div
    v-if="shown"
    v-bind="attrs"
    role="alert"
    :style="alertStyle"
    :class="[
      'flex w-full items-start gap-2 rounded-lg border text-sm leading-5 shadow-sm',
      paddingClass,
      textClass,
      attrs.class
    ]"
  >
    <span v-if="icon" :class="['mdi mt-0.5 shrink-0 text-lg leading-none', icon]" aria-hidden="true"></span>
    <div class="min-w-0 flex-1">
      <slot>{{ text }}</slot>
    </div>
    <button
      v-if="closable"
      type="button"
      class="-mr-1 -mt-1 inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full opacity-75 transition hover:bg-white/12 hover:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40"
      aria-label="Close alert"
      @click="shown = false"
    >
      <span class="mdi mdi-close text-base leading-none" aria-hidden="true"></span>
    </button>
  </div>
</template>
