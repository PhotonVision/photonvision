<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";

const wrapToPi = (delta: number): number => {
  let ret = delta;
  while (ret < -Math.PI) ret += Math.PI * 2;
  while (ret > Math.PI) ret -= Math.PI * 2;
  return ret;
};

const calculateStdDev = (values: number[]): number => {
  if (values.length < 2) return 0;

  // Use mean of cosine/sine components to handle angle wrapping
  const cosines = values.map((it) => Math.cos(it));
  const sines = values.map((it) => Math.sin(it));
  const cosmean = cosines.reduce((sum, number) => sum + number, 0) / values.length;
  const sinmean = sines.reduce((sum, number) => sum + number, 0) / values.length;

  // Borrowed from WPILib's Rotation2d
  const hypot = Math.hypot(cosmean, sinmean);
  let mean;
  if (hypot > 1e-6) {
    mean = Math.atan2(sinmean / hypot, cosmean / hypot);
  } else {
    mean = 0;
  }

  return Math.sqrt(values.map((x) => Math.pow(wrapToPi(x - mean), 2)).reduce((a, b) => a + b) / values.length);
};
const resetCurrentBuffer = () => {
  // Need to clear the array in place
  while (useStateStore().currentMultitagBuffer?.length != 0) useStateStore().currentMultitagBuffer?.pop();
};
</script>

<template>
  <div>
    <v-row align="start" class="pb-4" style="height: 300px">
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
                <th class="text-center">Ambiguity Ratio</th>
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
                <td>{{ target.ambiguity >= 0 ? target.ambiguity.toFixed(2) : "(In Multi-Target)" }}</td>
              </template>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </v-row>
    <v-container
      v-if="
        (useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
          useCameraSettingsStore().currentPipelineType === PipelineType.Aruco) &&
        useCameraSettingsStore().currentPipelineSettings.doMultiTarget &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
      "
    >
      <v-row class="pb-4 white--text">
        <v-card-subtitle class="ma-0 pa-0 pb-4" style="font-size: 16px"
          >Multi-tag pose, field-to-camera</v-card-subtitle
        >
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
            <td>
              {{
                (
                  (useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_x || 0) *
                  (180.0 / Math.PI)
                ).toFixed(2)
              }}&deg;
            </td>
            <td>
              {{
                (
                  (useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_y || 0) *
                  (180.0 / Math.PI)
                ).toFixed(2)
              }}&deg;
            </td>
            <td>
              {{
                (
                  (useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_z || 0) *
                  (180.0 / Math.PI)
                ).toFixed(2)
              }}&deg;
            </td>
            <td>{{ useStateStore().currentPipelineResults?.multitagResult?.fiducialIDsUsed }}</td>
          </tbody>
        </v-simple-table>
      </v-row>
      <v-row class="pb-4 white--text" style="display: flex; flex-direction: column">
        <v-card-subtitle class="ma-0 pa-0 pb-4 pr-4" style="font-size: 16px"
          >Multi-tag pose standard deviation over the last
          {{ useStateStore().currentMultitagBuffer?.length || "NaN" }}/100 samples
        </v-card-subtitle>
        <v-btn color="secondary" class="mb-4 mt-1" style="width: min-content" depressed @click="resetCurrentBuffer"
          >Reset Samples</v-btn
        >
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
            <td>
              {{
                calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.x) || []).toFixed(5)
              }}&nbsp;m
            </td>
            <td>
              {{
                calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.y) || []).toFixed(5)
              }}&nbsp;m
            </td>
            <td>
              {{
                calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.z) || []).toFixed(5)
              }}&nbsp;m
            </td>
            <td>
              {{
                calculateStdDev(
                  useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.angle_x * (180.0 / Math.PI)) || []
                ).toFixed(5)
              }}&deg;
            </td>
            <td>
              {{
                calculateStdDev(
                  useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.angle_y * (180.0 / Math.PI)) || []
                ).toFixed(5)
              }}&deg;
            </td>
            <td>
              {{
                calculateStdDev(
                  useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.angle_z * (180.0 / Math.PI)) || []
                ).toFixed(5)
              }}&deg;
            </td>
          </tbody>
        </v-simple-table>
      </v-row>
    </v-container>
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
