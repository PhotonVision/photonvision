<script setup lang="ts">
import type { Component } from "vue";

withDefaults(
  defineProps<{
    icon: Component;
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
</script>

<template>
  <div>
    <pv-tooltip :text="tooltip" :location="right ? 'right' : 'left'">
      <pv-icon
        :class="hover ? 'hover' : ''"
        :icon="icon"
        :color="color"
        :disabled="disabled"
        @click="$emit('click')"
      />
    </pv-tooltip>

  </div>
</template>

<style scoped>
.hover:hover {
  color: white !important;
}
</style>
