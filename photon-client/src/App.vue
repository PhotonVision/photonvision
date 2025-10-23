<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import { inject, onBeforeMount } from "vue";
import PhotonSidebar from "@/components/app/photon-sidebar.vue";
import PhotonLogView from "@/components/app/photon-log-view.vue";
import PhotonErrorSnackbar from "@/components/app/photon-error-snackbar.vue";
import { useTheme } from "vuetify";
import { restoreThemeConfig } from "@/lib/ThemeManager";

const is_demo = import.meta.env.MODE === "demo";
if (!is_demo) {
  const websocket = new AutoReconnectingWebsocket(
    `ws://${inject("backendHost")}/websocket_data`,
    () => {
      useStateStore().$patch({ backendConnected: true });
    },
    (data) => {
      if (data.log !== undefined) {
        useStateStore().addLogFromWebsocket(data.log);
      }
      if (data.settings !== undefined) {
        useSettingsStore().updateGeneralSettingsFromWebsocket(data.settings);
      }
      if (data.cameraSettings !== undefined) {
        useCameraSettingsStore().updateCameraSettingsFromWebsocket(data.cameraSettings);
      }
      if (data.ntConnectionInfo !== undefined) {
        useStateStore().updateNTConnectionStatusFromWebsocket(data.ntConnectionInfo);
      }
      if (data.metrics !== undefined) {
        useSettingsStore().updateMetricsFromWebsocket(data.metrics);
      }
      if (data.updatePipelineResult !== undefined) {
        useStateStore().updateBackendResultsFromWebsocket(data.updatePipelineResult);
      }
      if (data.mutatePipelineSettings !== undefined && data.cameraUniqueName !== undefined) {
        useCameraSettingsStore().changePipelineSettingsInStore(data.mutatePipelineSettings, data.cameraUniqueName);
      }
      if (data.calibrationData !== undefined) {
        useStateStore().updateCalibrationStateValuesFromWebsocket(data.calibrationData);
      }
      if (data.visionSourceManager !== undefined) {
        useStateStore().updateDiscoveredCameras(data.visionSourceManager);
      }
    },
    () => {
      useStateStore().$patch({ backendConnected: false });
    }
  );
  useStateStore().$patch({ websocket: websocket });
}

const theme = useTheme();
onBeforeMount(() => {
  restoreThemeConfig(theme);
});
</script>

<template>
  <v-app>
    <photon-sidebar />
    <v-main>
      <v-container class="main-container" fluid fill-height>
        <v-layout>
          <v-container class="align-start pa-0 ma-0" fluid>
            <router-view />
          </v-container>
        </v-layout>
      </v-container>
    </v-main>

    <photon-log-view />
    <photon-error-snackbar />
  </v-app>
</template>

<style lang="scss">
@use "@/assets/styles/settings";
@use "@/assets/styles/variables";
@use "sass:map";

@media #{map.get(settings.$display-breakpoints, 'md-and-down')} {
  html {
    font-size: 14px !important;
  }
}

/* Custom scrollbar styles */
::-webkit-scrollbar {
  width: 12px;
}

::-webkit-scrollbar-track {
  background: rgb(var(--v-theme-background));
}

::-webkit-scrollbar-thumb {
  background-color: rgb(var(--v-theme-accent));
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background-color: rgb(var(--v-theme-primary));
}

.main-container {
  padding: 0 !important;
}

.v-overlay__scrim {
  background-color: #111111;
}

div.v-layout {
  overflow: unset !important;
}
</style>
