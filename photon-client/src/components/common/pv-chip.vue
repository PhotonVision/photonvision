<script setup lang="ts">
import { computed, useAttrs } from "vue";

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

const themeColor = computed(() => `rgb(var(--v-theme-${props.color}))`);
const translucentBg = computed(() => `rgba(var(--v-theme-${props.color}), 0.15)`);

const isRawColor = computed(
  () => props.color.startsWith("#") || props.color.startsWith("rgb") || props.color.startsWith("var(")
);

const chipStyle = computed(() => {
  if (props.variant === "text") {
    const c = isRawColor.value ? props.color : themeColor.value;
    return { color: c };
  }
  // filled
  if (isRawColor.value) {
    return { backgroundColor: props.color, color: "#fff" };
  }
  return {
    backgroundColor: translucentBg.value,
    color: themeColor.value
  };
});
</script>

<template>
  <span
    v-bind="attrs"
    :style="chipStyle"
    :class="[
      'pv-chip',
      { 'pv-chip--label': label, 'pv-chip--text': variant === 'text' },
      attrs.class
    ]"
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
