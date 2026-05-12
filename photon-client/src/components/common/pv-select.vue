<script setup lang="ts" generic="T extends string | number">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import {
  SelectContent,
  SelectIcon,
  SelectItem as SelectItemPrimitive,
  SelectItemIndicator,
  SelectItemText,
  SelectPortal,
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectViewport
} from "reka-ui";

export interface SelectItem<TValue extends string | number> {
  name: string | number;
  value: TValue;
  disabled?: boolean;
}

type SelectItems = SelectItem<T>[] | ReadonlyArray<T>;
const value = defineModel<T>({ required: true });

const displayValue = computed(() => {
  const selectedItem = items.value.find((item) => item.value === value.value);
  return selectedItem ? selectedItem.name : "";
});

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
const labelWidth = computed(() => `${((12 - props.selectCols) / 12) * 100}%`);
const selectWidth = computed(() => `${(props.selectCols / 12) * 100}%`);

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

const placeholder = computed(() => (props.label ? `Select ${props.label}` : "Select an option"));
</script>

<template>
  <div class="flex flex-col gap-2 py-2 sm:flex-row sm:items-center sm:gap-3">
    <div class="sm:shrink-0" :style="{ flexBasis: labelWidth }">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div class="min-w-0 sm:flex-1" :style="{ flexBasis: selectWidth }">
      <select-root v-model="value" :disabled="disabled">
        <select-trigger
          class="flex h-10 w-full items-center justify-between gap-3 rounded-xl border border-white/12 bg-black/15 px-3 text-left text-sm text-white shadow-sm outline-none transition data-[placeholder]:text-white/45 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <!-- This allows us to work around Reka #2160-->
          <select-value :data-slot="value != null ? 'value' : 'placeholder'">
            <slot :model-value="modelValue">
              {{ displayValue ?? placeholder ?? "&nbsp;" }}
            </slot>
          </select-value>

          <select-icon class="shrink-0 text-white/70">
            <span class="mdi mdi-chevron-down text-lg leading-none" aria-hidden="true"></span>
          </select-icon>
        </select-trigger>
        <select-portal defer>
          <select-content
            position="popper"
            position-strategy="fixed"
            side="bottom"
            align="start"
            :side-offset="8"
            :collision-padding="12"
            class="z-[2500] overflow-hidden rounded-xl border border-white/12 bg-pv-surface text-white shadow-2xl shadow-black/45 ring-1 ring-white/8"
            :style="{ width: 'var(--reka-select-trigger-width)' }"
          >
            <select-viewport class="max-h-72 p-1">
              <select-item-primitive
                v-for="item in items"
                :key="item.value"
                :value="item.value"
                :disabled="item.disabled"
                :text-value="String(item.name)"
                class="relative flex min-h-9 cursor-default items-center rounded-lg py-2 pr-8 pl-3 text-sm outline-none transition data-[disabled]:pointer-events-none data-[disabled]:opacity-35 data-[highlighted]:bg-pv-primary/20 data-[highlighted]:text-pv-primary data-[state=checked]:text-pv-primary"
              >
                <select-item-text>{{ item.name }}</select-item-text>
                <select-item-indicator class="absolute right-3 inline-flex items-center justify-center">
                  <span class="mdi mdi-check text-sm leading-none" aria-hidden="true"></span>
                </select-item-indicator>
              </select-item-primitive>
            </select-viewport>
          </select-content>
        </select-portal>
      </select-root>
    </div>
  </div>
</template>
