<script setup lang="ts">
import { computed, ref, watchEffect } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { CalibrationBoardTypes, CalibrationTagFamilies, type VideoFormat } from "@/types/SettingTypes";
import MonoLogo from "@/assets/images/logoMono.png";
import CharucoImage from "@/assets/images/ChArUco_Marker8x8.png";

import { useStateStore } from "@/stores/StateStore";

import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import { getResolutionString, resolutionsAreEqual } from "@/lib/PhotonUtils";
import CameraCalibrationInfoCard from "@/components/cameras/CameraCalibrationInfoCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import IconInformation from "~icons/mdi/information";
import IconAlertBox from "~icons/mdi/alert-box";
import IconCheck from "~icons/mdi/check";
import IconClose from "~icons/mdi/close";
import IconDownload from "~icons/mdi/download";
import IconAlertCircleOutline from "~icons/mdi/alert-circle-outline";
import IconCamera from "~icons/mdi/camera";
import IconFlagOutline from "~icons/mdi/flag-outline";
import IconFlagCheckered from "~icons/mdi/flag-checkered";
import IconFlagOffOutline from "~icons/mdi/flag-off-outline";
import IconCancel from "~icons/mdi/cancel";
import IconHelpCircleOutline from "~icons/mdi/help-circle-outline";

const jspdf = import("jspdf");

const MM_PER_INCH = 25.4;

type RuleValue = string | number | null;

const positiveNumberRule = (value: RuleValue) => (typeof value === "number" && value > 0) || "Size must be positive";
const minWidthRule = (value: RuleValue) => (typeof value === "number" && value >= 4) || "Width must be at least 4";
const minHeightRule = (value: RuleValue) => (typeof value === "number" && value >= 4) || "Height must be at least 4";

const settingsValid = computed(() => {
  if (!Number.isFinite(squareSize.value) || squareSize.value <= 0) return false;
  if (
    boardType.value === CalibrationBoardTypes.Charuco &&
    (!Number.isFinite(markerSize.value) || markerSize.value <= 0)
  )
    return false;
  if (!Number.isFinite(patternWidth.value) || patternWidth.value < 4) return false;
  if (!Number.isFinite(patternHeight.value) || patternHeight.value < 4) return false;

  return true;
});

const getUniqueVideoFormatsByResolution = (): VideoFormat[] => {
  const uniqueResolutions: VideoFormat[] = [];
  if (useCameraSettingsStore().currentCameraSettings.validVideoFormats.length === 0) return uniqueResolutions;
  useCameraSettingsStore().currentCameraSettings.validVideoFormats.forEach((format) => {
    const index = uniqueResolutions.findIndex((v) => resolutionsAreEqual(v.resolution, format.resolution));
    const contains = index !== -1;
    let skip = false;
    if (contains && format.fps > uniqueResolutions[index].fps) {
      uniqueResolutions.splice(index, 1);
    } else if (contains) {
      skip = true;
    }

    if (!skip) {
      const calib = useCameraSettingsStore().getCalibrationCoeffs(format.resolution);

      // minPixelCount is the multiplied area of a 640x480 (the minimum for proper calibration) resolution
      const minPixelCount = 640 * 480;
      const resArea = format.resolution.width * format.resolution.height;

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

      if (resArea >= minPixelCount) {
        uniqueResolutions.push(format);
      }
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

const uniqueVideoResolutionIndex = ref(getUniqueVideoResolutionStrings()?.[0]?.value);

// Use a watchEffect so the value is populated/reacts when the stores become available or update.
// This avoids trying to index into an array that may be empty during page reload.
watchEffect(() => {
  const names = useCameraSettingsStore().currentCameraSettings.validVideoFormats.map((f) =>
    getResolutionString(f.resolution)
  );
  const currentFormatIndex = useCameraSettingsStore().currentVideoFormat.index ?? 0;
  // Checks if the current resolution is present in the list of valid formats, if not defaults to the last index (which is usually the highest resolution)
  const currentIndex =
    getUniqueVideoResolutionStrings()
      .map((x) => x.name)
      .find((n) => n === names[currentFormatIndex]) !== undefined
      ? currentFormatIndex
      : names.length - 1;
  useStateStore().calibrationData.videoFormatIndex = currentIndex;
  uniqueVideoResolutionIndex.value = currentIndex;
});
const dimensionUnit = ref<"in" | "mm">("in");
const squareSizeIn = ref(1);
const markerSizeIn = ref(0.75);
const patternWidth = ref(8);
const patternHeight = ref(8);
const boardType = ref<CalibrationBoardTypes>(CalibrationBoardTypes.Charuco);
const useOldPattern = ref(false);
const tagFamily = ref<CalibrationTagFamilies>(CalibrationTagFamilies.Dict_4X4_1000);
const requestedVideoFormatIndex = ref(0);

const convertInchesToDisplay = (valueInInches: number) =>
  dimensionUnit.value === "mm" ? valueInInches * MM_PER_INCH : valueInInches;

const convertDisplayToInches = (displayValue: number) =>
  dimensionUnit.value === "mm" ? displayValue / MM_PER_INCH : displayValue;

const squareSize = computed({
  get: () => convertInchesToDisplay(squareSizeIn.value),
  set(value) {
    squareSizeIn.value = convertDisplayToInches(value);
  }
});

const markerSize = computed({
  get: () => convertInchesToDisplay(markerSizeIn.value),
  set(value) {
    markerSizeIn.value = convertDisplayToInches(value);
  }
});

const dimensionStep = computed(() => (dimensionUnit.value === "mm" ? 0.1 : 0.01));

// Emperical testing - with stack size limit of 1MB, we can handle at -least- 700k points
const tooManyPoints = computed(
  () => useStateStore().calibrationData.imageCount * patternWidth.value * patternHeight.value > 700000
);

const downloadCalibBoard = async () => {
  const { jsPDF } = await jspdf;
  const { font } = await import("@/assets/fonts/PromptRegular");
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
          if (squareY % 2 !== squareX % 2) {
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
  () => useCameraSettingsStore().currentCameraSettings.currentPipelineIndex === WebsocketPipelineType.Calib3d.valueOf()
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
  requestedVideoFormatIndex.value = useStateStore().calibrationData.videoFormatIndex;
};
const showCalibEndDialog = ref(false);
const calibCanceled = ref(false);
const calibSuccess = ref<boolean | undefined>(undefined);
const calibEndpointFail = ref(false);
const endCalibration = () => {
  calibSuccess.value = undefined;
  calibEndpointFail.value = false;

  if (!hasEnoughImages.value) {
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

const bypassVal = ref(false);
const minCount = computed(() => (bypassVal.value ? 10 : 100));
const hasEnoughImages = computed(() => useStateStore().calibrationData.imageCount >= minCount.value);

const showCalDialog = ref(false);
const selectedVideoFormat = ref<VideoFormat | undefined>(undefined);
const setSelectedVideoFormat = (format: VideoFormat) => {
  selectedVideoFormat.value = format;
  showCalDialog.value = true;
};

const updateVideoFormatIndex = (value: number) => {
  useStateStore().calibrationData.videoFormatIndex = value;
};

const updateStreamingFrameDivisor = (value: number | string) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ streamingFrameDivisor: Number(value) }, false);
};

const updateDrawAllSnapshots = (value: boolean | undefined) => {
  if (value === undefined) {
    return;
  }
  useCameraSettingsStore().changeCurrentPipelineSetting({ drawAllSnapshots: value }, false);
};

const updateCameraAutoExposure = (value: boolean | undefined) => {
  if (value === undefined) {
    return;
  }
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraAutoExposure: value }, false);
};

const updateCameraExposure = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraExposureRaw: value }, false);
};

const updateCameraBrightness = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraBrightness: value }, false);
};

const updateCameraGain = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraGain: value }, false);
};

const updateCameraRedGain = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraRedGain: value }, false);
};

const updateCameraBlueGain = (value: number) => {
  useCameraSettingsStore().changeCurrentPipelineSetting({ cameraBlueGain: value }, false);
};
</script>

<template>
  <div>
    <pv-card class="mb-3">
      <div class="pb-2 text-lg font-semibold">Camera Calibration</div>
      <div class="pt-0">
        <div v-if="!isCalibrating" class="pb-0">
          <div class="pb-3">
            <pv-tooltipped-label
              label="Current Calibrations"
              :icon="IconInformation"
              location="top"
              tooltip="Click on a resolution to view detailed calibration information and import/export a calibration."
            />
          </div>
          <pv-table fixed-header height="100%">
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
              <pv-tooltip
                v-for="(value, index) in getUniqueVideoFormatsByResolution()"
                :key="index"
                location="bottom"
                :delay="100"
                text="View calibration information"
              >
                <tr @click="setSelectedVideoFormat(value)">
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
              </pv-tooltip>
            </tbody>
          </pv-table>
        </div>
        <div v-if="useCameraSettingsStore().isConnected" class="d-flex flex-column">
          <div class="pt-4 pb-3 pl-0 text-base font-semibold opacity-100">Configure New Calibration</div>
          <div>
            <pv-select
              v-model="uniqueVideoResolutionIndex"
              label="Resolution"
              :select-cols="8"
              :disabled="isCalibrating"
              tooltip="Resolution to calibrate at (you will have to calibrate every resolution you use 3D mode on)"
              :items="getUniqueVideoResolutionStrings()"
              @update:model-value="updateVideoFormatIndex"
            />
            <pv-select
              v-model="boardType"
              label="Board Type"
              tooltip="Calibration board pattern to use"
              :select-cols="8"
              :items="[
                { value: CalibrationBoardTypes.Charuco, name: 'ChArUco' },
                { value: CalibrationBoardTypes.Chessboard, name: 'Chessboard' }
              ]"
              :disabled="isCalibrating"
            />
            <pv-alert
              v-if="boardType !== CalibrationBoardTypes.Charuco"
              closable
              variant="tonal"
              color="warning"
              :icon="IconAlertBox"
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
              @update:modelValue="updateStreamingFrameDivisor"
            />
            <pv-select
              v-if="boardType === CalibrationBoardTypes.Charuco"
              v-model="tagFamily"
              label="Tag Family"
              tooltip="Dictionary of ArUco markers on the ChArUco board"
              :select-cols="8"
              :items="[
                { value: CalibrationTagFamilies.Dict_4X4_1000, name: 'Dict_4X4_1000' },
                { value: CalibrationTagFamilies.Dict_5X5_1000, name: 'Dict_5X5_1000' },
                { value: CalibrationTagFamilies.Dict_6X6_1000, name: 'Dict_6X6_1000' },
                { value: CalibrationTagFamilies.Dict_7X7_1000, name: 'Dict_7X7_1000' }
              ]"
              :disabled="isCalibrating"
            />
            <pv-select
              v-model="dimensionUnit"
              label="Dimension Unit"
              tooltip="Units used for pattern spacing and marker size inputs"
              :select-cols="8"
              :items="[
                { value: 'in', name: 'Inches' },
                { value: 'mm', name: 'Millimeters' }
              ]"
              :disabled="isCalibrating"
            />
            <pv-number-input
              v-model="squareSize"
              :label="`Pattern Spacing (${dimensionUnit})`"
              :tooltip="`Spacing between pattern features in ${dimensionUnit === 'mm' ? 'millimeters' : 'inches'}`"
              :disabled="isCalibrating"
              :rules="[positiveNumberRule]"
              :label-cols="4"
              :step="dimensionStep"
            />
            <pv-number-input
              v-if="boardType === CalibrationBoardTypes.Charuco"
              v-model="markerSize"
              :label="`Marker Size (${dimensionUnit})`"
              :tooltip="`Size of the tag markers in ${dimensionUnit === 'mm' ? 'millimeters' : 'inches'}; must be smaller than pattern spacing`"
              :disabled="isCalibrating"
              :rules="[positiveNumberRule]"
              :label-cols="4"
              :step="dimensionStep"
            />
            <pv-number-input
              v-model="patternWidth"
              label="Board Width (squares)"
              tooltip="Width of the board in dots or chessboard squares"
              :disabled="isCalibrating"
              :rules="[minWidthRule]"
              :label-cols="4"
            />
            <pv-number-input
              v-model="patternHeight"
              label="Board Height (squares)"
              tooltip="Height of the board in dots or chessboard squares"
              :disabled="isCalibrating"
              :rules="[minHeightRule]"
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
          </div>
        </div>
        <pv-alert
          closable
          class="mb-5"
          :color="useSettingsStore().general.mrCalWorking ? 'buttonPassive' : 'error'"
          :icon="useSettingsStore().general.mrCalWorking ? IconCheck : IconClose"
          :text="
            useSettingsStore().general.mrCalWorking
              ? 'Mrcal was successfully loaded and will be used!'
              : 'MrCal failed to load, check journalctl logs for details.'
          "
        />
        <div class="flex pb-5">
          <div class="w-1/2 p-0 pr-2">
            <pv-button
              size="sm"
              variant="primary"
              :icon="isCalibrating ? IconCamera : IconFlagOutline"
              block
              :disabled="!settingsValid || tooManyPoints"
              @click="isCalibrating ? useCameraSettingsStore().takeCalibrationSnapshot() : startCalibration()"
            >
              <span class="calib-btn-label">{{ isCalibrating ? "Take Snapshot" : "Start Calibration" }}</span>
            </pv-button>
          </div>
          <div class="w-1/2 p-0 pl-2">
            <pv-button
              size="sm"
              :variant="!isCalibrating || hasEnoughImages ? 'primary' : 'danger'"
              :icon="!isCalibrating || hasEnoughImages ? IconFlagCheckered : IconFlagOffOutline"
              block
              :disabled="!isCalibrating || !settingsValid"
              @click="endCalibration"
            >
              <span class="calib-btn-label">
                {{ !isCalibrating || hasEnoughImages ? "Finish Calibration" : "Cancel Calibration" }}
              </span>
            </pv-button>
          </div>
        </div>
        <div>
          <pv-button
            size="sm"
            variant="passive"
            :icon="IconDownload"
            block
            :disabled="!settingsValid"
            @click="downloadCalibBoard"
          >
            <span class="calib-btn-label">Generate Board</span>
          </pv-button>
        </div>
        <pv-alert
          v-if="tooManyPoints"
          class="mt-5"
          color="error"
          text="Too many corners. Finish calibration now!"
          :icon="IconAlertCircleOutline"
        />
        <div v-if="isCalibrating" class="pt-5">
          <div class="flex flex-wrap items-center justify-between gap-4 pb-5">
            <pv-chip label :color="hasEnoughImages ? 'buttonPassive' : 'light-grey'">
              Snapshots: {{ useStateStore().calibrationData.imageCount }} of at least
              {{ minCount }}
            </pv-chip>
            <pv-switch
              v-model="bypassVal"
              label="Bypass minimum"
              :label-cols="6"
              :switch-cols="6"
              tooltip="Bypass the minimum recommended amount of snapshots for a calibration. Should only be used for dev work or temporary tests not competitions. Still requires 10 images to calibrate."
            />
          </div>
          <pv-switch
            v-model="drawAllSnapshots"
            label="Draw Collected Corners"
            :switch-cols="8"
            tooltip="Draw all snapshots"
            @update:modelValue="updateDrawAllSnapshots"
          />
          <pv-switch
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraAutoExposure"
            label="Auto Exposure"
            :label-cols="4"
            tooltip="Enables or Disables camera automatic adjustment for current lighting conditions"
            @update:modelValue="updateCameraAutoExposure"
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
            @update:modelValue="updateCameraExposure"
          />
          <pv-slider
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraBrightness"
            label="Brightness"
            :min="0"
            :max="100"
            :slider-cols="8"
            @update:modelValue="updateCameraBrightness"
          />
          <pv-slider
            v-if="useCameraSettingsStore().currentPipelineSettings.cameraGain >= 0"
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraGain"
            label="Camera Gain"
            tooltip="Controls camera gain, similar to brightness"
            :min="0"
            :max="100"
            :slider-cols="8"
            @update:modelValue="updateCameraGain"
          />
          <pv-slider
            v-if="useCameraSettingsStore().currentPipelineSettings.cameraRedGain !== -1"
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraRedGain"
            label="Red AWB Gain"
            :min="0"
            :max="100"
            :slider-cols="8"
            tooltip="Controls red automatic white balance gain, which affects how the camera captures colors in different conditions"
            @update:modelValue="updateCameraRedGain"
          />
          <pv-slider
            v-if="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain !== -1"
            v-model="useCameraSettingsStore().currentPipelineSettings.cameraBlueGain"
            label="Blue AWB Gain"
            :min="0"
            :max="100"
            :slider-cols="8"
            tooltip="Controls blue automatic white balance gain, which affects how the camera captures colors in different conditions"
            @update:modelValue="updateCameraBlueGain"
          />
        </div>
      </div>
    </pv-card>
    <pv-dialog v-model="showCalibEndDialog" width="500px" persistent>
      <pv-card>
        <div class="pb-2 text-lg font-semibold">Camera Calibration</div>
        <div style="text-align: center">
          <template v-if="calibCanceled">
            <pv-icon color="primary" size="70" :icon="IconCancel" />
            <div>
              Camera calibration has been canceled. The backend
              {{ !isCalibrating ? "is attempting" : "has attempted" }} to cleanly cancel the calibration process.
            </div>
          </template>
          <!-- No result reported yet -->
          <template v-else-if="calibSuccess === undefined">
            <pv-progress :show-percentage="false" color="primary" />
            <div>Camera is being calibrated. This process may take several minutes...</div>
          </template>
          <!-- Got positive result -->
          <template v-else-if="calibSuccess">
            <pv-icon color="#00ff00" size="70" :icon="IconCheck" />
            <div>
              Camera has been successfully calibrated for
              {{
                useCameraSettingsStore().currentCameraSettings.validVideoFormats.map((f) =>
                  getResolutionString(f.resolution)
                )[requestedVideoFormatIndex]
              }}!
            </div>
          </template>
          <template v-else-if="calibEndpointFail">
            <pv-icon color="gray" size="70" :icon="IconHelpCircleOutline" />
            <div>
              Unable to determine if calibration was successful. Refresh this page and manually check if calibration was
              successful.
            </div>
          </template>
          <template v-else>
            <pv-icon color="red" size="70" :icon="IconClose" />
            <div>
              Camera calibration failed! Make sure that the photos are taken such that the rainbow grid circles align
              with the corners of the chessboard, and try again. More information is available in the program logs.
            </div>
          </template>
        </div>
        <div class="flex items-center justify-end pt-0">
          <pv-button v-if="!isCalibrating" variant="text" @click="showCalibEndDialog = false">OK</pv-button>
        </div>
      </pv-card>
    </pv-dialog>
    <pv-dialog v-model="showCalDialog" width="80em">
      <CameraCalibrationInfoCard v-if="selectedVideoFormat" :video-format="selectedVideoFormat" />
    </pv-dialog>
  </div>
</template>

<style scoped>
th {
  text-align: center !important;
  padding: 0 8px !important;
}

.pv-table {
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
    background-color: var(--color-pv-accent);
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
