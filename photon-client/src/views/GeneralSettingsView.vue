<script setup lang="ts">
import MetricsCard from "@/components/settings/MetricsCard.vue";
import DeviceControlCard from "@/components/settings/DeviceControlCard.vue";
import ObjectDetectionCard from "@/components/settings/ObjectDetectionCard.vue";
import NetworkingCard from "@/components/settings/NetworkingCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import ApriltagControlCard from "@/components/settings/ApriltagControlCard.vue";
</script>

<template>
  <div class="pa-3">
    <MetricsCard />
    <DeviceControlCard />
    <NetworkingCard />
    <ObjectDetectionCard v-if="useSettingsStore().general.supportedBackends.length > 0" />
    <Suspense>
      <!-- Allows us to import three js when it's actually needed  -->
      <ApriltagControlCard />

      <template #fallback> Loading... </template>
    </Suspense>
  </div>
</template>
