<script setup lang="ts">
defineProps<{
  label?: string;
  tooltip?: string;
  icon?: string;
  location?: "top" | "bottom" | "left" | "right";
}>();
</script>

<template>
  <div>
    <v-tooltip :disabled="tooltip === undefined" :location="location ?? 'right'" open-delay="300">
      <template #activator="{ props }">
        <span style="cursor: text !important" class="text-white" v-bind="props">{{ label }}</span>
        <v-icon v-if="icon" small class="ml-2" color="primary" v-bind="props">
          {{ icon }}
        </v-icon>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>
