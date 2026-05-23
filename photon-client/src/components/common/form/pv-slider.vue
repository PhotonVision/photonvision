<script setup lang="ts">
import { useColFlexBasis, sliderNumberInputClass, sliderThumbClass } from "../../../lib/ComponentUtils";
import { computed, useId } from "vue";
import { SliderRange, SliderRoot, SliderThumb, SliderTrack } from "reka-ui";
import IconMenuLeft from "~icons/mdi/menu-left";
import IconMenuRight from "~icons/mdi/menu-right";

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    modelValue: number;
    min: number;
    max: number;
    step?: number;
    disabled?: boolean;
    sliderCols?: number;
    id?: string;
  }>(),
  { step: 1, disabled: false, sliderCols: 8 }
);

const uniqueId = useId();
const inputId = computed(() => props.id || uniqueId);
const emit = defineEmits<{ (e: "update:modelValue", value: number): void }>();
const { labelWidth } = useColFlexBasis(() => props.sliderCols);

// Debounce function
function debounce(func: (...args: number[]) => void, wait: number) {
  let timeout: ReturnType<typeof setTimeout>;
  return function (...args: number[]) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}

const debouncedEmit = debounce((v: number) => {
  if (v < props.min) {
    emit("update:modelValue", props.min);
  } else if (v > props.max) {
    emit("update:modelValue", props.max);
  } else {
    emit("update:modelValue", v);
  }
}, 20);

const localValue = computed({
  get: () => props.modelValue,
  set: (v) => debouncedEmit(parseFloat(v as unknown as string))
});

const sliderModel = computed<number[]>({
  get: () => [props.modelValue],
  set: (v) => {
    const nextValue = v[0];
    if (nextValue === undefined) return;
    debouncedEmit(nextValue);
  }
});

const clampValue = (value: number) => {
  if (value < props.min) return props.min;
  if (value > props.max) return props.max;
  return value;
};

const stepValue = (direction: -1 | 1) => {
  const nextValue = clampValue(props.modelValue + props.step * direction);
  emit("update:modelValue", nextValue);
};

const updateFromInput = (rawValue: string) => {
  debouncedEmit(parseFloat(rawValue));
};
</script>

<template>
  <div class="flex flex-col gap-2 py-2 sm:flex-row sm:items-center sm:gap-3">
    <div class="sm:shrink-0" :style="{ flexBasis: labelWidth }">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" :target-id="inputId" />
    </div>
    <div class="flex min-w-0 items-center gap-3 sm:flex-1">
      <pv-button
        size="icon"
        variant="passive"
        :icon="IconMenuLeft"
        :disabled="disabled"
        :aria-label="`Decrease ${label} value`"
        :aria-controls="uniqueId"
        @click="stepValue(-1)"
      />
      <slider-root
        v-model="sliderModel"
        class="relative flex h-10 w-full touch-none items-center select-none"
        :min="min"
        :max="max"
        :step="step"
        :disabled="disabled"
        :aria-controls="inputId"
      >
        <slider-track class="pv-slider-track relative h-2 w-full rounded-full bg-white/12 data-disabled:opacity-50">
          <slider-range class="bg-pv-primary pv-slider-range absolute h-full rounded-full" />
        </slider-track>
        <slider-thumb :class="sliderThumbClass" :aria-label="label" />
      </slider-root>
      <pv-button
        size="icon"
        variant="passive"
        :icon="IconMenuRight"
        :disabled="disabled"
        :aria-label="`Increase ${label} value`"
        :aria-controls="uniqueId"
        @click="stepValue(1)"
      />
      <input
        :id="inputId"
        :value="localValue"
        :max="max"
        :min="min"
        :disabled="disabled"
        :class="sliderNumberInputClass"
        type="number"
        :step="step"
        :aria-label="`${label} value`"
        @keyup.enter="updateFromInput(($event.target as HTMLInputElement).value)"
        @blur="updateFromInput(($event.target as HTMLInputElement).value)"
      />
    </div>
  </div>
</template>
