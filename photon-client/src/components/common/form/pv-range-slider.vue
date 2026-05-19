<script setup lang="ts">
import { computed, useId } from "vue";
import { useColFlexBasis, sliderNumberInputClass, sliderThumbClass } from "../lib";
import type { WebsocketNumberPair } from "@/types/WebsocketDataTypes";
import { SliderRange, SliderRoot, SliderThumb, SliderTrack } from "reka-ui";

const value = defineModel<[number, number] | WebsocketNumberPair>({
  required: true
});
const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    min: number;
    max: number;
    step?: number;
    sliderCols?: number;
    disabled?: boolean;
    inverted?: boolean;
    id?: string;
  }>(),
  {
    step: 1,
    disabled: false,
    inverted: false,
    sliderCols: 10
  }
);

const id = useId();
const inputId = computed(() => props.id || id);
const { labelWidth } = useColFlexBasis(() => props.sliderCols);

const localValue = computed<[number, number]>({
  get: (): [number, number] => {
    return Object.values(value.value) as [number, number];
  },
  set: (v) => {
    for (let i = 0; i < v.length; i++) {
      v[i] = parseFloat(v[i] as unknown as string);
    }
    value.value = v;
  }
});

const changeFromSlot = (v: string, i: number) => {
  // v comes in as a string, not a number, for some reason
  // if v is undefined, take a guess and set it to 0
  const val = Math.max(props.min, Math.min(parseFloat(v) || 0, props.max));

  // localValue.value must be replaced for a reactive change to take place
  const temp = localValue.value;
  temp[i] = val;
  localValue.value = temp;
};

const sliderModel = computed<number[]>({
  get: () => [...localValue.value],
  set: (v) => {
    if (v.length !== 2) return;
    localValue.value = [v[0], v[1]];
  }
});
</script>

<template>
  <div class="flex flex-col gap-2 py-2 sm:flex-row sm:items-center sm:gap-3">
    <div class="sm:shrink-0" :style="{ flexBasis: labelWidth }">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" :for="inputId" />
    </div>
    <div class="flex min-w-0 items-center gap-3 sm:flex-1">
      <input
        :id="inputId"
        :value="localValue[0]"
        :max="max"
        :min="min"
        :disabled="disabled"
        :step="step"
        :class="sliderNumberInputClass"
        type="number"
        :aria-label="`Minimum ${label}`"
        @input="(event) => changeFromSlot((event.target as HTMLInputElement).value, 0)"
      />
      <slider-root
        v-model="sliderModel"
        class="relative flex h-10 w-full touch-none items-center select-none"
        :min="min"
        :max="max"
        :step="step"
        :disabled="disabled"
        :min-steps-between-thumbs="0"
      >
        <slider-track
          class="pv-slider-track relative h-2 w-full rounded-full"
          :class="inverted ? 'bg-pv-primary' : 'bg-white/12'"
        >
          <slider-range
            class="pv-slider-range absolute h-full rounded-full"
            :class="inverted ? 'bg-white/12' : 'bg-pv-primary'"
          />
        </slider-track>
        <slider-thumb :class="sliderThumbClass" :aria-label="`Minimum ${label}`" />
        <slider-thumb :class="sliderThumbClass" :aria-label="`Maximum ${label}`" />
      </slider-root>
      <input
        :value="localValue[1]"
        :max="max"
        :min="min"
        :disabled="disabled"
        :step="step"
        :class="sliderNumberInputClass"
        type="number"
        :aria-label="`Maximum ${label}`"
        @input="(event) => changeFromSlot((event.target as HTMLInputElement).value, 1)"
      />
    </div>
  </div>
</template>
