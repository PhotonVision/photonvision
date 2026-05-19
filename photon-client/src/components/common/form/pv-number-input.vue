<script setup lang="ts">
import { colWidthClass } from "../lib";
import { computed } from "vue";
const value = defineModel<number>({
  required: true
});
const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    disabled?: boolean;
    labelCols?: number;
    rules?: ((v: string | number | null) => boolean | string)[];
    step?: number;
  }>(),
  {
    disabled: false,
    labelCols: 2,
    step: 1
  }
);

const labelWidthClass = computed(() => colWidthClass(props.labelCols));

const localValue = computed({
  get: () => value.value,
  set: (v) => (value.value = parseFloat(v as unknown as string))
});
</script>

<template>
  <div class="flex gap-2 sm:gap-3">
    <div :class="labelWidthClass" class="pt-10px pb-10px flex items-center pl-0">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div class="pt-10px pb-10px flex-1 pr-0">
      <pv-text-field
        v-model="localValue"
        density="compact"
        hide-details
        type="number"
        variant="outline"
        :step="step"
        :disabled="disabled"
        :rules="rules"
        raw-value
        :input-style="{ width: '70px' }"
      />
    </div>
  </div>
</template>
