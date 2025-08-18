<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";
const value = defineModel<number>({
  required: true
});
withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
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

const localValue = computed({
  get: () => value.value,
  set: (v) => (value.value = parseFloat(v as unknown as string))
});
</script>

<template>
  <div class="d-flex">
    <v-col :cols="labelCols" class="d-flex pl-0 pt-10px pb-10px align-center">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col class="pr-0 pt-10px pb-10px">
      <v-text-field
        v-model="localValue"
        class="mt-0 pt-0"
        density="compact"
        hide-details
        single-line
        color="primary"
        type="number"
        variant="underlined"
        style="width: 70px"
        :step="step"
        :disabled="disabled"
        :rules="rules"
      />
    </v-col>
  </div>
</template>
