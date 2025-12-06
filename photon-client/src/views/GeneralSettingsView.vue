<script setup lang="ts">
import MetricsCard from "@/components/settings/MetricsCard.vue";
import DeviceControlCard from "@/components/settings/DeviceControlCard.vue";
import ObjectDetectionCard from "@/components/settings/ObjectDetectionCard.vue";
import GlobalSettingsCard from "@/components/settings/GlobalSettingsCard.vue";
import LightingControlCard from "@/components/settings/LEDControlCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import ApriltagControlCard from "@/components/settings/ApriltagControlCard.vue";
</script>

<template>
  <div class="pa-3">
    <MetricsCard />
    <DeviceControlCard />
    <GlobalSettingsCard />
    <ObjectDetectionCard v-if="useSettingsStore().general.supportedBackends.length > 0" />
    <LightingControlCard v-if="useSettingsStore().lighting.supported" />
    <Suspense>
      <!-- Allows us to import three js when it's actually needed  -->
      <ApriltagControlCard />

      <template #fallback> Loading... </template>
    </Suspense>
  </div>
</template>
