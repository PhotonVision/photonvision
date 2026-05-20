<script setup lang="ts">
import { computed, useAttrs } from "vue";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    variant?: "surface" | "transparent" | "ghost";
    bordered?: boolean;
    elevated?: boolean;
  }>(),
  {
    variant: "surface",
    bordered: true,
    elevated: false
  }
);

const attrs = useAttrs();

const baseClass = "text-white rounded-xl p-4";

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
  <div :class="[baseClass, variantClass, borderClass, elevationClass, attrs.class]">
    <slot />
  </div>
</template>
