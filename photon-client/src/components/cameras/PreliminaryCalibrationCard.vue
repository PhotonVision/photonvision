<script setup lang="ts">
import { computed, ref } from "vue";
import axios from "axios";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import type { CameraCalibrationResult, VideoFormat } from "@/types/SettingTypes";
import CameraCalibrationInfoCard from "@/components/cameras/CameraCalibrationInfoCard.vue";
import { useTheme } from "vuetify";

const theme = useTheme();

const generating = ref(false);
const preliminaryCalibration = ref<CameraCalibrationResult | null>(null);
const showDialog = ref(false);

const hasSnapshots = computed(() => useStateStore().calibrationData.imageCount > 0);

const calibrationVideoFormat = computed<VideoFormat | undefined>(() => {
  const format =
    useCameraSettingsStore().currentCameraSettings.validVideoFormats[useStateStore().calibrationData.videoFormatIndex];
  if (!format || !preliminaryCalibration.value) return format;

  const cal = preliminaryCalibration.value;
  const intrinsics = cal.cameraIntrinsics.data;
  const fx = intrinsics[0];
  const fy = intrinsics[4];
  const { width, height } = cal.resolution;

  const fov = (sensorSize: number, focalLength: number): number =>
    (2 * Math.atan2(sensorSize, 2 * focalLength) * 180) / Math.PI;

  const meanErrors =
    cal.meanErrors ??
    cal.observations?.map((obs) => {
      const used = obs.reprojectionErrors.filter((_, i) => obs.cornersUsed[i]);
      if (used.length === 0) return NaN;
      return used.reduce((sum, pt) => sum + Math.hypot(pt.x, pt.y), 0) / used.length;
    });

  return {
    ...format,
    resolution: cal.resolution,
    mean: meanErrors?.length ? meanErrors.reduce((a, b) => a + b, 0) / meanErrors.length : undefined,
    horizontalFOV: fov(width, fx),
    verticalFOV: fov(height, fy),
    diagonalFOV: fov(Math.hypot(width, height), Math.hypot(fx, fy))
  };
});

const generatePreliminaryCalibration = async () => {
  generating.value = true;
  try {
    const response = await useCameraSettingsStore().generatePreliminaryCalibration();
    preliminaryCalibration.value = response.data;
    useStateStore().showSnackbarMessage({
      color: "success",
      message: "Preliminary calibration generated! Review it, then finish calibration to save."
    });
  } catch (error) {
    preliminaryCalibration.value = null;
    const responseData = axios.isAxiosError(error) ? error.response?.data : undefined;
    useStateStore().showSnackbarMessage({
      color: "error",
      message:
        typeof responseData === "string" && responseData ? responseData : "Failed to generate preliminary calibration"
    });
  } finally {
    generating.value = false;
  }
};
</script>

<template>
  <div>
    <v-card-subtitle class="pa-0 opacity-100">
      Generate a calibration from the snapshots collected so far to preview the results.
    </v-card-subtitle>
    <div class="d-flex pt-3">
      <v-col cols="6" class="pa-0 pr-2">
        <v-btn
          size="small"
          block
          color="buttonActive"
          :variant="theme.global.current.value.dark ? 'outlined' : 'elevated'"
          :loading="generating"
          :disabled="!hasSnapshots"
          @click="generatePreliminaryCalibration"
        >
          <v-icon start class="calib-btn-icon" size="large">mdi-calculator</v-icon>
          <span class="calib-btn-label">Generate Preliminary</span>
        </v-btn>
      </v-col>
      <v-col cols="6" class="pa-0 pl-2">
        <v-btn
          size="small"
          block
          color="buttonPassive"
          :variant="theme.global.current.value.dark ? 'outlined' : 'elevated'"
          :disabled="!preliminaryCalibration"
          @click="showDialog = true"
        >
          <v-icon start class="calib-btn-icon" size="large">mdi-eye-outline</v-icon>
          <span class="calib-btn-label">View Preliminary</span>
        </v-btn>
      </v-col>
    </div>
  </div>
  <v-dialog v-model="showDialog" width="80em">
    <CameraCalibrationInfoCard
      v-if="calibrationVideoFormat && preliminaryCalibration"
      :video-format="calibrationVideoFormat"
      :calibration="preliminaryCalibration"
    />
  </v-dialog>
</template>

<style scoped>
@media only screen and (max-width: 512px) {
  .calib-btn-icon {
    margin: 0 !important;
  }
  .calib-btn-label {
    display: none;
  }
}
</style>
