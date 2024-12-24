<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { PlaceholderCameraSettings, PVCameraInfo, type UiCameraConfiguration } from "@/types/SettingTypes";
import { getResolutionString } from "@/lib/PhotonUtils";
import PvCameraInfoCard from "@/components/common/pv-camera-info-card.vue";

const formatUrl = (port) => `http://${inject("backendHostname")}:${port}/stream.mjpg`;
const host = inject<string>("backendHost");

const reactivateModule = (moduleUniqueName: string) => {
  const url = new URL(`http://${host}/api/utils/activateMatchedCamera`);
  url.searchParams.set("uniqueName", moduleUniqueName);

  fetch(url.toString(), {
    method: "POST"
  });
};
const activateCamera = (cameraInfo: PVCameraInfo) => {
  const url = new URL(`http://${host}/api/utils/assignUnmatchedCamera`);
  url.searchParams.set("cameraInfo", JSON.stringify(cameraInfo));

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

const uniquePathForCamera = (info: PVCameraInfo) => {
  if (info.PVUsbCameraInfo) {
    return info.PVUsbCameraInfo.uniquePath;
  }
  if (info.PVCSICameraInfo) {
    return info.PVCSICameraInfo.uniquePath;
  }
  if (info.PVFileCameraInfo) {
    return info.PVFileCameraInfo.uniquePath;
  }

  // TODO - wut
  return "";
};

/**
 * Find the PVCameraInfo currently occupying the same uniquepath as the the given module
 */
const getMatchedDevice = (module: UiCameraConfiguration) => {
  return (
    useStateStore().vsmState.allConnectedCameras.find(
      (it) => uniquePathForCamera(it) === uniquePathForCamera(module.matchedCameraInfo)
    ) || {
      PVFileCameraInfo: {
        name: "!",
        path: "!",
        uniquePath: "!"
      }
    }
  );
};

/**
 * Check if a module's matched camera's unique path is present in any of the list of currently connected cameras
 * @param cameraInfo: The camera info we want to know if is connected, by unique path
 */
const isCameraConnected = (cameraInfo: PVCameraInfo) => {
  return useStateStore()
    .vsmState.allConnectedCameras.map((it) => uniquePathForCamera(it))
    .includes(uniquePathForCamera(cameraInfo));
};

const unmatchedCameras = computed(() => {
  const activeVmPaths = useCameraSettingsStore().cameras.map((it) => uniquePathForCamera(it.matchedCameraInfo));
  const disabledVmPaths = useStateStore().vsmState.disabledConfigs.map((it) =>
    uniquePathForCamera(it.matchedCameraInfo)
  );

  return useStateStore().vsmState.allConnectedCameras.filter(
    (it) => !activeVmPaths.includes(uniquePathForCamera(it)) && !disabledVmPaths.includes(uniquePathForCamera(it))
  );
});

const activeVisionModules = computed(() => useCameraSettingsStore().cameras);
const disabledVisionModules = computed(() => useStateStore().vsmState.disabledConfigs);

let isExpanded = ref(false);
</script>

<template>
  <div class="pa-3">
    <v-card dark class="mb-3 pr-6 pb-3" color="primary">
      <v-card-title style="display: flex; justify-content: space-between">
        <span class="ml-3">Active Vision Modules</span>
      </v-card-title>

      <v-banner
        class="pa-2 ma-3"
        v-if="
          useCameraSettingsStore().cameras.length === 0 ||
          (useCameraSettingsStore().cameras.length === 1 &&
            JSON.stringify(useCameraSettingsStore().cameras[0]) === JSON.stringify(PlaceholderCameraSettings))
        "
        rounded
        dark
        color="red"
        >No vision modules created. Activate a camera to get started!</v-banner
      >
      <v-row
        class="ml-3"
        v-if="
          !(
            useCameraSettingsStore().cameras.length === 0 ||
            (useCameraSettingsStore().cameras.length === 1 &&
              JSON.stringify(useCameraSettingsStore().cameras[0]) === JSON.stringify(PlaceholderCameraSettings))
          )
        "
      >
        <v-col cols="12">
          <v-card
            dark
            class="camera-card pa-4 mb-4 mr-3"
            color="secondary"
            v-for="(module, index) in activeVisionModules"
            v-if="JSON.stringify(module) !== JSON.stringify(PlaceholderCameraSettings)"
            :value="index"
          >
            <v-card-title class="pb-8">{{ module.nickname }}</v-card-title>

            <v-card-text>
              <v-btn color="primary" @click="isExpanded = !isExpanded">
                <v-icon>{{ isExpanded ? "mdi-chevron-up" : "mdi-chevron-down" }} </v-icon>
                <span>Show Detailed Camera Info</span>
              </v-btn>
              <v-expand-transition>
                <v-row v-if="isExpanded">
                  <v-col cols="6">
                    <v-card color="primary">
                      <span>Saved camera info:</span>
                      <PvCameraInfoCard :camera="module.matchedCameraInfo" />
                    </v-card>
                  </v-col>
                  <v-col cols="6">
                    <v-card color="primary">
                      <span>Matched camera info:</span>
                      <PvCameraInfoCard :camera="getMatchedDevice(module)" class="m-2" />
                    </v-card>
                  </v-col>
                </v-row>
              </v-expand-transition>
              <v-row>
                <v-col cols="8">
                  <v-simple-table dark dense height="100%" class="camera-card-table mt-2">
                    <tbody>
                      <tr>
                        <td>Streams:</td>
                        <td>
                          <a :href="formatUrl(module.stream.inputPort)" target="_blank"> Input Stream </a> /
                          <a :href="formatUrl(module.stream.outputPort)" target="_blank" > Output Stream </a>
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
                        <td>{{ isCameraConnected(module.matchedCameraInfo) }}</td>
                      </tr>
                      <tr>
                        <td>Calibrations</td>
                        <td>
                          {{
                            module.completeCalibrations.map((it) => getResolutionString(it.resolution)).join(", ") ||
                            "Not calibrated"
                          }}
                        </td>
                      </tr>
                      <tr>
                        <td>Actions</td>
                        <td>
                          <v-btn class="ma-2 black--text" @click="deactivateCamera(module.uniqueName)" color="accent"
                            >Deactivate</v-btn
                          >
                        </td>
                      </tr>
                    </tbody>
                  </v-simple-table>
                </v-col>
                <v-col cols="4">
                  <photon-camera-stream
                    id="output-camera-stream"
                    stream-type="Processed"
                    style="width: 100%; height: auto"
                  />
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-card>

    <v-card dark class="mb-3 pr-6 pb-3" color="primary">
      <v-card-title>
        <span> Disabled Vision Modules </span>
      </v-card-title>

      <span class="ml-3" v-if="disabledVisionModules.length === 0">No unassigned cameras to show.</span>

      <v-row class="ml-3 mb-0" v-if="disabledVisionModules.length > 0">
        <v-card
          dark
          color="secondary"
          class="camera-card pa-4 mb-4 mr-3"
          v-for="(module, index) in disabledVisionModules"
          :value="index"
          :key="module.uniqueName"
        >
          <v-card-title class="pb-8">{{ module.nickname }}</v-card-title>
          <v-card-text>
            <span>Created for camera:</span>

            <v-card color="primary">
              <PvCameraInfoCard :camera="module.matchedCameraInfo" />
            </v-card>

            <v-simple-table dense height="100%" class="mt-2">
              <tbody>
                <tr>
                  <td>Device Path</td>
                  <td>
                    {{ module.cameraPath }}
                  </td>
                </tr>

                <tr>
                  <td>Pipelines</td>
                  <td>{{ module.pipelineNicknames.join(", ") }}</td>
                </tr>
                <tr>
                  <td>Connected?</td>
                  <td>{{ isCameraConnected(module.matchedCameraInfo) }}</td>
                </tr>
                <tr>
                  <td>Calibrations</td>
                  <td>
                    {{
                      module.calibrations.map((it2) => getResolutionString(it2.resolution)).join(", ") || "Not calibrated"
                    }}
                  </td>
                </tr>
                <tr>
                  <td>Actions</td>
                  <td>
                    <v-btn class="ma-2 black--text" @click="reactivateModule(module.uniqueName)" color="accent"
                      >Reactivate
                    </v-btn>
                  </td>
                </tr>
              </tbody>
            </v-simple-table>
          </v-card-text>
        </v-card>
      </v-row>
    </v-card>

    <v-card dark class="mb-3 pr-6 pb-3" color="primary">
      <v-card-title>
        <span> Unassigned Cameras </span>
      </v-card-title>

      <v-banner
        class="pa-2 ma-3"
        v-if="
          unmatchedCameras.length === 0 &&
          (useCameraSettingsStore().cameras.length === 0 ||
            (useCameraSettingsStore().cameras.length === 1 &&
              JSON.stringify(useCameraSettingsStore().cameras[0]) === JSON.stringify(PlaceholderCameraSettings)))
        "
        rounded
        dark
        color="red"
        >No cameras connected or active. Plug one in or activate an existing one to get started!</v-banner
      >

      <v-row class="ml-3 mb-0" v-if="unmatchedCameras.length > 0">
        <v-card class="mb-3 pa-8" color="secondary" v-for="(camera, index) in unmatchedCameras" :key="index">
          <v-card color="primary">
            <PvCameraInfoCard :camera="camera" />
          </v-card>

          <v-row>
            <v-btn class="ma-2 mt-5 black--text" @click="activateCamera(camera)" color="accent">Activate Camera</v-btn>
          </v-row>
        </v-card>
      </v-row>
    </v-card>
  </div>
</template>

<style scoped>
.v-data-table {
  background-color: #006492 !important;
}

a:link {
  color: rgb(14, 240, 14);
  background-color: transparent;
  text-decoration: none;
}

a:visited {
  color: pink;
  background-color: transparent;
  text-decoration: none;
}

a:hover {
  color: red;
  background-color: transparent;
  text-decoration: underline;
}

a:active {
  color: yellow;
  background-color: transparent;
  text-decoration: underline;
}
</style>
