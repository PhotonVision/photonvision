<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ActivePipelineSettings, PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";
import { angleModulus, toDeg } from "@/lib/MathUtils";
import { computed } from "vue";
import { useTheme } from "vuetify";

const theme = useTheme();

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
    <v-row class="pb-4">
      <v-table density="compact" class="pt-2 pb-12 pl-3 pr-3">
        <template #default>
          <thead>
            <tr>
              <th
                v-if="
                  currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
                  currentPipelineSettings.pipelineType === PipelineType.Aruco
                "
                class="text-center text-white"
              >
                Fiducial ID
              </th>
              <template v-if="currentPipelineSettings.pipelineType === PipelineType.ObjectDetection">
                <th class="text-center text-white">Class</th>
                <th class="text-center text-white">Confidence</th>
              </template>
              <template v-if="!useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <th class="text-center text-white">Pitch &theta;&deg;</th>
                <th class="text-center text-white">Yaw &theta;&deg;</th>
                <th class="text-center text-white">Skew &theta;&deg;</th>
                <th class="text-center text-white">Area %</th>
              </template>
              <template v-else>
                <th class="text-center text-white">X meters</th>
                <th class="text-center text-white">Y meters</th>
                <th class="text-center text-white">Z Angle &theta;&deg;</th>
              </template>
              <template
                v-if="
                  (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
                    currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
                  useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
                "
              >
                <th class="text-center text-white">Ambiguity Ratio</th>
              </template>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(target, index) in useStateStore().currentPipelineResults?.targets"
              :key="index"
              class="text-white"
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
                class="text-center text-white"
              >
                {{ useStateStore().currentPipelineResults?.classNames[target.classId] }}
              </td>
              <td
                v-if="currentPipelineSettings.pipelineType === PipelineType.ObjectDetection"
                class="text-center text-white"
              >
                {{ target.confidence.toFixed(2) }}
              </td>
              <template v-if="!useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled">
                <td class="text-center">{{ target.pitch.toFixed(2) }}&deg;</td>
                <td class="text-center">{{ target.yaw.toFixed(2) }}&deg;</td>
                <td class="text-center">{{ target.skew.toFixed(2) }}&deg;</td>
                <td class="text-center">{{ target.area.toFixed(2) }}%</td>
              </template>
              <template v-else>
                <td class="text-center">{{ target.pose?.x.toFixed(3) }}&nbsp;m</td>
                <td class="text-center">{{ target.pose?.y.toFixed(3) }}&nbsp;m</td>
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
      </v-table>
    </v-row>
    <v-container
      v-if="
        (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
          currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
        currentPipelineSettings.doMultiTarget &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
      "
      class="pl-3 pr-3"
    >
      <v-row class="pb-4 text-white">
        <v-card-subtitle class="ma-0 pa-0 pb-4" style="font-size: 16px"
          >Multi-tag pose, field-to-camera</v-card-subtitle
        >
        <v-table density="compact">
          <template #default>
            <thead>
              <tr class="text-white">
                <th class="text-center text-white">X meters</th>
                <th class="text-center text-white">Y meters</th>
                <th class="text-center text-white">Z meters</th>
                <th class="text-center text-white">X Angle &theta;&deg;</th>
                <th class="text-center text-white">Y Angle &theta;&deg;</th>
                <th class="text-center text-white">Z Angle &theta;&deg;</th>
                <th class="text-center text-white">Tags</th>
              </tr>
            </thead>
            <tbody v-show="useStateStore().currentPipelineResults?.multitagResult">
              <tr>
                <td class="text-center text-white">
                  {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.x.toFixed(3) }}&nbsp;m
                </td>
                <td class="text-center text-white">
                  {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.y.toFixed(3) }}&nbsp;m
                </td>
                <td class="text-center text-white">
                  {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.z.toFixed(3) }}&nbsp;m
                </td>
                <td class="text-center text-white">
                  {{
                    toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_x || 0).toFixed(
                      2
                    )
                  }}&deg;
                </td>
                <td class="text-center text-white">
                  {{
                    toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_y || 0).toFixed(
                      2
                    )
                  }}&deg;
                </td>
                <td class="text-center text-white">
                  {{
                    toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_z || 0).toFixed(
                      2
                    )
                  }}&deg;
                </td>
                <td class="text-center text-white">
                  {{ useStateStore().currentPipelineResults?.multitagResult?.fiducialIDsUsed }}
                </td>
              </tr>
            </tbody>
          </template>
        </v-table>
      </v-row>
      <v-row class="pb-4 text-white" style="display: flex; flex-direction: column">
        <v-card-subtitle class="ma-0 pa-0 pb-4 pr-4" style="font-size: 16px"
          >Multi-tag pose standard deviation over the last
          {{ useStateStore().currentMultitagBuffer?.length || "NaN" }}/100 samples
        </v-card-subtitle>
        <v-btn
          color="buttonActive"
          class="mb-4 mt-1"
          style="width: min-content"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="resetCurrentBuffer"
          >Reset Samples</v-btn
        >
        <v-table density="compact">
          <template #default>
            <thead>
              <tr>
                <th class="text-center text-white">X meters</th>
                <th class="text-center text-white">Y meters</th>
                <th class="text-center text-white">Z meters</th>
                <th class="text-center text-white">X Angle &theta;&deg;</th>
                <th class="text-center text-white">Y Angle &theta;&deg;</th>
                <th class="text-center text-white">Z Angle &theta;&deg;</th>
              </tr>
            </thead>
            <tbody v-show="useStateStore().currentPipelineResults?.multitagResult">
              <tr>
                <td class="text-center text-white">
                  {{
                    calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.x) || []).toFixed(
                      5
                    )
                  }}&nbsp;m
                </td>
                <td class="text-center text-white">
                  {{
                    calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.y) || []).toFixed(
                      5
                    )
                  }}&nbsp;m
                </td>
                <td class="text-center text-white">
                  {{
                    calculateStdDev(useStateStore().currentMultitagBuffer?.map((v) => v.bestTransform.z) || []).toFixed(
                      5
                    )
                  }}&nbsp;m
                </td>
                <td class="text-center text-white">
                  {{
                    calculateStdDev(
                      useStateStore().currentMultitagBuffer?.map((v) => toDeg(v.bestTransform.angle_x)) || []
                    ).toFixed(5)
                  }}&deg;
                </td>
                <td class="text-center text-white">
                  {{
                    calculateStdDev(
                      useStateStore().currentMultitagBuffer?.map((v) => toDeg(v.bestTransform.angle_y)) || []
                    ).toFixed(5)
                  }}&deg;
                </td>
                <td class="text-center text-white">
                  {{
                    calculateStdDev(
                      useStateStore().currentMultitagBuffer?.map((v) => toDeg(v.bestTransform.angle_z)) || []
                    ).toFixed(5)
                  }}&deg;
                </td>
              </tr>
            </tbody>
          </template>
        </v-table>
      </v-row>
    </v-container>
  </div>
</template>

<style scoped lang="scss">
th {
  padding-left: 8px !important;
  padding-right: 8px !important;
}
.v-table {
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
    tr {
      td {
        padding: 0 !important;
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
    background-color: rgb(var(--v-theme-accent));
    border-radius: 10px;
  }
}
</style>
