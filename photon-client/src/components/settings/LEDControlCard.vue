<script setup lang="ts">
import PvNumberSlider from "@/components/common/pv-number-slider.vue";

import { useServerStore } from "@/stores/ServerStore";

const serverStore = useServerStore();
</script>

<template>
  <v-card v-if="serverStore.settings?.lighting">
    <v-card-title class="mb-3 mt-2">LED Control</v-card-title>
    <pv-number-slider
      v-model="serverStore.settings.lighting.brightness"
      class="pt-2 pl-4 pr-4 pb-3"
      label="Brightness"
      :label-cols="4"
      :max="100"
      :min="0"
      @update:model-value="(args) => serverStore.updateLEDBrightness(args)"
    />
  </v-card>
</template>
