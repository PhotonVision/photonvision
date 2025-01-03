<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    color?: string;
  }>(),
  {
    color: "white"
  }
);
</script>

<template>
  <div>
    <v-tooltip :disabled="tooltip === undefined" right open-delay="300">
      <template #activator="{ on, attrs }">
        <span :class="`${color}--text`" style="cursor: text !important" v-bind="attrs" v-on="on">{{ label }}</span>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>
