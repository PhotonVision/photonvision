<script setup lang="ts">
import { ref, computed, inject, useTemplateRef } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { type ObjectDetectionModelProperties } from "@/types/SettingTypes";
import PvButton from "@/components/common/pv-button.vue";
import PvDeleteModal from "@/components/common/pv-delete-modal.vue";
import PvDialog from "@/components/common/pv-dialog.vue";
import PvCard from "@/components/common/pv-card.vue";
import PvTextField from "@/components/common/pv-text-field.vue";
import { axiosPost } from "@/lib/PhotonUtils";

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

  if (
    await axiosPost("/objectdetection/import", "import an object detection model", formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: ({ progress }: { progress?: number }) => {
        const uploadPercentage = (progress || 0) * 100.0;
        if (uploadPercentage < 99.5) {
          useStateStore().showSnackbarMessage({
            message: "Object Detection Model Upload in Process, " + uploadPercentage.toFixed(2) + "% complete",
            color: "secondary",
            timeout: -1
          });
        }
      }
    })
  ) {
    useStateStore().showSnackbarMessage({
      message: "Processing uploaded Object Detection Model...",
      color: "secondary",
      timeout: -1
    });
  }

  showImportDialog.value = false;

  importModelFile.value = null;
  importLabels.value = null;
  importHeight.value = null;
  importWidth.value = null;
  importVersion.value = null;
};

const deleteModel = async (model: ObjectDetectionModelProperties) => {
  await axiosPost("/objectdetection/delete", "delete an object detection model", {
    modelPath: model.modelPath
  });
};

const renameModel = async (model: ObjectDetectionModelProperties, newName: string) => {
  useStateStore().showSnackbarMessage({
    message: "Renaming Object Detection Model...",
    color: "secondary",
    timeout: -1
  });

  await axiosPost("/objectdetection/rename", "rename an object detection model", {
    modelPath: model.modelPath,
    newName: newName
  });
  showRenameDialog.value.show = false;
};

// Filters out models that are not supported by the current backend, and returns a flattened list.
const supportedModels = computed(() => {
  const { availableModels, supportedBackends } = useSettingsStore().general;
  const isSupported = (model: ObjectDetectionModelProperties) => {
    // Check if model's family is in the list of supported backends
    return supportedBackends.some((backend: string) => backend.toLowerCase() === model.family.toLowerCase());
  };

  // Filter models where the family is supported and flatten the list
  return availableModels.filter(isSupported);
});

const exportModels = useTemplateRef("exportModels");
const openExportPrompt = () => {
  exportModels.value?.click();
};

const exportIndividualModel = useTemplateRef("exportIndividualModel");
const openExportIndividualModelPrompt = () => {
  exportIndividualModel.value?.click();
};

const showNukeDialog = ref(false);
const nukeModels = async () => {
  await axiosPost("/objectdetection/nuke", "clear and reset object detection models");
};

const showBulkImportDialog = ref(false);
const importFile = ref<File | null>(null);
const handleBulkImport = async () => {
  if (importFile.value === null) return;

  const formData = new FormData();
  formData.append("data", importFile.value);

  if (
    await axiosPost("/objectdetection/bulkimport", "import object detection models", formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: ({ progress }: { progress?: number }) => {
        const uploadPercentage = (progress || 0) * 100.0;
        if (uploadPercentage < 99.5) {
          useStateStore().showSnackbarMessage({
            message: "Object Detection Models Upload in Progress",
            color: "secondary",
            timeout: -1,
            progressBar: uploadPercentage,
            progressBarColor: "primary"
          });
        }
      }
    })
  ) {
    useStateStore().showSnackbarMessage({
      message: "Importing New Object Detection Models...",
      color: "secondary",
      timeout: -1
    });
  }
  showImportDialog.value = false;
  importFile.value = null;
};
</script>

<template>
  <pv-card class="mb-3" padding="none">
    <div class="p-5 pb-2 text-lg font-semibold">Object Detection</div>
    <div class="p-5 pt-0">
      <div class="flex flex-wrap -mx-3">
        <div class="w-full px-3 sm:w-1/2">
          <pv-button
            variant="primary"
            icon="mdi-import"
            class="justify-center"
            @click="() => (showImportDialog = true)"
          >
            <span class="open-label">Import Model</span>
          </pv-button>
          <pv-dialog
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
            <pv-card padding="none" class="p-5">
              <div class="pb-2 text-lg font-semibold">Import New Object Detection Model</div>
              <div>
                <span v-if="useSettingsStore().general.supportedBackends?.includes('RKNN')"
                  >Upload a new object detection model to this device that can be used in a pipeline. Note that ONLY
                  640x640 YOLOv5, YOLOv8, and YOLOv11 models trained and converted to `.rknn` format for RK3588 SOCs are
                  currently supporter!</span
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
                <div class="p-5 pb-0">
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
                  <pv-text-field
                    v-model="importLabels"
                    label="Labels"
                    placeholder="Comma separated labels, no spaces"
                    type="text"
                    variant="underlined"
                  />
                  <pv-text-field v-model="importWidth" variant="underlined" label="Width" type="number" />
                  <pv-text-field v-model="importHeight" variant="underlined" label="Height" type="number" />
                  <pv-select
                    v-model="importVersion"
                    variant="underlined"
                    label="Model Version"
                    data-testid="import-version-select"
                    :items="
                      useSettingsStore().general.supportedBackends?.includes('RKNN')
                        ? ['YOLOv5', 'YOLOv8', 'YOLO11']
                        : ['YOLOv8', 'YOLO11']
                    "
                  />
                  <pv-button
                    variant="primary"
                    icon="mdi-import"
                    block
                    :disabled="
                      importModelFile === null ||
                      importLabels === null ||
                      importWidth === null ||
                      importHeight === null ||
                      importVersion === null
                    "
                    @click="handleImport()"
                  >
                    <span class="open-label">Import Object Detection Model</span>
                  </pv-button>
                </div>
              </div>
            </pv-card>
          </pv-dialog>
        </div>
        <div class="w-full px-3 sm:w-1/2">
          <pv-button
            variant="primary"
            icon="mdi-import"
            class="justify-center"
            @click="() => (showBulkImportDialog = true)"
          >
            <span class="open-label">Bulk Import</span>
          </pv-button>
          <pv-dialog v-model="showBulkImportDialog" width="600">
            <pv-card padding="none" class="p-5">
              <div class="pb-2 text-lg font-semibold">Import Multiple Object Detection Models</div>
              <div>
                Upload a zip file containing multiple object detection models to this device. Note this zip file should
                only come from a previous export of object detection models.
                <div class="p-5 pb-0">
                  <v-file-input v-model="importFile" variant="underlined" label="Zip File" accept=".zip" />
                  <pv-button
                    variant="primary"
                    icon="mdi-import"
                    block
                    :disabled="importFile === null"
                    @click="handleBulkImport()"
                  >
                    <span class="open-label">Bulk Import</span>
                  </pv-button>
                </div>
              </div>
            </pv-card>
          </pv-dialog>
        </div>
        <div class="w-full px-3 sm:w-1/2">
          <pv-button variant="passive" icon="mdi-export" @click="openExportPrompt">
            <span class="open-label">Export Models</span>
          </pv-button>
          <a
            ref="exportModels"
            style="color: black; text-decoration: none; display: none"
            :href="`http://${address}/api/objectdetection/export`"
            download="photonvision-object-detection-models-export.zip"
            target="_blank"
          />
        </div>
        <div class="w-full px-3 sm:w-1/2">
          <pv-button variant="danger" icon="mdi-trash" @click="() => (showNukeDialog = true)">
            <span class="open-label">Clear and reset models</span>
          </pv-button>
        </div>
      </div>
      <div class="flex flex-wrap">
        <div class="flex-1">
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
            <tbody data-testid="model-table">
              <tr v-for="model in supportedModels" :key="model.modelPath">
                <td>{{ model.nickname }}</td>
                <td>{{ model.labels.join(", ") }}</td>
                <td class="text-right">
                  <pv-button
                    size="icon"
                    variant="danger"
                    title="Delete Model"
                    @click="() => (confirmDeleteDialog = { show: true, model })"
                  >
                    <span class="mdi mdi-trash-can-outline text-lg leading-none" aria-hidden="true"></span>
                  </pv-button>
                </td>
                <td class="text-right">
                  <pv-button
                    size="icon"
                    variant="primary"
                    title="Rename Model"
                    @click="() => (showRenameDialog = { show: true, model, newName: '' })"
                  >
                    <span class="mdi mdi-pencil text-lg leading-none" aria-hidden="true"></span>
                  </pv-button>
                </td>
                <td class="text-right">
                  <pv-button size="icon" variant="passive" @click="() => (showInfo = { show: true, model })">
                    <span class="mdi mdi-information text-lg leading-none" aria-hidden="true"></span>
                  </pv-button>
                </td>
              </tr>
            </tbody>
          </v-table>

          <pv-delete-modal
            v-model="confirmDeleteDialog.show"
            :width="500"
            :on-confirm="() => deleteModel(confirmDeleteDialog.model)"
            title="Delete Object Detection Model"
            :description="`Are you sure you want to delete the model ${confirmDeleteDialog.model.nickname}?`"
            delete-text="Delete model"
          />

          <pv-dialog v-model="showRenameDialog.show" width="600">
            <pv-card padding="none" class="p-5">
              <div class="pb-2 text-lg font-semibold">Rename Object Detection Model</div>
              <div class="pt-0">
                Enter a new name for the model "{{ showRenameDialog.model.nickname }}":
                <div class="p-5 pb-0">
                  <pv-text-field
                    v-model="showRenameDialog.newName"
                    hide-details
                    label="New Name"
                    variant="underlined"
                  />
                </div>
                <div class="pt-5 pb-0 pr-0 d-flex justify-end">
                  <pv-button variant="danger" @click="showRenameDialog.show = false">Cancel</pv-button>
                  <pv-button variant="primary" @click="renameModel(showRenameDialog.model, showRenameDialog.newName)"
                    >Rename</pv-button
                  >
                </div>
              </div>
            </pv-card>
          </pv-dialog>
          <pv-dialog v-model="showInfo.show" width="600">
            <pv-card padding="none" class="p-5">
              <div class="pb-2 text-lg font-semibold">Object Detection Model Info</div>
              <div class="pt-0">
                <pv-button variant="passive" icon="mdi-export" block @click="openExportIndividualModelPrompt">
                  <span class="open-label">Export Model</span>
                </pv-button>
                <a
                  ref="exportIndividualModel"
                  style="color: black; text-decoration: none; display: none"
                  :href="`http://${address}/api/objectdetection/exportIndividual?modelPath=${showInfo.model.modelPath}`"
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
              </div>
            </pv-card>
          </pv-dialog>
        </div>
      </div>
    </div>

    <pv-delete-modal
      v-model="showNukeDialog"
      :on-backup="openExportPrompt"
      :on-confirm="nukeModels"
      title="Delete and Reset All Object Detection Models"
      description="This will delete ALL object detection models and re-extract the default object detection models. This action cannot be undone."
      expected-confirmation-text="Delete Models"
      delete-text="Delete all models"
    />
  </pv-card>
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
