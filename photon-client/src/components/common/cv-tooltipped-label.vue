<script setup lang="ts">
defineProps<{
  label?: string;
  tooltip?: string;
}>();
</script>

<template>
  <div>
    <v-tooltip :disabled="tooltip === undefined" right open-delay="300">
      <template #activator="{ on, attrs }">
        <span style="cursor: text !important" class="white--text" v-bind="attrs" v-on="on">{{ label }}</span>
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>
