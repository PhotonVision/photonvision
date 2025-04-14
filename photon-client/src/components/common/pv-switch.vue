<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    // TODO fully update v-model usage in custom components on Vue3 update
    value: boolean;
    disabled?: boolean;
    labelCols?: number;
    switchCols?: number;
    dense?: boolean;
  }>(),
  {
    disabled: false,
    labelCols: 2,
    switchCols: 8,
    dense: false
  }
);

const emit = defineEmits<{
  (e: "input", value: boolean): void;
}>();

const localValue = computed({
  get: () => props.value,
  set: (v) => emit("input", v)
});
</script>

<template>
  <div class="d-flex">
    <v-col :cols="12 - switchCols || labelCols" class="d-flex align-center pl-0">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col :cols="switchCols || 12 - labelCols" class="d-flex align-center pr-0">
      <v-switch v-model="localValue" dark :disabled="disabled" color="#ffd843" hide-details="auto" class="pb-1" />
    </v-col>
  </div>
</template>
<style scoped>
.v-input--selection-controls {
  margin-top: 0px;
}
</style>
