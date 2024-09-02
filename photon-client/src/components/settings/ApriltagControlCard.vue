<script setup lang="ts">
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { Euler, Quaternion as ThreeQuat } from "three";
import type { Quaternion } from "@/types/PhotonTrackingTypes";
import { toDeg } from "@/lib/MathUtils";
import { computed } from "vue";

const flattenedEulerTags = computed(() => useSettingsStore().currentFieldLayout.tags.map((tag) => {
  const eu = quaternionToEuler(tag.pose.rotation.quaternion);
  return {
    ID: tag.ID,
    ...tag.pose.translation,
    x_t: eu.x,
    y_t: eu.y,
    z_t: eu.y
  };
}));

const quaternionToEuler = (rotQuat: Quaternion): { x: number; y: number; z: number } => {
  const quat = new ThreeQuat(rotQuat.X, rotQuat.Y, rotQuat.Z, rotQuat.W);
  const euler = new Euler().setFromQuaternion(quat, "ZYX");

  return {
    x: toDeg(euler.x),
    y: toDeg(euler.y),
    z: toDeg(euler.z)
  };
};

</script>

<template>
  <v-card>
    <v-card-title class="mb-3 mt-2">AprilTag Field Layout</v-card-title>
    <v-data-table
      class="mb-3 pl-4 pr-4"
      :headers="Array(7).fill({})"
      hide-default-footer
      :items="flattenedEulerTags"
    >
      <template #top>
        <p class="pl-2">Field width: {{ useSettingsStore().currentFieldLayout.field.width.toFixed(2) }} meters</p>
        <p class="pl-2">Field length: {{ useSettingsStore().currentFieldLayout.field.length.toFixed(2) }} meters</p>
      </template>

      <template #headers>
        <tr>
          <th :key="'ID'">ID</th>
          <th :key="'x'">X meters</th>
          <th :key="'y'">Y meters</th>
          <th :key="'z'">Z meters</th>
          <th :key="'x_t'">θ<sub>x</sub>&deg;</th>
          <th :key="'y_t'">θ<sub>y</sub>&deg;</th>
          <th :key="'z_t'">θ<sub>z</sub>&deg;</th>
        </tr>
      </template>
      <template #no-data>
        <span>No Tags Detected</span>
      </template>
    </v-data-table>
  </v-card>
</template>

<style scoped>
* >>> .v-data-table-rows-no-data >>> *{
  column-span: all;
}
</style>
