<script lang="ts">
export interface SelectItem<TValue extends string | number = string | number> {
  name: string | number;
  value: TValue;
  disabled?: boolean;
}
</script>

<script setup lang="ts" generic="T extends string | number = string | number">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

type PrimitiveItem = string | number;
type SelectItems = ReadonlyArray<SelectItem> | ReadonlyArray<PrimitiveItem>;
const value = defineModel<T | number | undefined>({ required: true });

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    selectCols?: number;
    disabled?: boolean;
    items: SelectItems;
  }>(),
  {
    selectCols: 9,
    disabled: false
  }
);

const areSelectItems = (items: SelectItems): items is ReadonlyArray<SelectItem> => {
  const firstItem = items[0];
  return typeof firstItem === "object" && firstItem !== null;
};

// Computed in case items changes
const items = computed<SelectItem[]>(() => {
  // Trivial case for empty list; we have no data
  if (!props.items.length) {
    return [];
  }

  if (areSelectItems(props.items)) {
    return [...props.items];
  }

  return props.items.map((item, i) => ({ name: item, value: i }));
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
