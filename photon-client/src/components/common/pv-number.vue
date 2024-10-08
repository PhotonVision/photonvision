<script setup lang="ts">
import PvInputLayout from "@/components/common/pv-input-layout.vue";

withDefaults(
  defineProps<{
    label: string;
    tooltip?: string;
    labelCols?: number;
    disabled?: boolean;
    step?: number;
    min?: number;
    max?: number;
  }>(),
  {
    labelCols: 4,
    disabled: false,
    step: 1
  }
);

const model = defineModel<number>({ required: true });
</script>

<template>
  <pv-input-layout :label="label" :label-cols="labelCols" :tooltip="tooltip" tooltip-location="right">
    <v-number-input
      v-model="model"
      base-color="accent"
      control-variant="stacked"
      :disabled="disabled"
      hide-details
      inset
      :max="max"
      :min="min"
      :step="step"
    />
  </pv-input-layout>
</template>
