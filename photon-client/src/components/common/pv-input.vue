<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import PvTextField from "@/components/common/pv-text-field.vue";
import { computed } from "vue";

const value = defineModel<string>({ required: true });

const props = withDefaults(
  defineProps<{
    type?: HTMLInputElement["type"];
    label?: string;
    tooltip?: string;
    disabled?: boolean;
    errorMessage?: string;
    placeholder?: string;
    labelCols?: number;
    inputCols?: number;
    rules?: ((v: string | number | null) => boolean | string)[];
    clearable?: boolean;
  }>(),
  {
    disabled: false,
    inputCols: 8,
    clearable: false,
    type: "text"
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
const labelWidthClass = computed(() => colWidthClass(props.labelCols || 12 - props.inputCols));
const inputWidthClass = computed(() => colWidthClass(props.inputCols));

const emit = defineEmits<{
  (e: "onEnter", value: string): void;
  (e: "onEscape"): void;
}>();

const handleKeydown = ({ key }: KeyboardEvent) => {
  switch (key) {
    case "Enter":
      // Explicitly check that all rule props return true
      if (!props.rules?.every((rule) => rule(value.value) === true)) return;

      emit("onEnter", value.value);
      break;
    case "Escape":
      emit("onEscape");
      break;
  }
};

// TODO: fix error text theming
</script>
<template>
  <div class="flex gap-2 sm:gap-3">
    <div :class="labelWidthClass" class="flex items-center pl-0 pt-10px pb-10px">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </div>

    <div :class="inputWidthClass" class="flex items-center pr-0 pt-10px pb-10px">
      <pv-text-field
        v-model="value"
        density="compact"
        :placeholder="placeholder"
        :type="type"
        :disabled="disabled"
        :error-message="errorMessage"
        :rules="rules"
        variant="outline"
        :clearable="clearable"
        @keydown="handleKeydown"
        v-bind="$attrs"
      />
    </div>
  </div>
</template>
