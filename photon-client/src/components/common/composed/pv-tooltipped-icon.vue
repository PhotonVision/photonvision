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
    <pv-tooltip :text="tooltip" :location="right ? 'right' : 'left'" :disabled="!tooltip">
      <pv-button size="icon">
        <pv-icon
          :class="hover ? 'hover:text-white' : ''"
          :icon="icon"
          :color="color"
          :disabled="disabled"
          @click="$emit('click')"
        />
      </pv-button>
    </pv-tooltip>
  </div>
</template>
