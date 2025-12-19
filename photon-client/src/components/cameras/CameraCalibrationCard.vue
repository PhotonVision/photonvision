<script setup lang="ts">
import { computed, ref, watchEffect } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { CalibrationBoardTypes, CalibrationTagFamilies, type VideoFormat } from "@/types/SettingTypes";
import MonoLogo from "@/assets/images/logoMono.png";
import CharucoImage from "@/assets/images/ChArUco_Marker8x8.png";
import PvSlider from "@/components/common/pv-slider.vue";
import { useStateStore } from "@/stores/StateStore";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvSelect from "@/components/common/pv-select.vue";
import PvNumberInput from "@/components/common/pv-number-input.vue";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import { getResolutionString, resolutionsAreEqual } from "@/lib/PhotonUtils";
import CameraCalibrationInfoCard from "@/components/cameras/CameraCalibrationInfoCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useTheme } from "vuetify";

const PromptRegular = import("@/assets/fonts/PromptRegular");
const jspdf = import("jspdf");

const theme = useTheme();

const settingsValid = ref(true);

const getUniqueVideoFormatsByResolution = (): VideoFormat[] => {
  const uniqueResolutions: VideoFormat[] = [];
  if (useCameraSettingsStore().currentCameraSettings.validVideoFormats.length === 0) return uniqueResolutions;
  useCameraSettingsStore().currentCameraSettings.validVideoFormats.forEach((format) => {
    const index = uniqueResolutions.findIndex((v) => resolutionsAreEqual(v.resolution, format.resolution));
    const contains = index != -1;
    let skip = false;
    if (contains && format.fps > uniqueResolutions[index].fps) {
      uniqueResolutions.splice(index, 1);
    } else if (contains) {
      skip = true;
    }

    if (!skip) {
      const calib = useCameraSettingsStore().getCalibrationCoeffs(format.resolution);
      if (calib !== undefined) {
        // Mean overall reprojection error
        // Calculated as average of each observation's mean error
        if (calib.meanErrors.length)
          format.mean = calib.meanErrors.reduce((a, b) => a + b, 0) / calib.meanErrors.length;
        else format.mean = NaN;

        format.horizontalFOV =
          2 * Math.atan2(format.resolution.width / 2, calib.cameraIntrinsics.data[0]) * (180 / Math.PI);
        format.verticalFOV =
          2 * Math.atan2(format.resolution.height / 2, calib.cameraIntrinsics.data[4]) * (180 / Math.PI);
        format.diagonalFOV =
          2 *
          Math.atan2(
            Math.sqrt(
              format.resolution.width ** 2 +
                (format.resolution.height / (calib.cameraIntrinsics.data[4] / calib.cameraIntrinsics.data[0])) ** 2
            ) / 2,
            calib.cameraIntrinsics.data[0]
          ) *
          (180 / Math.PI);
      }
      uniqueResolutions.push(format);
    }
  });
  uniqueResolutions.sort(
    (a, b) => b.resolution.width + b.resolution.height - (a.resolution.width + a.resolution.height)
  );
  return uniqueResolutions;
};

const getUniqueVideoResolutionStrings = (): { name: string; value: number }[] =>
  getUniqueVideoFormatsByResolution().map<{ name: string; value: number }>((f) => ({
    name: `${getResolutionString(f.resolution)}`,
    value: f.index || 0 // Index won't ever be undefined
  }));
const calibrationDivisors = computed(() =>
  [1, 2, 4].filter((v) => {
    const currentRes = useCameraSettingsStore().currentVideoFormat.resolution;
    return (currentRes.width / v >= 300 && currentRes.height / v >= 220) || v === 1;
  })
);

const uniqueVideoResolutionString = ref("");

// Use a watchEffect so the value is populated/reacts when the stores become available or update.
// This avoids trying to index into an array that may be empty during page reload.
watchEffect(() => {
  const currentIndex = useCameraSettingsStore().currentVideoFormat.index ?? 0;
  useStateStore().calibrationData.videoFormatIndex = currentIndex;
  const names = useCameraSettingsStore().currentCameraSettings.validVideoFormats.map((f) =>
    getResolutionString(f.resolution)
  );
  uniqueVideoResolutionString.value = names[currentIndex] ?? names[0] ?? "";
});
const squareSizeIn = ref(1);
const markerSizeIn = ref(0.75);
const patternWidth = ref(8);
const patternHeight = ref(8);
const boardType = ref<CalibrationBoardTypes>(CalibrationBoardTypes.Charuco);
const useOldPattern = ref(false);
const tagFamily = ref<CalibrationTagFamilies>(CalibrationTagFamilies.Dict_4X4_1000);

// Emperical testing - with stack size limit of 1MB, we can handle at -least- 700k points
const tooManyPoints = computed(
  () => useStateStore().calibrationData.imageCount * patternWidth.value * patternHeight.value > 700000
);

const downloadCalibBoard = async () => {
  const { jsPDF } = await jspdf;
  const { font } = await PromptRegular;
  const doc = new jsPDF({ unit: "in", format: "letter" });

  doc.addFileToVFS("Prompt-Regular.tff", font);
  doc.addFont("Prompt-Regular.tff", "Prompt-Regular", "normal");
  doc.setFont("Prompt-Regular");
  doc.setFontSize(12);

  const paperWidth = 8.5;
  const paperHeight = 11.0;

  switch (boardType.value) {
    case CalibrationBoardTypes.Chessboard:
      const chessboardStartX = (paperWidth - patternWidth.value * squareSizeIn.value) / 2;

      const chessboardStartY = (paperHeight - patternWidth.value * squareSizeIn.value) / 2;

      for (let squareY = 0; squareY < patternHeight.value; squareY++) {
        for (let squareX = 0; squareX < patternWidth.value; squareX++) {
          const xPos = chessboardStartX + squareX * squareSizeIn.value;
          const yPos = chessboardStartY + squareY * squareSizeIn.value;

          // Only draw the odd squares to create the chessboard pattern
          if (squareY % 2 != squareX % 2) {
            doc.rect(xPos, yPos, squareSizeIn.value, squareSizeIn.value, "F");
          }
        }
      }
      doc.text(`${patternWidth.value} x ${patternHeight.value} | ${squareSizeIn.value}in`, paperWidth - 1, 1.0, {
        maxWidth: (paperWidth - 2.0) / 2,
        align: "right"
      });
      break;

    case CalibrationBoardTypes.Charuco:
      // Add pregenerated ChArUco
      const charucoImage = new Image();
      charucoImage.src = CharucoImage;
      doc.addImage(charucoImage, "PNG", 0.25, 1.5, 8, 8);

      doc.text("8 x 8 | 1in & 0.75in", paperWidth - 1, 1.0, { maxWidth: (paperWidth - 2.0) / 2, align: "right" });

      break;
  }

  // Draw ruler pattern
  const lineStartX = 1.0;
  const lineEndX = paperWidth - lineStartX;
  const lineY = paperHeight - 1.0;

  doc.setLineWidth(0.01);
  doc.line(lineStartX, lineY, lineEndX, lineY);

  for (let tickX = lineStartX; tickX <= lineEndX; tickX++) {
    doc.line(tickX, lineY, tickX, lineY + 0.25);
    doc.text(`${tickX - 1}${tickX - 1 === 0 ? " in" : ""}`, tickX + 0.1, lineY + 0.25);
  }

  // Add branding
  const logoImage = new Image();
  logoImage.src = MonoLogo;
  doc.addImage(logoImage, "PNG", 1.0, 0.75, 1.4, 0.5);

  doc.save(`calibrationTarget-${CalibrationBoardTypes[boardType.value]}.pdf`);
};

const isCalibrating = computed(
  () => useCameraSettingsStore().currentCameraSettings.currentPipelineIndex === WebsocketPipelineType.Calib3d
);

const startCalibration = () => {
  useCameraSettingsStore().startPnPCalibration({
    squareSizeIn: squareSizeIn.value,
    markerSizeIn: markerSizeIn.value,
    patternHeight: patternHeight.value,
    patternWidth: patternWidth.value,
    boardType: boardType.value,
    useOldPattern: useOldPattern.value,
    tagFamily: tagFamily.value
  });
  // The Start PnP method already handles updating the backend so only a store update is required
  useCameraSettingsStore().currentCameraSettings.currentPipelineIndex = WebsocketPipelineType.Calib3d;
  // isCalibrating.value = true;
  calibCanceled.value = false;
};
const showCalibEndDialog = ref(false);
const calibCanceled = ref(false);
const calibSuccess = ref<boolean | undefined>(undefined);
const calibEndpointFail = ref(false);
const endCalibration = () => {
  calibSuccess.value = undefined;
  calibEndpointFail.value = false;

  if (!useStateStore().calibrationData.hasEnoughImages) {
    calibCanceled.value = true;
  }

  showCalibEndDialog.value = true;
  // Check if calibration finished cleanly or was canceled
  useCameraSettingsStore()
    .endPnPCalibration()
    .then(() => {
      calibSuccess.value = true;
    })
    .catch((e) => {
      if (e.response) {
        // Server returned a status code
      } else if (e.request) {
        // Something went wrong. Unsure if calibration actually worked
        calibEndpointFail.value = true;
      }
      calibSuccess.value = false;
    })
    .finally(() => {
      // isCalibrating.value = false;
      // backend deals with this for us
    });
};

const drawAllSnapshots = ref(true);

const showCalDialog = ref(false);
const selectedVideoFormat = ref<VideoFormat | undefined>(undefined);
const setSelectedVideoFormat = (format: VideoFormat) => {
  selectedVideoFormat.value = format;
  showCalDialog.value = true;
};
</script>

<template>
  <div>
    <v-card class="mb-3 rounded-12" color="surface" dark>
      <v-card-title>Camera Calibration</v-card-title>
      <v-card-text v-if="!isCalibrating" class="pb-0">
        <v-card-subtitle class="pa-0 pb-3 text-white">Current Calibrations</v-card-subtitle>
        <v-table fixed-header height="100%" density="compact">
          <thead>
            <tr>
              <th>Resolution</th>
              <th>Mean Error</th>
              <th>Horizontal FOV</th>
              <th>Vertical FOV</th>
              <th>Diagonal FOV</th>
            </tr>
          </thead>
          <tbody style="cursor: pointer">
            <v-tooltip
              v-for="(value, index) in getUniqueVideoFormatsByResolution()"
              :key="index"
              transition=""
              location="bottom"
              :open-delay="100"
            >
              <template #activator="{ props }">
                <tr :key="index" v-bind="props" @click="setSelectedVideoFormat(value)">
                  <td>{{ getResolutionString(value.resolution) }}</td>
                  <td>
                    {{
                      value.mean !== undefined ? (isNaN(value.mean) ? "Unknown" : value.mean.toFixed(2) + "px") : "-"
                    }}
                  </td>
                  <td>{{ value.horizontalFOV !== undefined ? value.horizontalFOV.toFixed(2) + "°" : "-" }}</td>
                  <td>{{ value.verticalFOV !== undefined ? value.verticalFOV.toFixed(2) + "°" : "-" }}</td>
                  <td>{{ value.diagonalFOV !== undefined ? value.diagonalFOV.toFixed(2) + "°" : "-" }}</td>
                </tr>
              </template>
              <span>View calibration information</span>
            </v-tooltip>
          </tbody>
        </v-table>
      </v-card-text>
      <v-card-text class="pt-0">
        <div v-if="useCameraSettingsStore().isConnected" class="d-flex flex-column">
          <v-card-subtitle v-if="!isCalibrating" class="pl-0 pb-3 pt-3 text-white"
            >Configure New Calibration</v-card-subtitle
          >
          <v-form ref="form" v-model="settingsValid">
            <v-alert
              closable
              density="compact"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
              :color="useSettingsStore().general.mrCalWorking ? 'buttonPassive' : 'error'"
              :icon="useSettingsStore().general.mrCalWorking ? 'mdi-check' : 'mdi-close'"
              :text="
                useSettingsStore().general.mrCalWorking
                  ? 'Mrcal was successfully loaded and will be used!'
                  : 'MrCal failed to load, check journalctl logs for details.'
              "
            />
            <pv-select
              v-model="uniqueVideoResolutionString"
              label="Resolution"
              :select-cols="8"
              :disabled="isCalibrating"
              tooltip="Resolution to calibrate at (you will have to calibrate every resolution you use 3D mode on)"
              :items="getUniqueVideoResolutionStrings()"
              @update:model-value="
                useStateStore().calibrationData.videoFormatIndex =
                  getUniqueVideoResolutionStrings().find((v) => v.value === $event)?.value || 0
              "
            />
            <pv-select
              v-model="boardType"
              label="Board Type"
              tooltip="Calibration board pattern to use"
              :select-cols="8"
              :items="['Chessboard', 'ChArUco']"
              :disabled="isCalibrating"
            />
            <v-alert
              v-if="boardType !== CalibrationBoardTypes.Charuco"
              closable
              density="compact"
              variant="tonal"
              color="warning"
              icon="mdi-alert-box"
              text="The usage of chessboards can result in bad calibration results if multiple
              similar images are taken. We strongly recommend that teams use ChArUco boards instead!"
            />
            <pv-select
              v-if="boardType !== CalibrationBoardTypes.Charuco"
              v-model="useCameraSettingsStore().currentPipelineSettings.streamingFrameDivisor"
              label="Decimation"
              tooltip="Resolution to which camera frames are downscaled for detection. Calibration still uses full-res"
              :items="calibrationDivisors"
              :select-cols="8"
              @update:modelValue="
                (v) => useCameraSettingsStore().changeCurrentPipelineSetting({ streamingFrameDivisor: +v }, false)
              "
            />
            <pv-select
              v-if="boardType === CalibrationBoardTypes.Charuco"
              v-model="tagFamily"
              label="Tag Family"
              tooltip="Dictionary of ArUco markers on the ChArUco board"
              :select-cols="8"
              :items="['Dict_4X4_1000', 'Dict_5X5_1000', 'Dict_6X6_1000', 'Dict_7X7_1000']"
              :disabled="isCalibrating"
            />
            <pv-number-input
              v-model="squareSizeIn"
              label="Pattern Spacing (in)"
              tooltip="Spacing between pattern features in inches"
              :disabled="isCalibrating"
              :rules="[(v) => v > 0 || 'Size must be positive']"
              :label-cols="4"
            />
            <pv-number-input
              v-if="boardType === CalibrationBoardTypes.Charuco"
              v-model="markerSizeIn"
              label="Marker Size (in)"
              tooltip="Size of the tag markers in inches must be smaller than pattern spacing"
              :disabled="isCalibrating"
              :rules="[(v) => v > 0 || 'Size must be positive']"
              :label-cols="4"
            />
            <pv-number-input
              v-model="patternWidth"
              label="Board Width (squares)"
              tooltip="Width of the board in dots or chessboard squares"
              :disabled="isCalibrating"
              :rules="[(v) => v >= 4 || 'Width must be at least 4']"
              :label-cols="4"
            />
            <pv-number-input
              v-model="patternHeight"
              label="Board Height (squares)"
              tooltip="Height of the board in dots or chessboard squares"
              :disabled="isCalibrating"
              :rules="[(v) => v >= 4 || 'Height must be at least 4']"
              :label-cols="4"
            />
            <pv-switch
              v-if="boardType === CalibrationBoardTypes.Charuco"
              v-model="useOldPattern"
              label="Old OpenCV Pattern"
              :disabled="isCalibrating"
              tooltip="If enabled, Photon will use the old OpenCV pattern for calibration."
              :label-cols="4"
            />
          </v-form>
        </div>
        <div v-if="isCalibrating">
          <pv-switch
            v-model="drawAllSnapshots"
            label="Draw Collected Corners"
            :switch-cols="8"
            tooltip="Draw all snapshots"
            @update:modelValue="
              (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ drawAllSnapshots: args }, false)
            "
          />
          <pv-switch
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraAutoExposure"
            label="Auto Exposure"
            :label-cols="4"
            tooltip="Enables or Disables camera automatic adjustment for current lighting conditions"
            @update:modelValue="
              (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraAutoExposure: args }, false)
            "
          />
          <pv-slider
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraExposureRaw"
            :disabled="useCameraSettingsStore().currentCameraSettings.pipelineSettings.cameraAutoExposure"
            label="Exposure"
            tooltip="Directly controls how long the camera shutter remains open. Units are dependant on the underlying driver."
            :min="useCameraSettingsStore().minExposureRaw"
            :max="useCameraSettingsStore().maxExposureRaw"
            :slider-cols="8"
            :step="1"
            @update:modelValue="
              (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraExposureRaw: args }, false)
            "
          />
          <pv-slider
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraBrightness"
            label="Brightness"
            :min="0"
            :max="100"
            :slider-cols="8"
            @update:modelValue="
              (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraBrightness: args }, false)
            "
          />
          <pv-slider
            v-if="useCameraSettingsStore().currentPipelineSettings.cameraGain >= 0"
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraGain"
            label="Camera Gain"
            tooltip="Controls camera gain, similar to brightness"
            :min="0"
            :max="100"
            :slider-cols="8"
            @update:modelValue="
              (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraGain: args }, false)
            "
          />
          <pv-slider
            v-if="useCameraSettingsStore().currentPipelineSettings.cameraRedGain !== -1"
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraRedGain"
            label="Red AWB Gain"
            :min="0"
            :max="100"
            :slider-cols="8"
            tooltip="Controls red automatic white balance gain, which affects how the camera captures colors in different conditions"
            @update:modelValue="
              (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraRedGain: args }, false)
            "
          />
          <pv-slider
            v-if="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain !== -1"
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain"
            label="Blue AWB Gain"
            :min="0"
            :max="100"
            :slider-cols="8"
            tooltip="Controls blue automatic white balance gain, which affects how the camera captures colors in different conditions"
            @update:modelValue="
              (args) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraBlueGain: args }, false)
            "
          />
        </div>
        <div v-if="isCalibrating" class="d-flex justify-center align-center pt-10px pb-5">
          <v-chip
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
            label
            :color="useStateStore().calibrationData.hasEnoughImages ? 'buttonPassive' : 'light-grey'"
          >
            Snapshots: {{ useStateStore().calibrationData.imageCount }} of at least
            {{ useStateStore().calibrationData.minimumImageCount }}
          </v-chip>
        </div>
        <div>
          <v-btn
            color="buttonPassive"
            size="small"
            block
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            :disabled="!settingsValid"
            @click="downloadCalibBoard"
          >
            <v-icon start class="calib-btn-icon" size="large"> mdi-download </v-icon>
            <span class="calib-btn-label">Generate Board</span>
          </v-btn>
        </div>
        <v-alert
          v-if="tooManyPoints"
          class="mt-5"
          color="error"
          density="compact"
          text="Too many corners. Finish calibration now!"
          icon="mdi-alert-circle-outline"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
        />
        <div class="d-flex pt-5">
          <v-col cols="6" class="pa-0 pr-2">
            <v-btn
              size="small"
              block
              color="buttonActive"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              :disabled="!settingsValid || tooManyPoints"
              @click="isCalibrating ? useCameraSettingsStore().takeCalibrationSnapshot() : startCalibration()"
            >
              <v-icon start class="calib-btn-icon" size="large">
                {{ isCalibrating ? "mdi-camera" : "mdi-flag-outline" }}
              </v-icon>
              <span class="calib-btn-label">{{ isCalibrating ? "Take Snapshot" : "Start Calibration" }}</span>
            </v-btn>
          </v-col>
          <v-col cols="6" class="pa-0 pl-2">
            <v-btn
              size="small"
              block
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              :color="useStateStore().calibrationData.hasEnoughImages ? 'buttonActive' : 'error'"
              :disabled="!isCalibrating || !settingsValid"
              @click="endCalibration"
            >
              <v-icon start class="calib-btn-icon" size="large">
                {{ useStateStore().calibrationData.hasEnoughImages ? "mdi-flag-checkered" : "mdi-flag-off-outline" }}
              </v-icon>
              <span class="calib-btn-label">{{
                useStateStore().calibrationData.hasEnoughImages ? "Finish Calibration" : "Cancel Calibration"
              }}</span>
            </v-btn>
          </v-col>
        </div>
      </v-card-text>
    </v-card>
    <v-dialog v-model="showCalibEndDialog" width="500px" :persistent="true">
      <v-card color="surface" dark>
        <v-card-title> Camera Calibration </v-card-title>
        <div style="text-align: center">
          <template v-if="calibCanceled">
            <v-icon color="primary" size="70"> mdi-cancel </v-icon>
            <v-card-text>
              Camera calibration has been canceled. The backend is attempting to cleanly cancel the calibration process.
            </v-card-text>
          </template>
          <!-- No result reported yet -->
          <template v-else-if="calibSuccess === undefined">
            <v-progress-circular indeterminate :size="70" :width="8" color="primary" />
            <v-card-text>Camera is being calibrated. This process may take several minutes...</v-card-text>
          </template>
          <!-- Got positive result -->
          <template v-else-if="calibSuccess">
            <v-icon color="#00ff00" size="70"> mdi-check </v-icon>
            <v-card-text>
              Camera has been successfully calibrated for
              {{
                useCameraSettingsStore().currentCameraSettings.validVideoFormats.map((f) =>
                  getResolutionString(f.resolution)
                )[useStateStore().calibrationData.videoFormatIndex]
              }}!
            </v-card-text>
          </template>
          <template v-else-if="calibEndpointFail">
            <v-icon color="gray" size="70"> mdi-help-circle-outline </v-icon>
            <v-card-text
              >Unable to determine if calibration was successful. Refresh this page and manually check if calibration
              was successful.</v-card-text
            >
          </template>
          <template v-else>
            <v-icon color="red" size="70"> mdi-close </v-icon>
            <v-card-text>
              Camera calibration failed! Make sure that the photos are taken such that the rainbow grid circles align
              with the corners of the chessboard, and try again. More information is available in the program logs.
            </v-card-text>
          </template>
        </div>
        <v-card-actions class="pa-5 pt-0">
          <v-spacer />
          <v-btn v-if="!isCalibrating" color="white" variant="text" @click="showCalibEndDialog = false"> OK </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="showCalDialog" width="80em">
      <CameraCalibrationInfoCard v-if="selectedVideoFormat" :video-format="selectedVideoFormat" />
    </v-dialog>
  </div>
</template>

<style scoped lang="scss">
th {
  text-align: center !important;
  padding: 0 8px !important;
}

.v-table {
  text-align: center;
  width: 100%;

  th,
  td {
    font-size: 1rem !important;
  }

  tbody :hover td {
    cursor: pointer;
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

@media only screen and (max-width: 512px) {
  .calib-btn-icon {
    margin: 0 !important;
  }
  .calib-btn-label {
    display: none;
  }
}
</style>
