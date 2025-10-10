<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
const value = defineModel<number>({
  required: true
});

withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    disabled?: boolean;
    inputCols?: number;
    list: string[];
  }>(),
  {
    disabled: false,
    inputCols: 8
  }
);
</script>

<template>
  <div class="d-flex">
    <v-col :cols="12 - inputCols" class="d-flex align-center pl-0 pt-10px pb-10px">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col :cols="inputCols" class="pr-0 pt-10px pb-10px">
      <v-radio-group v-model="value" row:mandatory="true" inline hide-details="auto">
        <v-radio
          v-for="(radioName, index) in list"
          :key="index"
          :value="index"
          color="rgb(var(--v-theme-primary))"
          :label="radioName"
          :model-value="index"
          :disabled="disabled"
        />
      </v-radio-group>
    </v-col>
  </div>
</template>
