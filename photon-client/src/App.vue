<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import { inject } from "vue";
import PhotonSidebar from "@/components/app/photon-sidebar.vue";
import PhotonLogView from "@/components/app/photon-log-view.vue";
import PhotonErrorSnackbar from "@/components/app/photon-error-snackbar.vue";

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
  background: #232c37;
}

::-webkit-scrollbar-thumb {
  background-color: #ffd843;
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background-color: #e4c33c;
}

.main-container {
  background-color: #232c37;
  padding: 0 !important;
}

.v-overlay__scrim {
  background-color: #202020;
}

#title {
  color: #ffd843;
}
div.v-layout {
  overflow: unset !important;
}
</style>
