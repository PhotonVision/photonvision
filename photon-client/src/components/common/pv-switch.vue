<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";

const value = defineModel<boolean>();
const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
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
</script>

<template>
  <div class="d-flex">
    <v-col :cols="12 - switchCols || labelCols" class="d-flex align-center pl-0">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col :cols="switchCols || 12 - labelCols" class="d-flex align-center pr-0">
      <v-switch v-model="value" :disabled="disabled" color="#ffd843" hide-details="auto" class="pb-1" />
    </v-col>
  </div>
</template>
<style scoped>
.v-input--selection-controls {
  margin-top: 0px;
}
</style>
