<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col cols="2">
        <tooltipped-label
          :tooltip="tooltip"
          :label="label"
        />
      </v-col>
      <v-col cols="10">
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
          <template v-slot:prepend>
            <v-text-field
              dark
              color="accent"
              v-model="localValue[0]"
              :max="max"
              :min="min"
              class="mt-0 pt-0"
              hide-details
              single-line
              type="number"
              style="width: 60px"
              :step="step"
            />
          </template>

          <template v-slot:append>
            <v-text-field
              v-model="localValue[1]"
              dark
              color="accent"
              :max="max"
              :min="min"
              class="mt-0 pt-0"
              hide-details
              single-line
              type="number"
              style="width: 60px"
              :step="step"
            />
          </template>
        </v-range-slider>
      </v-col>
    </v-row>
  </div>
</template>


<script lang="ts">
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";
import {computed} from "vue";

export default {
  emits: ["input"],
  components: {TooltippedLabel},
  props: {
    label: {type: String, required: false},
    tooltip: {type: String, required: false},
    min: {type: Number, required: true},
    max: {type: Number, required: true},
    step: {type: Number, required: false, default: 1},
    disabled: {type: Boolean, required: false},
    inverted: {type: Boolean, required: false},
    value: {type: Array, required: true, validator: (v: number[]) => v.length === 2}
  },
  setup(props: {value: [number, number]}, {emit}) {

    const localValue = computed({
      get: () => props.value,
      set: v => emit("input", v)
    });

    return {
      localValue
    };
  }
};
</script>
