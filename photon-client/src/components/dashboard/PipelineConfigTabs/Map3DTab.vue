<script setup lang="ts">
import { computed } from "vue";
import type { PhotonTarget } from "@/types/PhotonTrackingTypes";
import { useStateStore } from "@/stores/StateStore";
import Photon3dVue from "@/components/app/photon-3d-visualizer.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";

// TODO the VideoFormat interface already has the horizontal FOV prop from the backend, is this calculation needed?
const horizontalFOV = computed<number>(() => {
  const currentResolution = useCameraSettingsStore().currentVideoFormat.resolution;
  const currentFOV = useCameraSettingsStore().currentCameraSettings.fov.value;

  const diagonalView = currentFOV * (Math.PI / 180.0);
  const diagonalAspect = Math.hypot(currentResolution.width, currentResolution.height);

  return Math.atan(Math.tan(diagonalView / 2) * (currentResolution.width / diagonalAspect)) * 2 * (180 / Math.PI);

});

const trackedTargets = computed<PhotonTarget[]>(() => useStateStore().pipelineResults?.targets || []);
</script>

<template>
  <div>
    <v-row style="width: 100%">
      <v-col
      >
        <span class="white--text">Target Visualization</span>
      </v-col>
    </v-row>
    <v-row style="width: 100%">
      <v-col style="display: flex; align-items: center; justify-content: center">
        <photon3d-vue :horizontal-f-o-v="horizontalFOV" :targets="trackedTargets"/>
      </v-col>
    </v-row>
  </div>
</template>
