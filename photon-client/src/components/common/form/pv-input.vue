<script setup lang="ts">
import { colWidthClass } from "../lib";
import { computed, useId } from "vue";

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
    id?: string;
  }>(),
  {
    disabled: false,
    inputCols: 8,
    clearable: false,
    type: "text"
  }
);

const id = useId();
const inputId = computed(() => props.id || id);

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
    <div :class="labelWidthClass" class="flex items-center pt-3 pb-3 pl-0">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" :for="inputId" />
    </div>

    <div :class="inputWidthClass" class="flex items-center pt-3 pr-0 pb-3">
      <pv-text-field
        :id="inputId"
        v-model="value"
        
        :placeholder="placeholder"
        :type="type"
        :disabled="disabled"
        :error-message="errorMessage"
        :rules="rules"
        variant="outline"
        :clearable="clearable"
        v-bind="$attrs"
        @keydown="handleKeydown"
      />
    </div>
  </div>
</template>
