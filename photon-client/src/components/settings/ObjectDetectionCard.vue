<script setup lang="ts">
import { ref } from "vue";
import PvSelect from "@/components/common/pv-select.vue";
import axios from "axios";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";

const showObjectDetectionImportDialog = ref(false);
const importRKNNFile = ref<File | null>(null);
const importLabelsFile = ref<File | null>(null);

const handleObjectDetectionImport = () => {
  if (importRKNNFile.value === null || importLabelsFile.value === null) return;

  const formData = new FormData();
  formData.append("rknn", importRKNNFile.value);
  formData.append("labels", importLabelsFile.value);

  useStateStore().showSnackbarMessage({
    message: "Importing Object Detection Model...",
    color: "secondary",
    timeout: -1,
  });

  axios
    .post("/utils/importObjectDetectionModel", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    .then((response) => {
      useStateStore().showSnackbarMessage({
        message: response.data.text || response.data,
        color: "success",
      });
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: error.response.data.text || error.response.data,
        });
      } else if (error.request) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message:
            "Error while trying to process the request! The backend didn't respond.",
        });
      } else {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "An error occurred while trying to process the request.",
        });
      }
    });

  showObjectDetectionImportDialog.value = false;
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
  <v-card dark class="mb-3" style="background-color: #006492">
    <v-card-title class="pa-6">Object Detection</v-card-title>
    <div class="pa-6 pt-0">
      <v-row>
        <v-col cols="12">
          <v-btn
            color="secondary"
            @click="() => (showObjectDetectionImportDialog = true)"
          >
            <v-icon left class="open-icon"> mdi-import </v-icon>
            <span class="open-label">Import New Model</span>
          </v-btn>
          <v-dialog
            v-model="showObjectDetectionImportDialog"
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
                Upload a new object detection model to this device that can be used in a
                pipeline. Naming convention is that the labels file ought to have the same
                name as the RKNN file, with -labels appended to the end. For example, if
                the RKNN file is named <i>foo.rknn</i>, the labels file should be named
                <i>foo-labels.txt</i>.
                <v-row class="mt-6 ml-4 mr-8">
                  <v-file-input
                    label="RKNN File"
                    v-model="importRKNNFile"
                    accept=".rknn"
                  />
                </v-row>
                <v-row class="mt-6 ml-4 mr-8">
                  <v-file-input
                    label="Labels File"
                    v-model="importLabelsFile"
                    accept=".txt"
                  />
                </v-row>
                <v-row
                  class="mt-12 ml-8 mr-8 mb-1"
                  style="display: flex; align-items: center; justify-content: center"
                  align="center"
                >
                  <v-btn
                    color="secondary"
                    :disabled="importRKNNFile === null || importLabelsFile === null"
                    @click="handleObjectDetectionImport"
                  >
                    <v-icon left class="open-icon"> mdi-import </v-icon>
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
          <v-simple-table>
            <thead>
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

<style scoped>
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
</style>
