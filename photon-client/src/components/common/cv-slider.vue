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

<script lang="ts">

import {computed, defineComponent} from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

export default defineComponent({
  emits: ["input"],
  components: {TooltippedLabel},
  props: {
    value: {type: Number, required: true},
    tooltip: {type: String, required: false},
    label: {type: String, required: false},
    min: {type: Number, required: true},
    max: {type: Number, required: true},
    step: {type: Number, required: false, default: 1},
    sliderCols: {type: Number, required: false, default: 8},
    disabled: {type: Boolean, required: false}
  },
  setup(props: {value: number}, { emit }) {
    const localValue = computed({
      get: () => props.value,
      set: v => emit("input", v)
    });

    return {
      localValue
    };
  }
});
</script>
