<script setup lang="ts">
import { computed } from "vue";
import { ToggleGroupItem, ToggleGroupRoot } from "reka-ui";

export interface ToggleItem<TValue extends string> {
  label: string;
  value: TValue;
  icon?: string;
  disabled?: boolean;
}

const model = defineModel<string[] | string>({ required: true });

const props = withDefaults(
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
      class="inline-flex min-h-11 w-full items-center justify-center gap-2 border border-white/12 bg-black/15 px-4 py-2 text-sm font-semibold text-white/88 shadow-sm outline-none transition hover:bg-white/6 focus-visible:ring-2 focus-visible:ring-pv-primary/50 data-on:z-10 data-[state=on]:border-pv-button-active data-on:bg-pv-button-active data-[state=on]:text-slate-950   first:rounded-t-xl last:rounded-b-xl sm:first:rounded-l-xl sm:last:rounded-r-xl sm:first:rounded-tr-none sm:last:rounded-bl-none disabled:cursor-not-allowed disabled:opacity-45"
    >
      <span v-if="item.icon" :class="['mdi mode-btn-icon text-lg leading-none', item.icon]" aria-hidden="true"></span>
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
