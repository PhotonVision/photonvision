<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

interface SelectItem {
  name: string | number;
  value: string | number;
  disabled?: boolean;
}

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    selectCols?: number;
    // TODO fully update v-model usage in custom components on Vue3 update
    value: number;
    disabled?: boolean;
    items: string[] | number[] | SelectItem[];
  }>(),
  {
    selectCols: 9,
    disabled: false
  }
);

const emit = defineEmits<{
  (e: "input", value: number): void;
}>();

const localValue = computed({
  get: () => props.value,
  set: (v) => emit("input", v)
});

// Computed in case items changes
const items = computed<SelectItem[]>(() => {
  // Trivial case for empty list; we have no data
  if (!props.items.length) {
    return [];
  }

  // Check if the prop exists on the object to infer object type
  if ((props.items[0] as SelectItem).name) {
    return props.items as SelectItem[];
  }
  return props.items.map((v, i) => ({ name: v, value: i }));
});
</script>

<template>
  <div>
    <v-row dense align="center">
      <v-col :cols="12 - selectCols">
        <tooltipped-label :tooltip="tooltip" :label="label" />
      </v-col>
      <v-col :cols="selectCols">
        <v-select
          v-model="localValue"
          :items="items"
          item-text="name"
          item-value="value"
          item-disabled="disabled"
          dark
          color="accent"
          item-color="secondary"
          :disabled="disabled"
        />
      </v-col>
    </v-row>
  </div>
</template>
