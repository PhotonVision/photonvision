<script setup lang="ts">
import type { CameraCalibrationResult, VideoFormat } from "@/types/SettingTypes";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed, inject, ref } from "vue";
import { axiosPost, getResolutionString, parseJsonFile } from "@/lib/PhotonUtils";
import { useTheme } from "vuetify";
import PvDeleteModal from "@/components/common/pv-delete-modal.vue";

const theme = useTheme();

const props = defineProps<{
  videoFormat: VideoFormat;
}>();

const confirmRemoveDialog = ref({ show: false, vf: props.videoFormat as VideoFormat });

const removeCalibration = (vf: VideoFormat) => {
  axiosPost("/calibration/remove", "delete a camera calibration", {
    cameraUniqueName: useCameraSettingsStore().currentCameraSettings.uniqueName,
    width: vf.resolution.width,
    height: vf.resolution.height
  });
};

const exportCalibration = ref();
const openExportCalibrationPrompt = () => {
  exportCalibration.value.click();
};

const importCalibrationFromPhotonJson = ref();
const openUploadPhotonCalibJsonPrompt = () => {
  importCalibrationFromPhotonJson.value.click();
};
const importCalibration = async () => {
  const files = importCalibrationFromPhotonJson.value.files;
  if (files.length === 0) return;
  const uploadedJson = files[0];

  const data = await parseJsonFile<CameraCalibrationResult>(uploadedJson);

  if (
    data.resolution.height != props.videoFormat.resolution.height ||
    data.resolution.width != props.videoFormat.resolution.width
  ) {
    useStateStore().showSnackbarMessage({
      color: "error",
      message: `The resolution of the calibration export doesn't match the current resolution ${props.videoFormat.resolution.height}x${props.videoFormat.resolution.width}`
    });
    return;
  }

  useCameraSettingsStore()
    .importCalibrationFromData({ calibration: data })
    .then((response) => {
      useStateStore().showSnackbarMessage({
        color: "success",
        message: response.data.text || response.data
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

interface ObservationDetails {
  mean: number;
  index: number;
}

const currentCalibrationCoeffs = computed<CameraCalibrationResult | undefined>(() =>
  useCameraSettingsStore().getCalibrationCoeffs(props.videoFormat.resolution)
);

const getObservationDetails = (): ObservationDetails[] | undefined => {
  const coefficients = currentCalibrationCoeffs.value;

  return coefficients?.meanErrors.map((m, i) => ({
    index: i,
    mean: parseFloat(m.toFixed(2))
  }));
};

const exportCalibrationURL = computed<string>(() =>
  useCameraSettingsStore().getCalJSONUrl(inject("backendHost") as string, props.videoFormat.resolution)
);
const calibrationImageURL = (index: number) =>
  useCameraSettingsStore().getCalImageUrl(inject<string>("backendHost") as string, props.videoFormat.resolution, index);
</script>
<template>
  <v-card color="surface" dark>
    <div class="d-flex flex-wrap pt-2 pl-2 pr-2 align-center">
      <v-col cols="12" md="6">
        <v-card-title class="pa-0"> Calibration Details </v-card-title>
      </v-col>
      <v-col cols="12" md="6" class="d-flex align-center pt-0 pt-md-3">
        <v-btn
          color="buttonPassive"
          class="mr-2"
          style="flex: 1"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="openUploadPhotonCalibJsonPrompt"
        >
          <v-icon start size="large">mdi-import</v-icon>
          <span>Import</span>
        </v-btn>
        <input
          ref="importCalibrationFromPhotonJson"
          type="file"
          accept=".json"
          style="display: none"
          @change="importCalibration"
        />
        <v-btn
          color="buttonPassive"
          class="mr-2"
          :disabled="!currentCalibrationCoeffs"
          style="flex: 1"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="openExportCalibrationPrompt"
        >
          <v-icon start size="large">mdi-export</v-icon>
          <span>Export</span>
        </v-btn>
        <a
          ref="exportCalibration"
          style="color: black; text-decoration: none; display: none"
          :href="exportCalibrationURL"
          target="_blank"
        />
        <v-btn
          color="error"
          :disabled="!currentCalibrationCoeffs"
          style="flex: 1"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="() => (confirmRemoveDialog = { show: true, vf: props.videoFormat })"
        >
          <v-icon start size="large">mdi-delete</v-icon>
          <span>Delete</span>
        </v-btn>
      </v-col>
    </div>
    <v-card-title class="pt-0 pb-0"
      >{{ useCameraSettingsStore().currentCameraName }}@{{ getResolutionString(videoFormat.resolution) }}</v-card-title
    >
    <v-card-text v-if="!currentCalibrationCoeffs">
      <v-alert
        class="pt-3 pb-3"
        color="primary"
        density="compact"
        text="The selected video format has not been calibrated."
        icon="mdi-alert-circle-outline"
        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
      />
    </v-card-text>
    <v-card-text class="pt-0">
      <v-table density="compact" style="width: 100%">
        <template #default>
          <thead>
            <tr>
              <th class="text-left">Name</th>
              <th class="text-left">Value</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Fx</td>
              <td>
                {{
                  useCameraSettingsStore()
                    .getCalibrationCoeffs(props.videoFormat.resolution)
                    ?.cameraIntrinsics.data[0].toFixed(2) || 0.0
                }}
                mm
              </td>
            </tr>
            <tr>
              <td>Fy</td>
              <td>
                {{
                  useCameraSettingsStore()
                    .getCalibrationCoeffs(props.videoFormat.resolution)
                    ?.cameraIntrinsics.data[4].toFixed(2) || 0.0
                }}
                mm
              </td>
            </tr>
            <tr>
              <td>Cx</td>
              <td>
                {{
                  useCameraSettingsStore()
                    .getCalibrationCoeffs(props.videoFormat.resolution)
                    ?.cameraIntrinsics.data[2].toFixed(2) || 0.0
                }}
                px
              </td>
            </tr>
            <tr>
              <td>Cy</td>
              <td>
                {{
                  useCameraSettingsStore()
                    .getCalibrationCoeffs(props.videoFormat.resolution)
                    ?.cameraIntrinsics.data[5].toFixed(2) || 0.0
                }}
                px
              </td>
            </tr>
            <tr>
              <td>Distortion</td>
              <td>
                {{
                  useCameraSettingsStore()
                    .getCalibrationCoeffs(props.videoFormat.resolution)
                    ?.distCoeffs.data.map((it) => parseFloat(it.toFixed(3))) || []
                }}
              </td>
            </tr>
            <tr>
              <td>Mean Err</td>
              <td>
                {{
                  videoFormat.mean !== undefined
                    ? isNaN(videoFormat.mean)
                      ? "NaN"
                      : videoFormat.mean.toFixed(2) + "px"
                    : "-"
                }}
              </td>
            </tr>
            <tr>
              <td>Horizontal FOV</td>
              <td>
                {{ videoFormat.horizontalFOV !== undefined ? videoFormat.horizontalFOV.toFixed(2) + "°" : "-" }}
              </td>
            </tr>
            <tr>
              <td>Vertical FOV</td>
              <td>{{ videoFormat.verticalFOV !== undefined ? videoFormat.verticalFOV.toFixed(2) + "°" : "-" }}</td>
            </tr>
            <tr>
              <td>Diagonal FOV</td>
              <td>{{ videoFormat.diagonalFOV !== undefined ? videoFormat.diagonalFOV.toFixed(2) + "°" : "-" }}</td>
            </tr>
            <!-- Board warp, only shown for mrcal-calibrated cameras -->
            <tr v-if="currentCalibrationCoeffs?.calobjectWarp?.length === 2">
              <td>Board warp, X/Y</td>
              <td>
                {{
                  useCameraSettingsStore()
                    .getCalibrationCoeffs(props.videoFormat.resolution)
                    ?.calobjectWarp?.map((it) => (it * 1000).toFixed(2) + " mm")
                    .join(" / ")
                }}
              </td>
            </tr>
          </tbody>
        </template>
      </v-table>
    </v-card-text>
    <v-card-title v-if="currentCalibrationCoeffs" class="pt-0 pb-0">Individual Observations</v-card-title>
    <v-card-text v-if="currentCalibrationCoeffs" class="pt-0">
      <v-data-table
        density="compact"
        style="width: 100%"
        :headers="[
          { title: 'Observation Id', key: 'index' },
          { title: 'Mean Reprojection Error', key: 'mean' },
          { title: '', key: 'data-table-expand' }
        ]"
        :items="getObservationDetails()"
        item-value="index"
        show-expand
      >
        <template #item.data-table-expand="{ internalItem, toggleExpand }">
          <v-btn
            icon="mdi-eye"
            class="text-none"
            color="medium-emphasis"
            size="small"
            variant="text"
            slim
            @click="toggleExpand(internalItem)"
          ></v-btn>
        </template>

        <template #expanded-row="{ columns, item }">
          <td :colspan="columns.length">
            <div style="display: flex; justify-content: center; width: 100%">
              <img :src="calibrationImageURL(item.index)" alt="observation image" class="snapshot-preview pt-2 pb-2" />
            </div>
          </td>
        </template>
      </v-data-table>
    </v-card-text>
  </v-card>

  <pv-delete-modal
    v-model="confirmRemoveDialog.show"
    :width="500"
    :title="'Delete Calibration'"
    :description="`Are you sure you want to delete the calibration for '${confirmRemoveDialog.vf.resolution.width}x${confirmRemoveDialog.vf.resolution.height}'? This action cannot be undone.`"
    :on-confirm="() => removeCalibration(confirmRemoveDialog.vf)"
  />
</template>

<style scoped>
.snapshot-preview {
  max-width: 55%;
}
@media only screen and (max-width: 512px) {
  .snapshot-preview {
    max-width: 100%;
  }
}
</style>
