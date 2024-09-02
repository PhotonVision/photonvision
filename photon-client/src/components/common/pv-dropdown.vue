<script setup lang="ts" generic="V">
import PvInputLayout from "@/components/common/pv-input-layout.vue";
import type { DropdownSelectItem, ValidationRule } from "@/types/Components";

withDefaults(
  defineProps<{
    label: string;
    tooltip?: string;
    labelCols?: number;
    disabled?: boolean;
    items: DropdownSelectItem<V>[];
    density?: "default" | "comfortable" | "compact";
    rules?: ValidationRule[];
  }>(),
  {
    labelCols: 4,
    disabled: false,
    density: "default"
  }
);

const model = defineModel<V>({ required: true });
</script>

<template>
  <pv-input-layout :label="label" :label-cols="labelCols" :tooltip="tooltip" tooltip-location="right">
    <v-select
      v-model="model"
      base-color="accent"
      :density="density"
      :disabled="disabled"
      :hide-details="!rules?.length"
      item-disabled="disabled"
      item-title="name"
      item-value="value"
      :items="items"
      :rules="rules"
    />
  </pv-input-layout>
</template>
