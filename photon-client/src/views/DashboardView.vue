<script setup lang="ts">
import { computed, ref } from "vue";
import CamerasCard from "@/components/dashboard/CamerasCard.vue";
import CameraAndPipelineSelectCard from "@/components/dashboard/CameraAndPipelineSelectCard.vue";
import StreamConfigCard from "@/components/dashboard/StreamConfigCard.vue";
import PipelineConfigCard from "@/components/dashboard/ConfigOptions.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useTheme } from "vuetify";

const theme = useTheme();
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
  <v-container class="pa-3" fluid>
    <v-alert
      v-if="arducamWarningShown"
      class="mb-3"
      color="error"
      density="compact"
      icon="mdi-alert-circle-outline"
      :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
    >
      <span>
        Arducam camera detected! Please configure the camera model in the <a href="#/cameras">Camera tab</a>!
      </span>
    </v-alert>
    <v-alert
      v-if="conflictingHostnameShown"
      class="mb-3"
      color="error"
      density="compact"
      icon="mdi-alert-circle-outline"
      :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
    >
      <span>
        Conflicting hostname detected! Please change the hostname in the <a href="#/settings">Settings tab</a>!
      </span>
    </v-alert>
    <v-alert
      v-if="fpsLimitWarningShown"
      class="mb-3"
      color="error"
      density="compact"
      icon="mdi-alert-circle-outline"
      :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
    >
      <span
        >One or more cameras have an FPS limit set! This may cause performance issues. Check your logs for more
        information.
      </span>
    </v-alert>
    <v-alert
      v-if="conflictingCameraShown"
      class="mb-3"
      color="error"
      density="compact"
      icon="mdi-alert-circle-outline"
      :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
    >
      <span
        >Conflicting camera name(s) detected! Please change the name(s) of
        {{ useSettingsStore().general.conflictingCameras }}!
      </span>
    </v-alert>
    <v-banner
      v-if="cameraMismatchWarningShown"
      v-model="cameraMismatchWarningShown"
      rounded
      color="error"
      dark
      class="mb-3"
      icon="mdi-alert-circle-outline"
    >
      <span
        >Camera Mismatch Detected! Visit the <a href="#/cameraConfigs">Camera Matching</a> page for more information.
        Note: Camera matching is done by USB port. Ensure cameras are plugged into the same USB ports as when they were
        activated.
      </span>
    </v-banner>
    <v-row no-gutters>
      <v-col cols="12" class="pb-3 pr-lg-3" lg="8" align-self="stretch">
        <CamerasCard v-model="cameraViewType" />
      </v-col>
      <v-col cols="12" class="pb-3" lg="4" style="display: flex; flex-direction: column" align-self="stretch">
        <CameraAndPipelineSelectCard />
        <StreamConfigCard v-model="cameraViewType" />
      </v-col>
    </v-row>
    <PipelineConfigCard />

    <!-- TODO - not sure this belongs here -->
    <!-- Need v-model to allow the dialog to be dismissed and v-if to only display when cameras need configuration -->
    <v-dialog
      v-if="useCameraSettingsStore().needsCameraConfiguration"
      v-model="showCameraSetupDialog"
      max-width="800"
      dark
    >
      <v-card flat color="surface">
        <v-card-title>Set up some cameras to get started!</v-card-title>
        <v-card-text class="pt-0">
          No cameras activated - head to the
          <router-link to="/cameraConfigs">camera matching tab</router-link> to set some up!
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
a:link {
  color: rgb(var(--v-theme-buttonActive));
  background-color: transparent;
  text-decoration: none;
}
a:visited {
  color: rgb(var(--v-theme-buttonActive));
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
