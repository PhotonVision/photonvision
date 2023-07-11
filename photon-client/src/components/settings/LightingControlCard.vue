<script setup lang="ts">
import CvSlider from "@/components/common/cv-slider.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
</script>

<template>
  <v-card
      dark
      class="mb-3 pr-6 pb-3"
      style="background-color: #006492;"
  >
    <v-card-title>Lighting Control</v-card-title>
    <div class="ml-5">
      <cv-slider
          v-model="useSettingsStore().lighting.brightness"
          label="Brightness"
          class="pt-2"
          :slider-cols="12"
          :min="0"
          :max="100"
          @input="args => useSettingsStore().changeLEDBrightness(args)"
      />
    </div>
  </v-card>
</template>
