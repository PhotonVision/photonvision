<script setup lang="ts">
import PvIcon from "@/components/common/pv-icon.vue";

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
    <v-tooltip :right="right" :location="!right ? 'bottom' : undefined" offset="10" :disabled="tooltip === undefined">
      <template #activator="{ props }">
        <pv-icon
          :class="hover ? 'hover' : ''"
          :icon="iconName"
          :color="color"
          v-bind="props"
          :disabled="disabled"
          @click="$emit('click')"
        />
      </template>
      <span>{{ tooltip }}</span>
    </v-tooltip>
  </div>
</template>

<style scoped>
.hover:hover {
  color: white !important;
}
</style>
