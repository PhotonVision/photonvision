<script setup lang="ts">
import { PVUsbCamera, PVCSICamera, PVFileCamera, type PVCameraInfo } from "@/types/SettingTypes";

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

function field(obj: PVCameraInfo, key: string): unknown {
  return key in obj ? (obj as unknown as Record<string, unknown>)[key] : undefined;
}

const { saved, current } = defineProps<{ saved: PVCameraInfo; current: PVCameraInfo }>();
</script>

<template>
  <div>
    <pv-table>
      <tbody>
        <tr>
          <th></th>
          <th>Saved</th>
          <th>Current</th>
        </tr>
        <tr
          v-if="('dev' in saved || 'dev' in current) && field(saved, 'dev') !== null"
          :class="field(saved, 'dev') !== field(current, 'dev') ? 'mismatch' : ''"
        >
          <td>Device Number:</td>
          <td>{{ field(saved, "dev") }}</td>
          <td>{{ field(current, "dev") }}</td>
        </tr>
        <tr v-if="saved.name !== null" :class="saved.name !== current.name ? 'mismatch' : ''">
          <td>Name:</td>
          <td>{{ saved.name }}</td>
          <td>{{ current.name }}</td>
        </tr>
        <tr
          v-if="('baseName' in saved || 'baseName' in current) && field(saved, 'baseName') !== null"
          :class="field(saved, 'baseName') !== field(current, 'baseName') ? 'mismatch' : ''"
        >
          <td>Base Name:</td>
          <td>{{ field(saved, "baseName") }}</td>
          <td>{{ field(current, "baseName") }}</td>
        </tr>
        <tr>
          <td>Type:</td>
          <td v-if="saved.type === PVUsbCamera" class="mb-3">USB Camera</td>
          <td v-else-if="saved.type === PVCSICamera" class="mb-3">CSI Camera</td>
          <td v-else-if="saved.type === PVFileCamera" class="mb-3">File Camera</td>
          <td v-else>Unidentified Camera Type</td>
          <td v-if="current.type === PVUsbCamera" class="mb-3">USB Camera</td>
          <td v-else-if="current.type === PVCSICamera" class="mb-3">CSI Camera</td>
          <td v-else-if="current.type === PVFileCamera" class="mb-3">File Camera</td>
          <td v-else>Unidentified Camera Type</td>
        </tr>
        <tr
          v-if="('vendorId' in saved || 'vendorId' in current) && field(saved, 'vendorId') !== null"
          :class="field(saved, 'vendorId') !== field(current, 'vendorId') ? 'mismatch' : ''"
        >
          <td>Vendor ID:</td>
          <td>{{ field(saved, "vendorId") }}</td>
          <td>{{ field(current, "vendorId") }}</td>
        </tr>
        <tr
          v-if="('productId' in saved || 'productId' in current) && field(saved, 'productId') !== null"
          :class="field(saved, 'productId') !== field(current, 'productId') ? 'mismatch' : ''"
        >
          <td>Product ID:</td>
          <td>{{ field(saved, "productId") }}</td>
          <td>{{ field(current, "productId") }}</td>
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
          v-if="('otherPaths' in saved || 'otherPaths' in current) && field(saved, 'otherPaths') !== null"
          :class="isEqual(field(saved, 'otherPaths'), field(current, 'otherPaths')) ? '' : 'mismatch'"
        >
          <td>Other Paths:</td>
          <td>{{ field(saved, "otherPaths") }}</td>
          <td>{{ field(current, "otherPaths") }}</td>
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
