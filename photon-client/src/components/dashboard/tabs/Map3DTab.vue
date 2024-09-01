<script setup lang="ts">
import { computed } from "vue";
import type { PhotonTarget } from "@/types/PhotonTrackingTypes";
import { useStateStore } from "@/stores/StateStore";
import Photon3dVisualizer from "@/components/app/photon-3d-visualizer.vue";

const trackedTargets = computed<PhotonTarget[]>(() => useStateStore().currentPipelineResults?.targets || []);
</script>

<template>
  <div class="pt-4">
    <photon3d-visualizer :targets="trackedTargets" />
  </div>
</template>
