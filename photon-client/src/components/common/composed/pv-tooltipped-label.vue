<script setup lang="ts">
import type { Component } from "vue";
import { Label } from "reka-ui";

defineProps<{
  label?: string;
  tooltip?: string;
  icon?: Component;
  location?: "top" | "bottom" | "left" | "right";
  targetId?: string;
}>();
</script>

<template>
  <div class="inline-flex max-w-full items-center">
    <Label
      v-if="!tooltip"
      :for="targetId"
      class="text-pv-on-surface inline-flex max-w-full items-center gap-2 text-sm font-medium"
    >
      <span class="truncate">{{ label }}</span>
      <component :is="icon" v-if="icon" class="text-pv-primary size-4" aria-hidden="true" />
    </Label>

    <pv-tooltip v-else :text="tooltip" :location="location">
      <Label
        :for="targetId"
        as="button"
        type="button"
        class="text-pv-on-surface inline-flex max-w-full cursor-help items-center gap-2 text-left text-sm font-medium outline-none select-text"
        v-bind="$attrs"
      >
        <span class="truncate">{{ label }}</span>
        <component :is="icon" v-if="icon" class="text-pv-primary size-4" aria-hidden="true" />
      </Label>
    </pv-tooltip>
  </div>
</template>
