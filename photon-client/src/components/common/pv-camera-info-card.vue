<script setup lang="ts">
import { PVCameraInfo } from "@/types/SettingTypes";

const { camera } = defineProps({
  camera: {
    type: PVCameraInfo,
    required: true
  }
});

const cameraInfoFor = (camera: PVCameraInfo) => {
  if (camera.PVUsbCameraInfo) {
    return camera.PVUsbCameraInfo;
  }
  if (camera.PVCSICameraInfo) {
    return camera.PVCSICameraInfo;
  }
  if (camera.PVFileCameraInfo) {
    return camera.PVFileCameraInfo;
  }
  return {};
};
</script>

<template>
  <v-card dark class="camera-info-card pa-4 mb-4 mr-3">
    <v-card-title v-if="camera.PVUsbCameraInfo"> USB Camera Info </v-card-title>
    <v-card-title v-if="camera.PVCSICameraInfo"> CSI Camera Info </v-card-title>
    <v-card-title v-if="camera.PVFileCameraInfo"> File Camera Info </v-card-title>

    <v-card-text>
      <v-simple-table class="camera-info-card-table mt-2">
        <tbody>
          <!-- Quick debugging hack - we can make this pretty later-->
          <tr v-for="(value, key) in cameraInfoFor(camera)" :key="key">
            <td>{{ key }}</td>
            <td>{{ value }}</td>
          </tr>
        </tbody>
      </v-simple-table>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.camera-info-card {
  background-color: #30ad25 !important;
}

.camera-info-card-table {
  background-color: #7b368e !important;
}
</style>
