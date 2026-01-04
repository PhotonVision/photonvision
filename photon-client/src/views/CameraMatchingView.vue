<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import {
  PlaceholderCameraSettings,
  PVCameraInfo,
  type PVCSICameraInfo,
  type PVFileCameraInfo,
  type PVUsbCameraInfo
} from "@/types/SettingTypes";
import { axiosPost, getResolutionString } from "@/lib/PhotonUtils";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import PvDeleteModal from "@/components/common/pv-delete-modal.vue";
import PvCameraInfoCard from "@/components/common/pv-camera-info-card.vue";
import PvCameraMatchCard from "@/components/common/pv-camera-match-card.vue";
import { useTheme } from "vuetify";

const theme = useTheme();

const formatUrl = (port) => `http://${inject("backendHostname")}:${port}/stream.mjpg`;

const activatingModule = ref(false);
const activateModule = (moduleUniqueName: string) => {
  if (activatingModule.value) return;
  activatingModule.value = true;

  axiosPost("/utils/activateMatchedCamera", "activate a matched camera", {
    cameraUniqueName: moduleUniqueName
  }).finally(() => (activatingModule.value = false));
};

const assigningCamera = ref(false);
const assignCamera = (cameraInfo: PVCameraInfo) => {
  if (assigningCamera.value) return;
  assigningCamera.value = true;

  const payload = {
    cameraInfo: cameraInfo
  };

  axiosPost("/utils/assignUnmatchedCamera", "assign an unmatched camera", payload).finally(
    () => (assigningCamera.value = false)
  );
};

const deactivatingModule = ref(false);
const deactivateModule = (cameraUniqueName: string) => {
  if (deactivatingModule.value) return;
  deactivatingModule.value = true;
  axiosPost("/utils/unassignCamera", "unassign a camera", { cameraUniqueName: cameraUniqueName }).finally(
    () => (deactivatingModule.value = false)
  );
};

const confirmDeleteDialog = ref({ show: false, nickname: "", cameraUniqueName: "" });
const deletingCamera = ref<string | null>(null);

const deleteThisCamera = (cameraUniqueName: string) => {
  if (deletingCamera.value) return;
  deletingCamera.value = cameraUniqueName;
  axiosPost("/utils/nukeOneCamera", "delete a camera", { cameraUniqueName: cameraUniqueName }).finally(() => {
    deletingCamera.value = null;
  });
};

const cameraConnected = (uniquePath: string): boolean => {
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
    .filter((camera) => camera !== PlaceholderCameraSettings)
    // Display connected cameras first
    .sort(
      (first, second) =>
        (cameraConnected(cameraInfoFor(second.matchedCameraInfo).uniquePath) ? 1 : 0) -
        (cameraConnected(cameraInfoFor(first.matchedCameraInfo).uniquePath) ? 1 : 0)
    )
);

const disabledVisionModules = computed(() => useStateStore().vsmState.disabledConfigs);

const viewingDetails = ref(false);
const viewingCamera = ref<[PVCameraInfo | null, boolean | null]>([null, null]);
const setCameraView = (camera: PVCameraInfo | null, isConnected: boolean | null) => {
  viewingDetails.value = camera !== null && isConnected !== null;
  viewingCamera.value = [camera, isConnected];
};

/**
 * Get the connection-type-specific camera info from the given PVCameraInfo object.
 */
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
 * Find the PVCameraInfo currently occupying the same uniquePath as the the given module
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
          <v-card-subtitle v-if="!cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath)"
            >Status: <span class="inactive-status">Disconnected</span></v-card-subtitle
          >
          <v-card-subtitle
            v-else-if="cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath) && !module.mismatch"
            >Status: <span class="active-status">Active</span></v-card-subtitle
          >
          <v-card-subtitle v-else>Status: <span class="mismatch-status">Mismatch</span></v-card-subtitle>
          <v-card-text class="pt-3">
            <v-table density="compact">
              <tbody>
                <tr
                  v-if="
                    cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath) &&
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
              v-if="cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath)"
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
                      cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath)
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
                  :loading="module.uniqueName === deletingCamera"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                  @click="
                    () =>
                      (confirmDeleteDialog = {
                        show: true,
                        nickname: module.nickname,
                        cameraUniqueName: module.uniqueName
                      })
                  "
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
                  <td>{{ cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath) }}</td>
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
                      cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath)
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
                  :loading="module.uniqueName === deletingCamera"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                  @click="
                    () =>
                      (confirmDeleteDialog = {
                        show: true,
                        nickname: module.nickname,
                        cameraUniqueName: module.uniqueName
                      })
                  "
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
            activeVisionModules.find(
              (it) => cameraInfoFor(it.matchedCameraInfo).uniquePath === cameraInfoFor(viewingCamera[0]).uniquePath
            )?.mismatch
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
          <PvCameraMatchCard :saved="viewingCamera[0]" :current="getMatchedDevice(viewingCamera[0])" />
        </v-card-text>
        <v-card-text v-else>
          <PvCameraInfoCard :camera="getMatchedDevice(viewingCamera[0])" />
        </v-card-text>
      </v-card>
    </v-dialog>

    <pv-delete-modal
      v-model="confirmDeleteDialog.show"
      title="Delete Camera"
      :description="`Are you sure you want to delete the camera '${useCameraSettingsStore().currentCameraSettings.nickname}'? This action cannot be undone.`"
      :expected-confirmation-text="confirmDeleteDialog.nickname"
      :on-confirm="() => deleteThisCamera(confirmDeleteDialog.cameraUniqueName)"
    />
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
