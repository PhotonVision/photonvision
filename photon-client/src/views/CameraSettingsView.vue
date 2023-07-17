<script setup lang="ts">
import CamerasCard from "@/components/cameras/CameraSettingsCard.vue";
import CalibrationCard from "@/components/cameras/CalibrationCard.vue";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { onMounted } from "vue";

onMounted(() => {
  // Camera View uses Driver Mode
  useCameraSettingsStore().changeCurrentPipelineIndex(-1, true);
});
</script>

<template>
  <div>
    <v-row
        no-gutters
        class="pa-3"
    >
      <v-col
          cols="12"
          md="7"
      >
        <CamerasCard/>
        <CalibrationCard/>
      </v-col>
      <v-col
          class="pl-md-3 pt-3 pt-md-0"
          cols="12"
          style="display: flex; align-items: center"
          md="5"
      >
        <photon-camera-stream
            stream-type="Processed"
            style="width: 100%"
        />
      </v-col>
    </v-row>
  </div>
</template>
