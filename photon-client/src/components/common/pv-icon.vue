<script setup lang="ts">
import mdiIcons from "@iconify-json/mdi/icons.json";
import { addCollection, Icon } from "@iconify/vue";
import { computed, useSlots } from "vue";
import { useTheme } from "vuetify";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    icon?: string;
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

addCollection(mdiIcons);

const slots = useSlots();
const theme = useTheme();

const slotIcon = computed(() => {
  const children = slots.default?.()[0]?.children;
  return typeof children === "string" ? children.trim() : undefined;
});

const iconName = computed(() => {
  const name = props.icon ?? slotIcon.value ?? "";
  return name.startsWith("mdi-") ? name.replace("mdi-", "mdi:") : name;
});

const resolvedColor = computed(() => {
  if (!props.color) return undefined;
  return theme.current.value.colors[props.color] ?? props.color;
});

const colorClass = computed(() => {
  if (!props.color || resolvedColor.value !== props.color) return undefined;
  if (props.color.startsWith("#") || props.color.startsWith("rgb") || props.color.startsWith("var(")) return undefined;
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
    <Icon :icon="iconName" :width="iconSize" :height="iconSize" />
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
  opacity: var(--v-disabled-opacity);
  pointer-events: none;
}

.pv-icon--start {
  margin-inline-end: 8px;
}

.pv-icon--end {
  margin-inline-start: 8px;
}
</style>
