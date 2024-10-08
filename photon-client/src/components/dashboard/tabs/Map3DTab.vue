<script setup lang="ts">
import { computed } from "vue";
import type { TagTrackedTarget } from "@/types/PhotonTrackingTypes";
import Photon3dVisualizer from "@/components/app/photon-3d-visualizer.vue";
import { useClientStore } from "@/stores/ClientStore";
import { CameraConfig } from "@/types/SettingTypes";

const clientStore = useClientStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
}>();

const trackedTargets = computed<TagTrackedTarget[]>(() => (clientStore.pipelineResultsFromCameraIndex(props.cameraSettings.cameraIndex)?.targets || []) as TagTrackedTarget[]);
</script>

<template>
  <div class="pt-4">
    <photon3d-visualizer :targets="trackedTargets" />
  </div>
</template>
