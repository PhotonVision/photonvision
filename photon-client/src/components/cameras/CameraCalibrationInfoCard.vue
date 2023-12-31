<script setup lang="ts">
import type { BoardObservation, CameraCalibrationResult, VideoFormat } from "@/types/SettingTypes";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { onBeforeMount, ref } from "vue";
import axios from "axios";
import loadingImage from "@/assets/images/loading.svg";

type JSONFileUploadEvent = Event & { target: HTMLInputElement | null };

const props = defineProps<{
  videoFormat: VideoFormat;
}>();

const getCalibrationCoeffs = (): CameraCalibrationResult | undefined => {
  return useCameraSettingsStore().currentCameraSettings.completeCalibrations.find(
    (cal) =>
      cal.resolution.width === props.videoFormat.resolution.width &&
      cal.resolution.height === props.videoFormat.resolution.height
  );
};
const getMeanFromView = (o: BoardObservation) => {
  // Is this the right formula for RMS error? who knows! not me!
  const perViewSumSquareReprojectionError = o.reprojectionErrors.flatMap((it2) => [it2.x, it2.y]);

  // For each error, square it, sum the squares, and divide by total points N
  return Math.sqrt(
    perViewSumSquareReprojectionError.map((it) => Math.pow(it, 2)).reduce((a, b) => a + b, 0) /
      perViewSumSquareReprojectionError.length
  );
};
const getResolutionString = (): string =>
  `${props.videoFormat.resolution.width}x${props.videoFormat.resolution.height}`;

// Import and export functions
const downloadCalibration = () => {
  const calibData = getCalibrationCoeffs();
  if (calibData === undefined) {
    useStateStore().showSnackbarMessage({
      color: "error",
      message:
        "Calibration data isn't available for the requested resolution, please calibrate the requested resolution first"
    });
    return;
  }

  const camUniqueName = useCameraSettingsStore().currentCameraSettings.uniqueName;
  const filename = `photon_calibration_${camUniqueName}_${calibData.resolution.width}x${calibData.resolution.height}.json`;
  const fileData = JSON.stringify(calibData);

  const element = document.createElement("a");
  element.style.display = "none";
  element.setAttribute("href", "data:text/plain;charset=utf-8," + encodeURIComponent(fileData));
  element.setAttribute("download", filename);

  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
};
const parseJsonFile = async (file: File): Promise<any> => {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();
    fileReader.onload = (event) => {
      const target: FileReader | null = event.target;
      if (target === null) reject();
      else resolve(JSON.parse(target.result as string));
    };
    fileReader.onerror = (error) => reject(error);
    fileReader.readAsText(file);
  });
};
const importCalibrationFromPhotonJson = ref();
const openUploadPhotonCalibJsonPrompt = () => {
  importCalibrationFromPhotonJson.value.click();
};
const importCalibration = async (payload: JSONFileUploadEvent) => {
  if (payload.target == null || !payload.target?.files) return;
  const files: FileList = payload.target.files as FileList;
  const uploadedJson = files[0];

  const data: CameraCalibrationResult = await parseJsonFile(uploadedJson);

  if (
    data.resolution.height != props.videoFormat.resolution.height ||
    data.resolution.width != props.videoFormat.resolution.width
  ) {
    useStateStore().showSnackbarMessage({
      color: "error",
      message: `The resolution of the calibration export doesn't match the current resolution ${props.videoFormat.resolution.height}x${props.videoFormat.resolution.width}`
    });
  }

  console.log(data);

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
  snapshotSrc: any;
  mean: number;
  index: number;
}
const getObservationDetails = (): ObservationDetails[] | undefined => {
  return getCalibrationCoeffs()?.observations.map((o, i) => ({
    index: i,
    mean: parseFloat(getMeanFromView(o).toFixed(2)),
    snapshotSrc: observationImgData.value[i] || loadingImage
  }));
};

const observationImgData = ref<string[]>([]);
onBeforeMount(() => {
  axios
    .get("/settings/camera/getCalibImages")
    .then(
      (response: { data: Record<string, Record<string, { snapshotData: string; snapshotFilename: string }[]>> }) => {
        observationImgData.value = response.data[useCameraSettingsStore().currentCameraName][getResolutionString()].map(
          (r) => "data:image/jpg;base64," + r.snapshotData
        );
      }
    )
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
});
</script>

<template>
  <v-card color="primary" class="pa-6" dark>
    <v-card-title class="pl-0 ml-0"
      >Calibration Details: {{ useCameraSettingsStore().currentCameraName }}@{{ getResolutionString() }}</v-card-title
    >
    <v-row v-if="getCalibrationCoeffs() !== undefined" class="pt-2">
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
              <td>{{ getCalibrationCoeffs().cameraIntrinsics.data[0].toFixed(2) }} mm</td>
            </tr>
            <tr>
              <td>Fy</td>
              <td>{{ getCalibrationCoeffs().cameraIntrinsics.data[4].toFixed(2) }} mm</td>
            </tr>
            <tr>
              <td>Cx</td>
              <td>{{ getCalibrationCoeffs().cameraIntrinsics.data[2].toFixed(2) }} px</td>
            </tr>
            <tr>
              <td>Cy</td>
              <td>{{ getCalibrationCoeffs().cameraIntrinsics.data[5].toFixed(2) }} px</td>
            </tr>
            <tr>
              <td>Distortion</td>
              <td>{{ getCalibrationCoeffs().cameraExtrinsics.data.map((it) => parseFloat(it.toFixed(3))) }}</td>
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
              <td>{{ videoFormat.horizontalFOV !== undefined ? videoFormat.horizontalFOV.toFixed(2) + "°" : "-" }}</td>
            </tr>
            <tr>
              <td>Vertical FOV</td>
              <td>{{ videoFormat.verticalFOV !== undefined ? videoFormat.verticalFOV.toFixed(2) + "°" : "-" }}</td>
            </tr>
            <tr>
              <td>Diagonal FOV</td>
              <td>{{ videoFormat.diagonalFOV !== undefined ? videoFormat.diagonalFOV.toFixed(2) + "°" : "-" }}</td>
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
              <img :src="item.snapshotSrc" alt="observation image" class="snapshot-preview pt-2 pb-2" />
            </div>
          </td>
        </template>
      </v-data-table>
    </v-row>
    <v-row v-else class="pt-2 mb-0 pb-0">
      The selected video format doesn't have any additional information as it has yet to be calibrated.
    </v-row>
    <v-row class="pt-8">
      <v-col>
        <v-btn color="secondary" style="width: 100%" @click="openUploadPhotonCalibJsonPrompt">
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
          :disabled="getCalibrationCoeffs() === undefined"
          style="width: 100%"
          @click="downloadCalibration"
        >
          <v-icon>mdi-export</v-icon>
          <span>Export</span>
        </v-btn>
      </v-col>
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
