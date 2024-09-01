<script setup lang="ts">
import PvInputLayout from "@/components/common/pv-input-layout.vue";
import type { ValidationRule } from "@/types/Components";

withDefaults(
  defineProps<{
    label: string;
    tooltip?: string;
    labelCols?: number;
    disabled?: boolean;
    errorMessage?: string;
    placeholder?: string;
    rules?: ValidationRule[];
  }>(),
  {
    labelCols: 4,
    disabled: false
  }
);

const model = defineModel<string>({ required: true });
</script>

<template>
  <pv-input-layout :label="label" :label-cols="labelCols" :tooltip="tooltip" tooltip-location="right">
    <v-text-field
      v-model="model"
      base-color="accent"
      :disabled="disabled"
      :error-message="errorMessage"
      :placeholder="placeholder"
      :rules="rules"
    />
  </pv-input-layout>
</template>
