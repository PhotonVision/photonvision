<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";
const value = defineModel<number>({
  required: true
});

const props = withDefaults(
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

const colWidthClasses: Record<number, string> = {
  1: "w-1/12",
  2: "w-1/6",
  3: "w-1/4",
  4: "w-1/3",
  5: "w-5/12",
  6: "w-1/2",
  7: "w-7/12",
  8: "w-2/3",
  9: "w-3/4",
  10: "w-5/6",
  11: "w-11/12",
  12: "w-full"
};

const colWidthClass = (cols: number) => colWidthClasses[cols] ?? "flex-1";
const labelWidthClass = computed(() => colWidthClass(12 - props.inputCols));
const inputWidthClass = computed(() => colWidthClass(props.inputCols));
</script>

<template>
  <div class="flex">
    <div :class="labelWidthClass" class="flex items-center pl-0 pt-10px pb-10px">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div :class="inputWidthClass" class="pr-0 pt-10px pb-10px">
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
    </div>
  </div>
</template>
