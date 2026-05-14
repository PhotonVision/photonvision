<script setup lang="ts">
import {
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuPortal,
  DropdownMenuRoot,
  DropdownMenuTrigger
} from "reka-ui";
import PvIcon from "@/components/common/pv-icon.vue";

export interface DropdownMenuAction {
  /** MDI icon name, e.g. "mdi-pencil" */
  icon: string;
  /** Tooltip / accessible label */
  label: string;
  /** Icon color (resolved via pv-icon) */
  color?: string;
  /** Disable the item */
  disabled?: boolean;
}

defineProps<{
  /** List of action items to show in the menu */
  items: DropdownMenuAction[];
}>();

const emit = defineEmits<{
  (e: "select", index: number): void;
}>();
</script>

<template>
  <DropdownMenuRoot>
    <DropdownMenuTrigger as-child>
      <slot name="trigger" />
    </DropdownMenuTrigger>

    <DropdownMenuPortal defer>
      <DropdownMenuContent :side-offset="7" align="start" position-strategy="fixed" 
        class="z-[2500] min-w-[160px] overflow-hidden rounded-xl border border-white/12 bg-pv-surface p-1 text-white shadow-2xl shadow-black/45 ring-1 ring-white/8">
        <DropdownMenuItem v-for="(item, index) in items" :key="index" :disabled="item.disabled"
          class="relative flex min-h-9 cursor-default items-center gap-2.5 rounded-lg px-3 py-1.5 text-sm outline-none data-[disabled]:pointer-events-none data-[disabled]:opacity-35 data-[highlighted]:bg-pv-primary/20"
          @select="emit('select', index)">
          <pv-icon :color="item.color ?? '#c5c5c5'" :icon="item.icon" />
          <span class="whitespace-nowrap">{{ item.label }}</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenuPortal>
  </DropdownMenuRoot>
</template>
