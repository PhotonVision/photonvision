<script setup lang="ts">
import { ref, computed, inject } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";

const showImportDialog = ref(false);
const confirmDeleteDialog = ref({ show: false, model: { UID: "", name: "" } });
const showRenameDialog = ref({ show: false, model: { UID: "", name: "" }, newName: "" });

const address = inject<string>("backendHost");

const importModelFile = ref<File | null>(null);
const importLabels = ref<String | null>(null);
const importHeight = ref<number | null>(null);
const importWidth = ref<number | null>(null);
const importVersion = ref<String | null>(null);

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

const deleteModel = async (model: string) => {
  useStateStore().showSnackbarMessage({
    message: "Deleting Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  axios
    .post("/objectdetection/delete", {
      modelPath: model
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

const renameModel = async (model: string, newName: string) => {
  useStateStore().showSnackbarMessage({
    message: "Renaming Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  axios
    .post("/objectdetection/rename", {
      modelPath: model,
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
  return supportedBackends.flatMap((backend) => availableModels[backend] || []);
});

const exportModels = ref();
const openExportPrompt = () => {
  exportModels.value.click();
};
</script>

<template>
  <v-card dark class="mb-3" style="background-color: #006492">
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
            @input="
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
                <v-row
                  class="mt-12 ml-8 mr-8 mb-1"
                  style="display: flex; align-items: center; justify-content: center"
                  align="center"
                >
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
                    <v-icon left class="open-icon"> mdi-import </v-icon>
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
            download="photonvision-object-detection-models.zip"
            target="_blank"
          />
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-simple-table fixed-header height="100%" dense dark>
            <thead style="font-size: 1.25rem">
              <tr>
                <th class="text-left">Available Models</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="model in supportedModels" :key="model.UID">
                <td>{{ model.name }}</td>
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
              </tr>
            </tbody>
          </v-simple-table>
          <v-dialog v-model="confirmDeleteDialog.show" width="600">
            <v-card color="primary" dark>
              <v-card-title>Delete Object Detection Model</v-card-title>
              <v-card-text>
                Are you sure you want to delete the model {{ confirmDeleteDialog.model.UID }}?
                <v-row class="mt-12 ml-8 mr-8 mb-1" style="display: flex; align-items: center; justify-content: center">
                  <v-btn text @click="confirmDeleteDialog.show = false">Cancel</v-btn>
                  <v-btn color="error" @click="deleteModel(confirmDeleteDialog.model.UID)">Delete</v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
          <v-dialog v-model="showRenameDialog.show" width="600">
            <v-card color="primary" dark>
              <v-card-title>Rename Object Detection Model</v-card-title>
              <v-card-text>
                Enter a new name for the model {{ showRenameDialog.model }}:
                <v-row class="mt-6 ml-4 mr-8">
                  <v-text-field v-model="showRenameDialog.newName" label="New Name" />
                </v-row>
                <v-row>
                  <v-btn text @click="showRenameDialog.show = false">Cancel</v-btn>
                  <v-btn text color="primary" @click="renameModel(showRenameDialog.model.UID, showRenameDialog.newName)"
                    >Rename</v-btn
                  >
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
      </v-row>
    </div>
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
.v-data-table {
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
