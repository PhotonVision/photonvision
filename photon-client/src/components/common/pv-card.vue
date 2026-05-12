<script setup lang="ts">
import { computed, useAttrs } from "vue";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    variant?: "surface" | "transparent" | "ghost";
    padding?: "none" | "sm" | "md" | "lg";
    rounded?: "md" | "lg" | "xl";
    bordered?: boolean;
    elevated?: boolean;
  }>(),
  {
    variant: "surface",
    padding: "md",
    rounded: "xl",
    bordered: true,
    elevated: true
  }
);

const attrs = useAttrs();

const baseClass = "text-white";

const paddingClass = computed(() => {
  switch (props.padding) {
    case "none":
      return "";
    case "sm":
      return "p-3";
    case "lg":
      return "p-6";
    default:
      return "p-4";
  }
});

const roundedClass = computed(() => {
  switch (props.rounded) {
    case "md":
      return "rounded-lg";
    case "lg":
      return "rounded-xl";
    default:
      return "rounded-2xl";
  }
});

const variantClass = computed(() => {
  switch (props.variant) {
    case "transparent":
      return "bg-transparent";
    case "ghost":
      return "bg-black/15";
    default:
      return "bg-pv-surface";
  }
});

const borderClass = computed(() => (props.bordered ? "border border-white/10" : ""));
const elevationClass = computed(() => (props.elevated ? "shadow-2xl shadow-black/45" : "shadow-none"));
</script>

<template>
  <div :class="[baseClass, paddingClass, roundedClass, variantClass, borderClass, elevationClass, attrs.class]">
    <slot />
  </div>
</template>
