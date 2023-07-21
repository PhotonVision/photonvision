<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";
</script>

<template>
  <div>
    <v-row
      align="start"
      class="pb-4"
      style="height: 300px;"
    >
      <!-- Simple table height must be set here and in the CSS for the fixed-header to work -->
      <v-simple-table
        fixed-header
        height="100%"
        dense
        dark
      >
        <template #default>
          <thead style="font-size: 1.25rem;">
            <tr>
              <th class="text-center">
                Target Count
              </th>
              <th
                v-if="useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag || useCameraSettingsStore().currentPipelineType === PipelineType.Aruco"
                class="text-center"
              >
                Fiducial ID
              </th>
              <template v-if="!useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <th class="text-center">
                  Pitch &theta;&deg;
                </th>
                <th class="text-center">
                  Yaw &theta;&deg;
                </th>
                <th class="text-center">
                  Skew &theta;&deg;
                </th>
                <th class="text-center">
                  Area %
                </th>
              </template>
              <template v-else>
                <th class="text-center">
                  X meters
                </th>
                <th class="text-center">
                  Y meters
                </th>
                <th class="text-center">
                  Z Angle &theta;&deg;
                </th>
              </template>
              <template v-if="useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag && useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <th class="text-center">
                  Ambiguity %
                </th>
              </template>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(target, index) in useStateStore().pipelineResults?.targets"
              :key="index"
            >
              <td>{{ index }}</td>
              <td v-if="useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag || useCameraSettingsStore().currentPipelineType === PipelineType.Aruco">
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
                <td>{{ (target.pose?.angle_z * 180.0 / Math.PI).toFixed(2) }}&deg;</td>
              </template>
              <template v-if="useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag && useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <td>{{ target.ambiguity?.toFixed(2) }}%</td>
              </template>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </v-row>
  </div>
</template>

<style scoped>
.v-data-table {
  text-align: center;
  background-color: transparent !important;
  width: 100%;
  height: 100%;
  overflow-y: auto;
}

.v-data-table th {
  background-color: #006492 !important;
}

.v-data-table th,td {
  font-size: 1rem !important;
}

.v-data-table td {
  font-family: monospace !important;
}
</style>
