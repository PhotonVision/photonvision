<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import { inject } from "vue";
import PhotonSidebar from "@/components/app/photon-sidebar.vue";
import PhotonLogView from "@/components/app/photon-log-view.vue";
import PhotonErrorSnackbar from "@/components/app/photon-error-snackbar.vue";

const websocket = new AutoReconnectingWebsocket(
    `ws://${inject("backendHost")}/websocket_data`,
    () => {
      useStateStore().$patch({ backendConnected: true });
    },
    (data) => {
      if(data.log !== undefined) {
        useStateStore().addLogFromWebsocket(data.log);
      }
      if(data.settings !== undefined) {
        useSettingsStore().updateGeneralSettingsFromWebsocket(data.settings);
      }
      if(data.cameraSettings !== undefined) {
        useCameraSettingsStore().updateCameraSettingsFromWebsocket(data.cameraSettings);
      }
      if(data.ntConnectionInfo !== undefined) {
        useStateStore().updateNTConnectionStatusFromWebsocket(data.ntConnectionInfo);
      }
      if(data.metrics !== undefined) {
        useSettingsStore().updateMetricsFromWebsocket(data.metrics);
      }
      if(data.updatePipelineResult !== undefined) {
        useStateStore().updatePipelineResultsFromWebsocket(data.updatePipelineResult);
      }
      if(data.mutatePipelineSettings !== undefined && data.cameraIndex !== undefined) {
        useCameraSettingsStore().changePipelineSettingsInStore(data.mutatePipelineSettings, data.cameraIndex);
      }
      if(data.calibrationData !== undefined) {
        useStateStore().updateCalibrationStateValuesFromWebsocket(data.calibrationData);
      }
    },
    () => {
      useStateStore().$patch({ backendConnected: false });
    }
);

useStateStore().$patch({ websocket: websocket });
</script>

<template>
  <v-app>
    <photon-sidebar />
    <v-main>
      <v-container
        class="main-container"
        fluid
        fill-height
      >
        <v-layout>
          <v-flex>
            <router-view />
          </v-flex>
        </v-layout>
      </v-container>
    </v-main>

    <photon-log-view />
    <photon-error-snackbar />
  </v-app>
</template>

<style lang="scss">
@import 'vuetify/src/styles/settings/_variables';

@media #{map-get($display-breakpoints, 'md-and-down')} {
  html {
    font-size: 14px !important;
  }
}

.main-container {
  background-color: #232c37;
  padding: 0 !important;
}

#title {
  color: #ffd843;
}
</style>
