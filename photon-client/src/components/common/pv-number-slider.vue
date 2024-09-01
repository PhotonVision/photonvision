<script setup lang="ts">
import PvInputLayout from "@/components/common/pv-input-layout.vue";

const model = defineModel<number>({ required: true });

withDefaults(
  defineProps<{
    label: string;
    tooltip?: string;
    labelCols?: number;
    disabled?: boolean;
    showNumberInput?: boolean;
    min: number;
    max: number;
    step?: number;
    trackColor?: string;
    trackFillColor?: string;
    trackThumbColor?: string;
    trackSize?: number;
  }>(),
  {
    labelCols: 4,
    disabled: false,
    showNumberInput: true,
    step: 1,
    trackColor: undefined,
    trackFillColor: "accent",
    trackThumbColor: "accent",
    trackSize: 2
  }
);
</script>

<template>
  <pv-input-layout :label="label" :label-cols="labelCols" :tooltip="tooltip" tooltip-location="right">
    <v-slider
      v-model="model"
      class="ml-0 mr-0"
      :disabled="disabled"
      hide-details
      :max="max"
      :min="min"
      :step="step"
      :thumb-color="trackThumbColor"
      :track-color="trackColor"
      :track-fill-color="trackFillColor"
      :track-size="trackSize"
    >
      <template #append>
        <v-text-field
          v-model="model"
          base-color="accent"
          class="pb-1"
          density="compact"
          :disabled="disabled"
          hide-details
          :max="max"
          :min="min"
          :step="step"
          style="width: 80px"
          type="number"
        />
      </template>
    </v-slider>
  </pv-input-layout>
</template>
