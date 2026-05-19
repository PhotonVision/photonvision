<script setup lang="ts">
import type { Component } from "vue";

defineProps<{
  label?: string;
  tooltip?: string;
  icon?: Component;
  location?: "top" | "bottom" | "left" | "right";
}>();
</script>

<template>
  <div class="inline-flex max-w-full items-center">
    <span v-if="!tooltip" class="inline-flex max-w-full items-center gap-2 text-sm font-medium text-white">
      <span class="truncate">{{ label }}</span>
      <component v-if="icon" :is="icon" class="size-4 text-pv-primary" aria-hidden="true" />
    </span>

    <pv-tooltip v-else :text="tooltip" :location="location">
      <button
        type="button"
        class="inline-flex max-w-full items-center gap-2 text-left text-sm font-medium text-white outline-none"
      >
        <span class="truncate">{{ label }}</span>
        <component v-if="icon" :is="icon" class="size-4 text-pv-primary" aria-hidden="true" />
      </button>
    </pv-tooltip>
  </div>
</template>
