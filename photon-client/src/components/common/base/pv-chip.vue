<script setup lang="ts">
import { computed, useAttrs } from "vue";
import { useThemeColor } from "../lib";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    color?: string;
    label?: boolean;
    variant?: "filled" | "text";
  }>(),
  {
    color: "primary",
    label: false,
    variant: "filled"
  }
);

const attrs = useAttrs();

const { solid, translucent, isRaw } = useThemeColor(() => props.color, { translucentAlpha: 0.15 });

const chipStyle = computed(() => {
  if (props.variant === "text") {
    return { color: solid.value };
  }
  // filled
  if (isRaw.value) {
    return { backgroundColor: solid.value, color: "#fff" };
  }
  return {
    backgroundColor: translucent.value,
    color: solid.value
  };
});
</script>

<template>
  <span
    v-bind="attrs"
    :style="chipStyle"
    :class="['pv-chip', { 'pv-chip--label': label, 'pv-chip--text': variant === 'text' }, attrs.class]"
  >
    <slot />
  </span>
</template>

<style scoped>
.pv-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25em;
  padding: 0.15em 0.65em;
  font-size: 0.875rem;
  font-weight: 500;
  line-height: 1.5;
  border-radius: 9999px;
  white-space: nowrap;
  user-select: none;
  vertical-align: middle;
}

.pv-chip--label {
  border-radius: 0.25rem;
}

.pv-chip--text {
  background: transparent !important;
  padding: 0;
}
</style>
