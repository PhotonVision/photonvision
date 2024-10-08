<script setup lang="ts">
import {
  AprilTagPipelineSettings, ArucoPipelineSettings,
  PipelineType,
  UserPipelineSettings
} from "@/types/PipelineTypes";
import { angleModulus, toDeg } from "@/lib/MathUtils";
import { computed } from "vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import { CameraConfig, VideoFormat } from "@/types/SettingTypes";
import {
  type MultitagResult,
  ObjectDetectionTrackedTarget,
  TagTrackedTarget,
  TrackedTarget
} from "@/types/PhotonTrackingTypes";
import { resolutionsAreEqual } from "@/lib/PhotonUtils";
import { CircularBuffer } from "@/lib/CircularBuffer";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
}>();

const targetPipelineSettings = computed<UserPipelineSettings>(() => serverStore.getActivePipelineSettingsByCameraIndex(props.cameraSettings.cameraIndex) as UserPipelineSettings);
const isTagPipeline = computed<boolean>(() => [PipelineType.AprilTag, PipelineType.Aruco].includes(targetPipelineSettings.value.pipelineType));
const isSolvePNPEnabled = computed<boolean>(() => Object.prototype.hasOwnProperty.call(targetPipelineSettings.value, "solvePNPEnabled") && targetPipelineSettings.value.solvePNPEnabled);
const isCalibrated = computed(() => {
  const targetIndex = targetPipelineSettings.value.cameraVideoModeIndex;
  const targetVideoMode = props.cameraSettings.videoFormats.find((v) => v.sourceIndex === targetIndex) as VideoFormat;
  return props.cameraSettings.calibrations.some((v) => resolutionsAreEqual(v.resolution, targetVideoMode.resolution));
});

const resultsTableHeaders = computed(() => {
  const headers = [];
  if (isTagPipeline.value) {
    headers.push({ title: "Fiducial ID", key: "id", align: "start", value: (item: TagTrackedTarget) => item.fiducialId });
  } else if (targetPipelineSettings.value.pipelineType === PipelineType.ObjectDetection) {
    headers.push({ title: "Class", key: "className", align: "center", value: (item: ObjectDetectionTrackedTarget) => item.confidence.toFixed(2) });
    headers.push({ title: "Confidence", key: "confidence", align: "center", value: (item: ObjectDetectionTrackedTarget) => clientStore.pipelineResultsFromCameraIndex(props.cameraSettings.cameraIndex)?.classNames[item.classId] });
  }

  if (isSolvePNPEnabled.value) {
    headers.push({
      title: "Pose",
      align: "center",
      children: [
        { title: "X Meters", key: "x", align: "center", value: (item: TagTrackedTarget) => item.bestTransform!.translation.x.toFixed(2) },
        { title: "Y Meters", key: "y", align: "center", value: (item: TagTrackedTarget) => item.bestTransform!.translation.y.toFixed(2) },
        { title: "Z Meters", key: "z", align: "center", value: (item: TagTrackedTarget) => item.bestTransform!.translation.z.toFixed(2) },
        { title: "Z Angle θ°", key: "z_theta", align: "center", value: (item: TagTrackedTarget) => toDeg(item.bestTransform!.rotation.angle_z || 0).toFixed(2) }
      ]
    });

    if (isTagPipeline.value) {
      headers.push({ title: "Ambiguity Ratio", key: "ambiguity", align: "center", value: (item: TagTrackedTarget) => item.ambiguity >= 0 ? item.ambiguity.toFixed(2) : "(In Multi-Target)" });
    }
  } else {
    headers.push({ title: "Pitch θ°", key: "pitch", align: "center", value: (item: TrackedTarget) => item.pitch.toFixed(2) });
    headers.push({ title: "Yaw θ°", key: "yaw", align: "center", value: (item: TrackedTarget) => item.yaw.toFixed(2) });
    headers.push({ title: "Skew θ°", key: "skew", align: "center", value: (item: TrackedTarget) => item.skew.toFixed(2) });
    headers.push({ title: "Area %", key: "area", align: "center", value: (item: TrackedTarget) => item.area.toFixed(2) });
  }

  return headers;
});

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

const targetMultitagBuffer = computed<CircularBuffer<MultitagResult> | undefined>(() => clientStore.getMultitagResultBufferFromCameraIndex(props.cameraSettings.cameraIndex));
const multitagStdDevData = computed(() => {
  const multitagBufferData = targetMultitagBuffer.value?.toArray();
  return {
    x: calculateStdDev(multitagBufferData?.map((v) => v.bestTransform.translation.x) || []),
    y: calculateStdDev(multitagBufferData?.map((v) => v.bestTransform.translation.y) || []),
    z: calculateStdDev(multitagBufferData?.map((v) => v.bestTransform.translation.z) || []),
    theta_x: calculateStdDev(multitagBufferData?.map((v) => toDeg(v.bestTransform.rotation?.angle_x || 0)) || []),
    theta_y: calculateStdDev(multitagBufferData?.map((v) => toDeg(v.bestTransform.rotation?.angle_y || 0)) || []),
    theta_z: calculateStdDev(multitagBufferData?.map((v) => toDeg(v.bestTransform.rotation?.angle_z || 0)) || [])
  };
});
</script>

<template>
  <div>
    <v-data-table
      :headers="resultsTableHeaders"
      hide-default-footer
      :items="clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)?.targets"
    >
      <template #no-data> No Targets Detected :( </template>
    </v-data-table>
    <div
      v-if="isTagPipeline &&
        (targetPipelineSettings as AprilTagPipelineSettings | ArucoPipelineSettings).doMultiTarget &&
        isSolvePNPEnabled &&
        isCalibrated
      "
    >
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
          <tr v-if="clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)?.multitagResult">
            <td class="text-center">
              {{ clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)!.multitagResult!.bestTransform.translation.x.toFixed(2) }}&nbsp;m
            </td>
            <td class="text-center">
              {{ clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)!.multitagResult!.bestTransform.translation.y.toFixed(2) }}&nbsp;m
            </td>
            <td class="text-center">
              {{ clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)!.multitagResult!.bestTransform.translation.z.toFixed(2) }}&nbsp;m
            </td>
            <td class="text-center">
              {{
                toDeg(clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)!.multitagResult!.bestTransform!.rotation.angle_x || 0).toFixed(2)
              }}&deg;
            </td>
            <td class="text-center">
              {{
                toDeg(clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)!.multitagResult!.bestTransform!.rotation.angle_y || 0).toFixed(2)
              }}&deg;
            </td>
            <td class="text-center">
              {{
                toDeg(clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)!.multitagResult!.bestTransform!.rotation.angle_z || 0).toFixed(2)
              }}&deg;
            </td>
            <td class="text-center">
              {{ clientStore.pipelineResultsFromCameraIndex(cameraSettings.cameraIndex)!.multitagResult!.fiducialIDsUsed }}
            </td>
          </tr>
          <tr v-else>
            <td class="text-center pt-3" colspan="7">No Multitag Result Available</td>
          </tr>
        </tbody>
      </v-table>
      <v-expansion-panels>
        <v-expansion-panel>
          <v-expansion-panel-title>
            Multi-tag pose standard deviation over the last
            {{ targetMultitagBuffer?.getBufferLength() || 0 }}/{{ clientStore.multitagResultBufferSize }}
            samples
          </v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-table class="pb-6" density="compact">
              <template #top>
                <v-row>
                  <v-col cols="12" md="6">
                    <v-number-input
                      v-model="clientStore.multitagResultBufferSize"
                      base-color="accent"
                      control-variant="stacked"
                      hide-details
                      inset
                      :min="0"
                      :step="1"
                    />
                  </v-col>
                  <v-col cols="12" md="6">
                    <v-btn
                      class="w-100"
                      color="accent"
                      prepend-icon="mdi-trash"
                      text="Clear Multitag Buffer"
                      @click="clientStore.clearMultitagResultBufferByCameraIndex(cameraSettings.cameraIndex)"
                    />
                  </v-col>
                </v-row>
              </template>

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
                <tr v-if="targetMultitagBuffer?.isEmpty() || true">
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
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
    </div>
  </div>
</template>
