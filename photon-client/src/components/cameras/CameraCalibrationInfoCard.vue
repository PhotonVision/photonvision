<script setup lang="ts">
import type { CameraCalibrationResult, VideoFormat } from "@/types/SettingTypes";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed, inject, ref } from "vue";
import { getResolutionString, parseJsonFile } from "@/lib/PhotonUtils";
import { useTheme } from "vuetify";

const theme = useTheme();

import { defineAsyncComponent } from "vue";

const PhotonCalibrationVisualizer = defineAsyncComponent({
  loader: () => import("@/components/app/photon-calibration-visualizer.vue")
});

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

var tab = ref("details");
var viewingImg = ref(0);
</script>

<template>
  <v-card color="surface" dark>
    <div class="d-flex flex-wrap pt-3 pl-3 pr-3">
      <v-col cols="12" md="6">
        <v-card-title class="pa-0"> Calibration Details </v-card-title>
      </v-col>
      <v-col cols="6" md="3" class="d-flex align-center pt-0 pt-md-3 pl-6 pl-md-3">
        <v-btn
          color="buttonPassive"
          style="width: 100%"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="openUploadPhotonCalibJsonPrompt"
        >
          <v-icon start size="large"> mdi-import</v-icon>
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
      <v-col cols="6" md="3" class="d-flex align-center pt-0 pt-md-3 pr-6 pr-md-3">
        <v-btn
          color="buttonPassive"
          :disabled="!currentCalibrationCoeffs"
          style="width: 100%"
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
      </v-col>
    </div>
    <v-card-text class="d-flex flex-row pa-0">
      <v-col cols="4" class="pt-0">
        <v-tabs v-model="tab" grow bg-color="surface" height="48" slider-color="buttonActive" class="pl-5">
          <v-tab key="details" value="details">Details</v-tab>
          <v-tab key="observations" value="observations">Observations</v-tab>
        </v-tabs>
        <v-tabs-window v-model="tab" class="pt-3">
          <v-tabs-window-item key="details" value="details">
            <v-card-text class="pt-0">
              <v-table style="width: 100%" density="compact">
                <template #default>
                  <tbody>
                    <tr>
                      <td>Camera</td>
                      <td>
                        {{ useCameraSettingsStore().currentCameraName }}
                      </td>
                    </tr>
                    <tr>
                      <td>Resolution</td>
                      <td>
                        {{ getResolutionString(videoFormat.resolution) }}
                      </td>
                    </tr>
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
                      <td>
                        {{ videoFormat.verticalFOV !== undefined ? videoFormat.verticalFOV.toFixed(2) + "°" : "-" }}
                      </td>
                    </tr>
                    <tr>
                      <td>Diagonal FOV</td>
                      <td>
                        {{ videoFormat.diagonalFOV !== undefined ? videoFormat.diagonalFOV.toFixed(2) + "°" : "-" }}
                      </td>
                    </tr>
                    <!-- Board warp, only shown for mrcal-calibrated cameras -->
                    <tr v-if="currentCalibrationCoeffs?.calobjectWarp?.length === 2">
                      <td>Board warp, X/Y</td>
                      <td>
                        {{
                          currentCalibrationCoeffs?.calobjectWarp
                            ?.map((it) => (it * 1000).toFixed(2) + " mm")
                            .join(" / ")
                        }}
                      </td>
                    </tr>
                  </tbody>
                </template>
              </v-table>
            </v-card-text>
          </v-tabs-window-item>
          <v-tabs-window-item key="observations" value="observations">
            <v-card-text class="pt-0 pb-0">
              <!-- <v-table fixed-header style="max-height: 500px" class="pb-3" density="compact">
                <thead>
                  <tr>
                    <th>Id</th>
                    <th>Mean Reprojection Error</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(value, index) in getObservationDetails()"
                    :key="index"
                    v-bind="props"
                    @click="viewingImg = index"
                  >
                    <td>{{ value.index }}</td>
                    <td>{{ value.mean }}</td>
                  </tr>
                </tbody>
              </v-table> -->
              <v-data-table
                density="compact"
                style="width: 100%"
                :headers="[
                  { title: 'Id', key: 'index' },
                  { title: 'Mean Reprojection Error', key: 'mean' }
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
                    @click="viewingImg = internalItem.index"
                  ></v-btn>
                </template>
              </v-data-table>
            </v-card-text>
          </v-tabs-window-item>
        </v-tabs-window>
      </v-col>
      <v-col cols="8" class="pt-0 pr-6 pb-6">
        <v-card-text class="pa-0 fill-height d-flex justify-center align-center">
          <div v-if="!currentCalibrationCoeffs">
            <v-alert
              class="pt-3 pb-3"
              color="primary"
              text="The selected video format has not been calibrated."
              icon="mdi-alert-circle-outline"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
            />
          </div>
          <Suspense v-else-if="tab === 'details'">
            <!-- Allows us to import three js when it's actually needed  -->
            <PhotonCalibrationVisualizer
              :camera-unique-name="useCameraSettingsStore().currentCameraSettings.uniqueName"
              :resolution="props.videoFormat.resolution"
              title="Camera to Board Transforms"
            />
            <template #fallback> Loading... </template>
          </Suspense>
          <div v-else style="display: flex; justify-content: center; width: 100%">
            <img :src="calibrationImageURL(viewingImg)" alt="observation image" class="snapshot-preview pt-2 pb-2" />
          </div>
        </v-card-text>
      </v-col>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.snapshot-preview {
  max-width: 100%;
}
</style>
