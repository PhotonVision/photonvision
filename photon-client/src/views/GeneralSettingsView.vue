<script setup lang="ts">
import LEDControlCard from "@/components/settings/LEDControlCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
</script>

<template>
  <div class="p-3">
    <DeviceCard />
    <GlobalSettingsCard />
    <ObjectDetectionCard v-if="useSettingsStore().general.supportedBackends.length > 0" />
    <LEDControlCard v-if="useSettingsStore().lighting.supported" />
    <Suspense>
      <!-- Allows us to import three js when it's actually needed  -->
      <ApriltagControlCard />
      <template #fallback> Loading... </template>
    </Suspense>
  </div>
</template>
