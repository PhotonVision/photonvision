<script setup lang="ts">
import { colWidthClass } from "../lib";
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
      <pv-tooltipped-label :tooltip="tooltip" :label="label" />
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
