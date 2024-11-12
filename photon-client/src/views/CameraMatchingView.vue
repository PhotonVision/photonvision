<script setup lang="ts">
import MetricsCard from "@/components/settings/MetricsCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { inject } from "vue";
import { useStateStore } from "@/stores/StateStore";

const formatUrl = (port) => `http://${inject("backendHostname")}:${port}/stream.mjpg`;
</script>

<template>
  <div class="pa-3">
    <v-card dark class="mb-3 pr-6 pb-3" style="background-color: #006492">
      <v-card-title style="display: flex; justify-content: space-between">
        <span class="ml-3">Active Vision Modules</span>
      </v-card-title>

      <v-row class="ml-3">
        <v-card
          dark
          class="camera-card pa-4 mb-4 mr-3"
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
                  <td>
                    {{ useStateStore().backendResults[index].sequenceID }} ({{
                      useStateStore().backendResults[index].fps
                    }}
                    FPS)
                  </td>
                </tr>
                <tr>
                  <td>Connected?</td>
                  <td>Via [USB, CSI, totally bjork]</td>
                </tr>
              </tbody>
            </v-simple-table>
          </v-card-text>
        </v-card>
      </v-row>
    </v-card>
    <v-card dark class="mb-3 pr-6 pb-3" style="background-color: #006492">
      <v-card-title>
        <span> USB Cameras </span>
      </v-card-title>

      <v-row class="ml-3">
        <v-card
          dark
          class="camera-card pa-4 mb-4 mr-3"
          v-for="(camera, index) in useSettingsStore().visionSourceManagerState.knownCameras"
          :value="index"
        >
          <v-card-title class="pb-8">{{ camera.name }}</v-card-title>
          <v-card-text>
            <v-simple-table dense height="100%" class="camera-card-table mt-2">
              <tbody>
                <tr>
                  <td>USB Product String Descriptor</td>
                  <td>
                    {{ camera.name }}
                  </td>
                </tr>
                <tr>
                  <td>USB Vendor ID</td>
                  <td>0x{{ camera.vendorId.toString(16).padStart(4, "0") }}</td>
                </tr>
                <tr>
                  <td>USB Product ID</td>
                  <td>0x{{ camera.productId.toString(16).padStart(4, "0") }}</td>
                </tr>
                <tr>
                  <td>Type</td>
                  <td>
                    {{ camera.cameraType }}
                  </td>
                </tr>
                <tr>
                  <td>USB Path(s)</td>
                  <td>
                    <span
                      v-for="(path, idx) in [camera.path].concat(camera.otherPaths)"
                      :key="idx"
                      style="display: block"
                    >
                      {{ path }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td>Device Number</td>
                  <td>
                    {{ camera.dev }}
                  </td>
                </tr>
              </tbody>
            </v-simple-table>
          </v-card-text>
        </v-card>
      </v-row>
    </v-card>
  </div>
</template>

<style scoped>
.camera-card {
  background-color: #005281 !important;
}
.camera-card-table {
  background-color: #b49b0d !important;
}
</style>
