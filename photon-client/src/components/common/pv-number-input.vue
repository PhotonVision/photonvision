<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    // TODO fully update v-model usage in custom components on Vue3 update
    value: number;
    disabled?: boolean;
    labelCols?: number;
    rules?: ((v: number) => boolean | string)[];
    step?: number;
  }>(),
  {
    disabled: false,
    labelCols: 2,
    step: 1
  }
);

const emit = defineEmits<{
  (e: "input", value: number): void;
}>();

const localValue = computed({
  get: () => props.value,
  set: (v) => emit("input", parseFloat(v as unknown as string))
});
</script>

<template>
  <div class="d-flex">
    <v-col :cols="labelCols" class="d-flex pl-0 align-center">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col class="pr-0">
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
  </div>
</template>
