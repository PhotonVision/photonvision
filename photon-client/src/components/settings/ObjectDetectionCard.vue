<script setup lang="ts">
import { ref, computed, inject } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";

const showImportDialog = ref(false);
const importRKNNFile = ref<File | null>(null);
const importLabelsFile = ref<File | null>(null);

const host = inject<string>("backendHost");

const areValidFileNames = (weights: string | null, labels: string | null) => {
  const weightsRegex = /^([a-zA-Z0-9._]+)-(\d+)-(\d+)-(yolov(?:5|8|11)[nsmlx]*)\.rknn$/;
  const labelsRegex = /^([a-zA-Z0-9._]+)-(\d+)-(\d+)-(yolov(?:5|8|11)[nsmlx]*)-labels\.txt$/;

  if (weights && labels) {
    const weightsMatch = weights.match(weightsRegex);
    const labelsMatch = labels.match(labelsRegex);

    if (weightsMatch && labelsMatch) {
      return (
        weightsMatch[1] === labelsMatch[1] &&
        weightsMatch[2] === labelsMatch[2] &&
        weightsMatch[3] === labelsMatch[3] &&
        weightsMatch[4] === labelsMatch[4]
      );
    }
  }
  return false;
};

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

// Filters out models that are not supported by the current backend, and returns a flattened list.
const supportedModels = computed(() => {
  const { availableModels, supportedBackends } = useSettingsStore().general;
  return supportedBackends.flatMap((backend) => availableModels[backend] || []);
});
</script>

<template>
  <v-card class="mb-3" style="background-color: #006492">
    <v-card-title class="pa-6">Object Detection</v-card-title>
    <div class="pa-6 pt-0">
      <v-row>
        <v-col cols="12 ">
          <v-btn color="secondary" class="justify-center" @click="() => (showImportDialog = true)">
            <v-icon start class="open-icon"> mdi-import </v-icon>
            <span class="open-label">Import New Model</span>
          </v-btn>
          <v-dialog
            v-model="showImportDialog"
            width="600"
            @update:modelValue="
              () => {
                importRKNNFile = null;
                importLabelsFile = null;
              }
            "
          >
            <v-card color="primary" dark>
              <v-card-title>Import New Object Detection Model</v-card-title>
              <v-card-text>
                Upload a new object detection model to this device that can be used in a pipeline. Note that ONLY
                640x640 YOLOv5, YOLOv8, and YOLOv11 models trained and converted to `.rknn` format for RK3588 CPUs are
                currently supported! See [the documentation]({{
                  host
                }}/docs/objectDetection/about-object-detection.html) for more details.
                <v-row class="mt-6 ml-4 mr-8">
                  <v-file-input v-model="importRKNNFile" label="RKNN File" accept=".rknn" />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-file-input v-model="importLabelsFile" label="Labels File" accept=".txt" />
                </v-row>
                <v-row class="mt-12 ml-8 mr-8 mb-1" style="display: flex; align-items: center; justify-content: center">
                  <v-btn
                    color="secondary"
                    :disabled="
                      importRKNNFile === null ||
                      importLabelsFile === null ||
                      !areValidFileNames(importRKNNFile.name, importLabelsFile.name)
                    "
                    @click="handleImport"
                  >
                    <v-icon start class="open-icon"> mdi-import </v-icon>
                    <span class="open-label">Import Object Detection Model</span>
                  </v-btn>
                </v-row>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-table fixed-header height="100%" density="compact" dark>
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
          </v-table>
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
