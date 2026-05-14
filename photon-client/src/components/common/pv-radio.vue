<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";
import { RadioGroupIndicator, RadioGroupItem, RadioGroupRoot } from "reka-ui";

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

const colWidthClass = (cols: number) => colWidthClasses[cols] ?? "flex-1";
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
    <div :class="labelWidthClass" class="flex items-center pl-0 pt-10px pb-10px">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div :class="inputWidthClass" class="flex items-center pr-0 pt-10px pb-10px">
      <RadioGroupRoot
        v-model="stringValue"
        :disabled="disabled"
        orientation="horizontal"
        class="flex flex-wrap gap-4"
      >
        <label
          v-for="(radioName, index) in list"
          :key="index"
          class="flex cursor-pointer items-center gap-2"
          :class="{ 'cursor-not-allowed opacity-50': disabled }"
        >
          <RadioGroupItem
            :value="String(index)"
            :disabled="disabled"
            class="flex size-5 items-center justify-center rounded-full border-2 border-white/30 bg-black/20 shadow-inner outline-none transition hover:border-pv-primary/60 focus-visible:ring-2 focus-visible:ring-pv-primary/50 data-[state=checked]:border-pv-primary disabled:cursor-not-allowed"
          >
            <RadioGroupIndicator
              class="relative flex items-center justify-center after:block after:size-2.5 after:rounded-full after:bg-pv-primary"
            />
          </RadioGroupItem>
          <span class="select-none text-sm">{{ radioName }}</span>
        </label>
      </RadioGroupRoot>
    </div>
  </div>
</template>
