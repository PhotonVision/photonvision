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
  <div>
    <h3 v-if="camera.PVUsbCameraInfo">USB Camera Info</h3>
    <h3 v-if="camera.PVCSICameraInfo">CSI Camera Info</h3>
    <h3 v-if="camera.PVFileCameraInfo">File Camera Info</h3>

    <v-simple-table dense class="mt-2 pb-8" :style="{ backgroundColor: 'var(--v-primary-base)' }">
      <tbody>
        <tr v-if="cameraInfoFor(camera).dev !== undefined && cameraInfoFor(camera).dev !== null">
          <td>Device Number:</td>
          <td>{{ cameraInfoFor(camera).dev }}</td>
        </tr>
        <tr v-if="cameraInfoFor(camera).name !== undefined && cameraInfoFor(camera).name !== null">
          <td>Name:</td>
          <td>{{ cameraInfoFor(camera).name }}</td>
        </tr>
        <tr v-if="cameraInfoFor(camera).baseName !== undefined && cameraInfoFor(camera).baseName !== null">
          <td>Base Name:</td>
          <td>{{ cameraInfoFor(camera).baseName }}</td>
        </tr>
        <tr v-if="cameraInfoFor(camera).vendorId !== undefined && cameraInfoFor(camera).vendorId !== null">
          <td>Vendor ID:</td>
          <td>{{ cameraInfoFor(camera).vendorId }}</td>
        </tr>
        <tr v-if="cameraInfoFor(camera).productId !== undefined && cameraInfoFor(camera).productId !== null">
          <td>Product ID:</td>
          <td>{{ cameraInfoFor(camera).productId }}</td>
        </tr>
        <tr v-if="cameraInfoFor(camera).path !== undefined && cameraInfoFor(camera).path !== null">
          <td>Path:</td>
          <td>{{ cameraInfoFor(camera).path }}</td>
        </tr>
        <tr v-if="cameraInfoFor(camera).otherPaths !== undefined && cameraInfoFor(camera).otherPaths !== null">
          <td>Other Paths:</td>
          <td>{{ cameraInfoFor(camera).otherPaths }}</td>
        </tr>
        <tr v-if="cameraInfoFor(camera).uniquePath !== undefined && cameraInfoFor(camera).uniquePath !== null">
          <td>Unique Path:</td>
          <td>{{ cameraInfoFor(camera).uniquePath }}</td>
        </tr>
      </tbody>
    </v-simple-table>
  </div>
</template>
