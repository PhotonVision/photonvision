<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, inject } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { PlaceholderCameraSettings } from "@/types/SettingTypes";
import { getResolutionString } from "@/lib/PhotonUtils";

const formatUrl = (port) => `http://${inject("backendHostname")}:${port}/stream.mjpg`;
const host = inject<string>("backendHost");
const activateCamera = (cameraUniqueName: string) => {
  const url = new URL(`http://${host}/api/utils/assignUnmatchedCamera`);
  url.searchParams.set("uniqueName", cameraUniqueName);

  fetch(url.toString(), {
    method: "POST"
  });
};
const deactivateCamera = (cameraUniqueName: string) => {
  const url = new URL(`http://${host}/api/utils/unassignCamera`);
  url.searchParams.set("uniqueName", cameraUniqueName);

  fetch(url.toString(), {
    method: "POST"
  });
};

const usbInfoForCam = (uniqueName) => {
  return useStateStore().vsmState.allConnectedCameras.find((it) => it.uniqueName === uniqueName);
};

const unmatchedCameras = computed(() => {
  const allCameras = useStateStore().vsmState.allConnectedCameras;
  const used = (uniqueName: string) =>
    useStateStore().vsmState.activeCameras.filter((it) => it.uniqueName == uniqueName).length > 0 ||
    useStateStore().vsmState.disabledCameras.filter((it) => it.uniqueName == uniqueName).length > 0;
  return allCameras.filter((it) => !used(it.uniqueName));
});
</script>

<template>
  <div class="pa-3">
    <v-card dark class="mb-3 pr-6 pb-3" style="background-color: #006492">
      <v-card-title style="display: flex; justify-content: space-between">
        <span class="ml-3">Active Vision Modules</span>
      </v-card-title>

      <v-banner
        class="pa-2 ma-3"
        v-if="
          useCameraSettingsStore().cameras.length === 0 ||
          (useCameraSettingsStore().cameras.length === 1 &&
            useCameraSettingsStore().cameras[0] == PlaceholderCameraSettings)
        "
        rounded
        dark
        color="red"
        >No VisionModules created :( Activate a camera to get started!</v-banner
      >
      <v-row class="ml-3">
        <v-card
          dark
          class="camera-card pa-4 mb-4 mr-3"
          v-for="(module, index) in useCameraSettingsStore().cameras"
          v-if="module !== PlaceholderCameraSettings"
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
                <tr>
                  <td>USB Info</td>
                  <td>
                    Product {{ module.cameraQuirks.baseName }} 
                    VID {{ module.cameraQuirks.usbVid }} 
                    PID {{ module.cameraQuirks.usbPid }}
                  </td>
                </tr>
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
                  <td>Via [USB, CSI, totally bjork] -- TODO implement</td>
                </tr>
                <tr>
                  <td>Calibrations</td>
                  <td>{{ module.completeCalibrations.map(it => getResolutionString(it.resolution)).join(", ") || "Not calibrated" }}</td>
                </tr>
                <tr>
                  <td>Actions</td>
                  <td>
                    <v-btn class="ma-2" @click="deactivateCamera(module.uniqueName)" color="primary">Deactivate</v-btn>
                  </td>
                </tr>
              </tbody>
            </v-simple-table>
          </v-card-text>
        </v-card>
      </v-row>
    </v-card>

    <v-card dark class="mb-3 pr-6 pb-3" style="background-color: #006492">
      <v-card-title>
        <span> Disabled Vision Modules </span>
      </v-card-title>

      <span class="ml-3" v-if="useStateStore().vsmState.disabledCameras.length === 0"
        >No unassigned cameras to show :)</span
      >

      <v-row class="ml-3 mb-0">
        <v-card
          dark
          class="camera-card pa-4 mb-4 mr-3"
          v-for="(camera, index) in useStateStore().vsmState.disabledCameras"
          :value="index"
        >
          <v-card-title class="pb-8">{{ camera.nickname }}</v-card-title>
          <v-card-text>
            <v-simple-table dense height="100%" class="camera-card-table mt-2">
              <tbody>
                <tr>
                  <td>Product Name</td>
                  <td>
                    {{ usbInfoForCam(camera.uniqueName)?.name }}
                  </td>
                </tr>
                <tr>
                  <td>USB Info</td>
                  <td>VID {{ camera.cameraQuirks.usbVid }} PID {{ camera.cameraQuirks.usbPid }}</td>
                </tr>
                <tr>
                  <td>Type</td>
                  <td>{{ camera.isCSICamera ? "CSI" : "USB" }} Camera</td>
                </tr>
                <tr>
                  <td>Path(s)</td>
                  <td>
                    <span
                      v-for="(path, idx) in [usbInfoForCam(camera.uniqueName)?.path].concat(
                        usbInfoForCam(camera.uniqueName)?.otherPaths
                      )"
                      :key="idx"
                      style="display: block"
                    >
                      {{ path }}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td>Pipelines</td>
                  <td>{{ camera.pipelineNicknames.join(", ") }}</td>
                </tr>
                <tr>
                  <td>Calibrations</td>
                  <td> {{ camera.calibrations.map(it => getResolutionString(it.resolution)).join(", ") || "Not calibrated" }}</td>
                </tr>
                <tr>
                  <td>Actions</td>
                  <td>
                    <v-btn class="ma-2" @click="activateCamera(camera.uniqueName)" color="primary">Activate</v-btn>
                  </td>
                </tr>
              </tbody>
            </v-simple-table>
          </v-card-text>
        </v-card>
      </v-row>
    </v-card>

    <!-- Show this card if there are unmatched discovered cameras, or if no cameras have been matched -->
    <v-card dark class="mb-3 pr-6 pb-3" style="background-color: #006492">
      <v-card-title>
        <span> Unassigned Cameras </span>
      </v-card-title>

      <span class="ml-3" v-if="unmatchedCameras.length === 0">No unassigned cameras to show :)</span>

      <v-row class="ml-3 mb-0">
        <v-banner
          v-if="unmatchedCameras.length === 0 && useCameraSettingsStore().cameras.length === 0"
          rounded
          dark
          color="red"
          >No cameras connected :( Plug one in to get started!</v-banner
        >
        <v-card dark class="camera-card pa-4 mb-4 mr-3" v-for="(camera, index) in unmatchedCameras" :value="index">
          <v-card-title class="pb-8">{{ camera.name }}</v-card-title>
          <v-card-text>
            <v-simple-table dense height="100%" class="camera-card-table mt-2">
              <tbody>
                <tr>
                  <td>Product Name</td>
                  <td>
                    {{ camera.name }}
                  </td>
                </tr>
                <tr>
                  <td>Type</td>
                  <td>
                    {{ camera.type }}
                  </td>
                </tr>
                <tr>
                  <td>Path(s)</td>
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
                  <td>Actions</td>
                  <td>
                    <v-btn class="ma-2" @click="activateCamera(camera.uniqueName)" color="primary">Activate</v-btn>
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
