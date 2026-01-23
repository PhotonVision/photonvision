<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";

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

// Debounce function
function debounce(func: (...args: any[]) => void, wait: number) {
  let timeout: ReturnType<typeof setTimeout>;
  return function (...args: any[]) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
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
</script>

<template>
  <div class="d-flex">
    <v-col :cols="12 - sliderCols" class="pl-0 pt-10px pb-10px d-flex align-center">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col :cols="sliderCols - 1" class="pl-0 pt-10px pb-10px">
      <v-slider
        v-model="localValue"
        class="align-center"
        :max="max"
        :min="min"
        hide-details
        color="primary"
        :disabled="disabled"
        :step="step"
        append-icon="mdi-menu-right"
        prepend-icon="mdi-menu-left"
        @click:append="localValue += step"
        @click:prepend="localValue -= step"
      />
    </v-col>
    <v-col :cols="1" class="pr-0 pt-10px pb-10px">
      <v-text-field
        :model-value="localValue"
        color="primary"
        :max="max"
        :min="min"
        :disabled="disabled"
        class="mt-0 pt-0"
        density="compact"
        hide-details
        single-line
        type="number"
        variant="underlined"
        style="width: 100%"
        :step="step"
        :hide-spin-buttons="true"
        @keyup.enter="localValue = $event.target.value"
        @blur="localValue = $event.target.value"
      />
    </v-col>
  </div>
</template>
