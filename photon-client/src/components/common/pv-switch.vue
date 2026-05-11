<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";
import { SwitchRoot, SwitchThumb } from "reka-ui";

const value = defineModel<boolean>();
const props = withDefaults(
  defineProps<{ label?: string; tooltip?: string; disabled?: boolean; labelCols?: number; switchCols?: number }>(),
  { disabled: false, labelCols: 2, switchCols: 8 }
);

const labelWidth = computed(() => `${((12 - props.switchCols) / 12) * 100}%`);
const switchWidth = computed(() => `${(props.switchCols / 12) * 100}%`);
</script>

<template>
  <div class="flex flex-col gap-2 py-1.5 sm:flex-row sm:items-center sm:gap-3">
    <div class="sm:shrink-0" :style="{ flexBasis: labelWidth }">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </div>
    <div class="flex sm:flex-1 sm:justify-start" :style="{ flexBasis: switchWidth }">
      <switch-root
        v-model="value"
        :disabled="disabled"
        class="peer inline-flex h-7 w-12 items-center rounded-full border border-white/15 bg-black/25 px-0.5 shadow-inner outline-none transition data-[state=checked]:bg-pv-primary disabled:cursor-not-allowed disabled:opacity-50"
      >
        <switch-thumb
          class="block size-5 rounded-full bg-white shadow-md transition-transform duration-150 data-[state=checked]:translate-x-5 data-[state=unchecked]:translate-x-0"
        />
      </switch-root>
    </div>
  </div>
</template>
