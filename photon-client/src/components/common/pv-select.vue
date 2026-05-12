<script setup lang="ts" generic="T extends string | number">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

export interface SelectItem<TValue extends string | number> {
  name: string | number;
  value: TValue;
  disabled?: boolean;
  chip?: { text: string; color?: string };
}

type SelectItems = SelectItem<T>[] | ReadonlyArray<T>;
const value = defineModel<T>({ required: true });

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

const areSelectItems = (items: SelectItems): items is SelectItem<T>[] => typeof items[0] === "object";

// Computed in case items changes
const items = computed<SelectItem<T>[]>(() => {
  // Trivial case for empty list; we have no data
  if (!props.items.length) {
    return [];
  }

  if (areSelectItems(props.items)) {
    return props.items;
  }

  return props.items.map((item) => ({ name: item, value: item }));
});

// Helper kept in script so the generic `<T>` never appears inside the template — Prettier's
// HTML parser misreads `<T>` as a tag and refuses to format the file.
const chipFor = (raw: unknown) => (raw as SelectItem<T>).chip;
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
        item-props
        :disabled="disabled"
        hide-details="auto"
        variant="underlined"
        density="compact"
      >
        <template #selection="{ item }">
          <span>{{ item.raw.name }}</span>
          <v-chip
            v-if="chipFor(item.raw)"
            class="ml-2"
            size="x-small"
            :color="chipFor(item.raw)?.color ?? 'info'"
          >
            {{ chipFor(item.raw)?.text }}
          </v-chip>
        </template>
      </v-select>
    </v-col>
  </div>
</template>
<style scoped>
.v-select {
  padding-top: 0px;
  margin-top: 0px;
}
</style>
