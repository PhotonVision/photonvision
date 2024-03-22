<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ActivePipelineSettings, PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";
import { angleModulus, toDeg } from "@/lib/MathUtils";
import { computed } from "vue";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ActivePipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings
);

const calculateStdDev = (values: number[]): number => {
  if (values.length < 2) return 0;

  // Use mean of cosine/sine components to handle angle wrapping
  const cosines = values.map((it) => Math.cos(it));
  const sines = values.map((it) => Math.sin(it));
  const cosmean = cosines.reduce((sum, number) => sum + number, 0) / values.length;
  const sinmean = sines.reduce((sum, number) => sum + number, 0) / values.length;

  // Borrowed from WPILib's Rotation2d
  const hypot = Math.hypot(cosmean, sinmean);
  const mean = hypot > 1e-6 ? Math.atan2(sinmean / hypot, cosmean / hypot) : 0;

  return Math.sqrt(values.map((x) => Math.pow(angleModulus(x - mean), 2)).reduce((a, b) => a + b) / values.length);
};
const resetCurrentBuffer = () => {
  // Need to clear the array in place
  if (useStateStore().currentMultitagBuffer) useStateStore().currentMultitagBuffer!.length = 0;
};
</script>

<template>
  <div>
    <v-row align="start" class="pb-4">
      <v-simple-table dense class="pt-2 pb-12">
        <template #default>
          <thead>
            <tr>
              <th
                v-if="
                  currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
                  currentPipelineSettings.pipelineType === PipelineType.Aruco
                "
                class="text-center white--text"
              >
                Fiducial ID
              </th>
              <template v-if="currentPipelineSettings.pipelineType === PipelineType.ObjectDetection">
                <th class="text-center white--text">Class</th>
                <th class="text-center white--text">Confidence</th>
              </template>
              <template v-if="!useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <th class="text-center white--text">Pitch &theta;&deg;</th>
                <th class="text-center white--text">Yaw &theta;&deg;</th>
                <th class="text-center white--text">Skew &theta;&deg;</th>
                <th class="text-center white--text">Area %</th>
              </template>
              <template v-else>
                <th class="text-center white--text">X meters</th>
                <th class="text-center white--text">Y meters</th>
                <th class="text-center white--text">Z Angle &theta;&deg;</th>
              </template>
              <template
                v-if="
                  (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
                    currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
                  useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
                "
              >
                <th class="text-center white--text">Ambiguity Ratio</th>
              </template>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(target, index) in useStateStore().currentPipelineResults?.targets"
              :key="index"
              class="white--text"
            >
              <td
                v-if="
                  currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
                  currentPipelineSettings.pipelineType === PipelineType.Aruco
                "
                class="text-center"
              >
                {{ target.fiducialId }}
              </td>
              <td
                v-if="currentPipelineSettings.pipelineType === PipelineType.ObjectDetection"
                class="text-center white--text"
              >
                {{ useStateStore().currentPipelineResults?.classNames[target.classId] }}
              </td>
              <td
                v-if="currentPipelineSettings.pipelineType === PipelineType.ObjectDetection"
                class="text-center white--text"
              >
                {{ target.confidence.toFixed(2) }}
              </td>
              <template v-if="!useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <td class="text-center">{{ target.pitch.toFixed(2) }}&deg;</td>
                <td class="text-center">{{ target.yaw.toFixed(2) }}&deg;</td>
                <td class="text-center">{{ target.skew.toFixed(2) }}&deg;</td>
                <td class="text-center">{{ target.area.toFixed(2) }}&deg;</td>
              </template>
              <template v-else>
                <td class="text-center">{{ target.pose?.x.toFixed(2) }}&nbsp;m</td>
                <td class="text-center">{{ target.pose?.y.toFixed(2) }}&nbsp;m</td>
                <td class="text-center">{{ toDeg(target.pose?.angle_z || 0).toFixed(2) }}&deg;</td>
              </template>
              <template
                v-if="
                  (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
                    currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
                  useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
                "
              >
                <td class="text-center">
                  {{ target.ambiguity >= 0 ? target.ambiguity.toFixed(2) : "(In Multi-Target)" }}
                </td>
              </template>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </v-row>
    <v-container
      v-if="
        (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
          currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
        currentPipelineSettings.doMultiTarget &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
      "
    >
      <v-row class="pb-4 white--text">
        <v-card-subtitle class="ma-0 pa-0 pb-4" style="font-size: 16px"
          >Multi-tag pose, field-to-camera</v-card-subtitle
        >
        <v-simple-table dense>
          <template #default>
            <thead>
              <tr class="white--text">
                <th class="text-center white--text">X meters</th>
                <th class="text-center white--text">Y meters</th>
                <th class="text-center white--text">Z meters</th>
                <th class="text-center white--text">X Angle &theta;&deg;</th>
                <th class="text-center white--text">Y Angle &theta;&deg;</th>
                <th class="text-center white--text">Z Angle &theta;&deg;</th>
                <th class="text-center white--text">Tags</th>
              </tr>
            </thead>
            <tbody v-show="useStateStore().currentPipelineResults?.multitagResult">
              <tr>
                <td class="text-center white--text">
                  {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.x.toFixed(2) }}&nbsp;m
                </td>
                <td class="text-center white--text">
                  {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.y.toFixed(2) }}&nbsp;m
                </td>
                <td class="text-center white--text">
                  {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.z.toFixed(2) }}&nbsp;m
                </td>
                <td class="text-center white--text">
                  {{
                    toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_x || 0).toFixed(
                      2
                    )
                  }}&deg;
                </td>
                <td class="text-center white--text">
                  {{
                    toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_y || 0).toFixed(
                      2
                    )
                  }}&deg;
                </td>
                <td class="text-center white--text">
                  {{
                    toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_z || 0).toFixed(
                      2
                    )
                  }}&deg;
                </td>
                <td class="text-center white--text">
                  {{ useStateStore().currentPipelineResults?.multitagResult?.fiducialIDsUsed }}
                </td>
              </tr>
            </tbody>
          </template>
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
        <v-simple-table dense>
          <template #default>
            <thead>
              <tr>
                <th class="text-center white--text">X meters</th>
                <th class="text-center white--text">Y meters</th>
                <th class="text-center white--text">Z meters</th>
                <th class="text-center white--text">X Angle &theta;&deg;</th>
                <th class="text-center white--text">Y Angle &theta;&deg;</th>
                <th class="text-center white--text">Z Angle &theta;&deg;</th>
              </tr>
            </thead>
            <tbody v-show="useStateStore().currentPipelineResults?.multitagResult">
              <tr>
                <td class="text-center white--text">
                  {{
                    calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.x) || []).toFixed(
                      5
                    )
                  }}&nbsp;m
                </td>
                <td class="text-center white--text">
                  {{
                    calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.y) || []).toFixed(
                      5
                    )
                  }}&nbsp;m
                </td>
                <td class="text-center white--text">
                  {{
                    calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.z) || []).toFixed(
                      5
                    )
                  }}&nbsp;m
                </td>
                <td class="text-center white--text">
                  {{
                    calculateStdDev(
                      useStateStore().currentMultitagBuffer?.map((v) => toDeg(v.bestTransform.angle_x)) || []
                    ).toFixed(5)
                  }}&deg;
                </td>
                <td class="text-center white--text">
                  {{
                    calculateStdDev(
                      useStateStore().currentMultitagBuffer?.map((v) => toDeg(v.bestTransform.angle_y)) || []
                    ).toFixed(5)
                  }}&deg;
                </td>
                <td class="text-center white--text">
                  {{
                    calculateStdDev(
                      useStateStore().currentMultitagBuffer?.map((v) => toDeg(v.bestTransform.angle_z)) || []
                    ).toFixed(5)
                  }}&deg;
                </td>
              </tr>
            </tbody>
          </template>
        </v-simple-table>
      </v-row>
    </v-container>
  </div>
</template>

<style scoped lang="scss">
.v-data-table {
  background-color: #006492 !important;
  width: 100%;
  font-size: 1rem !important;

  thead {
    tr {
      th {
        font-size: 1rem !important;
        color: white !important;
      }
    }
  }
  tbody {
    :hover {
      td {
        background-color: #005281 !important;
      }
    }
    tr {
      td {
        font-size: 1rem !important;
        color: white !important;
      }
    }
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
