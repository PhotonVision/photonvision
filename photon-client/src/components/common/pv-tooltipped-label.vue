<script setup lang="ts">
defineProps<{
  label?: string;
  tooltip?: string;
}>();
</script>

<template>
  <div>
    <v-tooltip :disabled="tooltip === undefined" location="right" open-delay="300">
      <template #activator="{ props }">
        <span style="cursor: text !important" class="text-white" v-bind="props">{{ label }}</span>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>
