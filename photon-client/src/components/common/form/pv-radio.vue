<script setup lang="ts">
import { colWidthClass } from "../../../lib/ComponentUtils";
import { computed } from "vue";
import { Label, RadioGroupIndicator, RadioGroupItem, RadioGroupRoot } from "reka-ui";

const value = defineModel<number>({
  required: true
});

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    disabled?: boolean;
    inputCols?: number;
    list: string[];
  }>(),
  {
    disabled: false,
    inputCols: 8
  }
);

const labelWidthClass = computed(() => colWidthClass(12 - props.inputCols));
const inputWidthClass = computed(() => colWidthClass(props.inputCols));

// Reka UI RadioGroup works with string values, so bridge between numeric index and string
const stringValue = computed({
  get: () => String(value.value),
  set: (v) => {
    value.value = Number(v);
  }
});
</script>

<template>
  <div class="flex gap-2 sm:gap-3">
    <div :class="labelWidthClass" class="flex items-center pt-3 pb-3 pl-0">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div :class="inputWidthClass" class="flex items-center pt-3 pr-0 pb-3">
      <RadioGroupRoot v-model="stringValue" :disabled="disabled" orientation="horizontal" class="flex flex-wrap gap-4">
        <Label
          v-for="(radioName, index) in list"
          :key="index"
          class="flex cursor-pointer items-center gap-2"
          :class="{ 'cursor-not-allowed opacity-50': disabled }"
        >
          <RadioGroupItem
            :value="String(index)"
            :disabled="disabled"
            class="hover:border-pv-primary/60 focus-visible:ring-pv-primary/50 data-[state=checked]:border-pv-primary flex size-5 items-center justify-center rounded-full border-2 border-white/30 bg-black/20 shadow-inner transition outline-none focus-visible:ring-2 disabled:cursor-not-allowed"
          >
            <RadioGroupIndicator
              class="after:bg-pv-primary relative flex items-center justify-center after:block after:size-2.5 after:rounded-full"
            />
          </RadioGroupItem>
          <span class="text-sm select-none">{{ radioName }}</span>
        </Label>
      </RadioGroupRoot>
    </div>
  </div>
</template>
