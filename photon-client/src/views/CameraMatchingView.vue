<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { PlaceholderCameraSettings, PVCameraInfo, type UiCameraConfiguration } from "@/types/SettingTypes";
import { getResolutionString } from "@/lib/PhotonUtils";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import PvInput from "@/components/common/pv-input.vue";
import PvCameraInfoCard from "@/components/common/pv-camera-info-card.vue";
import axios from "axios";
import PvCameraMatchCard from "@/components/common/pv-camera-match-card.vue";
import type { WebsocketCameraSettingsUpdate } from "@/types/WebsocketDataTypes";
import { camerasMatch, cameraInfoFor, getMatchedDevice } from "@/lib/MatchingUtils";
import { useTheme } from "vuetify";

const theme = useTheme();

const formatUrl = (port) => `http://${inject("backendHostname")}:${port}/stream.mjpg`;

const activatingModule = ref(false);
const activateModule = (moduleUniqueName: string) => {
  if (activatingModule.value) return;
  activatingModule.value = true;

  axios
    .post("/utils/activateMatchedCamera", { cameraUniqueName: moduleUniqueName })
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Camera activated successfully",
        color: "success"
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          message: "The backend is unable to fulfil the request to activate this camera.",
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
    .finally(() => (activatingModule.value = false));
};

const assigningCamera = ref(false);
const assignCamera = (cameraInfo: PVCameraInfo) => {
  if (assigningCamera.value) return;
  assigningCamera.value = true;

  const payload = {
    cameraInfo: cameraInfo
  };

  axios
    .post("/utils/assignUnmatchedCamera", payload)
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Unmatched camera assigned successfully",
        color: "success"
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          message: "The backend is unable to fulfil the request to assign this unmatched camera.",
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
    .finally(() => (assigningCamera.value = false));
};

const deactivatingModule = ref(false);
const deactivateModule = (cameraUniqueName: string) => {
  if (deactivatingModule.value) return;
  deactivatingModule.value = true;
  axios
    .post("/utils/unassignCamera", { cameraUniqueName: cameraUniqueName })
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Camera deactivated successfully",
        color: "success"
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          message: "The backend is unable to fulfil the request to deactivate this camera.",
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
    .finally(() => (deactivatingModule.value = false));
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
</script>

<template>
  <div class="pa-3">
    <v-row>
      <!-- Active modules -->
      <v-col
        v-for="(module, index) in activeVisionModules"
        :key="`enabled-${module.uniqueName}`"
        cols="12"
        sm="6"
        lg="4"
        class="pr-0"
      >
        <v-card color="surface" class="rounded-12">
          <v-card-title>{{ cameraInfoFor(module.matchedCameraInfo).name }}</v-card-title>
          <v-card-subtitle v-if="!cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath)"
            >Status: <span class="inactive-status">Disconnected</span></v-card-subtitle
          >
          <v-card-subtitle
            v-else-if="
              cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath) &&
              camerasMatch(
                getMatchedDevice(useStateStore().vsmState.allConnectedCameras, module.matchedCameraInfo),
                module.matchedCameraInfo
              )
            "
            >Status: <span class="active-status">Active</span></v-card-subtitle
          >
          <v-card-subtitle v-else>Status: <span class="mismatch-status">Mismatch</span></v-card-subtitle>
          <v-card-text class="pt-3">
            <v-table density="compact">
              <tbody>
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
                <tr v-else>
                  <td>Name</td>
                  <td>
                    {{ module.nickname }}
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
                <tr>
                  <td>Streams:</td>
                  <td>
                    <a :href="formatUrl(module.stream.inputPort)" target="_blank" class="stream-link"> Input </a>
                    /
                    <a :href="formatUrl(module.stream.outputPort)" target="_blank" class="stream-link"> Output </a>
                  </td>
                </tr>
              </tbody>
            </v-table>
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
                  color="buttonPassive"
                  style="width: 100%"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
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
                  class="text-black"
                  color="buttonActive"
                  style="width: 100%"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                  :loading="deactivatingModule"
                  @click="deactivateModule(module.uniqueName)"
                >
                  Deactivate
                </v-btn>
              </v-col>
              <v-col cols="6" md="3">
                <v-btn
                  class="pa-0"
                  color="error"
                  style="width: 100%"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                  @click="setCameraDeleting(module)"
                >
                  <v-icon size="x-large">mdi-trash-can-outline</v-icon>
                </v-btn>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Deactivated modules -->
      <v-col
        v-for="module in disabledVisionModules"
        :key="`disabled-${module.uniqueName}`"
        cols="12"
        sm="6"
        lg="4"
        class="pr-0"
      >
        <v-card class="pr-0 rounded-12" color="surface">
          <v-card-title>{{ module.cameraQuirks.baseName }}</v-card-title>
          <v-card-subtitle>Status: <span class="inactive-status">Deactivated</span></v-card-subtitle>
          <v-card-text class="pt-3">
            <v-table density="compact">
              <tbody>
                <tr>
                  <td>Name</td>
                  <td>
                    {{ module.nickname }}
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
                      module.calibrations.map((it2) => getResolutionString(it2.resolution)).join(", ") ||
                      "Not calibrated"
                    }}
                  </td>
                </tr>
                <tr>
                  <td>Connected</td>
                  <td>{{ cameraCononected(cameraInfoFor(module.matchedCameraInfo).uniquePath) }}</td>
                </tr>
              </tbody>
            </v-table>
          </v-card-text>
          <v-card-text class="pt-0">
            <v-row>
              <v-col cols="12" md="4" class="pr-md-0 pb-0 pb-md-3">
                <v-btn
                  color="buttonPassive"
                  style="width: 100%"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
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
                  class="text-black"
                  color="buttonActive"
                  style="width: 100%"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                  :loading="activatingModule"
                  @click="activateModule(module.uniqueName)"
                >
                  Activate
                </v-btn>
              </v-col>
              <v-col cols="6" md="3">
                <v-btn
                  class="pa-0"
                  color="error"
                  style="width: 100%"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                  @click="setCameraDeleting(module)"
                >
                  <v-icon size="x-large">mdi-trash-can-outline</v-icon>
                </v-btn>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Unassigned cameras -->
      <v-col v-for="(camera, index) in unmatchedCameras" :key="index" cols="12" sm="6" lg="4" class="pr-0">
        <v-card class="pr-0 rounded-12" color="surface">
          <v-card-title>
            <span v-if="camera.PVUsbCameraInfo">USB Camera:</span>
            <span v-else-if="camera.PVCSICameraInfo">CSI Camera:</span>
            <span v-else-if="camera.PVFileCameraInfo">File Camera:</span>
            <span v-else>Unknown Camera:</span>
            &nbsp;<span>{{ cameraInfoFor(camera)?.name ?? cameraInfoFor(camera)?.baseName }}</span>
          </v-card-title>
          <v-card-subtitle>Status: Unassigned</v-card-subtitle>
          <v-card-text class="pt-3">
            <span style="word-break: break-all">{{ cameraInfoFor(camera)?.path }}</span>
          </v-card-text>
          <v-card-text class="pt-0">
            <v-row>
              <v-col cols="6" class="pr-0">
                <v-btn
                  color="buttonPassive"
                  style="width: 100%"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                  @click="setCameraView(camera, false)"
                >
                  <span>Details</span>
                </v-btn>
              </v-col>
              <v-col cols="6">
                <v-btn
                  class="text-black"
                  color="buttonActive"
                  style="width: 100%"
                  :loading="assigningCamera"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
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
      <v-col cols="12" sm="6" lg="4" class="pr-0">
        <v-card
          dark
          flat
          class="pl-6 pr-6 d-flex flex-column justify-center"
          style="background-color: transparent; height: 100%"
        >
          <v-card-text class="d-flex flex-column align-center justify-center" style="flex-grow: 0">
            <v-icon size="64" color="primary">mdi-plus</v-icon>
          </v-card-text>
          <v-card-title>Additional plugged in cameras will display here!</v-card-title>
        </v-card>
      </v-col>
    </v-row>

    <!-- Camera details modal -->
    <v-dialog v-model="viewingDetails" max-width="800">
      <v-card v-if="viewingCamera[0] !== null" flat color="surface">
        <v-card-title class="d-flex justify-space-between">
          <span>{{ cameraInfoFor(viewingCamera[0])?.name ?? cameraInfoFor(viewingCamera[0])?.baseName }}</span>
          <v-btn variant="text" @click="setCameraView(null, null)">
            <v-icon size="x-large">mdi-close</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text v-if="!viewingCamera[1]">
          <PvCameraInfoCard :camera="viewingCamera[0]" />
        </v-card-text>
        <v-card-text
          v-else-if="
            !camerasMatch(
              getMatchedDevice(useStateStore().vsmState.allConnectedCameras, viewingCamera[0]),
              viewingCamera[0]
            )
          "
        >
          <v-alert
            class="mb-3"
            color="buttonActive"
            density="compact"
            text="A different camera may have been connected to this device! Compare the following information carefully."
            icon="mdi-information-outline"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
          />
          <PvCameraMatchCard
            :saved="viewingCamera[0]"
            :current="getMatchedDevice(useStateStore().vsmState.allConnectedCameras, viewingCamera[0])"
          />
        </v-card-text>
        <v-card-text v-else>
          <PvCameraInfoCard
            :camera="getMatchedDevice(useStateStore().vsmState.allConnectedCameras, viewingCamera[0])"
          />
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Camera delete modal -->
    <v-dialog v-model="viewingDeleteCamera" width="800">
      <v-card v-if="cameraToDelete !== null" class="dialog-container" color="surface" flat>
        <v-card-title> Delete {{ cameraToDelete.nickname }}? </v-card-title>
        <v-card-text class="pb-10px">
          Are you sure you want to delete "{{ cameraToDelete.nickname }}"? This cannot be undone.
        </v-card-text>
        <v-card-text class="pt-0 pb-10px">
          <pv-input
            v-model="yesDeleteMySettingsText"
            :label="'Type &quot;' + cameraToDelete.nickname + '&quot;:'"
            :label-cols="6"
            :input-cols="6"
          />
        </v-card-text>
        <v-card-actions class="pa-5 pt-0">
          <v-btn
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            color="primary"
            class="text-black"
            @click="cameraToDelete = null"
          >
            Cancel
          </v-btn>
          <v-btn
            color="error"
            :disabled="yesDeleteMySettingsText.toLowerCase() !== cameraToDelete.nickname.toLowerCase()"
            :loading="deletingCamera"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="deleteThisCamera(cameraToDelete.uniqueName)"
          >
            <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
            <span class="open-label">Delete</span>
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
td {
  padding: 0 !important;
}

.v-card-subtitle {
  padding-top: 0px !important;
  padding-bottom: 8px !important;
}

.v-card-title {
  padding-bottom: 0 !important;
  text-wrap-mode: wrap !important;
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
