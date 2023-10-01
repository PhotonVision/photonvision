<script setup lang="ts">
import CvSlider from "@/components/common/cv-slider.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { Euler, Quaternion } from "three";

function quatToEuler(quat) {
  console.log(quat);
  quat = new Quaternion(quat.X, quat.Y, quat.Z, quat.W);
  return new Euler().setFromQuaternion(quat, "ZYX");
}

// Convert from radians to degrees.
const degrees = function (radians) {
  return (radians * 180) / Math.PI;
};
</script>

<template>
  <v-card dark class="mb-3 pr-6 pb-3" style="background-color: #006492">
    <v-card-title>Apriltag Layout</v-card-title>
    <div class="ml-5">
      <p>Field width: {{ (useSettingsStore().currentFieldLayout.field.width * 3.28084).toFixed(2) }} ft</p>
      <p>Field length: {{ (useSettingsStore().currentFieldLayout.field.length * 3.28084).toFixed(2) }} ft</p>

      <!-- Simple table height must be set here and in the CSS for the fixed-header to work -->
      <v-simple-table fixed-header height="100%" dense dark>
        <template #default>
          <thead style="font-size: 1.25rem">
            <tr>
              <th class="text-center">ID</th>
              <th class="text-center">X, m</th>
              <th class="text-center">Y, m</th>
              <th class="text-center">Z, m</th>
              <th class="text-center">θ<sub>x</sub>, &deg;</th>
              <th class="text-center">θ<sub>y</sub>, &deg;</th>
              <th class="text-center">θ<sub>z</sub>, &deg;</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(tag, index) in useSettingsStore().currentFieldLayout.tags" :key="index">
              <td>{{ tag.ID }}</td>
              <td>{{ tag.pose.translation.x.toFixed(2) }}</td>
              <td>{{ tag.pose.translation.y.toFixed(2) }}</td>
              <td>{{ tag.pose.translation.z.toFixed(2) }}</td>
              <td>{{ degrees(quatToEuler(tag.pose.rotation.quaternion).x).toFixed(2) }}&deg;</td>
              <td>{{ degrees(quatToEuler(tag.pose.rotation.quaternion).y).toFixed(2) }}&deg;</td>
              <td>{{ degrees(quatToEuler(tag.pose.rotation.quaternion).z).toFixed(2) }}&deg;</td>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </div>
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
