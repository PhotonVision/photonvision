<script setup lang="ts">
import { computed, useAttrs } from "vue";
import { useThemeColor } from "../../../lib/ComponentUtils";

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
    return { "--pv-chip-fg": solid.value };
  }
  // filled
  if (isRaw.value) {
    return { "--pv-chip-bg": solid.value, "--pv-chip-fg": "#fff" };
  }
  return {
    "--pv-chip-bg": translucent.value,
    "--pv-chip-fg": solid.value
  };
});
</script>

<template>
  <span
    v-bind="attrs"
    :style="chipStyle"
    :class="[
      'inline-flex items-center gap-1 whitespace-nowrap select-none align-middle text-sm font-medium leading-6',
      label ? 'rounded-[0.25rem]' : 'rounded-full',
      variant === 'text'
        ? 'bg-transparent p-0 text-[color:var(--pv-chip-fg)]'
        : 'bg-[color:var(--pv-chip-bg)] px-[0.65em] py-[0.15em] text-[color:var(--pv-chip-fg)]',
      attrs.class
    ]"
  >
    <slot />
  </span>
</template>
<style scoped>
:root {
  --pv-chip-bg: transparent;
  --pv-chip-fg: inherit;
}
</style>
