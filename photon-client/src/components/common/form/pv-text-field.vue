<script setup lang="ts">
import { computed, useAttrs } from "vue";
import type { Component } from "vue";
import IconClose from "~icons/mdi/close";
import { fieldWrapperClasses } from "../lib";

defineOptions({
  inheritAttrs: false
});

type Density = "compact" | "comfortable" | "default";

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
    density?: Density;
    step?: number;
    min?: number;
    max?: number;
    inputClass?: string;
    inputStyle?: string | Record<string, string>;
    rawValue?: boolean;
    variant?: "underlined" | "outline";
  }>(),
  {
    type: "text",
    disabled: false,
    hideDetails: false,
    clearable: false,
    density: "compact",
    rawValue: false,
    variant: "underlined"
  }
);

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
    density: props.density,
    disabled: props.disabled,
    variant: props.variant,
    hasError: hasError.value
  })
);
</script>

<template>
  <div :class="wrapperClass" :style="wrapperStyle">
    <div v-if="label" class="text-xs font-medium text-white/70">
      {{ label }}
    </div>
    <div :class="fieldClass">
      <component :is="prependIcon" v-if="prependIcon" class="size-4 text-white/70" aria-hidden="true" />
      <input
        v-model="inputValue"
        v-bind="inputAttrs"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :step="step"
        :min="min"
        :max="max"
        :class="[
          'min-w-0 flex-1 bg-transparent text-white placeholder:text-white/40 outline-none',
          props.variant === 'outline' ? 'py-2' : 'py-1',
          inputClass
        ]"
        :style="inputStyle"
      />
      <button
        v-if="clearable && hasValue"
        type="button"
        class="inline-flex items-center justify-center text-white/60 transition hover:text-white"
        aria-label="Clear"
        @click="clearValue"
      >
        <IconClose class="size-4" aria-hidden="true" />
      </button>
    </div>
    <p v-if="!hideDetails && hasError" class="text-xs text-pv-error">
      {{ validationMessage }}
    </p>
  </div>
</template>
