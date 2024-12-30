<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { PlaceholderCameraSettings, PVCameraInfo, type UiCameraConfiguration } from "@/types/SettingTypes";
import { getResolutionString } from "@/lib/PhotonUtils";
import PvCameraInfoCard from "@/components/common/pv-camera-info-card.vue";

const formatUrl = (port) => `http://${inject("backendHostname")}:${port}/stream.mjpg`;
const host = inject<string>("backendHost");

const activateModule = (moduleUniqueName: string) => {
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

const cameraInfoFor: any = (camera: PVCameraInfo) => {
  if (camera.PVUsbCameraInfo) {
    return camera.PVUsbCameraInfo;
  }
  if (camera.PVCSICameraInfo) {
    return camera.PVCSICameraInfo;
  }
  if (camera.PVFileCameraInfo) {
    return camera.PVFileCameraInfo;
  }
  return {};
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

const isExpanded = ref({});
</script>

<template>
  <div class="pa-5">

    <v-row>
      <!-- Active modules -->
      <v-col cols="12" sm="6" lg="4" v-for="(module, index) in activeVisionModules" 
      v-if="JSON.stringify(module) !== JSON.stringify(PlaceholderCameraSettings)" :key="module.uniqueName">
        <v-card dark color="primary">
          <v-card-title>{{ module.nickname }}</v-card-title>
          <v-card-subtitle>Status: <span class="active-status">Active</span></v-card-subtitle>
          <v-card-text>
            <v-simple-table dark dense height="100%" class="camera-card-table">
              <tbody>
                <tr>
                  <td>Streams:</td>
                  <td>
                    <a :href="formatUrl(module.stream.inputPort)" target="_blank"> Input Stream </a> /
                    <a :href="formatUrl(module.stream.outputPort)" target="_blank"> Output Stream </a>
                  </td>
                </tr>
                <tr>
                  <td>Pipelines</td>
                  <td>{{ module.pipelineNicknames.join(", ") }}</td>
                </tr>
                <tr v-if="module.isConnected && useStateStore().backendResults[index]">
                  <td>Frames Processed</td>
                  <td>
                    {{ useStateStore().backendResults[index].sequenceID }} ({{
                      useStateStore().backendResults[index].fps
                    }}
                    FPS)
                  </td>
                </tr>
                <tr>
                  <td>Connected</td>
                  <td>{{ module.isConnected }}</td>
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
              </tbody>
            </v-simple-table>
            <photon-camera-stream
            class="mt-3"
              id="output-camera-stream"
              :camera-settings="module"
              stream-type="Processed"
              style="width: 100%; height: auto"
            />
          </v-card-text>
          <v-card-text class="pt-0">
            <v-row>
              <v-col cols="6">
                <v-btn
                  color="secondary"
                  @click="isExpanded[module.uniqueName] = !(isExpanded[module.uniqueName] ?? false)"
                  style="width: 100%"
                >
                  <v-icon>{{ isExpanded[module.uniqueName] ? "mdi-chevron-up" : "mdi-chevron-down" }} </v-icon>
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6">
                <v-btn
                  class="black--text"
                  @click="deactivateCamera(module.uniqueName)"
                  color="accent"
                  style="width: 100%"
                >
                  Deactivate
                </v-btn>
              </v-col>
            </v-row>
            <v-expand-transition>
              <div color="primary" v-if="isExpanded[module.uniqueName] ?? false" class="mt-3">
                <PvCameraMatchCard :saved="module.matchedCameraInfo" :matched="getMatchedDevice(module)" />
              </div>
            </v-expand-transition>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Disabled modules -->
      <v-col cols="12" sm="6" lg="4" v-for="(module, index) in disabledVisionModules" 
      :key="module.uniqueName">
        <v-card dark color="primary">
          <v-card-title>{{ module.nickname }}</v-card-title>
          <v-card-subtitle>Status: <span class="inactive-status">Deactivated</span></v-card-subtitle>
          <v-card-text>
            <v-simple-table dense height="100%">
              <tbody>
                <tr>
                  <td>Name</td>
                  <td>
                    {{ module.cameraQuirks.baseName }}
                  </td>
                </tr>
                <tr>
                  <td>Pipelines</td>
                  <td>{{ module.pipelineNicknames.join(", ") }}</td>
                </tr>
                <tr>
                  <td>Connected</td>
                  <td>{{ module.isConnected }}</td>
                </tr>
                <tr>
                  <td>Calibrations</td>
                  <td>
                    {{
                      module.calibrations.map((it2) => getResolutionString(it2.resolution)).join(", ") ||
                      "Not calibrated"
                    }}
                  </td>
                </tr>
              </tbody>
            </v-simple-table>
          </v-card-text>
          <v-card-text class="pt-0">
            <v-row>
              <v-col cols="6">
                <v-btn
                  color="secondary"
                  @click="isExpanded[module.uniqueName] = !(isExpanded[module.uniqueName] ?? false)"
                  style="width: 100%"
                >
                  <v-icon>{{ isExpanded[module.uniqueName] ? "mdi-chevron-up" : "mdi-chevron-down" }} </v-icon>
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6">
                <v-btn class="black--text" @click="activateModule(module.uniqueName)" color="accent" style="width: 100%">
                  Activate
                </v-btn>
              </v-col>
            </v-row>
            <v-expand-transition>
              <div color="primary" v-if="isExpanded[module.uniqueName] ?? false" class="mt-3">
                <PvCameraInfoCard :camera="module.matchedCameraInfo" />
              </div>
            </v-expand-transition>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Unassigned cameras -->
      <v-col cols="12" sm="6" lg="4" v-for="(camera, index) in unmatchedCameras"
      :key="index">
        <v-card dark color="primary">
          <v-card-title v-if="camera.PVUsbCameraInfo">USB Camera</v-card-title>
          <v-card-title v-else-if="camera.PVCSICameraInfo">CSI Camera</v-card-title>
          <v-card-title v-else-if="camera.PVFileCameraInfo">File Camera</v-card-title>
          <v-card-title v-else>Unknown Camera</v-card-title>
          <v-card-subtitle>Status: Unassigned</v-card-subtitle>
          <v-card-text>
            <v-simple-table dense>
              <tbody>
                <tr>
                  <td>Name</td>
                  <td>
                    {{ cameraInfoFor(camera).name }}
                  </td>
                </tr>
                <tr>
                  <td>Path</td>
                  <td>{{ cameraInfoFor(camera).path }}</td>
                </tr>
              </tbody>
            </v-simple-table>
          </v-card-text>
          <v-card-text class="pt-0">
            <v-row>
              <v-col cols="6">
                <v-btn
                  color="secondary"
                  @click="isExpanded[uniquePathForCamera(camera)] = !(isExpanded[uniquePathForCamera(camera)] ?? false)"
                  style="width: 100%"
                >
                  <v-icon
                    >{{ isExpanded[uniquePathForCamera(camera)] ? "mdi-chevron-up" : "mdi-chevron-down" }}
                  </v-icon>
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6">
                <v-btn class="black--text" @click="activateCamera(camera)" color="accent" style="width: 100%"
                  >Activate</v-btn
                >
              </v-col>
            </v-row>
          </v-card-text>
            <v-expand-transition>
              <v-card-text v-if="isExpanded[uniquePathForCamera(camera)] ?? false" class="pt-0">
                <PvCameraInfoCard :camera="camera" :showTitle="false" />
              </v-card-text>
            </v-expand-transition>
        </v-card>
      </v-col>

      <!-- Info card -->
      <v-col cols="12" sm="6" lg="4">
        <v-card dark style="background-color: transparent; box-shadow: none; height: 100%; display: flex; flex-direction: column; justify-content: center;">
          <v-card-text class="d-flex flex-column align-center justify-center">
            <v-icon size="64" color="primary">mdi-plus</v-icon>
          </v-card-text>
          <v-card-title>Additional plugged in cameras will display here!</v-card-title>
        </v-card>
      </v-col>
      
    </v-row>

  </div>
</template>

<style scoped>
.v-data-table {
  background-color: #006492 !important;
}

a:link, .active-status {
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

.inactive-status {
  color: red;
  background-color: transparent;
  text-decoration: none;
}

a:active {
  color: yellow;
  background-color: transparent;
  text-decoration: underline;
}

</style>
