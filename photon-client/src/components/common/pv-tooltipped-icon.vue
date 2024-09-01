<script setup lang="ts">
withDefaults(
  defineProps<{
    iconName: string;
    disabled?: boolean;
    color?: string;
    tooltip: string;
    right?: boolean;
    tooltipLocation?: "top" | "bottom" | "left" | "right" | "center";
    vuetifyProps?: any;
    size?: "x-small" | "small" | "default" | "large" | "x-large";
    clickable?: boolean;
  }>(),
  {
    right: false,
    disabled: false,
    tooltipLocation: "right",
    size: "default",
    clickable: false
  }
);

const emit = defineEmits<{
  click: [];
}>();
</script>

<template>
  <div>
    <v-btn
      v-if="clickable"
      :color="color"
      v-bind="vuetifyProps"
      density="compact"
      :disabled="disabled"
      :icon="iconName"
      :size="size"
      variant="plain"
      @click="emit('click')"
    />
    <v-icon
      v-else
      :color="color"
      :disabled="disabled"
      :icon="iconName"
      v-bind="vuetifyProps"
      :size="size"
    />
    <v-tooltip activator="parent" :location="tooltipLocation" open-delay="150">{{ tooltip }}</v-tooltip>
  </div>
</template>
