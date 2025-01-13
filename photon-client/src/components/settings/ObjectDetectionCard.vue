<script setup lang="ts">
import { ref, computed } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";

const showImportDialog = ref(false);
const importRKNNFile = ref<File | null>(null);
const importLabelsFile = ref<File | null>(null);

// TODO gray out the button when model is uploading
const handleImport = async () => {
  if (importRKNNFile.value === null || importLabelsFile.value === null) return;

  const formData = new FormData();
  formData.append("rknn", importRKNNFile.value);
  formData.append("labels", importLabelsFile.value);

  useStateStore().showSnackbarMessage({
    message: "Importing Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  axios
    .post("/utils/importObjectDetectionModel", formData, {
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

  importRKNNFile.value = null;
  importLabelsFile.value = null;
};

const deleteModel = ref<String | null>(null);
const showDeletionDialog = ref(false);

const handleDeletion = () => {
  useStateStore().showSnackbarMessage({
    message: "Deleting Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  if (deleteModel.value === null) return;

  axios
    .post("/utils/deleteObjectDetectionModel", { model: deleteModel.value.toString() })
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

const showNameEditDialog = ref(false);
const nameEditModel = ref<String | null>(null);
const newName = ref<String | null>(null);

const handleRename = () => {
  useStateStore().showSnackbarMessage({
    message: "Editing Object Detection Model Name...",
    color: "secondary",
    timeout: -1
  });

  var formData = new FormData();

  if (nameEditModel.value === null || newName.value === null) return;

  formData.append("model", nameEditModel.value.toString());
  formData.append("newName", newName.value.toString());

  axios
    .post("/utils/renameObjectDetectionModel", formData)
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

  showNameEditDialog.value = false;
  nameEditModel.value = null;
  newName.value = null;
};
</script>

<template>
  <v-card dark class="mb-3" style="background-color: #006492">
    <v-card-title class="pa-6">Object Detection</v-card-title>
    <div class="pa-6 pt-0">
      <v-row>
        <v-col cols="12 ">
          <v-btn color="secondary" @click="() => (showImportDialog = true)" class="justify-center">
            <v-icon left class="open-icon"> mdi-import </v-icon>
            <span class="open-label">Import New Model</span>
          </v-btn>
          <v-dialog
            v-model="showImportDialog"
            width="600"
            @input="
              () => {
                importRKNNFile = null;
                importLabelsFile = null;
              }
            "
          >
            <v-card color="primary" dark>
              <v-card-title>Import New Object Detection Model</v-card-title>
              <v-card-text>
                Upload a new object detection model to this device that can be used in a pipeline. Naming convention
                should be <code>name-verticalResolution-horizontalResolution-modelType</code>. Additionally, the labels
                file ought to have the same name as the RKNN file, with <code>-labels</code> appended to the end. For
                example, if the RKNN file is named <code>note-640-640-yolov5s.rknn</code>, the labels file should be
                named <code>note-640-640-yolov5s-labels.txt</code>. Note that ONLY 640x640 YOLOv5 & YOLOv8 models
                trained and converted to `.rknn` format for RK3588 CPUs are currently supported!
                <v-row class="mt-6 ml-4 mr-8">
                  <v-file-input label="RKNN File" v-model="importRKNNFile" accept=".rknn" />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-file-input label="Labels File" v-model="importLabelsFile" accept=".txt" />
                </v-row>
                <v-row
                  class="mt-12 ml-8 mr-8 mb-1"
                  style="display: flex; align-items: center; justify-content: center"
                  align="center"
                >
                  <v-btn
                    color="secondary"
                    :disabled="importRKNNFile === null || importLabelsFile === null"
                    @click="handleImport"
                  >
                    <v-icon left class="open-icon"> mdi-import </v-icon>
                    <span class="open-label">Import Object Detection Model</span>
                  </v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
          <v-dialog
            v-model="showNameEditDialog"
            width="600"
            @input="
              () => {
                newName = null;
              }
            "
          >
            <v-card color="primary" dark>
              <v-card-title>Edit Object Detection Model Name</v-card-title>
              <v-card-text>
                Change the name of <code>{{ nameEditModel }}</code
                >. The new name must be unique, and contain no spaces.
                <v-row class="mt-6 ml-4 mr-8">
                  <v-text-field v-model="newName" label="New Name" outlined dense clearable required></v-text-field>
                </v-row>
                <v-row
                  class="mt-12 ml-8 mr-8 mb-1"
                  style="display: flex; align-items: center; justify-content: center"
                  align="center"
                >
                  <v-btn color="secondary" :disabled="newName === null" @click="handleRename">
                    <v-icon left class="open-icon"> mdi-pencil </v-icon>
                    <span class="open-label">Edit Object Detection Model Name</span>
                  </v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
          <v-dialog
            v-model="showDeletionDialog"
            width="600"
            @input="
              () => {
                newName = null;
              }
            "
          >
            <v-card color="primary" dark>
              <v-card-title>Delete {{ deleteModel }}</v-card-title>
              <v-card-text>
                Are you sure you want to delete this model?
                <v-row
                  class="mt-12 ml-8 mr-8 mb-1"
                  style="display: flex; align-items: center; justify-content: center"
                  align="center"
                >
                  <v-btn color="error" @click="handleDeletion">
                    <v-icon left class="open-icon"> mdi-delete </v-icon>
                    <span class="open-label">Yes, I'm Sure</span>
                  </v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
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
              <tr v-for="model in supportedModels" :key="model">
                <td>{{ model }}</td>
              </tr>
            </tbody>
          </v-simple-table>
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
