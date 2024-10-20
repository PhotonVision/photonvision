<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    // TODO fully update v-model usage in custom components on Vue3 update
    value: number;
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

const emit = defineEmits<{
  (e: "input", value: number): void;
}>();

// Debounce function
function debounce(func: (...args: any[]) => void, wait: number) {
  let timeout: ReturnType<typeof setTimeout>;
  return function(...args: any[]) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
  };
}

const debouncedEmit = debounce((v: number) => {
  emit("input", v);
  console.log("debouncedEmit", v);
}, 20);

const localValue = computed({
  get: () => props.value,
  set: (v) => debouncedEmit(parseFloat(v as unknown as string))
});
</script>

<template>
  <div>
    <v-row dense align="center">
      <v-col :cols="12 - sliderCols">
        <tooltipped-label :tooltip="tooltip" :label="label" />
      </v-col>
      <v-col :cols="sliderCols">
        <v-slider
          v-model="localValue"
          dark
          class="align-center"
          :max="max"
          :min="min"
          hide-details
          color="accent"
          :disabled="disabled"
          :step="step"
        >
          <template #append>
            <v-text-field
              v-model="localValue"
              dark
              color="accent"
              :max="max"
              :min="min"
              :disabled="disabled"
              class="mt-0 pt-0"
              hide-details
              single-line
              type="number"
              style="width: 50px"
              :step="step"
            />
          </template>
        </v-slider>
      </v-col>
    </v-row>
  </div>
</template>