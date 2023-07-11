<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

const props = withDefaults(defineProps<{
  label?: string,
  tooltip?: string,
  // TODO fully update v-model usage in custom components on Vue3 update
  value: [number, number],
  min: number,
  max: number,
  step?: number
  disabled?: boolean,
  inverted?: boolean,
}>(), {
  step: 1,
  disabled: false,
  inverted: false
});

const emit = defineEmits<{
  (e: "input", value: [number, number]): void
}>();

const localValue = computed({
  get: () => props.value,
  set: v => emit("input", v)
});
</script>

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
