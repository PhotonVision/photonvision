<script setup lang="ts">
import MetricsCard from "@/components/settings/MetricsCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { inject } from "vue";

const formatUrl = (port) => `http://${inject("backendHostname")}:${port}/stream.mjpg`;
</script>

<template>
  <div class="pa-3">
    <v-card dark class="mb-3 pr-6 pb-3" style="background-color: #006492">
      <v-card-title style="display: flex; justify-content: space-between">
        <span>Camera Matching</span>
      </v-card-title>
      <span class="ml-3">Active Cameras</span>
      <v-row class="ml-3 mt-3">
        <v-col cols="6">
          <v-card
            dark
            class="camera-card pa-4"
            v-for="(module, index) in useCameraSettingsStore().cameras"
            :value="index"
          >
            <v-card-title class="pb-8">{{ module.nickname }}</v-card-title>
            <v-card-text>
              <v-simple-table dense height="100%" class="camera-card-table mt-2">
                <tbody>
                  <tr>
                    <td>Matched Path</td>
                    <td>
                      {{ module.cameraPath }}
                    </td>
                  </tr>
                  <tr>
                    <td>Streams:</td>
                    <td>
                      <a :href="formatUrl(module.stream.inputPort)" target="_blank"> Input Stream </a>/<a
                        :href="formatUrl(module.stream.outputPort)"
                        target="_blank"
                      >
                        Output Stream
                      </a>
                    </td>
                  </tr>
                  <tr>
                    <td>Pipelines</td>
                    <td>{{ module.pipelineNicknames.join(", ") }}</td>
                  </tr>
                  <tr>
                    <td>Frames Processed</td>
                    <td>todo</td>
                  </tr>
                  <tr>
                    <td>Connected?</td>
                    <td>Via [USB, CSI, totally bjork]</td>
                  </tr>
                </tbody>
              </v-simple-table>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-divider style="margin: 12px 0" />

      <v-row class="pt-2 pa-4 ma-0 ml-5 pb-1">
        <span> USB Cameras </span>
        <span>
          Matched cameras: {{ useSettingsStore().visionSourceManagerState.knownCameras }} <br />
          Unmatched cameras: {{ useSettingsStore().visionSourceManagerState.unmatchedLoadedConfigs }}
        </span>
      </v-row>
    </v-card>
  </div>
</template>

<style scoped>
.camera-card {
  background-color: #005281 !important;
}
.camera-card-table {
  background-color: #006492 !important;
}
</style>
