<script setup lang="ts">

import { PVCameraInfo } from "@/types/SettingTypes";
import { cameraInfoFor } from "@/lib/PhotonUtils";

function isEqual<T>(a: T, b: T): boolean {
  if (a === b) {
    return true;
  }

  const bothAreObjects = a && b && typeof a === "object" && typeof b === "object";

  return (
    bothAreObjects &&
    Object.keys(a).length === Object.keys(b).length &&
    Object.entries(a).every(([k, v]) => isEqual(v, b[k as keyof T]))
  );
}

const { saved, current } = defineProps<{ saved: PVCameraInfo; current: PVCameraInfo }>();
</script>

<template>
  <div>
    <pv-table density="compact" :style="{ backgroundColor: 'var(--color-pv-primary)' }">
      <tbody>
        <tr>
          <th></th>
          <th>Saved</th>
          <th>Current</th>
        </tr>
        <tr
          v-if="'dev' in saved && 'dev' in current && saved.dev !== null"
          :class="saved.dev !== current.dev ? 'mismatch' : ''"
        >
          <td>Device Number:</td>
          <td>{{ saved.dev }}</td>
          <td>{{ current.dev }}</td>
        </tr>
        <tr v-if="saved.name !== null" :class="saved.name !== current.name ? 'mismatch' : ''">
          <td>Name:</td>
          <td>{{ saved.name }}</td>
          <td>{{ current.name }}</td>
        </tr>
        <tr
          v-if="'baseName' in saved && 'baseName' in current && saved.baseName !== null"
          :class="saved.baseName !== current.baseName ? 'mismatch' : ''"
        >
          <td>Base Name:</td>
          <td>{{ saved.baseName }}</td>
          <td>{{ current.baseName }}</td>
        </tr>
        <tr>
          <td>Type:</td>
          <td v-if="saved.type === 'PVUsbCameraInfo'" class="mb-3">USB Camera</td>
          <td v-else-if="saved.type === 'PVCSICameraInfo'" class="mb-3">CSI Camera</td>
          <td v-else-if="saved.type === 'PVFileCameraInfo'" class="mb-3">File Camera</td>
          <td v-else>Unidentified Camera Type</td>
          <td v-if="current.type === 'PVUsbCameraInfo'" class="mb-3">USB Camera</td>
          <td v-else-if="current.type === 'PVCSICameraInfo'" class="mb-3">CSI Camera</td>
          <td v-else-if="current.type === 'PVFileCameraInfo'" class="mb-3">File Camera</td>
          <td v-else>Unidentified Camera Type</td>
        </tr>
        <tr
          v-if="'vendorId' in saved && 'vendorId' in current && saved.vendorId !== null"
          :class="saved.vendorId !== current.vendorId ? 'mismatch' : ''"
        >
          <td>Vendor ID:</td>
          <td>{{ saved.vendorId }}</td>
          <td>{{ current.vendorId }}</td>
        </tr>
        <tr
          v-if="'productId' in saved && 'productId' in current && saved.productId !== null"
          :class="saved.productId !== current.productId ? 'mismatch' : ''"
        >
          <td>Product ID:</td>
          <td>{{ saved.productId }}</td>
          <td>{{ current.productId }}</td>
        </tr>
        <tr v-if="saved.path !== null" :class="saved.path !== current.path ? 'mismatch' : ''">
          <td>Path:</td>
          <td style="word-break: break-all">{{ saved.path }}</td>
          <td style="word-break: break-all">{{ current.path }}</td>
        </tr>
        <tr v-if="saved.uniquePath !== null" :class="saved.uniquePath !== current.uniquePath ? 'mismatch' : ''">
          <td>Unique Path:</td>
          <td style="word-break: break-all">{{ saved.uniquePath }}</td>
          <td style="word-break: break-all">{{ current.uniquePath }}</td>
        </tr>
        <tr
          v-if="'otherPaths' in saved && 'otherPaths' in current && saved.otherPaths !== null"
          :class="isEqual(saved.otherPaths, current.otherPaths) ? '' : 'mismatch'"
        >
          <td>Other Paths:</td>
          <td>{{ saved.otherPaths }}</td>
          <td>{{ current.otherPaths }}</td>
        </tr>
      </tbody>
    </pv-table>
  </div>
</template>

<style scoped>
.mismatch {
  background: #39a4d546 !important;
}
</style>
