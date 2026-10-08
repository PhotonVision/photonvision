<script setup lang="ts">
import { TabsList, TabsRoot, TabsTrigger, TabsIndicator } from "reka-ui";

export interface PvTabItem<TValue extends string> {
  label: string;
  value: TValue;
}

const model = defineModel<number>({ required: true });

withDefaults(
  defineProps<{
    items: PvTabItem<string>[];
  }>(),
  {}
);
</script>

<template>
  <tabs-root v-model="model">
    <tabs-list
      class="relative grid min-h-12 w-full auto-cols-fr grid-flow-col rounded-2xl border border-white/10 bg-black/15 p-1"
    >
      <tabs-indicator
        class="absolute bottom-0 left-0 flex h-full w-(--reka-tabs-indicator-size) translate-x-(--reka-tabs-indicator-position) items-center transition-[translate,transform] duration-300"
      >
        <div class="bg-pv-primary h-10 w-full rounded-xl shadow-[inset_0_1px_0_rgba(255,255,255,0.35)]" />
      </tabs-indicator>
      <tabs-trigger
        v-for="(item, index) in items"
        :key="item.value"
        :value="index"
        class="focus-visible:ring-pv-primary/50 text-pv-on-surface/78 hover:text-pv-on-surface relative inline-flex min-h-10 items-center justify-center rounded-xl px-3 py-2 text-sm font-semibold transition outline-none focus-visible:ring-2 data-[state=active]:text-slate-950"
      >
        {{ item.label }}
      </tabs-trigger>
    </tabs-list>
  </tabs-root>
</template>
