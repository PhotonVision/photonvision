<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

const value = defineModel<boolean>();
withDefaults(
  defineProps<{ label?: string; tooltip?: string; disabled?: boolean; labelCols?: number; switchCols?: number }>(),
  { disabled: false, labelCols: 2, switchCols: 8 }
);
</script>

<template>
  <div class="d-flex">
    <v-col :cols="12 - switchCols || labelCols" class="d-flex align-center pl-0">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col :cols="switchCols || 12 - labelCols" class="d-flex align-center pr-0">
      <v-switch v-model="value" :disabled="disabled" color="primary" hide-details density="compact" />
    </v-col>
  </div>
</template>
<style scoped>
.v-col {
  padding-top: 6px !important;
  padding-bottom: 6px !important;
}
</style>
