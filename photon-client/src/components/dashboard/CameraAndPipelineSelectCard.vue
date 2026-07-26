<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import type { SelectItem } from "@/components/common/form/pv-select.vue";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import { computed, ref } from "vue";

import { PipelineType } from "@/types/PipelineTypes";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import IconContentSave from "~icons/mdi/content-save";
import IconCancel from "~icons/mdi/cancel";
import IconPencil from "~icons/mdi/pencil";
import IconContentCopy from "~icons/mdi/content-copy";
import IconPlus from "~icons/mdi/plus";
import IconTrashCanOutline from "~icons/mdi/trash-can-outline";
import IconMenu from "~icons/mdi/menu";

// Common RegEx used for naming both pipelines and cameras
const nameChangeRegex = /^[A-Za-z0-9_ \-)(]*[A-Za-z0-9][A-Za-z0-9_ \-)(.]*$/;

// Camera Name Edit
const isCameraNameEdit = ref(false);
const currentCameraName = ref(useCameraSettingsStore().currentCameraSettings.nickname);
const startCameraNameEdit = () => {
  currentCameraName.value = useCameraSettingsStore().currentCameraSettings.nickname;
  isCameraNameEdit.value = true;
};
const checkCameraName = (name: string): string | boolean => {
  if (!nameChangeRegex.test(name))
    return "A camera name can only contain letters, numbers, spaces, underscores, hyphens, parenthesis, and periods";
  if (useCameraSettingsStore().cameraNames.some((cameraName) => cameraName === name))
    return "This camera name has already been used";

  return true;
};
const saveCameraNameEdit = (newName: string) => {
  useCameraSettingsStore()
    .changeCameraNickname(newName, false)
    .then((response) => {
      useStateStore().showSnackbarMessage({ color: "success", message: response.data.text || response.data });
      useCameraSettingsStore().currentCameraSettings.nickname = newName;
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: error.response.data.text || error.response.data
        });
      } else if (error.request) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "Error while trying to process the request! The backend didn't respond."
        });
      } else {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "An error occurred while trying to process the request."
        });
      }
      currentCameraName.value = useCameraSettingsStore().currentCameraSettings.nickname;
    })
    .finally(() => (isCameraNameEdit.value = false));
};
const cancelCameraNameEdit = () => {
  isCameraNameEdit.value = false;
  currentCameraName.value = useCameraSettingsStore().currentCameraSettings.nickname;
};

// Pipeline Name Edit
const pipelineNamesWrapper = computed(() => {
  const pipelineNames = useCameraSettingsStore().pipelineNames.map((name, index) => ({ name: name, value: index }));

  if (useCameraSettingsStore().isDriverMode) {
    pipelineNames.push({ name: "Driver Mode", value: WebsocketPipelineType.DriverMode.valueOf() });
  }
  if (useCameraSettingsStore().isFocusMode) {
    pipelineNames.push({ name: "Focus Mode", value: WebsocketPipelineType.FocusCamera.valueOf() });
  }
  if (useCameraSettingsStore().isCalibrationMode) {
    pipelineNames.push({ name: "3D Calibration Mode", value: WebsocketPipelineType.Calib3d.valueOf() });
  }

  return pipelineNames;
});
const isPipelineNameEdit = ref(false);
const currentPipelineName = ref(useCameraSettingsStore().currentPipelineSettings.pipelineNickname);
const startPipelineNameEdit = () => {
  currentPipelineName.value = useCameraSettingsStore().currentPipelineSettings.pipelineNickname;
  isPipelineNameEdit.value = true;
};
const checkPipelineName = (name: string): string | boolean => {
  if (!nameChangeRegex.test(name))
    return "A pipeline name can only contain letters, numbers, spaces, underscores, hyphens, parenthesis, and periods";
  if (useCameraSettingsStore().pipelineNames.some((pipelineName) => pipelineName === name))
    return "This pipeline name has already been used";

  return true;
};
const savePipelineNameEdit = (name: string) => {
  useCameraSettingsStore().changeCurrentPipelineNickname(name);
  isPipelineNameEdit.value = false;
};
const cancelPipelineNameEdit = () => {
  isPipelineNameEdit.value = false;
  currentPipelineName.value = useCameraSettingsStore().currentPipelineSettings.pipelineNickname;
};

// Pipeline Creation
const showPipelineCreationDialog = ref(false);
const newPipelineName = ref("");
const newPipelineType = ref<WebsocketPipelineType>(useCameraSettingsStore().currentWebsocketPipelineType);
const validNewPipelineTypes = computed(() => {
  const pipelineTypes = [
    { name: "Reflective", value: WebsocketPipelineType.Reflective },
    { name: "Colored Shape", value: WebsocketPipelineType.ColoredShape },
    { name: "AprilTag", value: WebsocketPipelineType.AprilTag },
    { name: "ArUco", value: WebsocketPipelineType.Aruco }
  ];
  if (useSettingsStore().general.supportedBackends.length > 0) {
    pipelineTypes.push({ name: "Object Detection", value: WebsocketPipelineType.ObjectDetection });
  }
  return pipelineTypes;
});
const showCreatePipelineDialog = () => {
  newPipelineName.value = "";
  newPipelineType.value = useCameraSettingsStore().currentWebsocketPipelineType;
  showPipelineCreationDialog.value = true;
};
const createNewPipeline = () => {
  const type = newPipelineType.value;
  if (type === WebsocketPipelineType.DriverMode || type === WebsocketPipelineType.Calib3d) return;
  useCameraSettingsStore().createNewPipeline(newPipelineName.value, type);
  showPipelineCreationDialog.value = false;
};
const cancelPipelineCreation = () => {
  showPipelineCreationDialog.value = false;
  newPipelineName.value = "";
  newPipelineType.value = useCameraSettingsStore().currentWebsocketPipelineType;
};

// Pipeline Deletion
const showPipelineDeletionConfirmationDialog = ref(false);
const confirmDeleteCurrentPipeline = () => {
  useCameraSettingsStore().deleteCurrentPipeline();
  showPipelineDeletionConfirmationDialog.value = false;
};

// Pipeline Type Change
const showPipelineTypeChangeDialog = ref(false);
const pipelineTypesWrapper = computed<{ name: string; value: number }[]>(() => {
  const pipelineTypes = [
    { name: "Reflective", value: WebsocketPipelineType.Reflective },
    { name: "Colored Shape", value: WebsocketPipelineType.ColoredShape },
    { name: "AprilTag", value: WebsocketPipelineType.AprilTag },
    { name: "ArUco", value: WebsocketPipelineType.Aruco }
  ];
  if (useSettingsStore().general.supportedBackends.length > 0) {
    pipelineTypes.push({ name: "Object Detection", value: WebsocketPipelineType.ObjectDetection });
  }

  if (useCameraSettingsStore().isDriverMode) {
    pipelineTypes.push({ name: "Driver Mode", value: WebsocketPipelineType.DriverMode });
  }
  if (useCameraSettingsStore().isFocusMode) {
    pipelineTypes.push({ name: "Focus Mode", value: WebsocketPipelineType.FocusCamera });
  }
  if (useCameraSettingsStore().isCalibrationMode) {
    pipelineTypes.push({ name: "3D Calibration Mode", value: WebsocketPipelineType.Calib3d });
  }

  return pipelineTypes;
});
const pipelineType = ref<WebsocketPipelineType>(useCameraSettingsStore().currentWebsocketPipelineType);
const currentPipelineType = computed<WebsocketPipelineType>({
  get: () => {
    if (useCameraSettingsStore().isDriverMode) return WebsocketPipelineType.DriverMode;
    if (useCameraSettingsStore().isFocusMode) return WebsocketPipelineType.FocusCamera;
    if (useCameraSettingsStore().isCalibrationMode) return WebsocketPipelineType.Calib3d;
    return pipelineType.value;
  },
  set: (v) => {
    pipelineType.value = v;
  }
});
const confirmChangePipelineType = () => {
  const type = currentPipelineType.value;
  if (type === WebsocketPipelineType.DriverMode || type === WebsocketPipelineType.Calib3d) return;
  useCameraSettingsStore().changeCurrentPipelineType(type);
  showPipelineTypeChangeDialog.value = false;
};
const cancelChangePipelineType = () => {
  pipelineType.value = useCameraSettingsStore().currentWebsocketPipelineType;
  showPipelineTypeChangeDialog.value = false;
};

// Pipeline duplication'
const duplicateCurrentPipeline = () => {
  useCameraSettingsStore().duplicatePipeline(useCameraSettingsStore().currentCameraSettings.currentPipelineIndex);
};

// Change Props whenever the pipeline settings are changed
useCameraSettingsStore().$subscribe((mutation, state) => {
  const currentCameraSettings = state.cameras[useStateStore().currentCameraUniqueName];

  switch (currentCameraSettings.pipelineSettings.pipelineType) {
    case PipelineType.Reflective:
      pipelineType.value = WebsocketPipelineType.Reflective;
      break;
    case PipelineType.ColoredShape:
      pipelineType.value = WebsocketPipelineType.ColoredShape;
      break;
    case PipelineType.AprilTag:
      pipelineType.value = WebsocketPipelineType.AprilTag;
      break;
    case PipelineType.Aruco:
      pipelineType.value = WebsocketPipelineType.Aruco;
      break;
    case PipelineType.ObjectDetection:
      pipelineType.value = WebsocketPipelineType.ObjectDetection;
      break;
  }
});
const wrappedCameras = computed<SelectItem<string>[]>(() =>
  Object.keys(useCameraSettingsStore().cameras).map((cameraUniqueName) => ({
    name: useCameraSettingsStore().cameras[cameraUniqueName].nickname,
    value: cameraUniqueName
  }))
);
</script>

<template>
  <pv-card>
    <div class="flex flex-wrap pb-0">
      <div class="w-5/6 p-0">
        <pv-select
          v-if="!isCameraNameEdit"
          v-model="useStateStore().currentCameraUniqueName"
          label="Camera"
          :items="wrappedCameras"
          class="pt-0 pb-1"
          @update:modelValue="pipelineType = useCameraSettingsStore().currentWebsocketPipelineType"
        />
        <pv-input
          v-else
          v-model="currentCameraName"
          :input-cols="12 - 3"
          :rules="[(v) => typeof v === 'string' && checkCameraName(v)]"
          label="Camera"
          @onEnter="saveCameraNameEdit"
          @onEscape="cancelCameraNameEdit"
        />
      </div>
      <div class="flex w-1/6 items-center justify-center">
        <div v-if="isCameraNameEdit" style="display: flex; gap: 14px">
          <pv-tooltipped-icon
            :icon="IconContentSave"
            color="#c5c5c5"
            :disabled="checkCameraName(currentCameraName) !== true"
            @click="() => saveCameraNameEdit(currentCameraName)"
          />
          <pv-tooltipped-icon :icon="IconCancel" color="red-darken-2" @click="cancelCameraNameEdit" />
        </div>
        <pv-tooltipped-icon
          v-else
          color="#c5c5c5"
          :icon="IconPencil"
          tooltip="Edit Camera Name"
          @click="startCameraNameEdit"
        />
      </div>
    </div>
    <div class="flex flex-wrap pt-0 pb-0">
      <div class="w-5/6 p-0">
        <pv-select
          v-if="!isPipelineNameEdit"
          :model-value="useCameraSettingsStore().currentCameraSettings.currentPipelineIndex"
          label="Pipeline"
          tooltip="Each pipeline runs on a camera output and stores a unique set of processing settings"
          :disabled="
            useCameraSettingsStore().isDriverMode ||
            useCameraSettingsStore().isFocusMode ||
            useCameraSettingsStore().isCalibrationMode ||
            !useCameraSettingsStore().hasConnected
          "
          :items="pipelineNamesWrapper"
          class="pt-0 pb-1"
          @update:modelValue="(args) => useCameraSettingsStore().changeCurrentPipelineIndex(args, true)"
        />
        <pv-input
          v-else
          v-model="currentPipelineName"
          :input-cols="12 - 3"
          :rules="[(v) => typeof v === 'string' && checkPipelineName(v)]"
          label="Pipeline"
          @onEnter="(v) => savePipelineNameEdit(v)"
          @onEscape="cancelPipelineNameEdit"
        />
      </div>
      <div class="flex w-1/6 items-center justify-center p-0">
        <div v-if="isPipelineNameEdit" style="display: flex; gap: 14px">
          <pv-tooltipped-icon
            :icon="IconContentSave"
            color="#c5c5c5"
            :disabled="checkPipelineName(currentPipelineName) !== true"
            @click="() => savePipelineNameEdit(currentPipelineName)"
          />
          <pv-tooltipped-icon :icon="IconCancel" color="red-darken-2" @click="cancelPipelineNameEdit" />
        </div>
        <pv-dropdown-menu
          v-else-if="!useCameraSettingsStore().isDriverMode"
          :items="[
            { icon: IconPencil, label: 'Rename', color: '#c5c5c5' },
            { icon: IconContentCopy, label: 'Duplicate', color: '#c5c5c5' },
            { icon: IconPlus, label: 'New Pipeline', color: 'green' },
            { icon: IconTrashCanOutline, label: 'Delete', color: 'red-darken-2' }
          ]"
          @select="
            (i) => {
              switch (i) {
                case 0:
                  startPipelineNameEdit();
                  break;
                case 1:
                  duplicateCurrentPipeline();
                  break;
                case 2:
                  showCreatePipelineDialog();
                  break;
                case 3:
                  showPipelineDeletionConfirmationDialog = true;
                  break;
              }
            }
          "
        >
          <template #trigger>
            <pv-button size="icon">
              <pv-icon color="#c5c5c5" :icon="IconMenu" @click="cancelPipelineNameEdit" />
            </pv-button>
          </template>
        </pv-dropdown-menu>
        <pv-tooltipped-icon
          v-else-if="useCameraSettingsStore().isDriverMode && useCameraSettingsStore().pipelineNames.length === 0"
          color="#c5c5c5"
          :right="true"
          :icon="IconPlus"
          tooltip="Add new pipeline"
          @click="showCreatePipelineDialog"
        />
      </div>
    </div>
    <div class="flex flex-wrap pt-0">
      <div class="w-5/6 p-0">
        <pv-select
          v-model="currentPipelineType"
          label="Type"
          tooltip="Changes the pipeline type, which changes the type of processing that will happen on input frames"
          :disabled="
            useCameraSettingsStore().isDriverMode ||
            useCameraSettingsStore().isFocusMode ||
            useCameraSettingsStore().isCalibrationMode ||
            !useCameraSettingsStore().hasConnected
          "
          :items="pipelineTypesWrapper"
          class="pt-0 pb-1"
          @update:modelValue="showPipelineTypeChangeDialog = true"
        />
      </div>
    </div>
    <pv-dialog v-model="showPipelineCreationDialog" persistent width="500">
      <pv-card class="flex flex-col gap-3">
        <div class="text-lg font-semibold">Create New Pipeline</div>
        <div class="pt-0 pb-0">
          <pv-input
            v-model="newPipelineName"
            placeholder="Pipeline Name"
            :label-cols="4"
            :input-cols="12 - 4"
            label="Pipeline Name"
            :rules="[(v) => typeof v === 'string' && checkPipelineName(v)]"
          />
          <pv-select
            v-model="newPipelineType"
            :select-cols="12 - 4"
            label="Tracking Type"
            tooltip="Pipeline type, which changes the type of processing that will happen on input frames"
            :items="validNewPipelineTypes"
          />
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <pv-button variant="passive" @click="cancelPipelineCreation"> Cancel </pv-button>
          <pv-button
            variant="primary"
            :disabled="checkPipelineName(newPipelineName) !== true"
            @click="createNewPipeline"
          >
            Create
          </pv-button>
        </div>
      </pv-card>
    </pv-dialog>
    <pv-delete-modal
      v-model="showPipelineDeletionConfirmationDialog"
      :width="500"
      title="Delete Pipeline"
      description="Are you sure you want to delete the current pipeline? This action cannot be undone."
      :on-confirm="confirmDeleteCurrentPipeline"
    />
    <pv-dialog v-model="showPipelineTypeChangeDialog" persistent width="600">
      <pv-card class="flex flex-col gap-3">
        <div class="text-lg font-semibold">Change Pipeline Type</div>
        <div>
          Are you sure you want to change the current pipeline type? This will cause all the pipeline settings to be
          overwritten and they will be lost. If this isn't what you want, duplicate this pipeline first or export
          settings.
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <pv-button variant="passive" @click="cancelChangePipelineType"> Cancel </pv-button>
          <pv-button variant="danger" @click="confirmChangePipelineType"> Confirm </pv-button>
        </div>
      </pv-card>
    </pv-dialog>
  </pv-card>
</template>
