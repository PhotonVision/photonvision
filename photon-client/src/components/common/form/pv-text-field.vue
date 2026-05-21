<script setup lang="ts">
import { computed, useAttrs, useId } from "vue";
import type { Component } from "vue";
import IconClose from "~icons/mdi/close";
import { fieldWrapperClasses } from "../lib";

defineOptions({
  inheritAttrs: false
});

type RuleFn = (value: string | number | null) => boolean | string;

const value = defineModel<string | number | null>({ required: true });

const props = withDefaults(
  defineProps<{
    label?: string;
    placeholder?: string;
    type?: string;
    disabled?: boolean;
    errorMessage?: string;
    rules?: RuleFn[];
    hideDetails?: boolean;
    clearable?: boolean;
    prependIcon?: Component;
    step?: number;
    min?: number;
    max?: number;
    inputClass?: string;
    inputStyle?: string | Record<string, string>;
    rawValue?: boolean;
    variant?: "underlined" | "outline";
    id?: string;
  }>(),
  {
    type: "text",
    disabled: false,
    hideDetails: false,
    clearable: false,
    rawValue: false,
    variant: "underlined"
  }
);

const uniqueId = useId();
const inputId = computed(() => props.id || uniqueId);
const attrs = useAttrs();

const validationMessage = computed(() => {
  if (props.errorMessage) return props.errorMessage;
  if (!props.rules?.length) return "";

  for (const rule of props.rules) {
    const result = rule(value.value);
    if (result !== true) return typeof result === "string" ? result : "Invalid value";
  }

  return "";
});

const hasError = computed(() => Boolean(validationMessage.value));
const hasValue = computed(() => value.value !== null && value.value !== undefined && String(value.value).length > 0);

const wrapperClass = computed(() => ["flex flex-col gap-1", attrs.class] as string[]);
const wrapperStyle = computed(() => attrs.style as string);

const inputAttrs = computed(() => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { class: _class, style: _style, ...rest } = attrs;
  return rest;
});

const inputValue = computed({
  get: () => (value.value === null || value.value === undefined ? "" : String(value.value)),
  set: (val) => {
    if (props.type === "number" && !props.rawValue) {
      value.value = val === "" ? null : Number(val);
    } else {
      value.value = val;
    }
  }
});

const clearValue = () => {
  value.value = props.type === "number" && !props.rawValue ? null : "";
};

const fieldClass = computed(() =>
  fieldWrapperClasses({
    disabled: props.disabled,
    variant: props.variant,
    hasError: hasError.value
  })
);
</script>

<template>
  <div :class="wrapperClass" :style="wrapperStyle">
    <Label v-if="label" :for="inputId" class="text-pv-on-surface/70 text-xs font-medium">
      {{ label }}
    </Label>
    <div :class="fieldClass">
      <component :is="prependIcon" v-if="prependIcon" class="text-pv-on-surface/70 size-4" aria-hidden="true" />
      <input
        :id="inputId"
        v-model="inputValue"
        v-bind="inputAttrs"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :step="step"
        :min="min"
        :max="max"
        :class="[
          'text-pv-on-surface placeholder:text-pv-on-surface/40 min-w-0 flex-1 bg-transparent outline-none',
          props.variant === 'outline' ? 'py-2' : 'py-1',
          inputClass
        ]"
        :style="inputStyle"
      />
      <button
        v-if="clearable && hasValue"
        type="button"
        class="text-pv-on-surface/60 hover:text-pv-on-surface inline-flex items-center justify-center transition"
        aria-label="Clear"
        @click="clearValue"
      >
        <IconClose class="size-4" aria-hidden="true" />
      </button>
    </div>
    <p v-if="!hideDetails && hasError" class="text-pv-error text-xs">
      {{ validationMessage }}
    </p>
  </div>
</template>
