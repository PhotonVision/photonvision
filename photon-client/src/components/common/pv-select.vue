<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

export interface SelectItem {
  name: string | number;
  value: string | number;
  disabled?: boolean;
}
const value = defineModel<string | number | undefined>({ required: true });

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    selectCols?: number;
    disabled?: boolean;
    items: string[] | number[] | SelectItem[];
  }>(),
  {
    selectCols: 9,
    disabled: false
  }
);

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
  <div class="d-flex">
    <v-col :cols="12 - selectCols" class="d-flex align-center pl-0 pt-10px pb-10px">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col :cols="selectCols" class="d-flex align-center pr-0 pt-10px pb-10px">
      <v-select
        v-model="value"
        :items="items"
        item-title="name"
        item-value="value"
        item-props.disabled="disabled"
        :disabled="disabled"
        hide-details="auto"
        variant="underlined"
        density="compact"
      />
    </v-col>
  </div>
</template>
<style scoped>
.v-select {
  padding-top: 0px;
  margin-top: 0px;
}
</style>
