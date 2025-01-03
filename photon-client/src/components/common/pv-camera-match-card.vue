<script setup lang="ts">
import { PVCameraInfo } from "@/types/SettingTypes";

const { saved, current } = defineProps({
  saved: {
    type: PVCameraInfo,
    required: true
  },
  current: {
    type: PVCameraInfo,
    required: true
  }
});

const cameraInfoFor = (camera: PVCameraInfo): any => {
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
    <v-simple-table dense :style="{ backgroundColor: 'var(--v-primary-base)' }">
      <tbody>
        <tr>
          <th></th>
          <th>Saved</th>
          <th>Current</th>
        </tr>
        <tr v-if="cameraInfoFor(saved).dev !== undefined && cameraInfoFor(saved).dev !== null">
          <td>Device Number:</td>
          <td>{{ cameraInfoFor(saved).dev }}</td>
          <td>{{ cameraInfoFor(current).dev }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).name !== undefined && cameraInfoFor(saved).name !== null">
          <td>Name:</td>
          <td>{{ cameraInfoFor(saved).name }}</td>
          <td>{{ cameraInfoFor(current).name }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).baseName !== undefined && cameraInfoFor(saved).baseName !== null">
          <td>Base Name:</td>
          <td>{{ cameraInfoFor(saved).baseName }}</td>
          <td>{{ cameraInfoFor(current).baseName }}</td>
        </tr>
        <tr>
          <td>Type:</td>
          <td v-if="saved.PVUsbCameraInfo" class="mb-3">USB Camera</td>
          <td v-else-if="saved.PVCSICameraInfo" class="mb-3">CSI Camera</td>
          <td v-else-if="saved.PVFileCameraInfo" class="mb-3">File Camera</td>
          <td v-else>Unidentified Camera Type</td>
          <td v-if="current.PVUsbCameraInfo" class="mb-3">USB Camera</td>
          <td v-else-if="current.PVCSICameraInfo" class="mb-3">CSI Camera</td>
          <td v-else-if="current.PVFileCameraInfo" class="mb-3">File Camera</td>
          <td v-else>Unidentified Camera Type</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).vendorId !== undefined && cameraInfoFor(saved).vendorId !== null">
          <td>Vendor ID:</td>
          <td>{{ cameraInfoFor(saved).vendorId }}</td>
          <td>{{ cameraInfoFor(current).vendorId }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).productId !== undefined && cameraInfoFor(saved).productId !== null">
          <td>Product ID:</td>
          <td>{{ cameraInfoFor(saved).productId }}</td>
          <td>{{ cameraInfoFor(current).productId }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).path !== undefined && cameraInfoFor(saved).path !== null">
          <td>Path:</td>
          <td style="word-break: break-all">{{ cameraInfoFor(saved).path }}</td>
          <td style="word-break: break-all">{{ cameraInfoFor(current).path }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).otherPaths !== undefined && cameraInfoFor(saved).otherPaths !== null">
          <td>Other Paths:</td>
          <td>{{ cameraInfoFor(saved).otherPaths }}</td>
          <td>{{ cameraInfoFor(current).otherPaths }}</td>
        </tr>
        <tr v-if="cameraInfoFor(saved).uniquePath !== undefined && cameraInfoFor(saved).uniquePath !== null">
          <td>Unique Path:</td>
          <td style="word-break: break-all">{{ cameraInfoFor(saved).uniquePath }}</td>
          <td style="word-break: break-all">{{ cameraInfoFor(current).uniquePath }}</td>
        </tr>
      </tbody>
    </v-simple-table>
  </div>
</template>
