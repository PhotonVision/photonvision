<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, inject, ref } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { PlaceholderCameraSettings, PVCameraInfo } from "@/types/SettingTypes";
import { axiosPost, getResolutionString, cameraInfoFor } from "@/lib/PhotonUtils";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";

import IconTrashCanOutline from "~icons/mdi/trash-can-outline";
import IconClose from "~icons/mdi/close";
import IconInformationOutline from "~icons/mdi/information-outline";
import IconPlus from "~icons/mdi/plus";

const backendHostname = inject<string>("backendHostname");
const formatUrl = (port: number) => `http://${backendHostname}:${port}/stream.mjpg`;

const activatingModule = ref(false);
const activateModule = async (moduleUniqueName: string) => {
  if (activatingModule.value) return;
  activatingModule.value = true;

  await axiosPost("/utils/activateMatchedCamera", "activate a matched camera", {
    cameraUniqueName: moduleUniqueName
  });
  activatingModule.value = false;
};

const assigningCamera = ref(false);
const assignCamera = async (cameraInfo: PVCameraInfo) => {
  if (assigningCamera.value) return;
  assigningCamera.value = true;

  const payload = {
    cameraInfo: cameraInfo
  };

  await axiosPost("/utils/assignUnmatchedCamera", "assign an unmatched camera", payload);
  assigningCamera.value = false;
};

const deactivatingModule = ref(false);
const deactivateModule = async (cameraUniqueName: string) => {
  if (deactivatingModule.value) return;
  deactivatingModule.value = true;
  await axiosPost("/utils/unassignCamera", "unassign a camera", { cameraUniqueName: cameraUniqueName });
  deactivatingModule.value = false;
};

const confirmDeleteDialog = ref({ show: false, nickname: "", cameraUniqueName: "" });
const deletingCamera = ref<string | null>(null);

const deleteThisCamera = async (cameraUniqueName: string) => {
  if (deletingCamera.value) return;
  deletingCamera.value = cameraUniqueName;
  await axiosPost("/utils/nukeOneCamera", "delete a camera", { cameraUniqueName: cameraUniqueName });
  deletingCamera.value = null;
};

const cameraConnected = (uniquePath: string | undefined): boolean => {
  if (!uniquePath) return false;
  return useStateStore().vsmState.allConnectedCameras.find((it) => it.uniquePath === uniquePath) !== undefined;
};

const unmatchedCameras = computed(() => {
  const activeVmPaths = Object.values(useCameraSettingsStore().cameras).map((it) => it.matchedCameraInfo.uniquePath);
  const disabledVmPaths = useStateStore().vsmState.disabledConfigs.map((it) => it.matchedCameraInfo.uniquePath);

  return useStateStore().vsmState.allConnectedCameras.filter(
    (it) => !activeVmPaths.includes(it.uniquePath) && !disabledVmPaths.includes(it.uniquePath)
  );
});

const activeVisionModules = computed(() =>
  Object.values(useCameraSettingsStore().cameras)
    // Ignore placeholder camera
    .filter((camera) => camera !== PlaceholderCameraSettings)
    // Display connected cameras first
    .sort(
      (first, second) =>
        (cameraConnected(second.matchedCameraInfo.uniquePath) ? 1 : 0) -
        (cameraConnected(first.matchedCameraInfo.uniquePath) ? 1 : 0)
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
 * Find the PVCameraInfo currently occupying the same uniquePath as the the given module
 */
const getMatchedDevice = (info: PVCameraInfo | undefined): PVCameraInfo => {
  if (!info) {
    return {
      type: "PVFileCameraInfo",
      path: "",
      name: "",
      uniquePath: ""
    };
  }
  return (
    useStateStore().vsmState.allConnectedCameras.find((it) => it.uniquePath === info.uniquePath) || {
      type: "PVFileCameraInfo",
      path: "",
      name: "",
      uniquePath: ""
    }
  );
};
</script>

<template>
  <div class="p-3">
    <div class="-mx-3 flex flex-wrap">
      <!-- Active modules -->
      <div
        v-for="(module, index) in activeVisionModules"
        :key="`enabled-${module.uniqueName}`"
        class="w-full px-3 pb-3 sm:w-1/2 lg:w-1/3"
      >
        <pv-card class="rounded-2xl">
          <div class="text-lg font-semibold wrap-break-word">
            {{ cameraInfoFor(module.matchedCameraInfo).name }}
          </div>
          <div v-if="!cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath)" class="text-sm">
            Status: <span class="inactive-status">Disconnected</span>
          </div>
          <div
            v-else-if="cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath) && !module.mismatch"
            class="text-sm"
          >
            Status: <span class="active-status">Active</span>
          </div>
          <div v-else class="text-sm">Status: <span class="mismatch-status">Mismatch</span></div>
          <div class="pt-3">
            <pv-table>
              <tbody>
                <tr
                  v-if="
                    cameraConnected(module.matchedCameraInfo.uniquePath) &&
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
            </pv-table>
            <div
              v-if="cameraConnected(module.matchedCameraInfo.uniquePath)"
              :id="`stream-container-${index}`"
              class="d-flex flex-column align-center mt-3 justify-center"
              style="height: 250px"
            >
              <photon-camera-stream
                :id="`output-camera-stream-${index}`"
                :camera-settings="module"
                stream-type="Processed"
              />
            </div>
          </div>
          <div class="pt-0">
            <div class="-mx-2 flex flex-wrap">
              <div class="w-full px-2 pb-3 md:w-1/3 md:pb-0">
                <pv-button
                  variant="passive"
                  block
                  @click="
                    setCameraView(
                      module.matchedCameraInfo,
                      cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath)
                    )
                  "
                >
                  <span>Details</span>
                </pv-button>
              </div>
              <div class="w-1/2 px-2 md:w-[41.666%]">
                <pv-button
                  variant="primary"
                  block
                  :loading="deactivatingModule"
                  @click="deactivateModule(module.uniqueName)"
                >
                  Deactivate
                </pv-button>
              </div>
              <div class="w-1/2 px-2 md:w-1/4">
                <pv-button
                  size="icon"
                  variant="danger"
                  block
                  :loading="module.uniqueName === deletingCamera"
                  @click="
                    () =>
                      (confirmDeleteDialog = {
                        show: true,
                        nickname: module.nickname,
                        cameraUniqueName: module.uniqueName
                      })
                  "
                >
                  <IconTrashCanOutline class="size-6" aria-hidden="true" />
                </pv-button>
              </div>
            </div>
          </div>
        </pv-card>
      </div>

      <!-- Deactivated modules -->
      <div
        v-for="module in disabledVisionModules"
        :key="`disabled-${module.uniqueName}`"
        class="w-full px-3 pb-3 sm:w-1/2 lg:w-1/3"
      >
        <pv-card class="rounded-2xl">
          <div class="text-lg font-semibold wrap-break-word">
            {{ module.cameraQuirks.baseName }}
          </div>
          <div class="text-sm">Status: <span class="inactive-status">Deactivated</span></div>
          <div class="pt-3">
            <pv-table>
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
                  <td>{{ cameraConnected(module.matchedCameraInfo.uniquePath) }}</td>
                </tr>
              </tbody>
            </pv-table>
          </div>
          <div class="pt-0">
            <div class="-mx-2 flex flex-wrap">
              <div class="w-full px-2 pb-3 md:w-1/3 md:pb-0">
                <pv-button
                  variant="passive"
                  block
                  @click="
                    setCameraView(
                      module.matchedCameraInfo,
                      cameraConnected(cameraInfoFor(module.matchedCameraInfo).uniquePath)
                    )
                  "
                >
                  <span>Details</span>
                </pv-button>
              </div>
              <div class="w-1/2 px-2 md:w-[41.666%]">
                <pv-button
                  variant="primary"
                  block
                  :loading="activatingModule"
                  @click="activateModule(module.uniqueName)"
                >
                  Activate
                </pv-button>
              </div>
              <div class="w-1/2 px-2 md:w-1/4">
                <pv-button
                  size="icon"
                  variant="danger"
                  block
                  :loading="module.uniqueName === deletingCamera"
                  @click="
                    () =>
                      (confirmDeleteDialog = {
                        show: true,
                        nickname: module.nickname,
                        cameraUniqueName: module.uniqueName
                      })
                  "
                >
                  <IconTrashCanOutline class="size-6" aria-hidden="true" />
                </pv-button>
              </div>
            </div>
          </div>
        </pv-card>
      </div>

      <!-- Unassigned cameras -->
      <div v-for="(camera, index) in unmatchedCameras" :key="index" class="w-full px-3 pb-3 sm:w-1/2 lg:w-1/3">
        <pv-card class="rounded-2xl">
          <div class="text-lg font-semibold wrap-break-word">
            <span v-if="camera.PVUsbCameraInfo">USB Camera:</span>
            <span v-else-if="camera.PVCSICameraInfo">CSI Camera:</span>
            <span v-else-if="camera.PVFileCameraInfo">File Camera:</span>
            <span v-else>Unknown Camera:</span>
            &nbsp;<span>{{ cameraInfoFor(camera)?.name ?? cameraInfoFor(camera)?.baseName }}</span>
          </div>
          <div class="text-sm">Status: Unassigned</div>
          <div class="pt-3">
            <span style="word-break: break-all">{{ cameraInfoFor(camera)?.path }}</span>
          </div>
          <div class="pt-0">
            <div class="-mx-2 flex flex-wrap">
              <div class="w-1/2 px-2">
                <pv-button variant="passive" block @click="setCameraView(camera, false)">
                  <span>Details</span>
                </pv-button>
              </div>
              <div class="w-1/2 px-2">
                <pv-button variant="primary" block :loading="assigningCamera" @click="assignCamera(camera)">
                  Activate
                </pv-button>
              </div>
            </div>
          </div>
        </pv-card>
      </div>

      <!-- Info card -->
      <div class="w-full px-3 pb-3 sm:w-1/2 lg:w-1/3">
        <pv-card
          variant="transparent"
          :bordered="false"
          :elevated="false"
          class="flex h-full flex-col justify-center"
        >
          <div class="flex flex-col items-center justify-center">
            <pv-icon size="64" color="primary" :icon="IconPlus" />
          </div>
          <div class="pt-3 text-lg font-semibold">Additional plugged in cameras will display here!</div>
        </pv-card>
      </div>
    </div>

    <!-- Camera details modal -->
    <pv-dialog v-model="viewingDetails" :max-width="800">
      <pv-card v-if="viewingCamera[0] !== null" class="flex flex-col gap-3">
        <div class="flex items-center justify-between text-lg font-semibold">
          <span class="wrap-break-word">
            {{ cameraInfoFor(viewingCamera[0])?.name ?? cameraInfoFor(viewingCamera[0])?.baseName }}
          </span>
          <pv-button variant="text" size="icon" :icon="IconClose" @click="setCameraView(null, null)" />
        </div>
        <div v-if="!viewingCamera[1]">
          <PvCameraInfoCard :camera="viewingCamera[0]" />
        </div>
        <div
          v-else-if="
            activeVisionModules.find((it) => it.matchedCameraInfo.uniquePath === viewingCamera[0]?.uniquePath)?.mismatch
          "
        >
          <pv-alert
            class="mb-3"
            color="buttonActive"
            text="A different camera may have been connected to this device! Compare the following information carefully."
            :icon="IconInformationOutline"
          />
          <PvCameraMatchCard :saved="viewingCamera[0]" :current="getMatchedDevice(viewingCamera[0])" />
        </div>
        <div v-else>
          <PvCameraInfoCard :camera="getMatchedDevice(viewingCamera[0])" />
        </div>
      </pv-card>
    </pv-dialog>

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
