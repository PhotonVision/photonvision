<script setup lang="ts">
import { ref, computed } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";

const showImportDialog = ref(false);
const importModelFile = ref<File | null>(null);
const importLabels = ref<String | null>(null);
const importHeight = ref<number | null>(null);
const importWidth = ref<number | null>(null);
const importVersion = ref<String | null>(null);

// TODO gray out the button when model is uploading
const handleImport = async () => {
  if (importModelFile.value === null) return;

  const formData = new FormData();

  // Create JSON metadata
  const metadata = {
    version: importVersion.value,
    height: importHeight.value,
    width: importWidth.value,
    labels: importLabels.value
  };

  // Add JSON metadata as a separate part in the FormData
  formData.append(
    "metadata",
    new Blob([JSON.stringify(metadata)], {
      type: "application/json"
    })
  );
  formData.append("modelFile", importModelFile.value);

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

// TODO: write this
const handleExport = async () => {};

// Filters out models that are not supported by the current backend, and returns a flattened list.
const supportedModels = computed(() => {
  const { availableModels, supportedBackends } = useSettingsStore().general;
  return supportedBackends.flatMap((backend) => availableModels[backend] || []);
});
</script>

<template>
  <v-card dark class="mb-3" style="background-color: #006492">
    <v-card-title class="pa-6">Object Detection</v-card-title>
    <div class="pa-6 pt-0">
      <v-row>
        <v-col cols="12 ">
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
                  <v-text-field v-model="importHeight" label="Height" type="number" />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-text-field v-model="importWidth" label="Width" type="number" />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-select v-model="importVersion" label="Model Version" :items="['YOLOv5', 'YOLOv8', 'YOLO11']" />
                </v-row>
                <v-row
                  class="mt-12 ml-8 mr-8 mb-1"
                  style="display: flex; align-items: center; justify-content: center"
                  align="center"
                >
                  <v-btn color="secondary" :disabled="importModelFile === null" @click="handleImport()">
                    <v-icon left class="open-icon"> mdi-import </v-icon>
                    <span class="open-label">Import Object Detection Model</span>
                  </v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
        <v-col cols="12 ">
          <v-btn color="secondary" class="justify-center" @click="handleExport()">
            <v-icon left class="open-icon"> mdi-export </v-icon>
            <span class="open-label">Export Object Detection Models</span>
          </v-btn>
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
