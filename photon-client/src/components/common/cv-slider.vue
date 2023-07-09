<script setup lang="ts">
import {computed, defineEmits, defineProps} from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

const props = withDefaults(defineProps<{
  label?: string,
  tooltip?: string,
  modelValue: number,
  min: number,
  max: number,
  step?: number
  disabled?: boolean,
  sliderCols?: number,
}>(), {
  step: 1,
  disabled: false,
  sliderCols: 8
});

const emit = defineEmits(["input"]);

const localValue = computed({
  get: () => props.modelValue,
  set: v => emit("input", v)
});
</script>

<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="12 - sliderCols">
        <tooltipped-label
          :tooltip="tooltip"
          :label="label"
        />
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
          <template v-slot:append>
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
