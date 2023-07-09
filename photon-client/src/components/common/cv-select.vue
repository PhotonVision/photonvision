<script setup lang="ts">
import {computed, defineEmits, defineProps} from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

interface SelectItem {
  name: string | number,
  value: string | number
}

const props = withDefaults(defineProps<{
  label?: string,
  tooltip?: string,
  selectCols?: number,
  modelValue: number,
  disabled?: boolean,
  items: string[] | number[] | SelectItem[]
}>(), {
  selectCols: 9,
  disabled: false
});

const emit = defineEmits(["input"]);

const localValue = computed({
  get: () => props.modelValue,
  set: v => emit("input", v)
});

// Computed in case items changes
const items = computed<SelectItem[]>(() => {
  // Check if the prop exists on the object to infer object type
  if((props.items[0] as SelectItem).name) {
    return props.items as SelectItem[];
  }
  return props.items.map((v, i) => ({name: v, value: i}));
});
</script>

<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="12 - selectCols">
        <tooltipped-label
          :tooltip="tooltip"
          :label="label"
        />
      </v-col>
      <v-col :cols="selectCols">
        <v-select
          v-model="localValue"
          :items="items"
          item-text="name"
          item-value="value"
          dark
          color="accent"
          item-color="secondary"
          :disabled="disabled"
        />
      </v-col>
    </v-row>
  </div>
</template>
