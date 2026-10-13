<script setup lang="ts">
import { computed, useAttrs } from "vue";

defineOptions({
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    bordered?: boolean;
  }>(),
  {
    bordered: true
  }
);

const attrs = useAttrs();

const baseClass = "text-white rounded-xl p-4 bg-pv-surface";

const borderClass = computed(() => (props.bordered ? "border border-white/10" : ""));
</script>

<template>
  <div :class="[baseClass, borderClass, attrs.class]">
    <slot />
  </div>
</template>
