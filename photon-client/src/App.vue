<script lang="ts" setup>
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import { inject } from "vue";
import PhotonSidebar from "@/components/app/photon-sidebar.vue";
import PhotonLogViewer from "@/components/app/photon-log-viewer.vue";
import PhotonAlertLayout from "@/components/app/photon-alert-layout.vue";

const isDemo = import.meta.env.MODE === "demo";
if (!isDemo) {
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
      if (data.mutatePipelineSettings !== undefined && data.cameraIndex !== undefined) {
        useCameraSettingsStore().changePipelineSettingsInStore(data.mutatePipelineSettings, data.cameraIndex);
      }
      if (data.calibrationData !== undefined) {
        useStateStore().updateCalibrationStateValuesFromWebsocket(data.calibrationData);
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
  <photon-alert-layout>
    <v-app>
      <photon-sidebar />
      <v-main>
        <v-container class="main-container" fill-height fluid>
          <v-layout>
            <router-view />
          </v-layout>
        </v-container>
      </v-main>
      <photon-log-viewer />
    </v-app>
  </photon-alert-layout>
</template>

<style lang="scss">
@import "vuetify/settings";

.main-container {
  padding: 0 !important;
}

#title {
  color: #ffd843;
}

body {
  font-family: "Prompt", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

html {
  @media #{map-get($display-breakpoints, 'md-and-down')} {
    font-size: 14px !important;
  }
}
</style>
