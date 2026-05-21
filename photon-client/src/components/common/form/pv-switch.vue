<script setup lang="ts">
import { useColFlexBasis } from "../lib";
import { SwitchRoot, SwitchThumb } from "reka-ui";
import { computed, useId } from "vue";

const value = defineModel<boolean>();
const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    disabled?: boolean;
    switchCols?: number;
    id?: string;
  }>(),
  { disabled: false, switchCols: 8 }
);

const uniqueId = useId();
const switchId = computed(() => props.id || uniqueId);
const { labelWidth, contentWidth: switchWidth } = useColFlexBasis(() => props.switchCols);
</script>

<template>
  <div class="flex flex-col gap-2 py-1.5 sm:flex-row sm:items-center sm:gap-3">
    <div class="sm:shrink-0" :style="{ flexBasis: labelWidth }">
      <pv-tooltipped-label :tooltip="tooltip" :label="label" :target-id="switchId" />
    </div>
    <div class="flex sm:flex-1 sm:justify-start" :style="{ flexBasis: switchWidth }">
      <switch-root
        :id="switchId"
        v-model="value"
        :disabled="disabled"
        class="peer data-[state=checked]:bg-pv-primary focus-within:ring-pv-primary inline-flex h-7 w-12 items-center rounded-full border border-white/15 bg-black/25 px-0.5 shadow-inner transition outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-offset-black/20 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <switch-thumb
          class="block size-5 rounded-full bg-white shadow-md transition-transform duration-150 data-[state=checked]:translate-x-5 data-[state=unchecked]:translate-x-0"
        />
      </switch-root>
    </div>
  </div>
</template>
