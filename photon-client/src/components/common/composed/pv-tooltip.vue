<script setup lang="ts">
import { TooltipArrow, TooltipContent, TooltipPortal, TooltipProvider, TooltipRoot, TooltipTrigger } from "reka-ui";

withDefaults(
  defineProps<{
    text?: string;
    location?: "top" | "bottom" | "left" | "right";
    delay?: number;
  }>(),
  {
    location: "right",
    delay: 300
  }
);
</script>

<template>
  <tooltip-provider :delay-duration="delay">
    <tooltip-root>
      <tooltip-trigger as-child>
        <slot />
      </tooltip-trigger>
      <tooltip-portal>
        <tooltip-content
          :side="location"
          :side-offset="8"
          class="z-[2500] max-w-xs rounded-lg bg-pv-background px-3 py-2 text-xs leading-relaxed text-white shadow-xl shadow-black/35 ring-1 ring-white/8"
        >
          <slot name="content">{{ text }}</slot>
          <tooltip-arrow class="fill-pv-background stroke-white/8 stroke-2" :width="10" :height="5" />
        </tooltip-content>
      </tooltip-portal>
    </tooltip-root>
  </tooltip-provider>
</template>
