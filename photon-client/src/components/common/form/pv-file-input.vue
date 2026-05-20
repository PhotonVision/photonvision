<script setup lang="ts">
import { colWidthClass, fieldWrapperClasses } from "../lib";
import { computed, useTemplateRef } from "vue";
import { Label } from "reka-ui";
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
    <div :class="labelWidthClass" class="pt-3 pb-3 flex items-center pl-0">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" />
    </div>

    <div :class="inputWidthClass" class="pt-3 pb-3 flex items-center pr-0">
      <div class="flex w-full flex-col gap-1">
        <div :class="fieldClass">
          <Label
            class="flex min-w-0 flex-1 cursor-pointer items-center"
            :class="[{ 'text-pv-on-surface': value, 'text-pv-on-surface/40': !value }, props.variant === 'outline' ? 'py-2' : 'py-1']"
          >
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
          </Label>
          <button
            v-if="clearable && value"
            type="button"
            class="inline-flex items-center justify-center px-2 text-pv-on-surface/60 transition hover:text-pv-on-surface"
            aria-label="Clear"
            @click.prevent="clearValue"
          >
            <IconClose class="size-4" aria-hidden="true" />
          </button>
        </div>
        <p v-if="hasError" class="text-pv-error text-xs">
          {{ errorMessage }}
        </p>
      </div>
    </div>
  </div>
</template>
