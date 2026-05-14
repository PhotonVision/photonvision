<script setup lang="ts">
import PvTooltip from "@/components/common/pv-tooltip.vue";

defineProps<{
  label?: string;
  tooltip?: string;
  icon?: string;
  location?: "top" | "bottom" | "left" | "right";
}>();
</script>

<template>
  <div class="inline-flex max-w-full items-center">
    <span v-if="!tooltip" class="inline-flex max-w-full items-center gap-2 text-sm font-medium text-white">
      <span class="truncate">{{ label }}</span>
      <span v-if="icon" :class="['mdi text-pv-primary text-sm leading-none', icon]" aria-hidden="true"></span>
    </span>

    <pv-tooltip v-else :text="tooltip" :location="location">
      <button
        type="button"
        class="inline-flex max-w-full items-center gap-2 text-left text-sm font-medium text-white outline-none"
      >
        <span class="truncate">{{ label }}</span>
        <span v-if="icon" :class="['mdi text-pv-primary text-sm leading-none', icon]" aria-hidden="true"></span>
      </button>
    </pv-tooltip>
  </div>
</template>
