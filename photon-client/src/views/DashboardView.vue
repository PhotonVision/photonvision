<script setup lang="ts">
import { computed, ref } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import IconAlertCircleOutline from "~icons/mdi/alert-circle-outline";

import { PlaceholderCameraSettings } from "@/types/SettingTypes";

const cameraViewType = computed<number[]>({
  get: (): number[] => {
    // Only show the input stream in Color Picking Mode
    if (useStateStore().colorPickingMode) return [0];

    // Only show the output stream in Driver Mode or Calibration Mode or Focus Mode
    if (
      useCameraSettingsStore().isDriverMode ||
      useCameraSettingsStore().isCalibrationMode ||
      useCameraSettingsStore().isFocusMode
    )
      return [1];

    const ret: number[] = [];
    if (useCameraSettingsStore().currentPipelineSettings.inputShouldShow) {
      ret.push(0);
    }
    if (useCameraSettingsStore().currentPipelineSettings.outputShouldShow) {
      ret.push(1);
    }

    if (ret.length === 0) return [0];

    return ret;
  },
  set: (v) => {
    useCameraSettingsStore().changeCurrentPipelineSetting(
      {
        inputShouldShow: v.includes(0),
        outputShouldShow: v.includes(1)
      },
      true
    );
  }
});

const arducamWarningShown = computed<boolean>(() => {
  return Object.values(useCameraSettingsStore().cameras).some(
    (c) =>
      c.cameraQuirks?.quirks?.ArduCamCamera === true &&
      !(
        c.cameraQuirks?.quirks?.ArduOV2311Controls === true ||
        c.cameraQuirks?.quirks?.ArduOV9281Controls === true ||
        c.cameraQuirks?.quirks?.ArduOV9782Controls === true
      )
  );
});

const cameraMismatchWarningShown = computed<boolean>(() => {
  return (
    Object.values(useCameraSettingsStore().cameras)
      // Ignore placeholder camera
      .filter((camera) => camera !== PlaceholderCameraSettings)
      .some((camera) => camera.mismatch)
  );
});

const conflictingHostnameShown = computed<boolean>(() => {
  return useSettingsStore().general.conflictingHostname;
});

const conflictingCameraShown = computed<boolean>(() => {
  return useSettingsStore().general.conflictingCameras.length > 0;
});

const fpsLimitWarningShown = computed<boolean>(() => {
  return Object.values(useCameraSettingsStore().cameras).some((c) => c.fpsLimit > 0);
});

const showCameraSetupDialog = ref(useCameraSettingsStore().needsCameraConfiguration);
</script>

<template>
  <div class="w-full p-3">
    <pv-alert v-if="arducamWarningShown" class="mb-3" color="error" density="compact" :icon="IconAlertCircleOutline">
      <span>
        Arducam camera detected! Please configure the camera model in the <a href="#/cameras">Camera tab</a>!
      </span>
    </pv-alert>
    <pv-alert
      v-if="conflictingHostnameShown"
      class="mb-3"
      color="error"
      density="compact"
      :icon="IconAlertCircleOutline"
    >
      <span>
        Conflicting hostname detected! Please change the hostname in the <a href="#/settings">Settings tab</a>!
      </span>
    </pv-alert>
    <pv-alert v-if="fpsLimitWarningShown" class="mb-3" color="error" density="compact" :icon="IconAlertCircleOutline">
      <span
        >One or more cameras have an FPS limit set! This may cause performance issues. Check your logs for more
        information.
      </span>
    </pv-alert>
    <pv-alert v-if="conflictingCameraShown" class="mb-3" color="error" density="compact" :icon="IconAlertCircleOutline">
      <span
        >Conflicting camera name(s) detected! Please change the name(s) of
        {{ useSettingsStore().general.conflictingCameras }}!
      </span>
    </pv-alert>
    <pv-alert
      v-if="cameraMismatchWarningShown"
      v-model="cameraMismatchWarningShown"
      color="error"
      class="mb-3"
      :icon="IconAlertCircleOutline"
    >
      <span
        >Camera Mismatch Detected! Visit the <a href="#/cameraConfigs">Camera Matching</a> page for more information.
        Note: Camera matching is done by USB port. Ensure cameras are plugged into the same USB ports as when they were
        activated.
      </span>
    </pv-alert>
    <div class="flex flex-wrap">
      <div class="w-full lg:w-2/3 pb-3 lg:pr-3 self-stretch">
        <CamerasCard v-model="cameraViewType" />
      </div>
      <div class="w-full lg:w-1/3 pb-3 flex flex-col self-stretch">
        <CameraAndPipelineSelectCard />
        <StreamConfigCard v-model="cameraViewType" />
      </div>
    </div>
    <ConfigOptions />

    <!-- TODO - not sure this belongs here -->
    <!-- Need v-model to allow the dialog to be dismissed and v-if to only display when cameras need configuration -->
    <pv-dialog
      v-if="useCameraSettingsStore().needsCameraConfiguration"
      v-model="showCameraSetupDialog"
      :max-width="800"
    >
      <pv-card class="flex flex-col gap-2">
        <div class="text-lg font-semibold">Set up some cameras to get started!</div>
        <div class="pt-0">
          No cameras activated - head to the
          <router-link to="/cameraConfigs">camera matching tab</router-link> to set some up!
        </div>
      </pv-card>
    </pv-dialog>
  </div>
</template>

<style scoped>
a:link {
  color: var(--color-pv-button-active);
  background-color: transparent;
  text-decoration: none;
}
a:visited {
  color: var(--color-pv-button-active);
  background-color: transparent;
  text-decoration: none;
}
a:hover {
  background-color: transparent;
  text-decoration: underline;
}
a:active {
  background-color: transparent;
  text-decoration: none;
}
</style>
