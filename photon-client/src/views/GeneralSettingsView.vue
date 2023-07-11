<script setup lang="ts">
import MetricsCard from "@/components/settings/MetricsCard.vue";
import DeviceControlCard from "@/components/settings/DeviceControlCard.vue";
import NetworkingCard from "@/components/settings/NetworkingCard.vue";
import LightingControlCard from "@/components/settings/LightingControlCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
</script>

<template>
  <v-row
      class="pa-3"
      no-gutters
  >
    <v-col
        cols="12"
        style="max-width: 1400px"
    >
      <MetricsCard/>
      <DeviceControlCard/>
      <NetworkingCard/>
      <LightingControlCard v-if="useSettingsStore().lighting.supported"/>
    </v-col>
  </v-row>
</template>
