<script setup lang="ts">
import { computed } from "vue";
import type { Component } from "vue";
import { ToggleGroupItem, ToggleGroupRoot } from "reka-ui";

export interface ToggleItem<TValue extends string> {
  label: string;
  value: TValue;
  icon?: Component;
  disabled?: boolean;
}

const model = defineModel<string[] | string>({ required: true });

withDefaults(
  defineProps<{
    items: ToggleItem<string>[];
    multiple?: boolean;
    disabled?: boolean;
  }>(),
  {
    multiple: false,
    disabled: false
  }
);

const normalizedModel = computed({
  get: () => model.value,
  set: (value) => {
    model.value = value;
  }
});
</script>

<template>
  <toggle-group-root
    v-model="normalizedModel"
    :type="multiple ? 'multiple' : 'single'"
    :disabled="disabled"
    orientation="horizontal"
    class="grid sm:grid-cols-2"
  >
    <toggle-group-item
      v-for="item in items"
      :key="item.value"
      :value="item.value"
      :disabled="disabled || item.disabled"
      class="focus-visible:ring-pv-primary/50 data-[state=on]:border-pv-button-active data-[state=on]:bg-pv-button-active inline-flex min-h-11 w-full items-center justify-center gap-2 border border-white/12 bg-black/15 px-4 py-2 text-sm font-semibold text-pv-on-surface/88 shadow-sm transition outline-none first:rounded-t-xl last:rounded-b-xl hover:bg-white/6 focus-visible:ring-2 disabled:cursor-not-allowed disabled:opacity-45 data-[state=on]:z-10 data-[state=on]:text-slate-950 sm:first:rounded-l-xl sm:first:rounded-tr-none sm:last:rounded-r-xl sm:last:rounded-bl-none"
    >
      <component :is="item.icon" v-if="item.icon" class="mode-btn-icon size-5" aria-hidden="true" />
      <span class="mode-btn-label">{{ item.label }}</span>
    </toggle-group-item>
  </toggle-group-root>
</template>

<style scoped>
@media only screen and (max-width: 351px) {
  .mode-btn-icon {
    margin: 0 !important;
  }

  .mode-btn-label {
    display: none;
  }
}
</style>
