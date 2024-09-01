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
    <v-col :cols="labelCols">
      <span class="text-white" style="cursor: text !important">{{ label }}</span>
      <v-tooltip v-if="tooltip" activator="parent" :location="tooltipLocation" open-delay="150">
        {{ tooltip }}
      </v-tooltip>
    </v-col>
    <v-col>
      <slot />
    </v-col>
  </v-row>
</template>
