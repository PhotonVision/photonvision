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
  <div class="pa-3">
    <v-card dark class="mb-3" color="primary">
      <v-card-title style="display: flex; justify-content: space-between">
        <span>Active Vision Modules</span>
      </v-card-title>

      <v-card-text
        v-if="
          useCameraSettingsStore().cameras.length === 0 ||
          (useCameraSettingsStore().cameras.length === 1 &&
            JSON.stringify(useCameraSettingsStore().cameras[0]) === JSON.stringify(PlaceholderCameraSettings))
        "
      >
        <v-banner rounded color="red" icon="mdi-alert">
          No active vision modules. Activate a camera to get started!
        </v-banner>
      </v-card-text>

      <v-card-text v-else>
        <v-card
          dark
          color="secondary"
          v-for="(module, index) in activeVisionModules"
          v-if="JSON.stringify(module) !== JSON.stringify(PlaceholderCameraSettings)"
          :class="index > 0 ? 'mt-3' : ''"
          :value="index"
        >
          <v-card-title>
            <v-col cols="8" class="pa-0">
              <span>{{ module.nickname }}</span>
            </v-col>
            <v-col cols="2" class="pa-0 pr-3">
              <v-btn
                color="primary"
                @click="isExpanded[module.uniqueName] = !(isExpanded[module.uniqueName] ?? false)"
                style="width: 100%"
              >
                <v-icon>{{ isExpanded[module.uniqueName] ? "mdi-chevron-up" : "mdi-chevron-down" }} </v-icon>
                <span>Details</span>
              </v-btn>
            </v-col>
            <v-col cols="2" class="pa-0">
              <v-btn
                class="black--text"
                @click="deactivateCamera(module.uniqueName)"
                color="accent"
                style="width: 100%"
              >
                Deactivate
              </v-btn>
            </v-col>
          </v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="8">
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
              </v-col>
              <v-col cols="4" class="pl-0">
                <photon-camera-stream
                  id="output-camera-stream"
                  :camera-settings="module"
                  stream-type="Processed"
                  style="width: 100%; height: auto"
                />
              </v-col>
            </v-row>

            <v-expand-transition>
              <v-card color="primary" v-if="isExpanded[module.uniqueName] ?? false" class="mt-3">
                <PvCameraMatchCard :saved="module.matchedCameraInfo" :matched="getMatchedDevice(module)" />
              </v-card>
            </v-expand-transition>
          </v-card-text>
        </v-card>
      </v-card-text>
    </v-card>

    <v-card dark class="mb-3" color="primary">
      <v-card-title style="display: flex; justify-content: space-between">
        <span> Disabled Vision Modules </span>
      </v-card-title>

      <v-card-text v-if="disabledVisionModules.length === 0">
        <v-banner rounded dark icon="mdi-information"> No disabled cameras to show. </v-banner>
      </v-card-text>

      <v-card-text v-else>
        <v-card
          dark
          color="secondary"
          v-for="(module, index) in disabledVisionModules"
          :class="index > 0 ? 'mt-3' : ''"
          :value="index"
          :key="module.uniqueName"
        >
          <v-card-title>
            <v-col cols="8" class="pa-0">
              <span>{{ module.nickname }}</span>
            </v-col>
            <v-col cols="2" class="pa-0 pr-3">
              <v-btn
                color="primary"
                @click="isExpanded[module.uniqueName] = !(isExpanded[module.uniqueName] ?? false)"
                style="width: 100%"
              >
                <v-icon>{{ isExpanded[module.uniqueName] ? "mdi-chevron-up" : "mdi-chevron-down" }} </v-icon>
                <span>Details</span>
              </v-btn>
            </v-col>
            <v-col cols="2" class="pa-0">
              <v-btn class="black--text" @click="activateModule(module.uniqueName)" color="accent" style="width: 100%">
                Activate
              </v-btn>
            </v-col>
          </v-card-title>

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

            <v-expand-transition>
              <v-card color="primary" v-if="isExpanded[module.uniqueName] ?? false" class="mt-3">
                <PvCameraInfoCard :camera="module.matchedCameraInfo" />
              </v-card>
            </v-expand-transition>
          </v-card-text>
        </v-card>
      </v-card-text>
    </v-card>

    <v-card dark color="primary">
      <v-card-title>
        <span> Unassigned Cameras </span>
      </v-card-title>

      <v-card-text v-if="unmatchedCameras.length === 0">
        <v-banner rounded dark icon="mdi-information"> No unassigned cameras. Plug one in to get started! </v-banner>
      </v-card-text>

      <v-card-text v-else>
        <v-card
          color="secondary"
          v-for="(camera, index) in unmatchedCameras"
          :class="index > 0 ? 'mt-3' : ''"
          :key="index"
        >
          <v-card-title>
            <v-row>
              <v-col cols="8" class="pt-4 pb-4">
                <v-card-title v-if="camera.PVUsbCameraInfo" class="pa-0">USB Camera</v-card-title>
                <v-card-title v-else-if="camera.PVCSICameraInfo" class="pa-0 pb-1">CSI Camera</v-card-title>
                <v-card-title v-else-if="camera.PVFileCameraInfo" class="pa-0 pb-1">File Camera</v-card-title>
                <v-card-title v-else class="pa-0 pb-1">Unknown Camera</v-card-title>
              </v-col>
              <v-col cols="2" class="pl-0">
                <v-btn
                  color="primary"
                  @click="isExpanded[uniquePathForCamera(camera)] = !(isExpanded[uniquePathForCamera(camera)] ?? false)"
                  style="width: 100%"
                >
                  <v-icon
                    >{{ isExpanded[uniquePathForCamera(camera)] ? "mdi-chevron-up" : "mdi-chevron-down" }}
                  </v-icon>
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="2" class="pl-0">
                <v-btn class="black--text" @click="activateCamera(camera)" color="accent" style="width: 100%"
                  >Activate</v-btn
                >
              </v-col>
            </v-row>
          </v-card-title>

          <v-expand-transition>
            <v-card-text v-if="isExpanded[uniquePathForCamera(camera)] ?? false">
              <v-card color="primary">
                <PvCameraInfoCard :camera="camera" :showTitle="false" />
              </v-card>
            </v-card-text>
          </v-expand-transition>
        </v-card>
      </v-card-text>
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
