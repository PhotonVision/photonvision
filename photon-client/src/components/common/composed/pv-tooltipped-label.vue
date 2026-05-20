<script setup lang="ts">
import type { Component } from "vue";
import { Label } from "reka-ui";

defineProps<{
  label?: string;
  tooltip?: string;
  icon?: Component;
  location?: "top" | "bottom" | "left" | "right";
  for?: string;
}>();
</script>

<template>
  <div class="inline-flex max-w-full items-center">
    <Label v-if="!tooltip" :for="for" class="inline-flex max-w-full items-center gap-2 text-sm font-medium text-pv-on-surface">
      <span class="truncate">{{ label }}</span>
      <component :is="icon" v-if="icon" class="text-pv-primary size-4" aria-hidden="true" />
    </Label>

    <pv-tooltip v-else :text="tooltip" :location="location">
      <Label
        :for="for"
        as="button"
        type="button"
        class="inline-flex max-w-full items-center gap-2 text-left text-sm font-medium text-pv-on-surface outline-none"
      >
        <span class="truncate">{{ label }}</span>
        <component :is="icon" v-if="icon" class="text-pv-primary size-4" aria-hidden="true" />
      </Label>
    </pv-tooltip>
  </div>
</template>
