<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    // TODO fully update v-model usage in custom components on Vue3 update
    // value: [number, number] | WebsocketNumberPair, // Vue doesnt like Union types for the value prop for some reason.
    value: [number, number];
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

const emit = defineEmits<{
  (e: "input", value: [number, number]): void;
}>();

const localValue = computed<[number, number]>({
  get: (): [number, number] => {
    return Object.values(props.value) as [number, number];
  },
  set: (v) => {
    for (let i = 0; i < v.length; i++) {
      v[i] = parseFloat(v[i] as unknown as string);
    }
    emit("input", v);
  }
});

const changeFromSlot = (v: number, i: number) => {
  // localValue.value must be replaced for a reactive change to take place
  const temp = localValue.value;
  temp[i] = v;
  localValue.value = temp;
};
</script>

<template>
  <div>
    <v-row dense align="center">
      <v-col :cols="12 - sliderCols">
        <tooltipped-label :tooltip="tooltip" :label="label" />
      </v-col>
      <v-col :cols="sliderCols">
        <v-range-slider
          v-model="localValue"
          :max="max"
          :min="min"
          :disabled="disabled"
          hide-details
          class="align-center"
          dark
          :color="inverted ? 'rgba(255, 255, 255, 0.2)' : 'accent'"
          :track-color="inverted ? 'accent' : undefined"
          thumb-color="accent"
          :step="step"
        >
          <template #prepend>
            <v-text-field
              :value="localValue[0]"
              dark
              color="accent"
              class="mt-0 pt-0"
              hide-details
              single-line
              :max="max"
              :min="min"
              :step="step"
              type="number"
              style="width: 60px"
              @input="(v) => changeFromSlot(v, 0)"
            />
          </template>
          <template #append>
            <v-text-field
              :value="localValue[1]"
              dark
              color="accent"
              class="mt-0 pt-0"
              hide-details
              single-line
              :max="max"
              :min="min"
              :step="step"
              type="number"
              style="width: 60px"
              @input="(v) => changeFromSlot(v, 1)"
            />
          </template>
        </v-range-slider>
      </v-col>
    </v-row>
  </div>
</template>
