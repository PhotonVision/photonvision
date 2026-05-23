<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { AutoReconnectingWebsocket } from "@/lib/AutoReconnectingWebsocket";
import { inject, onBeforeMount } from "vue";
import { restoreThemeConfig } from "@/lib/ThemeManager";
import { ConfigProvider } from "reka-ui";
const is_demo = import.meta.env.MODE === "demo";
const backendHost = inject<string>("backendHost");
if (!is_demo) {
  const websocket = new AutoReconnectingWebsocket(
    `ws://${backendHost}/websocket_data`,
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

onBeforeMount(() => {
  restoreThemeConfig();
});
</script>

<template>
  <ConfigProvider :scroll-body="false">
    <div class="bg-pv-background text-pv-on-surface flex h-full w-full flex-row">
      <photon-sidebar />
      <main class="flex min-w-0 flex-1 flex-col overflow-auto">
        <div class="flex-1">
          <router-view />
        </div>
      </main>
      <photon-log-view />
      <photon-error-snackbar />
    </div>
  </ConfigProvider>
</template>
