<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";
const value = defineModel<number>({ required: true });
withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    min: number;
    max: number;
    step?: number;
    disabled?: boolean;
    sliderCols?: number;
  }>(),
  {
    step: 1,
    disabled: false,
    sliderCols: 8
  }
);

// Debounce function
function debounce(func: (...args: any[]) => void, wait: number) {
  let timeout: ReturnType<typeof setTimeout>;
  return function (...args: any[]) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
  };
}


const localValue = computed({
  get: () => value.value,
  set: (v) => debounce(() => value.value = parseFloat(v as unknown as string), 20)
});
</script>

<template>
  <div class="d-flex">
    <v-col :cols="12 - sliderCols" class="pl-0 d-flex align-center">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col :cols="sliderCols - 1">
      <v-slider
        v-model="value"
        class="align-center"
        :max="max"
        :min="min"
        hide-details
        color="accent"
        :disabled="disabled"
        :step="step"
        append-icon="mdi-menu-right"
        prepend-icon="mdi-menu-left"
        @click:append="value += step"
        @click:prepend="value -= step"
      />
    </v-col>
    <v-col :cols="1" class="pr-0">
      <v-text-field
        :model-value="value"
        color="accent"
        :max="max"
        :min="min"
        :disabled="disabled"
        class="mt-0 pt-0"
        density="compact"
        hide-details
        single-line
        type="number"
        style="width: 100%"
        :step="step"
        :hide-spin-buttons="true"
        @keyup.enter="value = $event.target.value"
        @blur="value = $event.target.value"
      />
    </v-col>
  </div>
</template>
