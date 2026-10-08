<script setup lang="ts">
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useTheme } from "vuetify";

const theme = useTheme();

const showAutoCalibrationHint = computed(
  () => useCameraSettingsStore().isCalibrationMode && useStateStore().calibrationData.autoCalibrate
);

const calibrationAlertState = computed<"missing" | "moved" | "tooClose">(() => {
  const movedFarEnough = useStateStore().calibrationData.movedFarEnough;

  if (movedFarEnough === null) {
    return "missing";
  }

  if (movedFarEnough) {
    return "moved";
  }

  return "tooClose";
});

const value = defineModel<number[]>({ required: true });

const bypassVal = computed<boolean>({
  get: () => useStateStore().bypassMinCalibrationImages,
  set: (v) => (useStateStore().bypassMinCalibrationImages = v)
});
const minCount = computed(() => (bypassVal.value ? 10 : 100));
const hasEnoughImages = computed(() => useStateStore().calibrationData.imageCount >= minCount.value);

const fpsTooLow = computed<boolean>(() => {
  const currFPS = useStateStore().currentPipelineResults?.fps || 0;
  const targetFPS = useCameraSettingsStore().currentVideoFormat?.fps || 0;
  const driverMode = useCameraSettingsStore().isDriverMode;
  const gpuAccel = useSettingsStore().general.gpuAcceleration !== undefined;
  const isReflective = useCameraSettingsStore().currentPipelineSettings.pipelineType === PipelineType.Reflective;

  return currFPS - targetFPS < -5 && currFPS !== 0 && !driverMode && gpuAccel && isReflective;
});
</script>

<template>
  <v-card
    id="camera-settings-camera-view-card"
    class="camera-settings-camera-view-card rounded-12"
    color="surface"
    dark
  >
    <v-card-title class="justify-space-between align-content-center pt-0 pb-0">
      <div class="d-flex flex-wrap align-center pt-4 pb-4">
        <span class="mr-4" style="white-space: nowrap"> Cameras </span>
        <v-chip
          v-if="useCameraSettingsStore().currentCameraSettings.isConnected"
          label
          :color="fpsTooLow ? 'error' : 'transparent'"
          style="font-size: 1rem; padding: 0; margin: 0"
        >
          <span
            class="pr-1"
            :style="{ color: fpsTooLow ? 'rgb(var(--v-theme-error))' : 'rgb(var(--v-theme-primary))' }"
          >
            &nbsp;{{ Math.round(useStateStore().currentPipelineResults?.fps || 0) }}&nbsp;FPS &ndash;
            {{ Math.min(Math.round(useStateStore().currentPipelineResults?.latency || 0), 9999) }} ms latency
          </span>
        </v-chip>
        <v-chip
          v-if="!useCameraSettingsStore().currentCameraSettings.isConnected"
          label
          color="red"
          variant="text"
          style="font-size: 1rem; padding: 0; margin: 0"
        >
          <span class="pr-1">Camera not connected</span>
        </v-chip>
        <v-chip
          v-if="useCameraSettingsStore().isFocusMode"
          label
          color="primary"
          variant="text"
          style="font-size: 1rem; padding: 0; margin: auto"
        >
          <span class="pr-1"> Focus: {{ Math.round(useStateStore().currentPipelineResults?.focus || 0) }} </span>
        </v-chip>
        <v-chip
          v-if="useCameraSettingsStore().isCalibrationMode"
          style="margin-inline: auto"
          :variant="theme.global.current.value.dark ? 'tonal' : 'elevated'"
          label
          :color="hasEnoughImages ? 'buttonPassive' : 'light-grey'"
        >
          {{ useStateStore().calibrationData.imageCount }} of at least
          {{ minCount }}
        </v-chip>
        <v-switch
          v-if="useCameraSettingsStore().isCalibrationMode"
          v-model="bypassVal"
          color="error"
          hide-details
          density="compact"
        >
          <template #label>
            <div class="bypass-label d-flex flex-column text-end">
              <tooltipped-label
                label="Bypass"
                tooltip="Bypass the minimum recommended amount of snapshots for a calibration. Should only be used for dev work or temporary tests not competitions. Still requires 10 images to calibrate."
              />
              <tooltipped-label
                label="minimum"
                tooltip="Bypass the minimum recommended amount of snapshots for a calibration. Should only be used for dev work or temporary tests not competitions. Still requires 10 images to calibrate."
              />
            </div>
          </template>
        </v-switch>
      </div>
    </v-card-title>
    <v-card-text class="stream-container">
      <div class="stream">
        <photon-camera-stream
          v-if="value.includes(0)"
          id="input-camera-stream"
          :camera-settings="useCameraSettingsStore().currentCameraSettings"
          stream-type="Raw"
          style="max-width: 100%"
        />
      </div>
      <div class="stream">
        <photon-camera-stream
          v-if="value.includes(1)"
          id="output-camera-stream"
          :camera-settings="useCameraSettingsStore().currentCameraSettings"
          stream-type="Processed"
          style="max-width: 100%"
        />
      </div>
    </v-card-text>
    <v-card-text v-if="showAutoCalibrationHint" class="pt-0 d-flex flex-column ga-2">
      <v-alert
        :type="'warning'"
        :disabled="calibrationAlertState !== 'missing'"
        variant="tonal"
        density="compact"
        icon="mdi-chessboard"
        :class="{ 'calibration-alert--inactive': calibrationAlertState !== 'missing' }"
      >
        No calibration board detected -- point the camera at the calibration board
      </v-alert>
      <v-alert
        :type="'success'"
        :disabled="calibrationAlertState !== 'moved'"
        variant="tonal"
        density="compact"
        icon="mdi-hand-back-right"
        :class="{ 'calibration-alert--inactive': calibrationAlertState !== 'moved' }"
      >
        Moved far enough -- hold still while the snapshot is taken
      </v-alert>
      <v-alert
        :type="'info'"
        :disabled="calibrationAlertState !== 'tooClose'"
        variant="tonal"
        density="compact"
        icon="mdi-arrow-expand"
        :class="{ 'calibration-alert--inactive': calibrationAlertState !== 'tooClose' }"
      >
        Move the calibration board farther from where the last snapshot was taken
      </v-alert>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.bypass-label {
  line-height: 1.15;
  white-space: nowrap;
}
.v-btn-toggle.fill {
  width: 100%;
}
.v-btn-toggle.fill > .v-btn {
  width: 50%;
  height: 100%;
}
th {
  width: 80px;
  text-align: center;
}

.stream-container {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.stream {
  display: flex;
  justify-content: center;
  width: 100%;
}

@media only screen and (min-width: 960px) {
  #camera-settings-camera-view-card {
    position: sticky;
    top: 12px;
  }
}
@media only screen and (min-width: 512px) and (max-width: 960px) {
  .stream-container {
    flex-wrap: nowrap;
    justify-content: center;
  }

  .stream {
    max-width: 50%;
  }
}
.calibration-alert--inactive {
  opacity: 0.45;
  filter: grayscale(0.8);
  pointer-events: none;
}

@media only screen and (max-width: 351px) {
  .mode-btn-icon {
    margin: 0 !important;
  }
  .mode-btn-label {
    display: none;
  }
}
</style>
