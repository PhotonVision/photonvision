<script setup lang="ts">
import PhotonCalibrationVisualizer from "@/components/app/photon-calibration-visualizer.vue";
import PvButton from "@/components/common/pv-button.vue";
import PvTabs, { type PvTabItem } from "@/components/common/pv-tabs.vue";
import PvAlert from "@/components/common/pv-alert.vue";
import type { CameraCalibrationResult, VideoFormat } from "@/types/SettingTypes";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed, inject, ref, useTemplateRef } from "vue";
import { axiosPost, getResolutionString, parseJsonFile } from "@/lib/PhotonUtils";
import PvDeleteModal from "@/components/common/pv-delete-modal.vue";
import PvCard from "@/components/common/pv-card.vue";

const props = defineProps<{
  videoFormat: VideoFormat;
}>();

const confirmRemoveDialog = ref({ show: false, vf: props.videoFormat });

const removeCalibration = async (vf: VideoFormat) => {
  await axiosPost("/calibration/remove", "delete a camera calibration", {
    cameraUniqueName: useCameraSettingsStore().currentCameraSettings.uniqueName,
    width: vf.resolution.width,
    height: vf.resolution.height
  });
};

const exportCalibration = useTemplateRef("exportCalibration");
const openExportCalibrationPrompt = () => {
  exportCalibration.value?.click();
};

const importCalibrationFromPhotonJson = useTemplateRef("importCalibrationFromPhotonJson");
const openUploadPhotonCalibJsonPrompt = () => {
  importCalibrationFromPhotonJson.value?.click();
};
const importCalibration = async () => {
  const files = importCalibrationFromPhotonJson.value?.files;
  if (!files?.length) return;
  const uploadedJson = files[0];

  const data = await parseJsonFile<CameraCalibrationResult>(uploadedJson);

  if (
    data.resolution.height !== props.videoFormat.resolution.height ||
    data.resolution.width !== props.videoFormat.resolution.width
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
  index: number;
  mean: number;
  numOutliers: number;
  numMissing: number;
}

const currentCalibrationCoeffs = computed<CameraCalibrationResult | undefined>(() =>
  useCameraSettingsStore().getCalibrationCoeffs(props.videoFormat.resolution)
);

const getObservationDetails = (): ObservationDetails[] | undefined => {
  const coefficients = currentCalibrationCoeffs.value;

  return coefficients?.meanErrors.map((m, i) => ({
    index: i,
    mean: parseFloat(m.toFixed(2)),
    numOutliers: coefficients.numOutliers[i],
    numMissing: coefficients.numMissing[i]
  }));
};

const exportCalibrationURL = computed<string>(() =>
  useCameraSettingsStore().getCalJSONUrl(inject("backendHost") as string, props.videoFormat.resolution)
);
const calibrationImageURL = (index: number) =>
  useCameraSettingsStore().getCalImageUrl(inject<string>("backendHost") as string, props.videoFormat.resolution, index);

const tab = ref("details");
const viewingImg = ref(0);
const tabItems: PvTabItem<string>[] = [
  { label: "Details", value: "details" },
  { label: "Observations", value: "observations" }
];
</script>

<template>
  <pv-card padding="none">
    <div class="p-4 pb-2">
      <div class="flex flex-wrap">
        <div class="w-full p-0 md:w-1/2">
          <div class="text-base font-semibold">Calibration Details</div>
        </div>
        <div class="flex w-1/2 items-center pt-0 pb-0 pl-0 md:w-1/4">
          <pv-button variant="passive" icon="mdi-import" block @click="openUploadPhotonCalibJsonPrompt"
            >Import</pv-button
          >
          <input
            ref="importCalibrationFromPhotonJson"
            type="file"
            accept=".json"
            style="display: none"
            @change="importCalibration"
          />
        </div>
        <div class="flex w-1/2 items-center pt-0 pb-0 pr-0 md:w-1/4">
          <pv-button
            variant="passive"
            icon="mdi-export"
            block
            :disabled="!currentCalibrationCoeffs"
            @click="openExportCalibrationPrompt"
          >
            Export
          </pv-button>
          <a
            ref="exportCalibration"
            style="color: black; text-decoration: none; display: none"
            :href="exportCalibrationURL"
            target="_blank"
          />
        </div>
      </div>
    </div>

    <div class="flex flex-row pt-0 px-4 pb-4">
      <div class="w-1/3 p-0">
        <pv-tabs v-model="tab" :items="tabItems" class="mt-2" />
        <div class="pt-3">
          <div v-if="tab === 'details'">
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
                      px
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
                      px
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
                        currentCalibrationCoeffs?.calobjectWarp?.map((it) => (it * 1000).toFixed(2) + " mm").join(" / ")
                      }}
                    </td>
                  </tr>
                </tbody>
              </template>
            </v-table>
          </div>
          <div v-else-if="tab === 'observations'">
            <v-data-table
              id="observations-table"
              items-per-page-text="Page size:"
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
              <template #item.data-table-expand="{ internalItem }">
                <pv-button
                  size="icon"
                  variant="text"
                  :class="viewingImg === internalItem.index ? 'text-pv-button-active' : 'text-white/70'"
                  @click="viewingImg = internalItem.index"
                >
                  <span class="mdi mdi-eye text-lg leading-none" aria-hidden="true"></span>
                </pv-button>
              </template>
            </v-data-table>
          </div>
        </div>
      </div>
      <div class="w-2/3 p-0 pl-6">
        <div class="flex h-full justify-center p-0 items-center">
          <div v-if="!currentCalibrationCoeffs">
            <pv-alert
              class="pt-3 pb-3"
              color="primary"
              text="The selected video format has not been calibrated."
              icon="mdi-alert-circle-outline"
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
        </div>
      </div>
    </div>
  </pv-card>

  <pv-delete-modal
    v-model="confirmRemoveDialog.show"
    :width="500"
    title="Delete Calibration"
    :description="`Are you sure you want to delete the calibration for '${confirmRemoveDialog.vf.resolution.width}x${confirmRemoveDialog.vf.resolution.height}'? This action cannot be undone.`"
    :on-confirm="() => removeCalibration(confirmRemoveDialog.vf)"
  />
</template>

<style scoped>
.snapshot-preview {
  max-width: 100%;
  max-height: 100%;
}
</style>
