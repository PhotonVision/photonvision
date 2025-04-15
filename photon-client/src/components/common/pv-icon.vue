<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    iconName: string;
    disabled?: boolean;
    color?: string;
    tooltip?: string;
    right?: boolean;
    hover?: boolean;
  }>(),
  {
    right: false,
    disabled: false,
    hover: false
  }
);

defineEmits<{
  (e: "click"): void;
}>();

const hoverClass = props.hover ? "hover" : "";
</script>

<template>
  <div>
    <v-tooltip :right="right" :location="!right ? 'bottom' : undefined" offset="10" :disabled="tooltip === undefined">
      <template #activator="{ props }">
        <v-icon
          :class="hoverClass"
          :color="color"
          v-bind="props"
          :disabled="disabled"
          @click="$emit('click')"
        >
          {{ iconName }}
        </v-icon>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>

<style scoped>
.hover:hover {
  color: white !important;
}
</style>
