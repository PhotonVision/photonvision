<script setup lang="ts">
import PvInputLayout from "@/components/common/pv-input-layout.vue";
import { onMounted, ref, watch } from "vue";

const model = defineModel<[number, number]>({ required: true });

const props = withDefaults(
  defineProps<{
    label: string;
    tooltip?: string;
    labelCols?: number;
    disabled?: boolean;
    range?: boolean;
    showNumberInput?: boolean;
    min: number;
    max: number;
    step?: number;
    trackColor?: string;
    trackFillColor?: string;
    trackThumbColor?: string;
    trackSize?: number;
    flipDirection?: boolean;
  }>(),
  {
    labelCols: 4,
    disabled: false,
    range: false,
    showNumberInput: true,
    step: 1,
    trackColor: undefined,
    trackFillColor: "accent",
    trackThumbColor: "accent",
    trackSize: 2,
    flipDirection: false
  }
);

const rangeSliderRef = ref(null);

const leftThumbPosition = ref();
const rightThumbPosition = ref();
const trackFillBgColor = ref();

const updatePositions = () => {
  const nativeFillBar = rangeSliderRef.value.$el.getElementsByClassName("v-slider-track__fill").item(0);
  const styleData = window.getComputedStyle(nativeFillBar);
  leftThumbPosition.value = styleData.insetInlineStart;
  rightThumbPosition.value = `calc(${styleData.insetInlineStart} + ${styleData.width})`;
};

onMounted(() => {
  updatePositions();
  const sliderTrackContainer: Element = rangeSliderRef.value.$el.getElementsByClassName("v-slider-track").item(0);
  const originalFillBar = sliderTrackContainer.getElementsByClassName("v-slider-track__fill").item(0);
  trackFillBgColor.value = window.getComputedStyle(originalFillBar).backgroundColor;

  const leftFillBar = document.createElement("div");
  const rightFillBar = document.createElement("div");

  leftFillBar.classList.add("v-slider-track__fill", "flip-div");
  rightFillBar.classList.add("v-slider-track__fill", "flip-div");

  sliderTrackContainer.appendChild(leftFillBar);
  sliderTrackContainer.appendChild(rightFillBar);
});

watch(
  () => props.trackFillColor,
  (newValue: string) => {
    trackFillBgColor.value = newValue;
  }
);
</script>

<template>
  <pv-input-layout :label="label" :label-cols="labelCols" :tooltip="tooltip" tooltip-location="right">
    <v-range-slider
      ref="rangeSliderRef"
      v-model="model"
      :class="`ml-0 mr-0 ${flipDirection ? 'flipped' : ''}`"
      :disabled="disabled"
      hide-details
      :max="max"
      :min="min"
      :step="step"
      strict
      :style="{
        '--left-thumb-position': leftThumbPosition,
        '--right-thumb-position': rightThumbPosition,
        '--track-fill-bg-color': trackFillBgColor
      }"
      :thumb-color="trackThumbColor"
      :track-color="trackColor"
      :track-fill-color="trackFillColor"
      :track-size="trackSize"
      @update:model-value="updatePositions"
    >
      <template #prepend>
        <v-text-field
          v-model="(model || [])[0]"
          base-color="accent"
          class="pb-1"
          density="compact"
          :disabled="disabled"
          hide-details
          :max="(model || [])[1]"
          :min="min"
          :step="step"
          style="width: 80px"
          type="number"
        />
      </template>
      <template #append>
        <v-text-field
          v-model="(model || [])[1]"
          base-color="accent"
          class="pb-1"
          density="compact"
          :disabled="disabled"
          hide-details
          :max="max"
          :min="(model || [])[0]"
          :step="step"
          style="width: 80px"
          type="number"
        />
      </template>
    </v-range-slider>
  </pv-input-layout>
</template>

<style scoped>
* >>> .flip-div {
  background-color: var(--track-fill-bg-color);
  display: none;
}
* >>> .flip-div:nth-last-child(2) {
  inset-inline-start: 0;
  width: var(--left-thumb-position);
}
* >>> .flip-div:nth-last-child(1) {
  inset-inline-start: var(--right-thumb-position);
  width: calc(100% - var(--right-thumb-position));
}

.flipped >>> .flip-div {
  display: block !important;
}
.flipped >>> .v-slider-track__fill:not(.flip-div) {
  display: none !important;
}
</style>
