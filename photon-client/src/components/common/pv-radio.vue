<script setup lang="ts" generic="T">
import PvInputLayout from "@/components/common/pv-input-layout.vue";
import type { RadioItem } from "@/types/Components";

const model = defineModel<T>({ required: true });

withDefaults(
  defineProps<{
    label: string;
    tooltip?: string;
    labelCols?: number;
    disabled?: boolean;
    tooltipLocation?: "top" | "bottom" | "left" | "right" | "center";
    items: RadioItem<T>[];
    inline?: boolean;
  }>(),
  {
    labelCols: 4,
    disabled: false,
    tooltipLocation: "right",
    inline: false
  }
);
</script>

<template>
  <pv-input-layout :label="label" :label-cols="labelCols" :tooltip="tooltip" tooltip-location="right">
    <v-radio-group v-model="model" :disabled="disabled" :inline="inline">
      <div v-for="(item, index) in items" :key="index">
        <v-radio color="accent" :disabled="item.disabled" :label="item.name" :value="item.value" />
        <v-tooltip v-if="item.tooltip" activator="parent" :location="tooltipLocation" open-delay="150">{{
          item.tooltip
        }}</v-tooltip>
      </div>
    </v-radio-group>
  </pv-input-layout>
</template>
