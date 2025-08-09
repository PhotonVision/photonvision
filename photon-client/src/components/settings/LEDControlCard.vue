<script setup lang="ts">
import PvSlider from "@/components/common/pv-slider.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
</script>

<template>
  <v-card class="mb-3 rounded-12" color="surface">
    <v-card-title class="pb-10px">LED Control</v-card-title>
    <v-card-text>
      <pv-slider
        v-model="useSettingsStore().lighting.brightness"
        label="Brightness"
        :min="0"
        :max="100"
        @update:modelValue="(args) => useSettingsStore().changeLEDBrightness(args)"
      />
    </v-card-text>
  </v-card>
</template>
