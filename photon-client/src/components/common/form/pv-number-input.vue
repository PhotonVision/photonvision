<script setup lang="ts">
import { colWidthClass } from "../../../lib/ComponentUtils";
import { computed, useId } from "vue";
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
    id?: string;
  }>(),
  {
    disabled: false,
    labelCols: 2,
    step: 1
  }
);

const uniqueId = useId();
const inputId = computed(() => props.id || uniqueId);

const labelWidthClass = computed(() => colWidthClass(props.labelCols));

const localValue = computed({
  get: () => value.value,
  set: (v) => (value.value = parseFloat(v as unknown as string))
});
</script>

<template>
  <div class="flex gap-2 sm:gap-3">
    <div :class="labelWidthClass" class="flex items-center pt-3 pb-3 pl-0">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" :target-id="inputId" />
    </div>
    <div class="flex-1 pt-3 pr-0 pb-3">
      <pv-text-field
        :id="inputId"
        v-model="localValue"
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
