<script setup lang="ts">
import { TabsList, TabsRoot, TabsTrigger,TabsIndicator } from "reka-ui";

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
    <tabs-list class="grid min-h-12 w-full auto-cols-fr grid-flow-col rounded-2xl border border-white/10 bg-black/15 p-1 relative">
      <tabs-indicator class="absolute w-(--reka-tabs-indicator-size) left-0  translate-x-(--reka-tabs-indicator-position)  transition-[translate,transform] duration-300 h-full bottom-0 flex items-center">
        <div class="h-10 w-full rounded-xl bg-pv-primary shadow-[inset_0_1px_0_rgba(255,255,255,0.35)]" />
      </tabs-indicator>
      <tabs-trigger
        v-for="(item, index) in items"
        :key="item.value"
        :value="index"
        class="inline-flex min-h-10 items-center justify-center rounded-xl px-3 py-2 text-sm font-semibold text-white/78 outline-none transition hover:text-white focus-visible:ring-2 focus-visible:ring-pv-primary/50  data-[state=active]:text-slate-950  relative"
      >
        {{ item.label }}
      </tabs-trigger>
    </tabs-list>
  </tabs-root>
</template>
