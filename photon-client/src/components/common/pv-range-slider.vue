<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
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
  }>(),
  {
    step: 1,
    disabled: false,
    inverted: false,
    sliderCols: 10
  }
);
const labelWidth = computed(() => `${((12 - props.sliderCols) / 12) * 100}%`);

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

const checkNumberRange = (v: string): boolean => {
  const val: number = parseFloat(v);
  return isFinite(val) && val >= props.min && val <= props.max;
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
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div class="flex min-w-0 items-center gap-3 sm:flex-1">
      <input
        :value="localValue[0]"
        :max="max"
        :min="min"
        :disabled="disabled"
        :step="step"
        class="h-10 w-20 shrink-0 rounded-xl border border-white/12 bg-black/15 pl-3 pr-1 text-left text-sm text-white outline-none transition focus:border-pv-primary disabled:cursor-not-allowed disabled:opacity-45"
        type="number"
        @input="(event) => changeFromSlot((event.target as HTMLInputElement).value, 0)"
      />
      <slider-root
        v-model="sliderModel"
        class="relative flex h-10 w-full touch-none select-none items-center"
        :min="min"
        :max="max"
        :step="step"
        :disabled="disabled"
        :min-steps-between-thumbs="0"
      >
        <slider-track class="relative h-2 w-full rounded-full" :class="inverted ? 'bg-pv-primary' : 'bg-white/12'">
          <slider-range class="absolute h-full rounded-full" :class="inverted ? 'bg-white/12' : 'bg-pv-primary'" />
        </slider-track>
        <slider-thumb
          class="block size-5 rounded-full border-2 border-pv-primary bg-white shadow-md outline-none transition focus-visible:ring-2 focus-visible:ring-pv-primary/50 disabled:pointer-events-none disabled:opacity-50"
        />
        <slider-thumb
          class="block size-5 rounded-full border-2 border-pv-primary bg-white shadow-md outline-none transition focus-visible:ring-2 focus-visible:ring-pv-primary/50 disabled:pointer-events-none disabled:opacity-50"
        />
      </slider-root>
      <input
        :value="localValue[1]"
        :max="max"
        :min="min"
        :disabled="disabled"
        :step="step"
        class="h-10 w-20 shrink-0 rounded-xl border border-white/12 bg-black/15 pl-3 pr-1 text-left text-sm text-white outline-none transition focus:border-pv-primary disabled:cursor-not-allowed disabled:opacity-45"
        type="number"
        @input="(event) => changeFromSlot((event.target as HTMLInputElement).value, 1)"
      />
    </div>
  </div>
</template>
