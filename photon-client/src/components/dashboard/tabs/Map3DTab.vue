<script setup lang="ts">
import { computed } from "vue";
import type { PhotonTarget } from "@/types/PhotonTrackingTypes";
import { useStateStore } from "@/stores/StateStore";
import Photon3dVisualizer from "@/components/app/photon-3d-visualizer.vue";

const trackedTargets = computed<PhotonTarget[]>(() => useStateStore().currentPipelineResults?.targets || []);
</script>

<template>
  <div>
    <v-row style="width: 100%">
      <v-col>
        <span class="white--text">Target Visualization</span>
      </v-col>
    </v-row>
    <v-row style="width: 100%">
      <v-col style="display: flex; align-items: center; justify-content: center">
        <photon3d-visualizer :targets="trackedTargets" />
      </v-col>
    </v-row>
  </div>
</template>
