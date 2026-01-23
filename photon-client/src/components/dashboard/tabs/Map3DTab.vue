<script setup lang="ts">
import { computed } from "vue";
import type { PhotonTarget } from "@/types/PhotonTrackingTypes";
import { useStateStore } from "@/stores/StateStore";
import Photon3dVisualizer from "@/components/app/photon-3d-visualizer.vue";

const trackedTargets = computed<PhotonTarget[]>(() => useStateStore().currentPipelineResults?.targets || []);
</script>

<template>
  <div>
    <Suspense>
      <!-- Allows us to import three js when it's actually needed  -->
      <photon3d-visualizer :targets="trackedTargets" />

      <template #fallback> Loading... </template>
    </Suspense>
  </div>
</template>
