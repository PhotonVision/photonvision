<script setup lang="ts">
import { Label } from "reka-ui";

defineProps<{
  for?: string;
}>();
</script>

<template>
  <Label :for="for" class="text-pv-on-surface/70 text-xs font-medium">
    <slot />
  </Label>
</template>
