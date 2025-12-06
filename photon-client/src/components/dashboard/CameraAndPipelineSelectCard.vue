<script setup lang="ts">
import PvSelect, { type SelectItem } from "@/components/common/pv-select.vue";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import { computed, ref } from "vue";
import PvIcon from "@/components/common/pv-icon.vue";
import PvInput from "@/components/common/pv-input.vue";
import { PipelineType } from "@/types/PipelineTypes";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useTheme } from "vuetify";
import PvDeleteModal from "@/components/common/pv-delete-modal.vue";

const theme = useTheme();

const changeCurrentCameraUniqueName = (cameraUniqueName: string) => {
  useCameraSettingsStore().setCurrentCameraUniqueName(cameraUniqueName, true);

  switch (useCameraSettingsStore().cameras[cameraUniqueName].pipelineSettings.pipelineType) {
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
};

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
const pipelineNamesWrapper = computed<SelectItem[]>(() => {
  const pipelineNames = useCameraSettingsStore().pipelineNames.map((name, index) => ({ name: name, value: index }));

  if (useCameraSettingsStore().isDriverMode) {
    pipelineNames.push({ name: "Driver Mode", value: WebsocketPipelineType.DriverMode });
  }
  if (useCameraSettingsStore().isFocusMode) {
    pipelineNames.push({ name: "Focus Mode", value: WebsocketPipelineType.FocusCamera });
  }
  if (useCameraSettingsStore().isCalibrationMode) {
    pipelineNames.push({ name: "3D Calibration Mode", value: WebsocketPipelineType.Calib3d });
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
const wrappedCameras = computed<SelectItem[]>(() =>
  Object.keys(useCameraSettingsStore().cameras).map((cameraUniqueName) => ({
    name: useCameraSettingsStore().cameras[cameraUniqueName].nickname,
    value: cameraUniqueName
  }))
);
</script>

<template>
  <v-card color="surface" class="rounded-12">
    <v-row no-gutters class="pl-4 pt-2 pb-0">
      <v-col cols="10" class="pa-0">
        <pv-select
          v-if="!isCameraNameEdit"
          v-model="useStateStore().currentCameraUniqueName"
          label="Camera"
          :items="wrappedCameras"
          @update:modelValue="changeCurrentCameraUniqueName"
        />
        <pv-input
          v-else
          v-model="currentCameraName"
          class="pt-2"
          :input-cols="12 - 3"
          :rules="[(v) => checkCameraName(v)]"
          label="Camera"
          @onEnter="saveCameraNameEdit"
          @onEscape="cancelCameraNameEdit"
        />
      </v-col>
      <v-col cols="2" style="display: flex; align-items: center; justify-content: center">
        <div v-if="isCameraNameEdit" style="display: flex; gap: 14px">
          <pv-icon
            icon-name="mdi-content-save"
            color="#c5c5c5"
            :disabled="checkCameraName(currentCameraName) !== true"
            @click="() => saveCameraNameEdit(currentCameraName)"
          />
          <pv-icon icon-name="mdi-cancel" color="red-darken-2" @click="cancelCameraNameEdit" />
        </div>
        <pv-icon
          v-else
          color="#c5c5c5"
          icon-name="mdi-pencil"
          tooltip="Edit Camera Name"
          @click="startCameraNameEdit"
        />
      </v-col>
    </v-row>
    <v-row no-gutters class="pl-4 pb-0 pt-0">
      <v-col cols="10" class="pa-0">
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
          @update:modelValue="(args) => useCameraSettingsStore().changeCurrentPipelineIndex(args, true)"
        />
        <pv-input
          v-else
          v-model="currentPipelineName"
          :input-cols="12 - 3"
          :rules="[(v) => checkPipelineName(v)]"
          label="Pipeline"
          @onEnter="(v) => savePipelineNameEdit(v)"
          @onEscape="cancelPipelineNameEdit"
        />
      </v-col>
      <v-col cols="2" class="pa-0" style="display: flex; align-items: center; justify-content: center">
        <div v-if="isPipelineNameEdit" style="display: flex; gap: 14px">
          <pv-icon
            icon-name="mdi-content-save"
            color="#c5c5c5"
            :disabled="checkPipelineName(currentPipelineName) !== true"
            @click="() => savePipelineNameEdit(currentPipelineName)"
          />
          <pv-icon icon-name="mdi-cancel" color="red-darken-2" @click="cancelPipelineNameEdit" />
        </div>
        <v-menu v-else-if="!useCameraSettingsStore().isDriverMode" offset="7">
          <template #activator="{ props }">
            <v-icon color="#c5c5c5" v-bind="props" @click="cancelPipelineNameEdit"> mdi-menu </v-icon>
          </template>
          <v-list density="compact" color="primary">
            <v-list-item @click="startPipelineNameEdit">
              <v-list-item-title>
                <pv-icon color="#c5c5c5" :right="true" icon-name="mdi-pencil" tooltip="Edit pipeline name" />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="duplicateCurrentPipeline">
              <v-list-item-title>
                <pv-icon color="#c5c5c5" :right="true" icon-name="mdi-content-copy" tooltip="Duplicate pipeline" />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="showCreatePipelineDialog">
              <v-list-item-title>
                <pv-icon color="green" :right="true" icon-name="mdi-plus" tooltip="Add new pipeline" />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="showPipelineDeletionConfirmationDialog = true">
              <v-list-item-title>
                <pv-icon
                  color="red-darken-2"
                  :right="true"
                  icon-name="mdi-trash-can-outline"
                  tooltip="Delete pipeline"
                />
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
        <pv-icon
          v-else-if="useCameraSettingsStore().isDriverMode && useCameraSettingsStore().pipelineNames.length === 0"
          color="#c5c5c5"
          :right="true"
          icon-name="mdi-plus"
          tooltip="Add new pipeline"
          @click="showCreatePipelineDialog"
        />
      </v-col>
    </v-row>
    <v-row no-gutters class="pl-4 pt-0 pb-4">
      <v-col cols="10" class="pa-0">
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
          @update:modelValue="showPipelineTypeChangeDialog = true"
        />
      </v-col>
    </v-row>
    <v-dialog v-model="showPipelineCreationDialog" persistent width="500">
      <v-card color="surface">
        <v-card-title class="pb-0"> Create New Pipeline </v-card-title>
        <v-card-text class="pt-0 pb-0">
          <pv-input
            v-model="newPipelineName"
            placeholder="Pipeline Name"
            :label-cols="4"
            :input-cols="12 - 4"
            label="Pipeline Name"
            :rules="[(v) => checkPipelineName(v)]"
          />
          <pv-select
            v-model="newPipelineType"
            :select-cols="12 - 4"
            label="Tracking Type"
            tooltip="Pipeline type, which changes the type of processing that will happen on input frames"
            :items="validNewPipelineTypes"
          />
        </v-card-text>
        <v-card-actions class="pr-5 pt-10px pb-5">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="cancelPipelineCreation"
          >
            Cancel
          </v-btn>
          <v-btn
            color="buttonActive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            :disabled="checkPipelineName(newPipelineName) !== true"
            @click="createNewPipeline"
          >
            Create
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <pv-delete-modal
      v-model="showPipelineDeletionConfirmationDialog"
      :width="500"
      title="Delete Pipeline"
      description="Are you sure you want to delete the current pipeline? This action cannot be undone."
      :on-confirm="confirmDeleteCurrentPipeline"
    />
    <v-dialog v-model="showPipelineTypeChangeDialog" persistent width="600">
      <v-card color="surface" dark>
        <v-card-title class="pb-0">Change Pipeline Type</v-card-title>
        <v-card-text>
          Are you sure you want to change the current pipeline type? This will cause all the pipeline settings to be
          overwritten and they will be lost. If this isn't what you want, duplicate this pipeline first or export
          settings.
        </v-card-text>
        <v-card-actions class="pa-5 pt-0">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            class="text-black"
            @click="cancelChangePipelineType"
          >
            Cancel
          </v-btn>
          <v-btn
            color="buttonActive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="confirmChangePipelineType"
          >
            Confirm
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>
