<script setup lang="ts" generic="V">
import PvInputLayout from "@/components/common/pv-input-layout.vue";
import type { DropdownSelectItem } from "@/types/Components";

withDefaults(
  defineProps<{
    label: string;
    tooltip?: string;
    labelCols?: number;
    disabled?: boolean;
    items: DropdownSelectItem<V>[];
    density?: "default" | "comfortable" | "compact";
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
      :disabled="false"
      hide-details
      item-disabled="disabled"
      item-title="name"
      item-value="value"
      :items="items"
    />
  </pv-input-layout>
</template>
