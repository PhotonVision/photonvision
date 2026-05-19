<script setup lang="ts">
import { computed } from "vue";
import type { Component } from "vue";
import { useTheme } from "@/composables/useTheme";
import { isRawCssColor } from "../lib";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    icon?: Component;
    color?: string;
    size?: string | number;
    disabled?: boolean;
    start?: boolean;
    end?: boolean;
    left?: boolean;
    right?: boolean;
  }>(),
  {
    disabled: false,
    start: false,
    end: false,
    left: false,
    right: false
  }
);

const emit = defineEmits<{
  (e: "click", event: MouseEvent): void;
}>();

const theme = useTheme();
const IconComponent = computed(() => props.icon ?? null);

const resolvedColor = computed(() => {
  if (!props.color) return undefined;
  const themeColors = theme.colors.value as Record<string, string>;
  return themeColors[props.color] ?? props.color;
});

const colorClass = computed(() => {
  if (!props.color || resolvedColor.value !== props.color) return undefined;
  if (isRawCssColor(props.color)) return undefined;
  if (!props.color.includes("-")) return undefined;
  return `text-${props.color}`;
});

const iconStyle = computed(() => ({
  color: colorClass.value ? undefined : resolvedColor.value
}));

const iconSize = computed(() => {
  if (props.size === undefined) return undefined;
  if (typeof props.size === "number") return `${props.size}px`;

  switch (props.size) {
    case "x-small":
      return "1em";
    case "small":
      return "1.25em";
    case "default":
      return "1.5em";
    case "large":
      return "1.75em";
    case "x-large":
      return "2em";
    default:
      return props.size;
  }
});

const classes = computed(() => [
  {
    "pv-icon": true,
    "pv-icon--disabled": props.disabled,
    "pv-icon--start": props.start || props.left,
    "pv-icon--end": props.end || props.right
  },
  colorClass.value
]);

const handleClick = (event: MouseEvent) => {
  if (props.disabled) {
    event.preventDefault();
    event.stopPropagation();
    return;
  }

  emit("click", event);
};
</script>

<template>
  <span
    v-bind="$attrs"
    :class="classes"
    :style="iconStyle"
    role="img"
    :aria-hidden="$attrs['aria-label'] === undefined"
    @click="handleClick"
  >
    <component :is="IconComponent" v-if="IconComponent" :width="iconSize" :height="iconSize" />
    <slot v-else />
  </span>
</template>

<style scoped>
.pv-icon {
  align-items: center;
  color: currentColor;
  display: inline-flex;
  flex: none;
  font-size: 1.5rem;
  height: 1em;
  justify-content: center;
  line-height: 1;
  user-select: none;
  vertical-align: middle;
  width: 1em;
}

.pv-icon--disabled {
  opacity: var(--pv-disabled-opacity);
  pointer-events: none;
}

.pv-icon--start {
  margin-inline-end: 8px;
}

.pv-icon--end {
  margin-inline-start: 8px;
}
</style>
