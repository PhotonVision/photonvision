<script setup lang="ts">
import { TabsList, TabsRoot, TabsTrigger } from "reka-ui";

export interface PvTabItem<TValue extends string> {
  label: string;
  value: TValue;
}

const model = defineModel<string>({ required: true });

withDefaults(
  defineProps<{
    items: PvTabItem<string>[];
  }>(),
  {}
);
</script>

<template>
  <tabs-root v-model="model">
    <tabs-list class="grid min-h-12 w-full auto-cols-fr grid-flow-col rounded-2xl border border-white/10 bg-black/15 p-1">
      <tabs-trigger
        v-for="item in items"
        :key="item.value"
        :value="item.value"
        class="inline-flex min-h-10 items-center justify-center rounded-xl px-3 py-2 text-sm font-semibold text-white/78 outline-none transition hover:text-white focus-visible:ring-2 focus-visible:ring-pv-primary/50 data-[state=active]:bg-pv-button-active data-[state=active]:text-slate-950 data-[state=active]:shadow-[inset_0_1px_0_rgba(255,255,255,0.35)]"
      >
        {{ item.label }}
      </tabs-trigger>
    </tabs-list>
  </tabs-root>
</template>
