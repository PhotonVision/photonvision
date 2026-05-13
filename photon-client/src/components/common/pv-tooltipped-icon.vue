<script setup lang="ts">
import PvIcon from "@/components/common/pv-icon.vue";
import { TooltipArrow, TooltipContent, TooltipPortal, TooltipProvider, TooltipRoot, TooltipTrigger } from "reka-ui";

withDefaults(
  defineProps<{
    iconName: string;
    disabled?: boolean;
    color?: string;
    tooltip?: string;
    right?: boolean;
    hover?: boolean;
  }>(),
  {
    right: false,
    disabled: false,
    hover: false
  }
);

defineEmits<{
  (e: "click"): void;
}>();
</script>

<template>
  <div>
    <tooltip-provider :delay-duration="300">
      <tooltip-root>
        <tooltip-trigger as-child>
          <pv-icon
            :class="hover ? 'hover' : ''"
            :icon="iconName"
            :color="color"
            :disabled="disabled"
            @click="$emit('click')"
          />
        </tooltip-trigger>
        <tooltip-portal>
          <tooltip-content
            :side="right ? 'right' : 'left'"
            :side-offset="8"
            class="z-[2500] max-w-xs rounded-lg bg-pv-background px-3 py-2 text-xs leading-relaxed text-white shadow-xl shadow-black/35 ring-1 ring-white/8"
          >
            {{ tooltip }}
            <tooltip-arrow class="fill-pv-background stroke-white/8 stroke-2" :width="10" :height="5" />
          </tooltip-content>
        </tooltip-portal>
      </tooltip-root>
    </tooltip-provider>

  </div>
</template>

<style scoped>
.hover:hover {
  color: white !important;
}
</style>
