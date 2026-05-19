<script setup lang="ts">
import { computed, ref, useAttrs } from "vue";
import type { Component } from "vue";
import IconClose from "~icons/mdi/close";
import { useThemeColor } from "../lib";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    color?: string;
    density?: "default" | "compact" | "comfortable";
    icon?: Component;
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

const { solid, translucent, border: borderColor, isRaw, isLightTone } = useThemeColor(() => props.color);

const alertStyle = computed(() => {
  const isTonalOrOutlined = props.variant === "tonal" || props.variant === "outlined";

  if (isRaw.value) {
    return {
      "--pv-alert-color": solid.value,
      backgroundColor: isTonalOrOutlined ? "transparent" : solid.value,
      borderColor: solid.value
    };
  }

  return {
    "--pv-alert-color": solid.value,
    backgroundColor:
      props.variant === "tonal" ? translucent.value : props.variant === "outlined" ? "transparent" : solid.value,
    borderColor: borderColor.value
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
    <component :is="icon" v-if="icon" class="mt-0.5 size-5 shrink-0" aria-hidden="true" />
    <div class="min-w-0 flex-1">
      <slot>{{ text }}</slot>
    </div>
    <button
      v-if="closable"
      type="button"
      class="-mt-1 -mr-1 inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full opacity-75 transition hover:bg-white/12 hover:opacity-100 focus-visible:ring-2 focus-visible:ring-white/40 focus-visible:outline-none"
      aria-label="Close alert"
      @click="shown = false"
    >
      <IconClose class="size-4" aria-hidden="true" />
    </button>
  </div>
</template>
