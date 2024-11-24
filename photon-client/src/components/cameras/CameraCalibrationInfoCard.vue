<script setup lang="ts">
import type { CameraCalibrationResult, VideoFormat } from "@/types/SettingTypes";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed, inject, ref } from "vue";
import { getResolutionString, parseJsonFile } from "@/lib/PhotonUtils";

const props = defineProps<{
  videoFormat: VideoFormat;
}>();

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
  <v-card color="primary" class="pa-6" dark>
    <v-row>
      <v-col cols="12" md="5">
        <v-card-title class="pl-0 ml-0"
          ><span class="text-no-wrap" style="white-space: pre !important">Calibration Details: </span
          ><span class="text-no-wrap"
            >{{ useCameraSettingsStore().currentCameraName }}@{{ getResolutionString(videoFormat.resolution) }}</span
          ></v-card-title
        >
      </v-col>
      <v-col>
        <v-btn color="secondary" class="mt-4" style="width: 100%" @click="openUploadPhotonCalibJsonPrompt">
          <v-icon left> mdi-import</v-icon>
          <span>Import</span>
        </v-btn>
        <input
          ref="importCalibrationFromPhotonJson"
          type="file"
          accept=".json"
          style="display: none"
          @change="importCalibration"
        />
      </v-col>
      <v-col>
        <v-btn
          color="secondary"
          class="mt-4"
          :disabled="!currentCalibrationCoeffs"
          style="width: 100%"
          @click="openExportCalibrationPrompt"
        >
          <v-icon left>mdi-export</v-icon>
          <span>Export</span>
        </v-btn>
        <a
          ref="exportCalibration"
          style="color: black; text-decoration: none; display: none"
          :href="exportCalibrationURL"
          target="_blank"
        />
      </v-col>
    </v-row>
    <v-row v-if="currentCalibrationCoeffs" class="pt-2">
      <v-card-subtitle>Calibration Details</v-card-subtitle>
      <v-simple-table dense style="width: 100%" class="pl-2 pr-2">
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
      </v-simple-table>
      <hr style="width: 100%" class="ma-6" />
      <v-card-subtitle>Per Observation Details</v-card-subtitle>
      <v-data-table
        dense
        style="width: 100%"
        class="pl-2 pr-2"
        :headers="[
          { text: 'Observation Id', value: 'index' },
          { text: 'Mean Reprojection Error', value: 'mean' }
        ]"
        :items="getObservationDetails()"
        item-key="index"
        show-expand
        expand-icon="mdi-eye"
      >
        <template #expanded-item="{ headers, item }">
          <td :colspan="headers.length">
            <div style="display: flex; justify-content: center; width: 100%">
              <img :src="calibrationImageURL(item.index)" alt="observation image" class="snapshot-preview pt-2 pb-2" />
            </div>
          </td>
        </template>
      </v-data-table>
    </v-row>
    <v-row v-else class="pt-2 mb-0 pb-0">
      The selected video format doesn't have any additional information as it has yet to be calibrated.
    </v-row>
  </v-card>
</template>

<style scoped>
.v-data-table {
  background-color: #006492 !important;
}
.snapshot-preview {
  max-width: 55%;
}

@media only screen and (max-width: 512px) {
  .snapshot-preview {
    max-width: 100%;
  }
}
</style>
