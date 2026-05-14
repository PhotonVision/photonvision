<script setup lang="ts">
import PvButton from "@/components/common/pv-button.vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";
import { SliderRange, SliderRoot, SliderThumb, SliderTrack } from "reka-ui";

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
  }>(),
  { step: 1, disabled: false, sliderCols: 8 }
);
const emit = defineEmits<{ (e: "update:modelValue", value: number): void }>();
const labelWidth = computed(() => `${((12 - props.sliderCols) / 12) * 100}%`);

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
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div class="flex min-w-0 items-center gap-3 sm:flex-1">
      <pv-button
        size="icon"
        variant="passive"
        icon="mdi-menu-left"
        :disabled="disabled"
        @click="stepValue(-1)"
      />
      <slider-root
        v-model="sliderModel"
        class="relative flex h-10 w-full touch-none select-none items-center"
        :min="min"
        :max="max"
        :step="step"
        :disabled="disabled"
      >
        <slider-track class="relative h-2 w-full rounded-full bg-white/12 data-disabled:opacity-50 pv-slider-track">
          <slider-range class="absolute h-full rounded-full bg-pv-primary pv-slider-range" />
        </slider-track>
        <slider-thumb
          class="block size-5 rounded-full border-2 border-pv-primary bg-white shadow-md outline-none transition focus-visible:ring-2 focus-visible:ring-pv-primary/50 disabled:pointer-events-none data-disabled:opacity-50 pv-slider-thumb"
        />
      </slider-root>
      <pv-button
        size="icon"
        variant="passive"
        icon="mdi-menu-right"
        :disabled="disabled"
        @click="stepValue(1)"
      />
      <input
        :value="localValue"
        :max="max"
        :min="min"
        :disabled="disabled"
        class="h-10 w-20 shrink-0 rounded-xl border border-white/12 bg-black/15 pl-3 pr-1 text-sm text-white outline-none transition focus:border-pv-primary disabled:cursor-not-allowed disabled:opacity-45 text-left"
        type="number"
        :step="step"
        @keyup.enter="updateFromInput(($event.target as HTMLInputElement).value)"
        @blur="updateFromInput(($event.target as HTMLInputElement).value)"
      />
    </div>
  </div>
</template>
