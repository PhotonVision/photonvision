<script setup lang="ts">
import { ref, computed, inject } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import type { ObjectDetectionModelProperties } from "@/types/SettingTypes";
import pvInput from "@/components/common/pv-input.vue";

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
const importLabels = ref<String | null>(null);
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

  axios
    .post("/objectdetection/import", formData, {
      headers: { "Content-Type": "multipart/form-data" }
    })
    .then((response) => {
      useStateStore().showSnackbarMessage({
        message: response.data.text || response.data,
        color: "success"
      });
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

  axios
    .post("/objectdetection/delete", {
      modelPath: model.modelPath
    })
    .then((response) => {
      useStateStore().showSnackbarMessage({
        message: response.data.text || response.data,
        color: "success"
      });
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
    });
};

const renameModel = async (model: ObjectDetectionModelProperties, newName: string) => {
  useStateStore().showSnackbarMessage({
    message: "Renaming Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  axios
    .post("/objectdetection/rename", {
      modelPath: model.modelPath,
      newName: newName
    })
    .then((response) => {
      useStateStore().showSnackbarMessage({
        message: response.data.text || response.data,
        color: "success"
      });
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
    });
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
  axios
    .post("/objectdetection/nuke")
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Successfully dispatched the clear models command.",
        color: "success"
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          message: "The backend is unable to fulfill the request to clear the models.",
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
    });
  showNukeDialog.value = false;
};
</script>

<template>
  <v-card class="mb-3" style="background-color: #006492">
    <v-card-title class="pa-6">Object Detection</v-card-title>
    <div class="pa-6 pt-0">
      <v-row>
        <v-col cols="12" sm="6">
          <v-btn color="secondary" class="justify-center" @click="() => (showImportDialog = true)">
            <v-icon left class="open-icon"> mdi-import </v-icon>
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
            <v-card color="primary" dark>
              <v-card-title>Import New Object Detection Model</v-card-title>
              <v-card-text>
                Upload a new object detection model to this device that can be used in a pipeline. Note that ONLY
                640x640 YOLOv5, YOLOv8, and YOLOv11 models trained and converted to `.rknn` format for RK3588 CPUs are
                currently supported!
                <v-row class="mt-6 ml-4 mr-8">
                  <v-file-input v-model="importModelFile" label="Model File" accept=".rknn" />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-text-field
                    v-model="importLabels"
                    label="Labels"
                    placeholder="Comma separated labels, no spaces"
                    type="text"
                  />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-text-field v-model="importWidth" label="Width" type="number" />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-text-field v-model="importHeight" label="Height" type="number" />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-select v-model="importVersion" label="Model Version" :items="['YOLOv5', 'YOLOv8', 'YOLO11']" />
                </v-row>
                <v-row class="mt-12 ml-8 mr-8 mb-1" style="display: flex; align-items: center; justify-content: center">
                  <v-btn
                    color="secondary"
                    :disabled="
                      importModelFile === null ||
                      importLabels === null ||
                      importWidth === null ||
                      importHeight === null ||
                      importVersion === null
                    "
                    @click="handleImport()"
                  >
                    <v-icon start class="open-icon"> mdi-import </v-icon>
                    <span class="open-label">Import Object Detection Model</span>
                  </v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
        <v-col cols="12" sm="6">
          <v-btn color="secondary" @click="openExportPrompt">
            <v-icon left class="open-icon"> mdi-export </v-icon>
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
          <v-btn color="error" @click="() => (showNukeDialog = true)">
            <v-icon left class="open-icon"> mdi-trash </v-icon>
            <span class="open-label">Clear and reset models</span>
          </v-btn>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-simple-table fixed-header height="100%" density="compact" dark>
            <thead style="font-size: 1.25rem">
              <tr>
                <th class="text-left">Available Models</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="model in supportedModels" :key="model.modelPath">
                <td>{{ model.nickname }}</td>
                <td class="text-right">
                  <v-btn
                    icon
                    small
                    color="error"
                    @click="() => (confirmDeleteDialog = { show: true, model })"
                    title="Delete Model"
                  >
                    <v-icon>mdi-delete</v-icon>
                  </v-btn>
                </td>
                <td class="text-right">
                  <v-btn
                    icon
                    small
                    color="primary"
                    @click="() => (showRenameDialog = { show: true, model, newName: '' })"
                    title="Rename Model"
                  >
                    <v-icon>mdi-pencil</v-icon>
                  </v-btn>
                </td>
                <td class="text-right">
                  <v-btn icon small color="info" @click="() => (showInfo = { show: true, model })">
                    <v-icon>mdi-information</v-icon>
                  </v-btn>
                </td>
              </tr>
            </tbody>
          </v-simple-table>
          <v-dialog v-model="confirmDeleteDialog.show" width="600">
            <v-card color="primary" dark>
              <v-card-title>Delete Object Detection Model</v-card-title>
              <v-card-text>
                Are you sure you want to delete the model
                {{ confirmDeleteDialog.model.nickname }}?
                <v-row class="mt-12 ml-8 mr-8 mb-1" style="display: flex; align-items: center; justify-content: center">
                  <v-btn text @click="confirmDeleteDialog.show = false">Cancel</v-btn>
                  <v-btn color="error" @click="deleteModel(confirmDeleteDialog.model)">Delete</v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
          <v-dialog v-model="showRenameDialog.show" width="600">
            <v-card color="primary" dark>
              <v-card-title>Rename Object Detection Model</v-card-title>
              <v-card-text>
                Enter a new name for the model {{ showRenameDialog.model.nickname }}:
                <v-row class="mt-6 ml-4 mr-8">
                  <v-text-field v-model="showRenameDialog.newName" label="New Name" />
                </v-row>
                <v-row>
                  <v-btn text @click="showRenameDialog.show = false">Cancel</v-btn>
                  <v-btn text color="primary" @click="renameModel(showRenameDialog.model, showRenameDialog.newName)"
                    >Rename</v-btn
                  >
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
          <v-dialog v-model="showInfo.show" width="600">
            <v-card color="primary" dark>
              <v-card-title>Object Detection Model Info</v-card-title>
              <v-btn color="secondary" @click="openExportIndividualModelPrompt">
                <v-icon left class="open-icon"> mdi-export </v-icon>
                <span class="open-label">Export Model</span>
              </v-btn>
              <a
                ref="exportIndividualModel"
                style="color: black; text-decoration: none; display: none"
                :href="`http://${address}/api/objectdetection/exportIndividual?modelPath=${showInfo.model.modelPath}`"
                :download="`${showInfo.model.nickname}_${showInfo.model.family}_${showInfo.model.version}_${showInfo.model.resolutionWidth}x${showInfo.model.resolutionHeight}_${showInfo.model.labels.join('_')}.${showInfo.model.family.toLowerCase()}`"
                target="_blank"
              />
              <v-card-text>
                <p>Model Path: {{ showInfo.model.modelPath }}</p>
                <p>Model Nickname: {{ showInfo.model.nickname }}</p>
                <p>Model Family: {{ showInfo.model.family }}</p>
                <p>Model Version: {{ showInfo.model.version }}</p>
                <p>Model Label(s): {{ showInfo.model.labels.join(", ") }}</p>
                <p>Model Resolution: {{ showInfo.model.resolutionWidth }} x {{ showInfo.model.resolutionHeight }}</p>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
      </v-row>
    </div>

    <v-dialog v-model="showNukeDialog" width="800" dark>
      <v-card color="primary" class="pa-3" flat>
        <v-card-title style="justify-content: center" class="pb-6">
          <span class="open-label">
            <v-icon end color="error" class="open-icon ma-1">mdi-nuke</v-icon>
            Clear and Reset Object Detection Models
            <v-icon end color="error" class="open-icon ma-1">mdi-nuke</v-icon>
          </span>
        </v-card-title>
        <v-card-text class="pt-3">
          <v-row class="align-center text-white">
            <v-col cols="12" md="6">
              <span class="mt-3"> This will delete ALL OF YOUR MODELS and re-extract the default models. </span>
            </v-col>
            <v-col cols="12" md="6">
              <v-btn color="secondary" style="float: right" @click="openExportPrompt">
                <v-icon start class="open-icon"> mdi-export </v-icon>
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
        <v-card-text>
          <pv-input
            v-model="yesDeleteMyModelsText"
            :label="'Type &quot;' + expected + '&quot;:'"
            :label-cols="6"
            :input-cols="6"
          />
        </v-card-text>
        <v-card-text>
          <v-btn
            color="error"
            :disabled="yesDeleteMyModelsText.toLowerCase() !== expected.toLowerCase()"
            @click="nukeModels"
          >
            <v-icon start class="open-icon"> mdi-trash-can-outline </v-icon>
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
.v-btn {
  width: 100%;
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
  background-color: #006492 !important;

  th,
  td {
    background-color: #006492 !important;
    font-size: 1rem !important;
    color: white !important;
  }

  td {
    font-family: monospace !important;
  }

  tbody :hover td {
    background-color: #005281 !important;
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
    background-color: #ffd843;
    border-radius: 10px;
  }
}
</style>
