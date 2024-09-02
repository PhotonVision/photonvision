<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string;
    tooltip?: string;
    tooltipLocation?: "top" | "bottom" | "left" | "right" | "center";
    labelCols: number;
  }>(),
  {
    labelCols: 4,
    tooltipLocation: "right"
  }
);
</script>

<template>
  <v-row align="center" dense>
    <v-col :cols="12" :sm="labelCols">
      <span class="text-white" style="cursor: text !important">{{ label }}</span>
      <v-tooltip v-if="tooltip" activator="parent" :location="tooltipLocation" open-delay="150">
        {{ tooltip }}
      </v-tooltip>
    </v-col>
    <v-col cols="12" :sm="12 - labelCols">
      <slot />
    </v-col>
  </v-row>
</template>
