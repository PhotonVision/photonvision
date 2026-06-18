<script setup lang="ts">
import { computed, ref, watchEffect } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import {
  CalibrationBoardTypes,
  CalibrationPaperTypes,
  CalibrationTagFamilies,
  type VideoFormat
} from "@/types/SettingTypes";
import MonoLogo from "@/assets/images/logoMono.png";
import PvSlider from "@/components/common/pv-slider.vue";
import { useStateStore } from "@/stores/StateStore";
import PvSwitch from "@/components/common/pv-switch.vue";
import PvSelect from "@/components/common/pv-select.vue";
import PvNumberInput from "@/components/common/pv-number-input.vue";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import {
  arucoTagDictionaryFor,
  arucoTagFamilyNameFor,
  getResolutionString,
  paperDimensionsFor,
  resolutionsAreEqual
} from "@/lib/PhotonUtils";
import CameraCalibrationInfoCard from "@/components/cameras/CameraCalibrationInfoCard.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useTheme } from "vuetify";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

const PromptRegular = import("@/assets/fonts/PromptRegular");
const jspdf = import("jspdf").then(async (jspdf) => {
  await import("svg2pdf.js");
  return jspdf;
});
const arucoMarker = import("aruco-marker");

const theme = useTheme();
const MM_PER_INCH = 25.4;

const settingsValid = ref(true);

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
const boardType = ref<CalibrationBoardTypes>(CalibrationBoardTypes.ChArUco);
const useOldPattern = ref(false);
const tagFamily = ref<CalibrationTagFamilies>(CalibrationTagFamilies.Dict_4X4_1000);
const requestedVideoFormatIndex = ref(0);
const paperType = ref<CalibrationPaperTypes>(CalibrationPaperTypes.Letter);
const paperOrientation = ref<"portrait" | "landscape">("portrait");

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
  const { font } = await PromptRegular;
  const doc = new jsPDF({
    unit: "in",
    format: CalibrationPaperTypes[paperType.value],
    orientation: paperOrientation.value
  });

  doc.addFileToVFS("Prompt-Regular.tff", font);
  doc.addFont("Prompt-Regular.tff", "Prompt-Regular", "normal");
  doc.setFont("Prompt-Regular");
  doc.setFontSize(12);

  const paperDimensions = paperDimensionsFor(paperType.value);

  const paperWidth = paperDimensions[paperOrientation.value === "portrait" ? 0 : 1].in.value;
  const paperHeight = paperDimensions[paperOrientation.value === "portrait" ? 1 : 0].in.value;

  const chessboardStartX = (paperWidth - patternWidth.value * squareSizeIn.value) / 2;
  const chessboardStartY = (paperHeight - patternHeight.value * squareSizeIn.value) / 2;

  switch (boardType.value) {
    case CalibrationBoardTypes.Chessboard:
      // This branch is inaccessible
      console.error("Chessboard generation is not supported");
      return;

    case CalibrationBoardTypes.ChArUco:
      const { arucoToSVGString } = await arucoMarker;
      // ChArUco boards place ArUco tags in reading order over a chessboard with a black square in the bottom left
      let markerIndex = 0;
      const squarePadding = (squareSizeIn.value - markerSizeIn.value) / 2;
      for (let squareY = 0; squareY < patternHeight.value; squareY++) {
        for (let squareX = 0; squareX < patternWidth.value; squareX++) {
          const xPos = chessboardStartX + squareX * squareSizeIn.value;
          const yPos = chessboardStartY + squareY * squareSizeIn.value;

          // Draw black squares on the even tiles and ArUco markers on the odd tiles
          // Parity is even in the top left corner unless using the old pattern, which starts in the bottom left corner
          if ((squareY + (useOldPattern.value ? patternHeight.value - 1 : 0)) % 2 === squareX % 2) {
            doc.rect(xPos, yPos, squareSizeIn.value, squareSizeIn.value, "F");
          } else {
            await doc.svg(
              new DOMParser()
                .parseFromString(
                  arucoToSVGString(markerIndex++, undefined, await arucoTagDictionaryFor(tagFamily.value)),
                  "image/svg+xml"
                )
                .getElementsByTagName("svg")[0],
              {
                x: xPos + squarePadding,
                y: yPos + squarePadding,
                width: markerSizeIn.value,
                height: markerSizeIn.value
              }
            );
          }
        }
      }
      doc.text(
        `${patternWidth.value} x ${patternHeight.value} | ${arucoTagFamilyNameFor(tagFamily.value)}\n${squareSize.value}${dimensionUnit.value} squares | ${markerSize.value}${dimensionUnit.value} markers`,
        paperWidth - 1,
        1.0,
        { maxWidth: (paperWidth - 2.0) / 2, align: "right" }
      );

      break;
  }

  // Draw ruler pattern
  const lineStartX = 1.0;
  const lineEndX = paperWidth - lineStartX;
  const lineY = paperHeight - 0.75;

  doc.setLineWidth(0.01);
  doc.line(lineStartX, lineY, lineEndX, lineY);

  for (let tickX = lineStartX; tickX <= lineEndX; tickX++) {
    doc.line(tickX, lineY, tickX, lineY + 0.25);
    doc.text(`${tickX - 1}${tickX - 1 === 0 ? " in" : ""}`, tickX + 0.1, lineY + 0.2);
  }

  for (let tickX = lineStartX; tickX <= lineEndX; tickX += (1 / MM_PER_INCH) * 20) {
    doc.line(tickX, lineY, tickX, lineY - 0.25);
    doc.text(`${Math.round((tickX - 1) * MM_PER_INCH)}${tickX - 1 === 0 ? " mm" : ""}`, tickX + 0.1, lineY - 0.05);
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
</script>

<template>
  <div>
    <v-card class="mb-3 rounded-12" color="surface" dark>
      <v-card-title>Camera Calibration</v-card-title>
      <v-card-text v-if="!isCalibrating" class="pb-0">
        <div class="pb-3">
          <tooltipped-label
            label="Curent Calibrations"
            icon="mdi-information"
            location="top"
            tooltip="Click on a resolution to view detailed calibration information and import/export a calibration."
          />
        </div>
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
          <v-card-subtitle v-if="!isCalibrating" class="pl-0 pb-3 pt-4 opacity-100"
            >Configure New Calibration</v-card-subtitle
          >
          <v-form v-model="settingsValid">
            <pv-select
              v-model="uniqueVideoResolutionIndex"
              label="Resolution"
              :select-cols="8"
              :disabled="isCalibrating"
              tooltip="Resolution to calibrate at (you will have to calibrate every resolution you use 3D mode on)"
              :items="getUniqueVideoResolutionStrings()"
              @update:model-value="(value) => (useStateStore().calibrationData.videoFormatIndex = value)"
            />
            <pv-select
              v-model="boardType"
              label="Board Type"
              tooltip="Calibration board pattern to use"
              :select-cols="8"
              :items="[
                { value: CalibrationBoardTypes.ChArUco, name: 'ChArUco' },
                { value: CalibrationBoardTypes.Chessboard, name: 'Chessboard' }
              ]"
              :disabled="isCalibrating"
            />
            <v-alert
              v-if="boardType !== CalibrationBoardTypes.ChArUco"
              closable
              density="compact"
              variant="tonal"
              color="warning"
              icon="mdi-alert-box"
              text="The usage of chessboards can result in bad calibration results if multiple
              similar images are taken. We strongly recommend that teams use ChArUco boards instead!"
            />
            <pv-select
              v-if="boardType !== CalibrationBoardTypes.ChArUco"
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
              v-if="boardType === CalibrationBoardTypes.ChArUco"
              v-model="tagFamily"
              label="Tag Family"
              tooltip="Dictionary of ArUco markers on the ChArUco board"
              :select-cols="8"
              :items="
                [
                  CalibrationTagFamilies.Dict_4X4_1000,
                  CalibrationTagFamilies.Dict_5X5_1000,
                  CalibrationTagFamilies.Dict_6X6_1000,
                  CalibrationTagFamilies.Dict_7X7_1000
                ].map((family) => ({ value: family, name: arucoTagFamilyNameFor(family) }))
              "
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
              :rules="[(v) => v > 0 || 'Size must be positive']"
              :label-cols="4"
              :step="dimensionStep"
            />
            <pv-number-input
              v-if="boardType === CalibrationBoardTypes.ChArUco"
              v-model="markerSize"
              :label="`Marker Size (${dimensionUnit})`"
              :tooltip="`Size of the tag markers in ${dimensionUnit === 'mm' ? 'millimeters' : 'inches'}; must be smaller than pattern spacing`"
              :disabled="isCalibrating"
              :rules="[(v) => v > 0 || 'Size must be positive']"
              :label-cols="4"
              :step="dimensionStep"
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
              v-if="boardType === CalibrationBoardTypes.ChArUco"
              v-model="useOldPattern"
              label="Old OpenCV Pattern"
              :disabled="isCalibrating"
              tooltip="If enabled, Photon will use the old OpenCV pattern for calibration."
              :label-cols="4"
            />
            <pv-select
              v-model="paperType"
              label="Paper Type"
              tooltip="Size of paper used when exporting a calibration board."
              :items="
                [
                  CalibrationPaperTypes.Letter,
                  CalibrationPaperTypes.Legal,
                  CalibrationPaperTypes.Tabloid,
                  CalibrationPaperTypes.A4,
                  CalibrationPaperTypes.A3,
                  CalibrationPaperTypes.A2
                ].map((paperType) => {
                  const dimensions = paperDimensionsFor(paperType);
                  return {
                    value: paperType,
                    name: `${CalibrationPaperTypes[paperType]} (${dimensions[0].value} ${dimensions[0].unit} x ${dimensions[1].value} ${dimensions[1].unit})`
                  };
                })
              "
              :select-cols="8"
            />
            <pv-select
              v-model="paperOrientation"
              label="Paper Orientation"
              tooltip="Orientation of paper used when exporting a calibration board."
              :items="[
                { value: 'landscape', name: 'Landscape' },
                { value: 'portrait', name: 'Portrait' }
              ]"
              :select-cols="8"
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
        <v-alert
          closable
          density="compact"
          class="mb-5"
          :variant="theme.global.current.value.dark ? 'tonal' : 'elevated'"
          :color="useSettingsStore().general.mrCalWorking ? 'buttonPassive' : 'error'"
          :icon="useSettingsStore().general.mrCalWorking ? 'mdi-check' : 'mdi-close'"
          :text="
            useSettingsStore().general.mrCalWorking
              ? 'Mrcal was successfully loaded and will be used!'
              : 'MrCal failed to load, check journalctl logs for details.'
          "
        />
        <div v-if="isCalibrating" class="d-flex justify-center align-center pb-5">
          <v-chip
            :variant="theme.global.current.value.dark ? 'tonal' : 'elevated'"
            label
            :color="hasEnoughImages ? 'buttonPassive' : 'light-grey'"
          >
            Snapshots: {{ useStateStore().calibrationData.imageCount }} of at least
            {{ minCount }}
          </v-chip>
          <v-spacer />
          <pv-switch
            v-model="bypassVal"
            color="error"
            hide-details
            class="ml-4"
            label="Bypass minimum"
            :label-cols="6"
            :switch-cols="6"
            tooltip="Bypass the minimum recommended amount of snapshots for a calibration. Should only be used for dev work or temporary tests not competitions. Still requires 10 images to calibrate."
          />
        </div>
        <div>
          <v-btn
            color="buttonPassive"
            size="small"
            block
            :variant="theme.global.current.value.dark ? 'outlined' : 'elevated'"
            :disabled="!settingsValid || boardType === CalibrationBoardTypes.Chessboard"
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
          :variant="theme.global.current.value.dark ? 'tonal' : 'elevated'"
        />
        <div class="d-flex pt-5">
          <v-col cols="6" class="pa-0 pr-2">
            <v-btn
              size="small"
              block
              color="buttonActive"
              :variant="theme.global.current.value.dark ? 'outlined' : 'elevated'"
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
              :variant="theme.global.current.value.dark ? 'outlined' : 'elevated'"
              :color="hasEnoughImages ? 'buttonActive' : 'error'"
              :disabled="!isCalibrating || !settingsValid"
              @click="endCalibration"
            >
              <v-icon start class="calib-btn-icon" size="large">
                {{ hasEnoughImages ? "mdi-flag-checkered" : "mdi-flag-off-outline" }}
              </v-icon>
              <span class="calib-btn-label">{{ hasEnoughImages ? "Finish Calibration" : "Cancel Calibration" }}</span>
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
                )[requestedVideoFormatIndex]
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
