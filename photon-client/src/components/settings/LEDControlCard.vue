<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
</script>

<template>
  <pv-card class="mb-3">
    <div class="pb-2 text-base font-semibold">LED Control</div>
    <div class="pt-1">
      <pv-slider
        v-model="useSettingsStore().lighting.brightness"
        label="Brightness"
        :min="0"
        :max="100"
        @update:modelValue="(args) => useSettingsStore().changeLEDBrightness(args)"
      />
    </div>
  </pv-card>
</template>
