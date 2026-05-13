<script setup lang="ts">
import { TooltipArrow, TooltipContent, TooltipPortal, TooltipProvider, TooltipRoot, TooltipTrigger } from "reka-ui";

defineProps<{
  label?: string;
  tooltip?: string;
  icon?: string;
  location?: "top" | "bottom" | "left" | "right";
}>();
</script>

<template>
  <div class="inline-flex max-w-full items-center">
    <span v-if="!tooltip" class="inline-flex max-w-full items-center gap-2 text-sm font-medium text-white">
      <span class="truncate">{{ label }}</span>
      <span v-if="icon" :class="['mdi text-pv-primary text-sm leading-none', icon]" aria-hidden="true"></span>
    </span>

    <tooltip-provider v-else :delay-duration="300">
      <tooltip-root>
        <tooltip-trigger as-child>
          <button
            type="button"
            class="inline-flex max-w-full items-center gap-2 text-left text-sm font-medium text-white outline-none"
          >
            <span class="truncate">{{ label }}</span>
            <span v-if="icon" :class="['mdi text-pv-primary text-sm leading-none', icon]" aria-hidden="true"></span>
          </button>
        </tooltip-trigger>
        <tooltip-portal>
          <tooltip-content
            :side="location ?? 'right'"
            :side-offset="8"
            class="z-[2500] max-w-xs rounded-lg  bg-pv-background px-3 py-2 text-xs leading-relaxed text-white shadow-xl shadow-black/35 ring-1 ring-white/8"
          >
            {{ tooltip }}
            <tooltip-arrow class="fill-pv-background stroke-white/8 stroke-2" :width="10" :height="5" />
          </tooltip-content>
        </tooltip-portal>
      </tooltip-root>
    </tooltip-provider>
  </div>
</template>
