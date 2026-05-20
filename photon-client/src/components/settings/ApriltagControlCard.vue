<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import type { Quaternion } from "@/types/PhotonTrackingTypes";
import { toDeg } from "@/lib/MathUtils";

const { Euler, Quaternion: ThreeQuat } = await import("three");

const quaternionToEuler = (rot_quat: Quaternion): { x: number; y: number; z: number } => {
  const quat = new ThreeQuat(rot_quat.X, rot_quat.Y, rot_quat.Z, rot_quat.W);
  const euler = new Euler().setFromQuaternion(quat, "ZYX");

  return { x: toDeg(euler.x), y: toDeg(euler.y), z: toDeg(euler.z) };
};
</script>

<template>
  <pv-card>
    <div class="flex items-center justify-between gap-2 pb-4">
      <div class="flex-1 text-lg font-semibold">AprilTag Field Layout</div>
      <p class="text-sm font-light text-gray-200">
        Field width: {{ useSettingsStore().currentFieldLayout.field.width.toFixed(2) }} meters
      </p>
      <p class="text-sm font-light text-gray-200">
        Field length: {{ useSettingsStore().currentFieldLayout.field.length.toFixed(2) }} meters
      </p>
    </div>
    <div>
      <!-- Simple table height must be set here and in the CSS for the fixed-header to work -->
      <pv-table fixed-header height="100%" >
        <template #default>
          <thead style="font-size: 1.25rem">
            <tr>
              <th class="">ID</th>
              <th class="">X meters</th>
              <th class="">Y meters</th>
              <th class="">Z meters</th>
              <th class="">θ<sub>x</sub>&deg;</th>
              <th class="">θ<sub>y</sub>&deg;</th>
              <th class="">θ<sub>z</sub>&deg;</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(tag, index) in useSettingsStore().currentFieldLayout.tags" :key="index">
              <td>{{ tag.ID }}</td>
              <td v-for="(val, idx) in Object.values(tag.pose.translation)" :key="idx">{{ val.toFixed(2) }}&nbsp;m</td>
              <td v-for="(val, idx) in Object.values(quaternionToEuler(tag.pose.rotation.quaternion))" :key="idx + 4">
                {{ val.toFixed(2) }}&deg;
              </td>
            </tr>
          </tbody>
        </template>
      </pv-table>
    </div>
  </pv-card>
</template>

<style scoped>
.pv-table {
  width: 100%;
  height: 100%;
  text-align: center;

  th,
  td {
    font-size: 1rem !important;
    text-align: center;
  }

  td {
    font-variant-numeric: tabular-nums;
  }

  ::-webkit-scrollbar {
    width: 0;
    height: 0.55em;
    border-radius: 5px;
  }

  ::-webkit-scrollbar-track {
    -webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
    border-radius: 10px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: var(--color-pv-accent);
    border-radius: 10px;
  }
}
</style>
