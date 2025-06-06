<script setup lang="ts">
import PvSlider from "@/components/common/pv-slider.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
</script>

<template>
  <v-card class="mb-3" style="background-color: #006492">
      <v-card-title class="pa-6 pb-3">LED Control</v-card-title>
      <pv-slider
      v-model="useSettingsStore().lighting.brightness"
      label="Brightness"
      class="pa-6 pt-0"
      :min="0"
      :max="100"
      @update:modelValue="(args) => useSettingsStore().changeLEDBrightness(args)"
      />
  </v-card>
</template>
