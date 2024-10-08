<script setup lang="ts">
import MetricsCard from "@/components/settings/MetricsCard.vue";
import DeviceControlCard from "@/components/settings/DeviceControlCard.vue";
import LightingControlCard from "@/components/settings/LEDControlCard.vue";
import ApriltagControlCard from "@/components/settings/ApriltagControlCard.vue";
import { useServerStore } from "@/stores/ServerStore";
import NetworkSettingsCard from "@/components/settings/NetworkSettingsCard.vue";
import MiscSettingsCard from "@/components/settings/MiscSettingsCard.vue";
</script>

<template>
  <div class="w-100 pa-3">
    <MetricsCard class="mb-3" />
    <DeviceControlCard class="mb-3" />
    <NetworkSettingsCard class="mb-3" />
    <MiscSettingsCard class="mb-3" />
    <LightingControlCard v-if="useServerStore().settings?.lighting.supported" class="mb-3" />
    <ApriltagControlCard />
  </div>
</template>
