<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { Euler, Quaternion as ThreeQuat } from "three";
import type { Quaternion } from "@/types/PhotonTrackingTypes";
import { toDeg } from "@/lib/MathUtils";

const quaternionToEuler = (rot_quat: Quaternion): { x: number; y: number; z: number } => {
  const quat = new ThreeQuat(rot_quat.X, rot_quat.Y, rot_quat.Z, rot_quat.W);
  const euler = new Euler().setFromQuaternion(quat, "ZYX");

  return {
    x: toDeg(euler.x),
    y: toDeg(euler.y),
    z: toDeg(euler.z)
  };
};
</script>

<template>
  <v-card dark style="background-color: #006492">
    <v-card-title class="pa-6">AprilTag Field Layout</v-card-title>
    <v-card-text class="pa-6 pt-0">
      <p>Field width: {{ useSettingsStore().currentFieldLayout.field.width.toFixed(2) }} meters</p>
      <p>Field length: {{ useSettingsStore().currentFieldLayout.field.length.toFixed(2) }} meters</p>

      <!-- Simple table height must be set here and in the CSS for the fixed-header to work -->
      <v-simple-table fixed-header height="100%" dense dark>
        <template #default>
          <thead style="font-size: 1.25rem">
            <tr>
              <th class="text-center">ID</th>
              <th class="text-center">X meters</th>
              <th class="text-center">Y meters</th>
              <th class="text-center">Z meters</th>
              <th class="text-center">θ<sub>x</sub>&deg;</th>
              <th class="text-center">θ<sub>y</sub>&deg;</th>
              <th class="text-center">θ<sub>z</sub>&deg;</th>
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
      </v-simple-table>
    </v-card-text>
  </v-card>
</template>

<style scoped lang="scss">
.v-data-table {
  width: 100%;
  height: 100%;
  text-align: center;
  background-color: #006492 !important;

  th,
  td {
    background-color: #006492 !important;
    font-size: 1rem !important;
    color: white !important;
  }

  td {
    font-family: monospace !important;
  }

  tbody :hover td {
    background-color: #005281 !important;
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
    background-color: #ffd843;
    border-radius: 10px;
  }
}
</style>
