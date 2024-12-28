<script setup lang="ts">
import { PVCameraInfo } from "@/types/SettingTypes";

const { saved, matched } = defineProps({
  saved: {
    type: PVCameraInfo,
    required: true
  },
  matched: {
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
  <div class="pa-3">
    <h3 v-if="saved.PVUsbCameraInfo" class="mb-3">USB Camera Info</h3>
    <h3 v-if="saved.PVCSICameraInfo" class="mb-3">CSI Camera Info</h3>
    <h3 v-if="saved.PVFileCameraInfo" class="mb-3">File Camera Info</h3>

    <v-simple-table dense :style="{ backgroundColor: 'var(--v-primary-base)' }">
      <tbody>
        <tr>
          <th></th>
          <th>Saved</th>
          <th>Matched</th>
        </tr>
        <tr v-if="cameraInfoFor(saved).dev !== undefined && cameraInfoFor(saved).dev !== null">
          <td>Device Number:</td>
          <td>{{ cameraInfoFor(saved).dev }}</td>
          <td>{{ cameraInfoFor(matched).dev }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).name !== undefined && cameraInfoFor(saved).name !== null">
          <td>Name:</td>
          <td>{{ cameraInfoFor(saved).name }}</td>
          <td>{{ cameraInfoFor(matched).name }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).baseName !== undefined && cameraInfoFor(saved).baseName !== null">
          <td>Base Name:</td>
          <td>{{ cameraInfoFor(saved).baseName }}</td>
          <td>{{ cameraInfoFor(matched).baseName }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).vendorId !== undefined && cameraInfoFor(saved).vendorId !== null">
          <td>Vendor ID:</td>
          <td>{{ cameraInfoFor(saved).vendorId }}</td>
          <td>{{ cameraInfoFor(matched).vendorId }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).productId !== undefined && cameraInfoFor(saved).productId !== null">
          <td>Product ID:</td>
          <td>{{ cameraInfoFor(saved).productId }}</td>
          <td>{{ cameraInfoFor(matched).productId }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).path !== undefined && cameraInfoFor(saved).path !== null">
          <td>Path:</td>
          <td>{{ cameraInfoFor(saved).path }}</td>
          <td>{{ cameraInfoFor(matched).path }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).otherPaths !== undefined && cameraInfoFor(saved).otherPaths !== null">
          <td>Other Paths:</td>
          <td>{{ cameraInfoFor(saved).otherPaths }}</td>
          <td>{{ cameraInfoFor(matched).otherPaths }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).uniquePath !== undefined && cameraInfoFor(saved).uniquePath !== null">
          <td>Unique Path:</td>
          <td>{{ cameraInfoFor(saved).uniquePath }}</td>
          <td>{{ cameraInfoFor(matched).uniquePath }}</td>
        </tr>
      </tbody>
    </v-simple-table>
  </div>
</template>
