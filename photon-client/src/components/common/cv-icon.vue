<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    iconName: string;
    color?: string;
    tooltip?: string;
    right?: boolean;
    hover?: boolean;
  }>(),
  {
    right: false,
    hover: false
  }
);

defineEmits<{
  (e: "click"): void;
}>();

const hoverClass = props.hover ? "hover" : "";
</script>

<template>
  <div>
    <v-tooltip :right="right" :bottom="!right" nudge-right="10" :disabled="tooltip === undefined">
      <template #activator="{ on, attrs }">
        <v-icon :class="hoverClass" :color="color" v-bind="attrs" v-on="on" @click="$emit('click')">
          {{ iconName }}
        </v-icon>
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
