<script setup lang="ts">
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";
import { computed, defineEmits, defineProps } from "vue";

const props = withDefaults(defineProps<{
  label?: string,
  tooltip?: string,
  value: number,
  disabled?: boolean,
  labelCols?: number,
  rules?: ((v: number) => boolean | string)[],
  step?: number
}>(), {
  disabled: false,
  labelCols: 2,
  step: 1
});

const emit = defineEmits(["input"]);

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
      <v-col :cols="labelCols">
        <tooltipped-label
          :tooltip="tooltip"
          :label="label"
        />
      </v-col>
      <v-col>
        <v-text-field
          v-model="localValue"
          dark
          class="mt-0 pt-0"
          hide-details
          single-line
          color="accent"
          type="number"
          style="width: 70px"
          :step="step"
          :disabled="disabled"
          :rules="rules"
        />
      </v-col>
    </v-row>
  </div>
</template>

