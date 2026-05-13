<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import PvTextField from "@/components/common/pv-text-field.vue";
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

const colWidthClasses: Record<number, string> = {
  1: "w-1/12",
  2: "w-1/6",
  3: "w-1/4",
  4: "w-1/3",
  5: "w-5/12",
  6: "w-1/2",
  7: "w-7/12",
  8: "w-2/3",
  9: "w-3/4",
  10: "w-5/6",
  11: "w-11/12",
  12: "w-full"
};

const labelWidthClass = computed(() => colWidthClasses[props.labelCols] ?? "flex-1");

const localValue = computed({
  get: () => value.value,
  set: (v) => (value.value = parseFloat(v as unknown as string))
});
</script>

<template>
  <div class="flex gap-2 sm:gap-3">
    <div :class="labelWidthClass" class="flex items-center pl-0 pt-10px pb-10px">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div class="flex-1 pr-0 pt-10px pb-10px">
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
