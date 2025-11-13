<script setup lang="ts">
import { ref, computed, inject } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import type { ObjectDetectionModelProperties } from "@/types/SettingTypes";
import pvInput from "@/components/common/pv-input.vue";
import { useTheme } from "vuetify";
import { axiosPost } from "@/lib/PhotonUtils";

const theme = useTheme();
const showImportDialog = ref(false);
const showInfo = ref({ show: false, model: {} as ObjectDetectionModelProperties });
const confirmDeleteDialog = ref({ show: false, model: {} as ObjectDetectionModelProperties });
const showRenameDialog = ref({
  show: false,
  model: {} as ObjectDetectionModelProperties,
  newName: ""
});

const address = inject<string>("backendHost");

const importModelFile = ref<File | null>(null);
const importLabels = ref<string | null>(null);
const importHeight = ref<number | null>(null);
const importWidth = ref<number | null>(null);
const importVersion = ref<string | null>(null);

// TODO gray out the button when model is uploading
const handleImport = async () => {
  if (importModelFile.value === null) return;

  const formData = new FormData();

  formData.append("modelFile", importModelFile.value);
  formData.append("labels", importLabels.value?.toString() || "");
  formData.append("height", importHeight.value?.toString() || "");
  formData.append("width", importWidth.value?.toString() || "");
  formData.append("version", importVersion.value?.toString() || "");

  useStateStore().showSnackbarMessage({
    message: "Importing Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  axiosPost("/objectdetection/import", "import an object detection model", formData, {
    headers: { "Content-Type": "multipart/form-data" },
    onUploadProgress: ({ progress }) => {
      const uploadPercentage = (progress || 0) * 100.0;
      if (uploadPercentage < 99.5) {
        useStateStore().showSnackbarMessage({
          message: "Object Detection Model Upload in Process, " + uploadPercentage.toFixed(2) + "% complete",
          color: "secondary",
          timeout: -1
        });
      } else {
        useStateStore().showSnackbarMessage({
          message: "Processing uploaded Object Detection Model...",
          color: "secondary",
          timeout: -1
        });
      }
    }
  });

  showImportDialog.value = false;

  importModelFile.value = null;
  importLabels.value = null;
  importHeight.value = null;
  importWidth.value = null;
  importVersion.value = null;
};

const deleteModel = async (model: ObjectDetectionModelProperties) => {
  useStateStore().showSnackbarMessage({
    message: "Deleting Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  axiosPost("/objectdetection/delete", "delete an object detection model", {
    modelPath: model.modelPath
  });

  confirmDeleteDialog.value.show = false;
};

const renameModel = async (model: ObjectDetectionModelProperties, newName: string) => {
  useStateStore().showSnackbarMessage({
    message: "Renaming Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  axiosPost("/objectdetection/rename", "rename an object detection model", {
    modelPath: model.modelPath.replace("file:", ""),
    newName: newName
  });
  showRenameDialog.value.show = false;
};

// Filters out models that are not supported by the current backend, and returns a flattened list.
const supportedModels = computed(() => {
  const { availableModels, supportedBackends } = useSettingsStore().general;
  const isSupported = (model: any) => {
    // Check if model's family is in the list of supported backends
    return supportedBackends.some((backend: string) => backend.toLowerCase() === model.family.toLowerCase());
  };

  // Filter models where the family is supported and flatten the list
  return availableModels.filter(isSupported);
});

const exportModels = ref();
const openExportPrompt = () => {
  exportModels.value.click();
};

const exportIndividualModel = ref();
const openExportIndividualModelPrompt = () => {
  exportIndividualModel.value.click();
};

const showNukeDialog = ref(false);
const expected = "Delete Models";
const yesDeleteMyModelsText = ref("");
const nukeModels = () => {
  axiosPost("/objectdetection/nuke", "clear and reset object detection models");
  showNukeDialog.value = false;
};

const showBulkImportDialog = ref(false);
const importFile = ref<File | null>(null);
const handleBulkImport = () => {
  if (importFile.value === null) return;

  const formData = new FormData();
  formData.append("data", importFile.value);

  axiosPost("/objectdetection/bulkimport", "import object detection models", formData, {
    headers: { "Content-Type": "multipart/form-data" },
    onUploadProgress: ({ progress }) => {
      const uploadPercentage = (progress || 0) * 100.0;
      if (uploadPercentage < 99.5) {
        useStateStore().showSnackbarMessage({
          message: "Object Detection Models Upload in Progress",
          color: "secondary",
          timeout: -1,
          progressBar: uploadPercentage,
          progressBarColor: "primary"
        });
      } else {
        useStateStore().showSnackbarMessage({
          message: "Importing New Object Detection Models...",
          color: "secondary",
          timeout: -1
        });
      }
    }
  });
  showImportDialog.value = false;
  importFile.value = null;
};
</script>

<template>
  <v-card class="mb-3" color="surface">
    <v-card-title>Object Detection</v-card-title>
    <div class="pa-5 pt-0">
      <v-row>
        <v-col cols="12" sm="6">
          <v-btn
            color="buttonActive"
            class="justify-center"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="() => (showImportDialog = true)"
          >
            <v-icon start class="open-icon"> mdi-import </v-icon>
            <span class="open-label">Import Model</span>
          </v-btn>
          <v-dialog
            v-model="showImportDialog"
            width="600"
            @update:modelValue="
              () => {
                importModelFile = null;
                importLabels = null;
                importHeight = null;
                importWidth = null;
                importVersion = null;
              }
            "
          >
            <v-card color="surface" dark>
              <v-card-title class="pb-0">Import New Object Detection Model</v-card-title>
              <v-card-text>
                <span v-if="useSettingsStore().general.supportedBackends?.includes('RKNN')"
                  >Upload a new object detection model to this device that can be used in a pipeline. Note that ONLY
                  640x640 YOLOv5, YOLOv8, YOLOv11, and YOLOv11 OBB models trained and converted to `.rknn` format for
                  RK3588 SOCs are currently supported!</span
                >
                <span v-else-if="useSettingsStore().general.supportedBackends?.includes('RUBIK')"
                  >Upload a new object detection model to this device that can be used in a pipeline. Note that ONLY
                  640x640 YOLOv8 and YOLOv11 models trained and converted to `.tflite` format for QCS6490 compatible
                  backends are currently supported!
                </span>
                <span v-else>
                  If you're seeing this, something broke; please file a ticket and tell us the details of your
                  situation.</span
                >
                <div class="pa-5 pb-0">
                  <v-file-input
                    v-model="importModelFile"
                    variant="underlined"
                    label="Model File"
                    :accept="
                      useSettingsStore().general.supportedBackends?.includes('RKNN')
                        ? '.rknn'
                        : useSettingsStore().general.supportedBackends?.includes('RUBIK')
                          ? '.tflite'
                          : ''
                    "
                  />
                  <v-text-field
                    v-model="importLabels"
                    label="Labels"
                    placeholder="Comma separated labels, no spaces"
                    type="text"
                    variant="underlined"
                  />
                  <v-text-field v-model="importWidth" variant="underlined" label="Width" type="number" />
                  <v-text-field v-model="importHeight" variant="underlined" label="Height" type="number" />
                  <v-select
                    v-model="importVersion"
                    variant="underlined"
                    label="Model Version"
                    :items="
                      useSettingsStore().general.supportedBackends?.includes('RKNN')
                        ? ['YOLOv5', 'YOLOv8', 'YOLO11', 'YOLO11OBB']
                        : ['YOLOv8', 'YOLO11']
                    "
                  />
                  <v-btn
                    color="buttonActive"
                    width="100%"
                    :disabled="
                      importModelFile === null ||
                      importLabels === null ||
                      importWidth === null ||
                      importHeight === null ||
                      importVersion === null
                    "
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    @click="handleImport()"
                  >
                    <v-icon start class="open-icon" size="large"> mdi-import </v-icon>
                    <span class="open-label">Import Object Detection Model</span>
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
        <v-col cols="12" sm="6">
          <v-btn
            color="buttonActive"
            class="justify-center"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="() => (showBulkImportDialog = true)"
          >
            <v-icon start class="open-icon"> mdi-import </v-icon>
            <span class="open-label">Bulk Import</span>
          </v-btn>
          <v-dialog v-model="showBulkImportDialog" width="600">
            <v-card color="surface" dark>
              <v-card-title class="pb-0">Import Multiple Object Detection Models</v-card-title>
              <v-card-text>
                Upload a zip file containing multiple object detection models to this device. Note this zip file should
                only come from a previous export of object detection models.
                <div class="pa-5 pb-0">
                  <v-file-input v-model="importFile" variant="underlined" label="Zip File" accept=".zip" />
                  <v-btn
                    color="buttonActive"
                    width="100%"
                    :disabled="importFile === null"
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    @click="handleBulkImport()"
                  >
                    <v-icon start class="open-icon" size="large"> mdi-import </v-icon>
                    <span class="open-label">Bulk Import</span>
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
        <v-col cols="12" sm="6">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="openExportPrompt"
          >
            <v-icon start class="open-icon"> mdi-export </v-icon>
            <span class="open-label">Export Models</span>
          </v-btn>
          <a
            ref="exportModels"
            style="color: black; text-decoration: none; display: none"
            :href="`http://${address}/api/objectdetection/export`"
            download="photonvision-object-detection-models-export.zip"
            target="_blank"
          />
        </v-col>
        <v-col cols="12" sm="6">
          <v-btn
            color="error"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="() => (showNukeDialog = true)"
          >
            <v-icon left class="open-icon"> mdi-trash </v-icon>
            <span class="open-label">Clear and reset models</span>
          </v-btn>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="">
          <v-table fixed-header height="100%" density="compact" dark>
            <thead style="font-size: 1.25rem">
              <tr>
                <th>Model Nicknames</th>
                <th>Labels</th>
                <th>Delete</th>
                <th>Edit</th>
                <th>Info</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="model in supportedModels" :key="model.modelPath">
                <td>{{ model.nickname }}</td>
                <td>{{ model.labels.join(", ") }}</td>
                <td class="text-right">
                  <v-btn
                    icon
                    small
                    color="error"
                    title="Delete Model"
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    @click="() => (confirmDeleteDialog = { show: true, model })"
                  >
                    <v-icon size="large">mdi-trash-can-outline</v-icon>
                  </v-btn>
                </td>
                <td class="text-right">
                  <v-btn
                    icon
                    small
                    color="buttonActive"
                    title="Rename Model"
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    @click="() => (showRenameDialog = { show: true, model, newName: '' })"
                  >
                    <v-icon size="large">mdi-pencil</v-icon>
                  </v-btn>
                </td>
                <td class="text-right">
                  <v-btn
                    icon
                    small
                    color="buttonPassive"
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    @click="() => (showInfo = { show: true, model })"
                  >
                    <v-icon size="large">mdi-information</v-icon>
                  </v-btn>
                </td>
              </tr>
            </tbody>
          </v-table>

          <v-dialog v-model="confirmDeleteDialog.show" width="600">
            <v-card color="surface" dark>
              <v-card-title>Delete Object Detection Model</v-card-title>
              <v-card-text class="pt-0">
                Are you sure you want to delete the model {{ confirmDeleteDialog.model.nickname }}?
                <v-card-actions class="pt-5 pb-0 pr-0" style="justify-content: flex-end">
                  <v-btn
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    color="buttonPassive"
                    @click="confirmDeleteDialog.show = false"
                  >
                    Cancel
                  </v-btn>
                  <v-btn
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    color="error"
                    @click="deleteModel(confirmDeleteDialog.model)"
                  >
                    Delete
                  </v-btn>
                </v-card-actions>
              </v-card-text>
            </v-card>
          </v-dialog>
          <v-dialog v-model="showRenameDialog.show" width="600">
            <v-card color="surface" dark>
              <v-card-title>Rename Object Detection Model</v-card-title>
              <v-card-text class="pt-0">
                Enter a new name for the model {{ showRenameDialog.model.nickname }}:
                <div class="pa-5 pb-0">
                  <v-text-field v-model="showRenameDialog.newName" hide-details label="New Name" variant="underlined" />
                </div>
                <v-card-actions class="pt-5 pb-0 pr-0" style="justify-content: flex-end">
                  <v-btn
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    color="error"
                    @click="showRenameDialog.show = false"
                    >Cancel</v-btn
                  >
                  <v-btn
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    color="buttonActive"
                    @click="renameModel(showRenameDialog.model, showRenameDialog.newName)"
                    >Rename</v-btn
                  >
                </v-card-actions>
              </v-card-text>
            </v-card>
          </v-dialog>
          <v-dialog v-model="showInfo.show" width="600">
            <v-card color="surface" dark>
              <v-card-title>Object Detection Model Info</v-card-title>
              <v-card-text class="pt-0">
                <v-btn
                  color="buttonPassive"
                  width="100%"
                  :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                  @click="openExportIndividualModelPrompt"
                >
                  <v-icon left class="open-icon" size="large"> mdi-export </v-icon>
                  <span class="open-label">Export Model</span>
                </v-btn>
                <a
                  ref="exportIndividualModel"
                  style="color: black; text-decoration: none; display: none"
                  :href="`http://${address}/api/objectdetection/exportIndividual?modelPath=${showInfo.model.modelPath.replace('file:', '')}`"
                  :download="`${showInfo.model.nickname}_${showInfo.model.family}_${showInfo.model.version}_${showInfo.model.resolutionWidth}x${showInfo.model.resolutionHeight}_${showInfo.model.labels.join('_')}.${showInfo.model.family.toLowerCase()}`"
                  target="_blank"
                />
                <div class="pt-5">
                  <p>Model Path: {{ showInfo.model.modelPath }}</p>
                  <p>Model Nickname: {{ showInfo.model.nickname }}</p>
                  <p>Model Family: {{ showInfo.model.family }}</p>
                  <p>Model Version: {{ showInfo.model.version }}</p>
                  <p>Model Label(s): {{ showInfo.model.labels.join(", ") }}</p>
                  <p>Model Resolution: {{ showInfo.model.resolutionWidth }} x {{ showInfo.model.resolutionHeight }}</p>
                </div>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
      </v-row>
    </div>

    <v-dialog v-model="showNukeDialog" width="800" dark>
      <v-card color="surface" flat>
        <v-card-title style="display: flex; justify-content: center">
          <span class="open-label">
            <v-icon end color="error" class="open-icon ma-1" size="large">mdi-alert-outline</v-icon>
            Clear and Reset Object Detection Models
            <v-icon end color="error" class="open-icon ma-1" size="large">mdi-alert-outline</v-icon>
          </span>
        </v-card-title>
        <v-card-text class="pt-0 pb-10px">
          <v-row class="align-center text-white">
            <v-col cols="12" md="6">
              <span> This will delete ALL OF YOUR MODELS and re-extract the default models. </span>
            </v-col>
            <v-col cols="12" md="6">
              <v-btn
                color="buttonActive"
                style="float: right"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="openExportPrompt"
              >
                <v-icon start class="open-icon" size="large"> mdi-export </v-icon>
                <span class="open-label">Backup Models</span>
                <a
                  ref="exportModels"
                  style="color: black; text-decoration: none; display: none"
                  :href="`http://${address}/api/objectdetection/export`"
                  download="photonvision-object-detection-models-export.zip"
                  target="_blank"
                />
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text class="pt-0 pb-0">
          <pv-input
            v-model="yesDeleteMyModelsText"
            :label="'Type &quot;' + expected + '&quot;:'"
            :label-cols="6"
            :input-cols="6"
          />
        </v-card-text>
        <v-card-text class="pt-10px">
          <v-btn
            color="error"
            width="100%"
            :disabled="yesDeleteMyModelsText.toLowerCase() !== expected.toLowerCase()"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="nukeModels"
          >
            <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
            <span class="open-label">
              {{ $vuetify.display.mdAndUp ? "Delete models, I have backed up what I need" : "Delete Models" }}
            </span>
          </v-btn>
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<style scoped lang="scss">
.v-col-12 > .v-btn {
  width: 100%;
}

.pt-10px {
  padding-top: 10px !important;
}

@media only screen and (max-width: 351px) {
  .open-icon {
    margin: 0 !important;
  }
  .open-label {
    display: none;
  }
}
.v-table {
  width: 100%;
  height: 100%;
  text-align: center;

  th,
  td {
    font-size: 1rem !important;
    color: white !important;
    text-align: center !important;
  }

  td {
    font-family: monospace !important;
  }

  ::-webkit-scrollbar {
    width: 0;
    height: 0.55em;
    border-radius: 5px;
  }

  ::-webkit-scrollbar-track {
    -webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
    border-radius: 10px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: rgb(var(--v-theme-accent));
    border-radius: 10px;
  }
}
</style>
