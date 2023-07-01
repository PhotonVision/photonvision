<script setup lang="ts">
import {useStateStore} from "@/stores/StateStore";
import {useSettingsStore} from "@/stores/settings/GeneralSettingsStore";
import {useCameraSettingsStore} from "@/stores/settings/CameraSettingsStore";
import {AutoReconnectingWebsocket} from "@/lib/AutoReconnectingWebsocket";
import {inject, onMounted} from "vue";
import PhotonSidebar from "@/components/app/photon-sidebar.vue";
import PhotonLogView from "@/components/app/photon-log-view.vue";

onMounted(() => {
  const stateStore = useStateStore();
  const cameraSettingsStore = useCameraSettingsStore();
  const settingsStore = useSettingsStore();

  const websocket = new AutoReconnectingWebsocket(
      `ws://${inject("backendAddress")}/websocket_data`,
      () => {
        stateStore.$patch({ backendConnected: true });
      },
      (data) => {
        if(data.log !== undefined) {
          stateStore.addLogFromWebsocket(data.log);
        }
        if(data.settings !== undefined) {
          settingsStore.updateGeneralSettingsFromWebsocket(data.settings);
        }
        if(data.cameraSettings !== undefined) {
          cameraSettingsStore.updateCameraSettingsFromWebsocket(data.cameraSettings);
        }
        if(data.ntConnectionInfo !== undefined) {
          stateStore.updateNTConnectionStatusFromWebsocket(data.ntConnectionInfo);
        }
        if(data.metrics !== undefined) {
          settingsStore.updateMetricsFromWebsocket(data.metrics);
        }
        if(data.updatePipelineResult !== undefined) {
          stateStore.updatePipelineResultsFromWebsocket(data.updatePipelineResult);
        }
        if(data.calibrationData !== undefined) {
          stateStore.updateCalibrationStateValuesFromWebsocket(data.calibrationData);
        }
      },
      () => {
        stateStore.$patch({ backendConnected: false });
      }
  );

  stateStore.$patch({ websocket: websocket });
});
</script>

<template>
  <v-app>
    <photon-sidebar/>
    <v-main>
      <v-container
          fluid
          fill-height
      >
        <v-layout>
          <v-flex>
            <router-view @switch-to-cameras="() => {}" />
          </v-flex>
        </v-layout>
      </v-container>
    </v-main>

    <photon-log-view/>
  </v-app>
</template>

<style lang="scss">
@import 'vuetify/src/styles/settings/_variables';

@media #{map-get($display-breakpoints, 'md-and-down')} {
  html {
    font-size: 14px !important;
  }
}

.container {
  background-color: #232c37;
  padding: 0 !important;
}

#title {
  color: #ffd843;
}
</style>
