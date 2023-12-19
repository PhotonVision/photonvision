<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";
import Vue, { ref, watch } from "vue";
import type { Transform3d } from "@/types/PhotonTrackingTypes";

let oldResults: {targets: Transform3d[]} = Vue.observable({ targets: [] });
let stdev = ref({
  x: 0,
  y: 0,
  z: 0,
  x_angle: 0,
  y_angle: 0,
  z_angle: 0
});

const standardDeviation = (arr, usePopulation = false) => {
  if (arr.length < 2) {
    return 0;
  }

  const mean = arr.reduce((acc, val) => acc + val, 0) / arr.length;
  return Math.sqrt(
    arr
      .reduce((acc, val) => acc.concat((val - mean) ** 2), [])
      .reduce((acc, val) => acc + val, 0) /
      (arr.length - (usePopulation ? 0 : 1))
  );
};

watch(() => useStateStore().currentPipelineResults?.multitagResult?.bestTransform, (newThing: any) => {
  oldResults.targets.push(newThing);
  oldResults.targets = oldResults.targets.filter(item => item).slice(-100);

  stdev.value.x = standardDeviation(oldResults.targets.map(it => it.x));
  stdev.value.y = standardDeviation(oldResults.targets.map(it => it.y));
  stdev.value.z = standardDeviation(oldResults.targets.map(it => it.z));
  stdev.value.x_angle = standardDeviation(oldResults.targets.map(it => it.angle_x));
  stdev.value.y_angle = standardDeviation(oldResults.targets.map(it => it.angle_y));
  stdev.value.z_angle = standardDeviation(oldResults.targets.map(it => it.angle_z));
});
</script>

<template>
  <div>
    <v-row align="start" class="pb-4" style="height: 300px">
      <!-- Simple table height must be set here and in the CSS for the fixed-header to work -->
      <v-simple-table fixed-header dense dark>
        <template #default>
          <thead style="font-size: 1.25rem">
            <tr>
              <th
                v-if="
                  useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
                  useCameraSettingsStore().currentPipelineType === PipelineType.Aruco
                "
                class="text-center"
              >
                Fiducial ID
              </th>
              <template v-if="!useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <th class="text-center">Pitch &theta;&deg;</th>
                <th class="text-center">Yaw &theta;&deg;</th>
                <th class="text-center">Skew &theta;&deg;</th>
                <th class="text-center">Area %</th>
              </template>
              <template v-else>
                <th class="text-center">X meters</th>
                <th class="text-center">Y meters</th>
                <th class="text-center">Z Angle &theta;&deg;</th>
              </template>
              <template
                v-if="
                  (useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
                    useCameraSettingsStore().currentPipelineType === PipelineType.Aruco) &&
                  useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
                "
              >
                <th class="text-center">Ambiguity %</th>
              </template>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(target, index) in useStateStore().currentPipelineResults?.targets" :key="index">
              <td
                v-if="
                  useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
                  useCameraSettingsStore().currentPipelineType === PipelineType.Aruco
                "
              >
                {{ target.fiducialId }}
              </td>
              <template v-if="!useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <td>{{ target.pitch.toFixed(2) }}&deg;</td>
                <td>{{ target.yaw.toFixed(2) }}&deg;</td>
                <td>{{ target.skew.toFixed(2) }}&deg;</td>
                <td>{{ target.area.toFixed(2) }}&deg;</td>
              </template>
              <template v-else>
                <td>{{ target.pose?.x.toFixed(2) }}&nbsp;m</td>
                <td>{{ target.pose?.y.toFixed(2) }}&nbsp;m</td>
                <td>{{ (((target.pose?.angle_z || 0) * 180.0) / Math.PI).toFixed(2) }}&deg;</td>
              </template>
              <template
                v-if="
                  (useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
                    useCameraSettingsStore().currentPipelineType === PipelineType.Aruco) &&
                  useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
                "
              >
                <td>{{ target.ambiguity >= 0 ? target.ambiguity?.toFixed(2) + "%" : "(In Multi-Target)" }}</td>
              </template>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </v-row>
    <template
      v-if="
        (useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
          useCameraSettingsStore().currentPipelineType === PipelineType.Aruco) &&
        useCameraSettingsStore().currentPipelineSettings.doMultiTarget  &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
      "
    >
    <v-row
      align="start"
      class="pb-4 white--text"
    >
      <v-card-subtitle class="ma-0 pa-0 pb-4" style="font-size: 16px">Multi-tag pose, field-to-camera</v-card-subtitle>
      <v-simple-table fixed-header height="100%" dense dark>
        <thead style="font-size: 1.25rem">
          <th class="text-center">X meters</th>
          <th class="text-center">Y meters</th>
          <th class="text-center">Z meters</th>
          <th class="text-center">X Angle &theta;&deg;</th>
          <th class="text-center">Y Angle &theta;&deg;</th>
          <th class="text-center">Z Angle &theta;&deg;</th>
          <th class="text-center">Tags</th>
        </thead>
        <tbody v-show="useStateStore().currentPipelineResults?.multitagResult">
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.x.toFixed(2) }}&nbsp;m</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.y.toFixed(2) }}&nbsp;m</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.z.toFixed(2) }}&nbsp;m</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_x.toFixed(2) }}&deg;</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_y.toFixed(2) }}&deg;</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_z.toFixed(2) }}&deg;</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.fiducialIDsUsed }}</td>
        </tbody>
      </v-simple-table>
    </v-row>
    <v-row align="start" class="pb-4 white--text">
      <v-card-subtitle class="ma-0 pa-0 pb-4" style="font-size: 16px">Multi-tag pose standard deviation over 100 samples</v-card-subtitle>
      <v-simple-table fixed-header height="100%" dense dark>
        <thead style="font-size: 1.25rem">
          <th class="text-center">X meters</th>
          <th class="text-center">Y meters</th>
          <th class="text-center">Z meters</th>
          <th class="text-center">X Angle &theta;&deg;</th>
          <th class="text-center">Y Angle &theta;&deg;</th>
          <th class="text-center">Z Angle &theta;&deg;</th>
        </thead>
        <tbody v-show="useStateStore().currentPipelineResults?.multitagResult">
          <td>{{ stdev.x.toFixed(5) }}&nbsp;m</td>
          <td>{{ stdev.y.toFixed(5) }}&nbsp;m</td>
          <td>{{ stdev.z.toFixed(5) }}&nbsp;m</td>
          <td>{{ stdev.x_angle.toFixed(5) }}&deg;</td>
          <td>{{ stdev.y_angle.toFixed(5) }}&deg;</td>
          <td>{{ stdev.z_angle.toFixed(5) }}&deg;</td>
        </tbody>
      </v-simple-table>
    </v-row>
    </template>
  </div>
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
