<script setup lang="ts">
import { colWidthClass, fieldWrapperClasses } from "../lib";
import { computed, useTemplateRef } from "vue";
import IconClose from "~icons/mdi/close";

const value = defineModel<File | File[] | null>({ required: true });

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    disabled?: boolean;
    errorMessage?: string;
    placeholder?: string;
    labelCols?: number;
    inputCols?: number;
    clearable?: boolean;
    accept?: string;
    multiple?: boolean;
    variant?: "underlined" | "outline";
  }>(),
  {
    disabled: false,
    inputCols: 8,
    clearable: false,
    multiple: false,
    variant: "outline"
  }
);

const labelWidthClass = computed(() => colWidthClass(props.labelCols || 12 - props.inputCols));
const inputWidthClass = computed(() => colWidthClass(props.inputCols));

const fileInputRef = useTemplateRef<HTMLInputElement>("fileInput");

const handleChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const files = target.files;
  if (!files || files.length === 0) {
    value.value = null;
    return;
  }
  if (props.multiple) {
    value.value = Array.from(files);
  } else {
    value.value = files[0];
  }
};

const clearValue = () => {
  value.value = null;
  if (fileInputRef.value) {
    fileInputRef.value.value = "";
  }
};

const hasError = computed(() => Boolean(props.errorMessage));

const fieldClass = computed(() =>
  fieldWrapperClasses({
    density: "compact",
    disabled: props.disabled,
    variant: props.variant,
    hasError: hasError.value
  })
);

const displayValue = computed(() => {
  if (!value.value) return props.placeholder || "No file chosen";
  if (Array.isArray(value.value)) {
    return value.value.map((f) => f.name).join(", ");
  }
  return value.value.name;
});
</script>

<template>
  <div class="flex gap-2 sm:gap-3">
    <div :class="labelWidthClass" class="flex items-center pl-0 pt-10px pb-10px">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" />
    </div>

    <div :class="inputWidthClass" class="flex items-center pr-0 pt-10px pb-10px">
      <div class="flex flex-col gap-1 w-full">
        <div :class="fieldClass">
          <label class="flex-1 min-w-0 cursor-pointer flex items-center" :class="[{'text-white': value, 'text-white/40': !value}, props.variant === 'outline' ? 'py-2' : 'py-1']">
            <input
              ref="fileInput"
              type="file"
              :accept="accept"
              :multiple="multiple"
              :disabled="disabled"
              class="hidden"
              @change="handleChange"
            />
            <span class="truncate">{{ displayValue }}</span>
          </label>
          <button
            v-if="clearable && value"
            type="button"
            class="inline-flex items-center justify-center text-white/60 transition hover:text-white px-2"
            aria-label="Clear"
            @click.prevent="clearValue"
          >
            <IconClose class="size-4" aria-hidden="true" />
          </button>
        </div>
        <p v-if="hasError" class="text-xs text-pv-error">
          {{ errorMessage }}
        </p>
      </div>
    </div>
  </div>
</template>
