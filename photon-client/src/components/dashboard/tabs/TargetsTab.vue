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

const resultsTableHeaders = computed(() => {
  const headers = [];
  if (
    useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
    useCameraSettingsStore().currentPipelineType === PipelineType.Aruco
  ) {
    headers.push({ title: "Fiducial ID", key: "id", align: "start" });
  } else if (useCameraSettingsStore().currentPipelineType === PipelineType.ObjectDetection) {
    headers.push({ title: "Class", key: "class", align: "center" });
    headers.push({ title: "Confidence", key: "confidence", align: "center" });
  }

  if (!useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled) {
    headers.push({ title: "Pitch θ°", key: "pitch", align: "center" });
    headers.push({ title: "Yaw θ°", key: "yaw", align: "center" });
    headers.push({ title: "Skew θ°", key: "skew", align: "center" });
    headers.push({ title: "Area %", key: "area", align: "center" });
  } else {
    headers.push({
      title: "Pose",
      align: "center",
      children: [
        { title: "X Meters", key: "x", align: "center" },
        { title: "Y Meters", key: "y", align: "center" },
        { title: "Z Angle θ°", key: "theta", align: "center" }
      ]
    });
  }

  if (
    (useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
      useCameraSettingsStore().currentPipelineType === PipelineType.Aruco) &&
    useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
  ) {
    headers.push({ title: "Ambiguity Ratio", key: "ambiguity", align: "center" });
  }

  return headers;
});
const resultsTableItems = computed(() =>
  useStateStore().currentPipelineResults?.targets.map((t) => ({
    id: t.fiducialId,
    class: useStateStore().currentPipelineResults?.classNames[t.classId],
    confidence: t.confidence.toFixed(2),
    pitch: t.pitch.toFixed(2),
    yaw: t.yaw.toFixed(2),
    skew: t.skew.toFixed(2),
    area: t.area.toFixed(2),
    x: t.pose?.x.toFixed(2),
    y: t.pose?.y.toFixed(2),
    theta: toDeg(t.pose?.angle_z || 0).toFixed(2),
    ambiguity: t.ambiguity >= 0 ? t.ambiguity.toFixed(2) : "(In Multi-Target)"
  }))
);

// clear buffer button
// resize buffer button

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
const multitagStdDevData = computed(() => {
  const multitagBufferData = useStateStore().currentMultitagBuffer?.toArray();
  return {
    x: calculateStdDev(multitagBufferData?.map((v) => v.bestTransform.x) || []),
    y: calculateStdDev(multitagBufferData?.map((v) => v.bestTransform.y) || []),
    z: calculateStdDev(multitagBufferData?.map((v) => v.bestTransform.z) || []),
    theta_x: calculateStdDev(multitagBufferData?.map((v) => toDeg(v.bestTransform.angle_x)) || []),
    theta_y: calculateStdDev(multitagBufferData?.map((v) => toDeg(v.bestTransform.angle_y)) || []),
    theta_z: calculateStdDev(multitagBufferData?.map((v) => toDeg(v.bestTransform.angle_z)) || [])
  };
});
</script>

<template>
  <div>
    <v-data-table :headers="resultsTableHeaders" hide-default-footer :items="resultsTableItems">
      <template #no-data> No Targets Detected :( </template>
    </v-data-table>
    <div
      v-if="
        (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
          currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
        currentPipelineSettings.doMultiTarget &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
      "
    >
      <div>
        <v-divider class="mb-3 mt-3" />
        <v-table class="pb-6" density="compact">
          <template #top>
            <span class="text-subtitle-1">Multi-tag Pose Results</span>
          </template>
          <thead>
            <tr>
              <th class="text-center">X meters</th>
              <th class="text-center">Y meters</th>
              <th class="text-center">Z meters</th>
              <th class="text-center">X Angle &theta;&deg;</th>
              <th class="text-center">Y Angle &theta;&deg;</th>
              <th class="text-center">Z Angle &theta;&deg;</th>
              <th class="text-center">Tags</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="useStateStore().currentPipelineResults?.multitagResult">
              <td class="text-center">
                {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.x.toFixed(2) }}&nbsp;m
              </td>
              <td class="text-center">
                {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.y.toFixed(2) }}&nbsp;m
              </td>
              <td class="text-center">
                {{ useStateStore().currentPipelineResults?.multitagResult?.bestTransform.z.toFixed(2) }}&nbsp;m
              </td>
              <td class="text-center">
                {{
                  toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_x || 0).toFixed(2)
                }}&deg;
              </td>
              <td class="text-center">
                {{
                  toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_y || 0).toFixed(2)
                }}&deg;
              </td>
              <td class="text-center">
                {{
                  toDeg(useStateStore().currentPipelineResults?.multitagResult?.bestTransform.angle_z || 0).toFixed(2)
                }}&deg;
              </td>
              <td class="text-center">
                {{ useStateStore().currentPipelineResults?.multitagResult?.fiducialIDsUsed }}
              </td>
            </tr>
            <tr v-else>
              <td class="text-center pt-3" colspan="7">No Multitag Result Available</td>
            </tr>
          </tbody>
        </v-table>
        <v-expansion-panels>
          <v-expansion-panel>
            <v-expansion-panel-title
              >Multi-tag pose standard deviation over the last
              {{ useStateStore().currentMultitagBuffer?.getBufferLength() || 0 }}/{{
                useStateStore().multitagResultBufferSize
              }}
              samples</v-expansion-panel-title
            >
            <v-expansion-panel-text>
              <v-table class="pb-6" density="compact">
                <thead>
                  <tr>
                    <th class="text-center">X meters</th>
                    <th class="text-center">Y meters</th>
                    <th class="text-center">Z meters</th>
                    <th class="text-center">X Angle &theta;&deg;</th>
                    <th class="text-center">Y Angle &theta;&deg;</th>
                    <th class="text-center">Z Angle &theta;&deg;</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="useStateStore().currentMultitagBuffer?.isEmpty() || true">
                    <td class="text-center pt-3" colspan="6">Multitag Result Buffer is Empty</td>
                  </tr>
                  <tr v-else>
                    <td class="text-center">{{ multitagStdDevData.x.toFixed(5) }}&nbsp;m</td>
                    <td class="text-center">{{ multitagStdDevData.y.toFixed(5) }}&nbsp;m</td>
                    <td class="text-center">{{ multitagStdDevData.z.toFixed(5) }}&nbsp;m</td>
                    <td class="text-center">{{ multitagStdDevData.theta_x.toFixed(5) }}&deg;</td>
                    <td class="text-center">{{ multitagStdDevData.theta_y.toFixed(5) }}&deg;</td>
                    <td class="text-center">{{ multitagStdDevData.theta_z.toFixed(5) }}&deg;</td>
                  </tr>
                </tbody>
              </v-table>
              <v-btn
                color="accent"
                :disabled="useStateStore().currentMultitagBuffer?.isEmpty() || true"
                style="width: 100%"
                text="Clear Sample Buffer"
                @click="useStateStore().currentMultitagBuffer?.clear()"
              />
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </div>
    </div>
  </div>
</template>
