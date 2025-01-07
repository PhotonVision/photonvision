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
      useStateStore().showSnackbarMessage({
        color: "success",
        message: response.data.text || response.data
      });
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
    { name: "Aruco", value: WebsocketPipelineType.Aruco }
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
    { name: "Aruco", value: WebsocketPipelineType.Aruco }
  ];
  if (useSettingsStore().general.supportedBackends.length > 0) {
    pipelineTypes.push({ name: "Object Detection", value: WebsocketPipelineType.ObjectDetection });
  }

  if (useCameraSettingsStore().isDriverMode) {
    pipelineTypes.push({ name: "Driver Mode", value: WebsocketPipelineType.DriverMode });
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
  <v-card color="primary">
    <v-row style="padding: 20px 12px 0 30px">
      <v-col cols="10" class="pa-0">
        <pv-select
          v-if="!isCameraNameEdit"
          v-model="useStateStore().currentCameraUniqueName"
          label="Camera"
          :items="wrappedCameras"
          @input="changeCurrentCameraUniqueName"
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
          <pv-icon icon-name="mdi-cancel" color="red darken-2" @click="cancelCameraNameEdit" />
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
    <v-row style="padding: 0 12px 0 30px">
      <v-col cols="10" class="pa-0">
        <pv-select
          v-if="!isPipelineNameEdit"
          :value="useCameraSettingsStore().currentCameraSettings.currentPipelineIndex"
          label="Pipeline"
          tooltip="Each pipeline runs on a camera output and stores a unique set of processing settings"
          :disabled="
            useCameraSettingsStore().isDriverMode ||
            useCameraSettingsStore().isCalibrationMode ||
            !useCameraSettingsStore().hasConnected
          "
          :items="pipelineNamesWrapper"
          @input="(args) => useCameraSettingsStore().changeCurrentPipelineIndex(args, true)"
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
          <pv-icon icon-name="mdi-cancel" color="red darken-2" @click="cancelPipelineNameEdit" />
        </div>
        <v-menu v-else-if="!useCameraSettingsStore().isDriverMode" offset-y nudge-bottom="7" auto>
          <template #activator="{ on }">
            <v-icon color="#c5c5c5" v-on="on" @click="cancelPipelineNameEdit"> mdi-menu </v-icon>
          </template>
          <v-list dark dense color="primary">
            <v-list-item @click="startPipelineNameEdit">
              <v-list-item-title>
                <pv-icon color="#c5c5c5" :right="true" icon-name="mdi-pencil" tooltip="Edit pipeline name" />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="showCreatePipelineDialog">
              <v-list-item-title>
                <pv-icon color="#c5c5c5" :right="true" icon-name="mdi-plus" tooltip="Add new pipeline" />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="showPipelineDeletionConfirmationDialog = true">
              <v-list-item-title>
                <pv-icon color="red darken-2" :right="true" icon-name="mdi-delete" tooltip="Delete pipeline" />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="duplicateCurrentPipeline">
              <v-list-item-title>
                <pv-icon color="#c5c5c5" :right="true" icon-name="mdi-content-copy" tooltip="Duplicate pipeline" />
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
    <v-row style="padding: 0 12px 24px 30px">
      <v-col cols="10" class="pa-0">
        <pv-select
          v-model="currentPipelineType"
          label="Type"
          tooltip="Changes the pipeline type, which changes the type of processing that will happen on input frames"
          :disabled="
            useCameraSettingsStore().isDriverMode ||
            useCameraSettingsStore().isCalibrationMode ||
            !useCameraSettingsStore().hasConnected
          "
          :items="pipelineTypesWrapper"
          @input="showPipelineTypeChangeDialog = true"
        />
      </v-col>
    </v-row>
    <v-dialog v-model="showPipelineCreationDialog" dark persistent width="500">
      <v-card dark color="primary">
        <v-card-title> Create New Pipeline </v-card-title>
        <v-card-text>
          <pv-input
            v-model="newPipelineName"
            placeholder="Pipeline Name"
            :label-cols="3"
            :input-cols="12 - 3"
            label="Pipeline Name"
            :rules="[(v) => checkPipelineName(v)]"
          />
          <pv-select
            v-model="newPipelineType"
            :select-cols="12 - 3"
            label="Tracking Type"
            tooltip="Pipeline type, which changes the type of processing that will happen on input frames"
            :items="validNewPipelineTypes"
          />
        </v-card-text>
        <v-divider />
        <v-card-actions>
          <v-spacer />
          <v-btn
            color="#ffd843"
            class="black--text"
            :disabled="checkPipelineName(newPipelineName) !== true"
            @click="createNewPipeline"
          >
            Save
          </v-btn>
          <v-btn color="error" @click="cancelPipelineCreation"> Cancel </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="showPipelineDeletionConfirmationDialog" dark width="500">
      <v-card dark color="primary">
        <v-card-title> Pipeline Deletion Confirmation </v-card-title>
        <v-card-text>
          Are you sure you want to delete the pipeline
          <b style="color: white; font-weight: bold">{{
            useCameraSettingsStore().currentPipelineSettings.pipelineNickname
          }}</b
          >? This cannot be undone.
        </v-card-text>
        <v-divider />
        <v-card-actions>
          <v-spacer />
          <v-btn color="error" @click="confirmDeleteCurrentPipeline"> Yes, I'm sure </v-btn>
          <v-btn color="#ffd843" class="black--text" @click="showPipelineDeletionConfirmationDialog = false">
            No, take me back
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="showPipelineTypeChangeDialog" persistent width="600">
      <v-card color="primary" dark>
        <v-card-title>Change Pipeline Type</v-card-title>
        <v-card-text>
          Are you sure you want to change the current pipeline type? This will cause all the pipeline settings to be
          overwritten and they will be lost. If this isn't what you want, duplicate this pipeline first or export
          settings.
        </v-card-text>
        <v-divider />
        <v-card-actions>
          <v-spacer />
          <v-btn color="error" @click="confirmChangePipelineType"> Yes, I'm sure </v-btn>
          <v-btn color="#ffd843" class="black--text" @click="cancelChangePipelineType"> No, take me back </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>
