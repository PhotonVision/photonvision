<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, inject, onMounted, ref } from "vue";
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
  }).finally(() => {
    activatingModule.value = false;
    setTimeout(() => enforceStreamHeight(), 1000);
  });
};

const assigningCamera = ref(false);
const assignCamera = (cameraInfo: PVCameraInfo) => {
  if (assigningCamera.value) return;
  assigningCamera.value = true;
  const url = new URL(`http://${host}/api/utils/assignUnmatchedCamera`);
  url.searchParams.set("cameraInfo", JSON.stringify(cameraInfo));

  fetch(url.toString(), {
    method: "POST"
  }).finally(() => {
    assigningCamera.value = false;
    setTimeout(() => enforceStreamHeight(), 1000);
  });
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

const cameraInfoFor = (camera: PVCameraInfo): PVUsbCameraInfo | PVCSICameraInfo | PVFileCameraInfo | any => {
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
const getMatchedDevice = (info: PVCameraInfo | undefined): PVCameraInfo => {
  if (!info) {
    return {
      PVFileCameraInfo: {
        name: "!",
        path: "!",
        uniquePath: "!"
      },
      PVCSICameraInfo: undefined,
      PVUsbCameraInfo: undefined
    };
  }
  return (
    useStateStore().vsmState.allConnectedCameras.find(
      (it) => uniquePathForCamera(it) === uniquePathForCamera(info)
    ) || {
      PVFileCameraInfo: {
        name: "!",
        path: "!",
        uniquePath: "!"
      },
      PVCSICameraInfo: undefined,
      PVUsbCameraInfo: undefined
    }
  );
};

const unmatchedCameras = computed(() => {
  const activeVmPaths = Object.values(useCameraSettingsStore().cameras).map((it) =>
    uniquePathForCamera(it.matchedCameraInfo)
  );
  const disabledVmPaths = useStateStore().vsmState.disabledConfigs.map((it) =>
    uniquePathForCamera(it.matchedCameraInfo)
  );

  return useStateStore().vsmState.allConnectedCameras.filter(
    (it) => !activeVmPaths.includes(uniquePathForCamera(it)) && !disabledVmPaths.includes(uniquePathForCamera(it))
  );
});

const activeVisionModules = computed(() =>
  Object.values(useCameraSettingsStore().cameras).filter(
    (camera) => JSON.stringify(camera) !== JSON.stringify(PlaceholderCameraSettings)
  )
);
const disabledVisionModules = computed(() => useStateStore().vsmState.disabledConfigs);

const viewingDetails = ref(false);
const viewingCamera = ref<PVCameraInfo | null>(null);
const setCameraView = (camera: PVCameraInfo | null) => {
  viewingDetails.value = camera !== null;
  viewingCamera.value = camera;
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

const enforceStreamHeight = () => {
  const streamWidth = document.getElementById("stream-container-0")?.offsetWidth ?? 0;
  if (streamWidth === 0) return;

  Object.values(useCameraSettingsStore().cameras)
    .filter((camera) => JSON.stringify(camera) !== JSON.stringify(PlaceholderCameraSettings))
    .forEach((element, index) => {
      let stream = document.getElementById(`outer-output-camera-stream-${index}`);
      if (!stream) return;

      stream?.classList.remove("tall-stream", "wide-stream", "d-none");
      let streamRes = element.validVideoFormats[0].resolution.width / element.validVideoFormats[0].resolution.height;
      let containerRes = streamWidth / 250.0;
      if (element.pipelineSettings.inputImageRotationMode % 2 == 1) streamRes = 1 / streamRes;
      if (streamRes > containerRes) stream?.classList.add("wide-stream");
      else stream?.classList.add("tall-stream");
    });
};

onMounted(() => {
  setTimeout(() => enforceStreamHeight(), 1000);
  window.addEventListener("resize", enforceStreamHeight);
});
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
          <v-card-subtitle v-if="camerasMatch(getMatchedDevice(module.matchedCameraInfo), module.matchedCameraInfo)"
            >Status: <span class="active-status">Active</span></v-card-subtitle
          >
          <v-card-subtitle v-else>Status: <span class="mismatch-status">Mismatch</span></v-card-subtitle>
          <v-card-text>
            <v-simple-table dark dense class="mb-3">
              <tbody>
                <tr>
                  <td>Streams:</td>
                  <td>
                    <a :href="formatUrl(module.stream.inputPort)" target="_blank" class="stream-link"> Input Stream </a>
                    /
                    <a :href="formatUrl(module.stream.outputPort)" target="_blank" class="stream-link">
                      Output Stream
                    </a>
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
                      module.completeCalibrations.map((it) => getResolutionString(it.resolution)).join(", ") ||
                      "Not calibrated"
                    }}
                  </td>
                </tr>
                <tr v-if="module.isConnected && useStateStore().backendResults[module.uniqueName]">
                  <td>Frames Processed</td>
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
              class="d-flex flex-column justify-center align-center"
              style="height: 250px"
              :id="`stream-container-${index}`"
            >
              <photon-camera-stream
                :camera-settings="module"
                stream-type="Processed"
                :outerId="`outer-output-camera-stream-${index}`"
                :id="`output-camera-stream-${index}`"
                class="d-none"
              />
            </div>
          </v-card-text>
          <v-card-text class="pt-0">
            <v-row>
              <v-col cols="12" md="4" class="pr-md-0 pb-0 pb-md-3">
                <v-btn color="secondary" @click="setCameraView(module.matchedCameraInfo)" style="width: 100%">
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6" md="5" class="pr-0">
                <v-btn
                  class="black--text"
                  @click="deactivateModule(module.uniqueName)"
                  color="accent"
                  style="width: 100%"
                  :loading="deactivatingModule"
                >
                  Deactivate
                </v-btn>
              </v-col>
              <v-col cols="6" md="3">
                <v-btn class="pa-0" @click="setCameraDeleting(module)" color="error" style="width: 100%">
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
          <v-card-subtitle>Status: <span class="inactive-status">Deactivated</span></v-card-subtitle>
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
              <v-col cols="12" md="4" class="pr-md-0 pb-0 pb-md-3">
                <v-btn color="secondary" style="width: 100%" @click="setCameraView(module.matchedCameraInfo)">
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
                <v-btn class="pa-0" @click="setCameraDeleting(module)" color="error" style="width: 100%">
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
          <v-card-title>
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
                <v-btn color="secondary" style="width: 100%" @click="setCameraView(camera)">
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6">
                <v-btn
                  class="black--text"
                  @click="assignCamera(camera)"
                  color="accent"
                  style="width: 100%"
                  :loading="assigningCamera"
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
      <v-card dark flat color="primary" v-if="viewingCamera !== null">
        <v-card-title class="d-flex justify-space-between">
          <span>{{ cameraInfoFor(viewingCamera)?.name ?? cameraInfoFor(viewingCamera)?.baseName }}</span>
          <v-btn text @click="setCameraView(null)">
            <v-icon>mdi-close-thick</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text v-if="!camerasMatch(getMatchedDevice(viewingCamera), viewingCamera)">
          <v-banner rounded color="error" text-color="white" icon="mdi-information-outline" class="mb-3">
            It looks like a different camera may have been connected to this device! Compare the following information
            carefully.
          </v-banner>
          <PvCameraMatchCard :saved="viewingCamera" :current="getMatchedDevice(viewingCamera)" />
        </v-card-text>
        <v-card-text v-else>
          <PvCameraInfoCard :camera="getMatchedDevice(viewingCamera)" />
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Camera delete modal -->
    <v-dialog v-model="viewingDeleteCamera" dark width="800">
      <v-card dark class="dialog-container pa-3 pb-2" color="primary" flat v-if="cameraToDelete !== null">
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
            @click="deleteThisCamera(cameraToDelete.uniqueName)"
            :loading="deletingCamera"
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

.wide-stream {
  width: 100%;
  height: auto;
}

.tall-stream {
  height: 100%;
  width: auto;
}
</style>
