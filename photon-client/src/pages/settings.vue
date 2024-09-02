<script setup lang="ts">
import MetricsCard from "@/components/settings/MetricsCard.vue";
import DeviceControlCard from "@/components/settings/DeviceControlCard.vue";
import GeneralSettingsControlCard from "@/components/settings/GeneralSettingsControlCard.vue";
import LightingControlCard from "@/components/settings/LEDControlCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import ApriltagControlCard from "@/components/settings/ApriltagControlCard.vue";
</script>

<template>
  <div class="w-100 pa-3">
    <MetricsCard class="mb-3" />
    <DeviceControlCard class="mb-3" />
    <GeneralSettingsControlCard class="mb-3" />
    <LightingControlCard v-if="useSettingsStore().lighting.supported" class="mb-3" />
    <ApriltagControlCard />
  </div>
</template>
