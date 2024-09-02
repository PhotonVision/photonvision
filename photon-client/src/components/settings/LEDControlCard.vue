<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
</script>

<template>
  <v-card>
    <v-card-title class="mb-3 mt-2">LED Control</v-card-title>
    <pv-number-slider
      v-model="useSettingsStore().lighting.brightness"
      class="pt-2 pl-4 pr-4 pb-3"
      label="Brightness"
      :label-cols="4"
      :max="100"
      :min="0"
      @update:model-value="(args) => useSettingsStore().changeLEDBrightness(args)"
    />
  </v-card>
</template>
