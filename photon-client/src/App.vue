<script lang="ts" setup>
import { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import { inject } from "vue";
import PhotonSidebar from "@/components/app/photon-sidebar.vue";
import PhotonLogViewer from "@/components/app/photon-log-viewer.vue";
import PhotonProvider from "@/components/app/photon-provider.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";

const isDemo = import.meta.env.MODE === "demo";
if (!isDemo) {
  useClientStore().websocket = new AutoReconnectingWebsocket(
    `ws://${inject("backendHost")}/websocket_data`,
    () => {
      useClientStore().backendConnected = true;
    },
    (data) => {
      if (data.instanceConfig) {
        useServerStore().updateInstanceConfigFromWebsocket(data.instanceConfig);
      }
      if (data.settings) {
        useServerStore().updateSettingsFromWebsocket(data.settings);
      }
      if (data.activeATFL) {
        useServerStore().updateATFLFromWebsocket(data.activeATFL);
      }
      if (data.cameras) {
        useServerStore().updateCamerasFromWebsocket(data.cameras);
      }
      if (data.log) {
        useClientStore().addLogFromWebsocket(data.log);
      }
      if (data.ntConnectionInfo) {
        useClientStore().updateNTConnectionStatusFromWebsocket(data.ntConnectionInfo);
      }
      if (data.metrics) {
        useServerStore().updatePlatformMetricsFromWebsocket(data.metrics);
      }
      if (data.updatePipelineResult) {
        useClientStore().updateBackendResultsFromWebsocket(data.updatePipelineResult);
      }
      if (data.networkInfo) {
        // Ignore this cause why do we even have it?
      }
      if (data.pipelineSettingMutation) {
        const { cameraIndex, pipelineIndex, mutation } = data.pipelineSettingMutation;
        useServerStore().updatePipelineSettings(cameraIndex, pipelineIndex, mutation, true, false);
      }
    },
    () => {
      useClientStore().backendConnected = false;
    }
  );
}
</script>

<template>
  <v-app>
    <photon-provider>
      <photon-sidebar />
      <v-main>
        <v-container class="align-start pa-0 ma-0" fluid>
          <router-view />
        </v-container>
      </v-main>
      <photon-log-viewer />
    </photon-provider>
  </v-app>
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
