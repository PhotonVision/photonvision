<script setup lang="ts">
import { Label } from "reka-ui";

defineProps<{
  for?: string;
}>();
</script>

<template>
  <Label :for="for" class="text-xs font-medium text-pv-on-surface/70">
    <slot />
  </Label>
</template>
