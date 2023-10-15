<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";
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
                  useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag &&
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
              <template v-else-if="useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <td>{{ target.pose?.x.toFixed(2) }}&nbsp;m</td>
                <td>{{ target.pose?.y.toFixed(2) }}&nbsp;m</td>
                <td>{{ (((target.pose?.angle_z || 0) * 180.0) / Math.PI).toFixed(2) }}&deg;</td>
              </template>
              <template
                v-if="
                  useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag &&
                  useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
                "
              >
                <td>{{ target.ambiguity >= 0 ? target.ambiguity?.toFixed(2)+"%" : "(In Multi-Target)" }}</td>
              </template>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </v-row>
    <v-row
      v-if="
        useCameraSettingsStore().currentPipelineSettings.pipelineType === PipelineType.AprilTag &&
        useCameraSettingsStore().currentPipelineSettings.doMultiTarget
      "
      align="start"
      class="pb-4 white--text"
    >
      <v-card-subtitle>Multi-tag pose, field-to-camera</v-card-subtitle>
      <v-simple-table fixed-header height="100%" dense dark>
        <thead style="font-size: 1.25rem">
          <th class="text-center">X meters</th>
          <th class="text-center">Y meters</th>
          <th class="text-center">Z Angle &theta;&deg;</th>
          <th class="text-center">Tags</th>
        </thead>
        <tbody>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.x.toFixed(2) }}</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.y.toFixed(2) }}</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_z.toFixed(2) }}</td>
          <td>{{ useStateStore().currentPipelineResults?.multitagResult?.fiducialIDsUsed }}</td>
        </tbody>
      </v-simple-table>
    </v-row>
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
