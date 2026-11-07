<script setup lang="ts">
import {
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuPortal,
  DropdownMenuRoot,
  DropdownMenuTrigger
} from "reka-ui";
import type { Component } from "vue";
import { popoverSurfaceClass } from "../../../lib/ComponentUtils";

export interface DropdownMenuAction {
  /** Icon component imported from unplugin-icons */
  icon: Component;
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
      <DropdownMenuContent
        :side-offset="7"
        align="start"
        position-strategy="fixed"
        :class="[popoverSurfaceClass, 'min-w-40 p-1']"
      >
        <DropdownMenuItem
          v-for="(item, index) in items"
          :key="index"
          :disabled="item.disabled"
          class="data-highlighted:bg-pv-primary/20 relative flex min-h-9 cursor-default items-center gap-2.5 rounded-lg px-3 py-1.5 text-sm outline-none data-disabled:pointer-events-none data-disabled:opacity-35"
          @select="emit('select', index)"
        >
          <pv-icon :color="item.color ?? '#c5c5c5'" :icon="item.icon" />
          <span class="whitespace-nowrap">{{ item.label }}</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenuPortal>
  </DropdownMenuRoot>
</template>
