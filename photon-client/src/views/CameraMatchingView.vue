<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import {
  PlaceholderCameraSettings,
  PVCameraInfo,
  type PVCSICameraInfo,
  type PVFileCameraInfo,
  type PVUsbCameraInfo,
  type UiCameraConfiguration
} from "@/types/SettingTypes";
import { getResolutionString } from "@/lib/PhotonUtils";
import PvCameraInfoCard from "@/components/common/pv-camera-info-card.vue";
import axios from "axios";
import PvCameraMatchCard from "@/components/common/pv-camera-match-card.vue";
import type { WebsocketCameraSettingsUpdate } from "@/types/WebsocketDataTypes";

const formatUrl = (port) => `http://${inject("backendHostname")}:${port}/stream.mjpg`;
const host = inject<string>("backendHost");

const activatingModule = ref(false);
const activateModule = (moduleUniqueName: string) => {
  if (activatingModule.value) return;
  activatingModule.value = true;
  const url = new URL(`http://${host}/api/utils/activateMatchedCamera`);
  url.searchParams.set("cameraUniqueName", moduleUniqueName);

  fetch(url.toString(), {
    method: "POST"
  }).finally(() => (activatingModule.value = false));
};

const assigningCamera = ref(false);
const assignCamera = (cameraInfo: PVCameraInfo) => {
  if (assigningCamera.value) return;
  assigningCamera.value = true;
  const url = new URL(`http://${host}/api/utils/assignUnmatchedCamera`);
  url.searchParams.set("cameraInfo", JSON.stringify(cameraInfo));

  fetch(url.toString(), {
    method: "POST"
  }).finally(() => (assigningCamera.value = false));
};

const deactivatingModule = ref(false);
const deactivateModule = (cameraUniqueName: string) => {
  if (deactivatingModule.value) return;
  deactivatingModule.value = true;
  const url = new URL(`http://${host}/api/utils/unassignCamera`);
  url.searchParams.set("cameraUniqueName", cameraUniqueName);

  fetch(url.toString(), {
    method: "POST"
  }).finally(() => (deactivatingModule.value = false));
};

const deletingCamera = ref(false);
const deleteThisCamera = (cameraName: string) => {
  if (deletingCamera.value) return;
  deletingCamera.value = true;
  const payload = {
    cameraUniqueName: cameraName
  };

  axios
    .post("/utils/nukeOneCamera", payload)
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Camera deleted successfully",
        color: "success"
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          message: "The backend is unable to fulfil the request to delete this camera.",
          color: "error"
        });
      } else if (error.request) {
        useStateStore().showSnackbarMessage({
          message: "Error while trying to process the request! The backend didn't respond.",
          color: "error"
        });
      } else {
        useStateStore().showSnackbarMessage({
          message: "An error occurred while trying to process the request.",
          color: "error"
        });
      }
    })
    .finally(() => {
      setCameraDeleting(null);
      deletingCamera.value = false;
    });
};

const camerasMatch = (camera1: PVCameraInfo, camera2: PVCameraInfo) => {
  if (camera1.PVUsbCameraInfo && camera2.PVUsbCameraInfo)
    return (
      camera1.PVUsbCameraInfo.name === camera2.PVUsbCameraInfo.name &&
      camera1.PVUsbCameraInfo.vendorId === camera2.PVUsbCameraInfo.vendorId &&
      camera1.PVUsbCameraInfo.productId === camera2.PVUsbCameraInfo.productId &&
      camera1.PVUsbCameraInfo.uniquePath === camera2.PVUsbCameraInfo.uniquePath
    );
  else if (camera1.PVCSICameraInfo && camera2.PVCSICameraInfo)
    return (
      camera1.PVCSICameraInfo.uniquePath === camera2.PVCSICameraInfo.uniquePath &&
      camera1.PVCSICameraInfo.baseName === camera2.PVCSICameraInfo.baseName
    );
  else if (camera1.PVFileCameraInfo && camera2.PVFileCameraInfo)
    return (
      camera1.PVFileCameraInfo.uniquePath === camera2.PVFileCameraInfo.uniquePath &&
      camera1.PVFileCameraInfo.name === camera2.PVFileCameraInfo.name
    );
  else return false;
};

const cameraInfoFor = (camera: PVCameraInfo | null): PVUsbCameraInfo | PVCSICameraInfo | PVFileCameraInfo | any => {
  if (!camera) return null;
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

/**
 * Find the PVCameraInfo currently occupying the same uniquepath as the the given module
 */
const getMatchedDevice = (info: PVCameraInfo | undefined): PVCameraInfo => {
  if (!info) {
    return {
      PVFileCameraInfo: undefined,
      PVCSICameraInfo: undefined,
      PVUsbCameraInfo: undefined
    };
  }
  return (
    useStateStore().vsmState.allConnectedCameras.find(
      (it) => cameraInfoFor(it).uniquePath === cameraInfoFor(info).uniquePath
    ) || {
      PVFileCameraInfo: undefined,
      PVCSICameraInfo: undefined,
      PVUsbCameraInfo: undefined
    }
  );
};

const cameraCononected = (uniquePath: string): boolean => {
  return (
    useStateStore().vsmState.allConnectedCameras.find((it) => cameraInfoFor(it).uniquePath === uniquePath) !== undefined
  );
};

const unmatchedCameras = computed(() => {
  const activeVmPaths = Object.values(useCameraSettingsStore().cameras).map(
    (it) => cameraInfoFor(it.matchedCameraInfo).uniquePath
  );
  const disabledVmPaths = useStateStore().vsmState.disabledConfigs.map(
    (it) => cameraInfoFor(it.matchedCameraInfo).uniquePath
  );

  return useStateStore().vsmState.allConnectedCameras.filter(
    (it) =>
      !activeVmPaths.includes(cameraInfoFor(it).uniquePath) && !disabledVmPaths.includes(cameraInfoFor(it).uniquePath)
  );
});

const activeVisionModules = computed(() =>
  Object.values(useCameraSettingsStore().cameras)
    // Ignore placeholder camera
    .filter((camera) => JSON.stringify(camera) !== JSON.stringify(PlaceholderCameraSettings))
    // Display connected cameras first
    .sort(
      (first, second) =>
        (cameraCononected(cameraInfoFor(second.matchedCameraInfo).uniquePath) ? 1 : 0) -
        (cameraCononected(cameraInfoFor(first.matchedCameraInfo).uniquePath) ? 1 : 0)
    )
);

const disabledVisionModules = computed(() => useStateStore().vsmState.disabledConfigs);

const viewingDetails = ref(false);
const viewingCamera = ref<[PVCameraInfo | null, boolean | null]>([null, null]);
const setCameraView = (camera: PVCameraInfo | null, isConnected: boolean | null) => {
  viewingDetails.value = camera !== null && isConnected !== null;
  viewingCamera.value = [camera, isConnected];
};

const viewingDeleteCamera = ref(false);
const cameraToDelete = ref<UiCameraConfiguration | WebsocketCameraSettingsUpdate | null>(null);
const setCameraDeleting = (camera: UiCameraConfiguration | WebsocketCameraSettingsUpdate | null) => {
  yesDeleteMySettingsText.value = "";
  viewingDeleteCamera.value = camera !== null;
  cameraToDelete.value = camera;
};
const yesDeleteMySettingsText = ref("");
const exportSettings = ref();
const openExportSettingsPrompt = () => {
  exportSettings.value.click();
};
</script>

<template>
  <div class="pa-5">
    <v-row>
      <!-- Active modules -->
      <v-col
        v-for="(module, index) in activeVisionModules"
        :key="`enabled-${module.uniqueName}`"
        cols="12"
        sm="6"
        lg="4"
      >
        <v-card dark color="primary">
          <v-card-title>{{ cameraInfoFor(module.matchedCameraInfo).name }}</v-card-title>
          <v-card-subtitle v-if="!cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath)" class="pb-2"
            >Status: <span class="inactive-status">Disconnected</span></v-card-subtitle
          >
          <v-card-subtitle
            v-else-if="
              cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath) &&
              camerasMatch(getMatchedDevice(module.matchedCameraInfo), module.matchedCameraInfo)
            "
            class="pb-2"
            >Status: <span class="active-status">Active</span></v-card-subtitle
          >
          <v-card-subtitle v-else class="pb-2">Status: <span class="mismatch-status">Mismatch</span></v-card-subtitle>
          <v-card-text>
            <v-simple-table dark dense>
              <tbody>
                <tr>
                  <td>Streams:</td>
                  <td>
                    <a :href="formatUrl(module.stream.inputPort)" target="_blank" class="stream-link"> Input </a>
                    /
                    <a :href="formatUrl(module.stream.outputPort)" target="_blank" class="stream-link"> Output </a>
                  </td>
                </tr>
                <tr>
                  <td>Pipelines</td>
                  <td>{{ module.pipelineNicknames.join(", ") }}</td>
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
                <tr
                  v-if="
                    cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath) &&
                    useStateStore().backendResults[module.uniqueName]
                  "
                >
                  <td style="width: 50%">Frames Processed</td>
                  <td>
                    {{ useStateStore().backendResults[module.uniqueName].sequenceID }} ({{
                      useStateStore().backendResults[module.uniqueName].fps
                    }}
                    FPS)
                  </td>
                </tr>
              </tbody>
            </v-simple-table>
            <div
              v-if="cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath)"
              :id="`stream-container-${index}`"
              class="d-flex flex-column justify-center align-center mt-3"
              style="height: 250px"
            >
              <photon-camera-stream
                :id="`output-camera-stream-${index}`"
                :camera-settings="module"
                stream-type="Processed"
              />
            </div>
          </v-card-text>
          <v-card-text class="pt-0">
            <v-row>
              <v-col cols="12" md="4" class="pr-md-0 pb-0 pb-md-3">
                <v-btn
                  color="secondary"
                  style="width: 100%"
                  @click="
                    setCameraView(
                      module.matchedCameraInfo,
                      cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath)
                    )
                  "
                >
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6" md="5" class="pr-0">
                <v-btn
                  class="black--text"
                  color="accent"
                  style="width: 100%"
                  :loading="deactivatingModule"
                  @click="deactivateModule(module.uniqueName)"
                >
                  Deactivate
                </v-btn>
              </v-col>
              <v-col cols="6" md="3">
                <v-btn class="pa-0" color="error" style="width: 100%" @click="setCameraDeleting(module)">
                  <v-icon>mdi-trash-can-outline</v-icon>
                </v-btn>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Disabled modules -->
      <v-col v-for="module in disabledVisionModules" :key="`disabled-${module.uniqueName}`" cols="12" sm="6" lg="4">
        <v-card dark color="primary">
          <v-card-title>{{ module.nickname }}</v-card-title>
          <v-card-subtitle class="pb-2">Status: <span class="inactive-status">Deactivated</span></v-card-subtitle>
          <v-card-text>
            <v-simple-table dense>
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
                  <td>{{ cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath) }}</td>
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
              <v-col cols="12" md="4" class="pr-md-0 pb-0 pb-md-3">
                <v-btn
                  color="secondary"
                  style="width: 100%"
                  @click="
                    setCameraView(
                      module.matchedCameraInfo,
                      cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath)
                    )
                  "
                >
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6" md="5" class="pr-0">
                <v-btn
                  class="black--text"
                  color="accent"
                  style="width: 100%"
                  :loading="activatingModule"
                  @click="activateModule(module.uniqueName)"
                >
                  Activate
                </v-btn>
              </v-col>
              <v-col cols="6" md="3">
                <v-btn class="pa-0" color="error" style="width: 100%" @click="setCameraDeleting(module)">
                  <v-icon>mdi-trash-can-outline</v-icon>
                </v-btn>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Unassigned cameras -->
      <v-col v-for="(camera, index) in unmatchedCameras" :key="index" cols="12" sm="6" lg="4">
        <v-card dark color="primary">
          <v-card-title class="pb-2">
            <span v-if="camera.PVUsbCameraInfo">USB Camera:</span>
            <span v-else-if="camera.PVCSICameraInfo">CSI Camera:</span>
            <span v-else-if="camera.PVFileCameraInfo">File Camera:</span>
            <span v-else>Unknown Camera:</span>
            &nbsp;<span>{{ cameraInfoFor(camera)?.name ?? cameraInfoFor(camera)?.baseName }}</span>
          </v-card-title>
          <v-card-subtitle>Status: Unassigned</v-card-subtitle>
          <v-card-text>
            <span style="word-break: break-all">{{ cameraInfoFor(camera)?.path }}</span>
          </v-card-text>
          <v-card-text class="pt-0">
            <v-row>
              <v-col cols="6" class="pr-0">
                <v-btn color="secondary" style="width: 100%" @click="setCameraView(camera, false)">
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6">
                <v-btn
                  class="black--text"
                  color="accent"
                  style="width: 100%"
                  :loading="assigningCamera"
                  @click="assignCamera(camera)"
                >
                  Activate
                </v-btn>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Info card -->
      <v-col cols="12" sm="6" lg="4">
        <v-card
          dark
          flat
          class="pl-6 pr-6 d-flex flex-column justify-center"
          style="background-color: transparent; height: 100%"
        >
          <v-card-text class="d-flex flex-column align-center justify-center">
            <v-icon size="64" color="primary">mdi-plus</v-icon>
          </v-card-text>
          <v-card-title>Additional plugged in cameras will display here!</v-card-title>
        </v-card>
      </v-col>
    </v-row>

    <!-- Camera details modal -->
    <v-dialog v-model="viewingDetails" max-width="800">
      <v-card v-if="viewingCamera[0] !== null" dark flat color="primary">
        <v-card-title class="d-flex justify-space-between">
          <span>{{ cameraInfoFor(viewingCamera[0])?.name ?? cameraInfoFor(viewingCamera[0])?.baseName }}</span>
          <v-btn text @click="setCameraView(null, null)">
            <v-icon>mdi-close-thick</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text v-if="!viewingCamera[1]">
          <PvCameraInfoCard :camera="viewingCamera[0]" />
        </v-card-text>
        <v-card-text v-else-if="!camerasMatch(getMatchedDevice(viewingCamera[0]), viewingCamera[0])">
          <v-banner rounded color="error" text-color="white" icon="mdi-information-outline" class="mb-3">
            It looks like a different camera may have been connected to this device! Compare the following information
            carefully.
          </v-banner>
          <PvCameraMatchCard :saved="viewingCamera[0]" :current="getMatchedDevice(viewingCamera[0])" />
        </v-card-text>
        <v-card-text v-else>
          <PvCameraInfoCard :camera="getMatchedDevice(viewingCamera[0])" />
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Camera delete modal -->
    <v-dialog v-model="viewingDeleteCamera" dark width="800">
      <v-card v-if="cameraToDelete !== null" dark class="dialog-container pa-3 pb-2" color="primary" flat>
        <v-card-title> Delete {{ cameraToDelete.nickname }}? </v-card-title>
        <v-card-text>
          <v-row class="align-center pt-6">
            <v-col cols="12" md="6">
              <span class="white--text"> This will delete ALL OF YOUR SETTINGS and restart PhotonVision. </span>
            </v-col>
            <v-col cols="12" md="6">
              <v-btn color="secondary" block @click="openExportSettingsPrompt">
                <v-icon left class="open-icon"> mdi-export </v-icon>
                <span class="open-label">Backup Settings</span>
                <a
                  ref="exportSettings"
                  style="color: black; text-decoration: none; display: none"
                  :href="`http://${host}/api/settings/photonvision_config.zip`"
                  download="photonvision-settings.zip"
                  target="_blank"
                />
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <pv-input
            v-model="yesDeleteMySettingsText"
            :label="'Type &quot;' + cameraToDelete.nickname + '&quot;:'"
            :label-cols="6"
            :input-cols="6"
          />
        </v-card-text>
        <v-card-text>
          <v-btn
            block
            color="error"
            :disabled="yesDeleteMySettingsText.toLowerCase() !== cameraToDelete.nickname.toLowerCase()"
            :loading="deletingCamera"
            @click="deleteThisCamera(cameraToDelete.uniqueName)"
          >
            <v-icon left class="open-icon"> mdi-trash-can-outline </v-icon>
            <span class="open-label">DELETE (UNRECOVERABLE)</span>
          </v-btn>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.v-data-table {
  background-color: #006492 !important;
}

.active-status {
  color: rgb(14, 240, 14);
  background-color: transparent;
  text-decoration: none;
}

.inactive-status {
  color: red;
  background-color: transparent;
  text-decoration: none;
}

a:hover {
  color: pink;
  background-color: transparent;
  text-decoration: underline;
}

a:active,
.stream-link,
.mismatch-status {
  color: yellow;
  background-color: transparent;
  text-decoration: none;
}
</style>
