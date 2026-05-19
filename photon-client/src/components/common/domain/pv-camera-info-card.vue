<script setup lang="ts">
import { PVCameraInfo } from "@/types/SettingTypes";
import { cameraInfoFor } from "@/lib/PhotonUtils";

const { camera } = defineProps<{ camera: PVCameraInfo }>();
</script>

<template>
  <div>
    <pv-table density="compact" :style="{ backgroundColor: 'var(--color-pv-primary)' }">
      <tbody>
        <tr v-if="'dev' in camera && camera.dev !== null">
          <td>Device Number:</td>
          <td>{{ camera.dev }}</td>
        </tr>
        <tr v-if="'name' in camera && camera.name !== null">
          <td>Name:</td>
          <td>{{ camera.name }}</td>
        </tr>
        <tr>
          <td>Type:</td>
          <td v-if="camera.type === 'PVUsbCameraInfo'" class="mb-3">USB Camera</td>
          <td v-else-if="camera.type === 'PVCSICameraInfo'" class="mb-3">CSI Camera</td>
          <td v-else-if="camera.type === 'PVFileCameraInfo'" class="mb-3">File Camera</td>
          <td v-else>Unidentified Camera Type</td>
        </tr>
        <tr v-if="'baseName' in camera && camera.baseName !== null">
          <td>Base Name:</td>
          <td>{{ camera.baseName }}</td>
        </tr>
        <tr v-if="'vendorId' in camera && camera.vendorId !== null">
          <td>Vendor ID:</td>
          <td>{{ camera.vendorId }}</td>
        </tr>
        <tr v-if="'productId' in camera && camera.productId !== null">
          <td>Product ID:</td>
          <td>{{ camera.productId }}</td>
        </tr>
        <tr v-if="'path' in camera && camera.path !== null">
          <td>Path:</td>
          <td style="word-break: break-all">{{ camera.path }}</td>
        </tr>
        <tr v-if="'uniquePath' in camera && camera.uniquePath !== null">
          <td>Unique Path:</td>
          <td style="word-break: break-all">{{ camera.uniquePath }}</td>
        </tr>
        <tr v-if="'otherPaths' in camera && camera.otherPaths !== null">
          <td>Other Paths:</td>
          <td>{{ camera.otherPaths }}</td>
        </tr>
      </tbody>
    </pv-table>
  </div>
</template>
