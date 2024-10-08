<script setup lang="ts">
import { computed, ref } from "vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { useDisplay } from "vuetify";
import PvRangeNumberSlider from "@/components/common/pv-range-number-slider.vue";
import PvEyedropper from "@/components/common/pv-eyedropper.vue";
import { ColorPicker, type HSV, type RGBA } from "@/lib/ColorPicker";
import type { RGBColor } from "@/types/Components";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import { CameraConfig } from "@/types/SettingTypes";
import {
  ColoredShapePipelineSettings,
  ReflectivePipelineSettings
} from "@/types/PipelineTypes";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
  pipelineIndex: number
}>();

const targetPipelineSettings = computed<ColoredShapePipelineSettings | ReflectivePipelineSettings>(() => props.cameraSettings.pipelineSettings.find((v) => v.pipelineIndex === props.pipelineIndex) as ColoredShapePipelineSettings | ReflectivePipelineSettings);

const averageHue = computed<number>(() => {
  const { hueInverted, hsvHue } = targetPipelineSettings.value;
  let val = Object.values(hsvHue).reduce((total, hue) => total + hue, 0);

  if (hueInverted) {
    val += 180;
  }

  return val % 360;
});

const hsvHue = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.hsvHue) as [number, number]);
const hsvSaturation = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.hsvSaturation) as [number, number]);
const hsvValue = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.hsvValue) as [number, number]);

enum ColorPickingModes {
  // eslint-disable-next-line no-unused-vars
  Undefined = -1,
  // eslint-disable-next-line no-unused-vars
  ShrinkRangeAroundVal = 0,
  // eslint-disable-next-line no-unused-vars
  ExpandRangeAroundVal = 1,
  // eslint-disable-next-line no-unused-vars
  AroundVal = 2
}

const colorPickerOpen = ref<boolean>(false);
const colorPickingMode = ref<ColorPickingModes>(ColorPickingModes.Undefined);
let inputShowing = true;
let outputShowing = false;
const startColorPicking = () => {
  clientStore.colorPickingFromCameraStream = true;
  inputShowing = targetPipelineSettings.value.inputShouldShow;
  outputShowing = targetPipelineSettings.value.outputShouldShow;
  serverStore.updatePipelineSettings(props.cameraSettings.cameraIndex, props.pipelineIndex,
    { outputShouldDraw: false, inputShouldShow: true, outputShouldShow: false },
    true, true
  );
};
const stopColorPicking = () => {
  clientStore.colorPickingFromCameraStream = false;
  serverStore.updatePipelineSettings(props.cameraSettings.cameraIndex, props.pipelineIndex,
    { outputShouldDraw: true, inputShouldShow: inputShowing, outputShouldShow: outputShowing },
    true, true
  );
};
const handleColorPick = (
  selectedColor: RGBColor,
  colorPickingMode: Exclude<ColorPickingModes, ColorPickingModes.Undefined>
) => {
  stopColorPicking();

  // Convert RGB to RGBA
  const rgba = Object.values(selectedColor);
  rgba[3] = 0;

  const pickerManager = new ColorPicker(rgba as RGBA);

  // Calculate HSV values based on the mode
  let selectedHSVData: [HSV, HSV] = [
    [0, 0, 0],
    [0, 0, 0]
  ];
  if (colorPickingMode === ColorPickingModes.AroundVal) {
    selectedHSVData = pickerManager.selectedColorRange();
  } else {
    const currentHue = Object.values(targetPipelineSettings.value.hsvHue);
    const currentSaturation = Object.values(targetPipelineSettings.value.hsvSaturation);
    const currentValue = Object.values(targetPipelineSettings.value.hsvValue);

    const currentData: [HSV, HSV] = [
      [currentHue[0], currentSaturation[0], currentValue[0]],
      [currentHue[1], currentSaturation[1], currentValue[1]]
    ];

    if (colorPickingMode === ColorPickingModes.ExpandRangeAroundVal) {
      selectedHSVData = pickerManager.expandColorRange(currentData);
    } else if (colorPickingMode === ColorPickingModes.ShrinkRangeAroundVal) {
      selectedHSVData = pickerManager.shrinkColorRange(currentData);
    }
  }

  serverStore.updatePipelineSettings(props.cameraSettings.cameraIndex, props.pipelineIndex,
    {
      hsvHue: [selectedHSVData[0][0], selectedHSVData[1][0]],
      hsvSaturation: [selectedHSVData[0][1], selectedHSVData[1][1]],
      hsvValue: [selectedHSVData[0][2], selectedHSVData[1][2]]
    },
    true, true
  );
};

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(() => mdAndDown.value && (!clientStore.sidebarFolded || serverStore.isDriverMode) ? 3 : 5);
</script>

<template>
  <div :style="{ '--averageHue': averageHue }">
    <pv-range-number-slider
      class="hsv-hue-slider"
      :flip-direction="targetPipelineSettings.hueInverted"
      label="Hue"
      :label-cols="labelCols"
      :max="180"
      :min="0"
      :model-value="hsvHue"
      :step="1"
      tooltip="Describes color"
      :track-size="8"
      @update:model-value="(value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { hsvHue: value }, true, true)"
    />
    <pv-range-number-slider
      class="hsv-sat-slider"
      label="Saturation"
      :label-cols="labelCols"
      :max="255"
      :min="0"
      :model-value="hsvSaturation"
      :step="1"
      tooltip="Describes colorfulness; the smaller this value the 'whiter' the color becomes"
      :track-size="8"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { hsvSaturation: value }, true, true)
      "
    />
    <pv-range-number-slider
      class="hsv-val-slider"
      label="Value"
      :label-cols="labelCols"
      :max="255"
      :min="0"
      :model-value="hsvValue"
      :step="1"
      tooltip="Describes lightness; the smaller this value the 'blacker' the color becomes"
      :track-size="8"
      @update:model-value="(value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { hsvValue: value }, true, true)"
    />
    <pv-switch
      label="Invert Hue"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.hueInverted"
      tooltip="Selects the hue range outside of the hue slider bounds instead of inside"
      @update:model-value="
        (value) => {
          serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { hueInverted: value }, true, true);
          colorPickingMode = ColorPickingModes.Undefined;
        }
      "
    />
    <v-divider class="mt-3 mb-3" />
    <v-row class="flex-nowrap" no-gutters>
      <v-col cols="11">
        <v-btn-toggle
          v-model="colorPickingMode"
          base-color="surface-variant"
          class="w-100"
          :disabled="clientStore.colorPickingFromCameraStream"
          divided
          mandatory
        >
          <v-btn append-icon="mdi-plus" class="w-33" text="Shrink Range" />
          <v-btn append-icon="mdi-plus" class="w-33" text="Expand Range" />
          <v-btn
            append-icon="mdi-plus-minus"
            class="w-33"
            :text="`${targetPipelineSettings.hueInverted ? 'Exclude' : 'Set to'} Average`"
          />
        </v-btn-toggle>
      </v-col>
      <v-col class="flex align-content-center pl-0 pl-sm-2 pl-md-4">
        <pv-eyedropper
          v-model="colorPickerOpen"
          :disabled="colorPickingMode === ColorPickingModes.Undefined"
          img-id="input-camera-stream"
          @update:canceled="stopColorPicking"
          @update:color-selected="
            (color) =>
              handleColorPick(color, colorPickingMode as Exclude<ColorPickingModes, ColorPickingModes.Undefined>)
          "
          @update:opened="startColorPicking"
        />
      </v-col>
    </v-row>
    <v-alert
      v-show="colorPickerOpen"
      class="mt-3"
      density="compact"
      rounded
      text="A known bug causes viewfinder color to not update unless mouse is moved again. Don't worry, clicking will select the right color."
      type="warning"
    />
  </div>
</template>

<style scoped>
.hsv-hue-slider >>> .v-slider .v-slider-track .v-slider-track__background {
  background: linear-gradient(
    to right,
    #f00 0%,
    #ff0 16.66%,
    #0f0 33.33%,
    #0ff 50%,
    #00f 66.66%,
    #f0f 83.33%,
    #f00 100%
  ) !important;
}
.hsv-sat-slider >>> .v-slider .v-slider-track .v-slider-track__background {
  background: linear-gradient(to right, #fff 0%, hsl(var(--averageHue), 100%, 50%) 100%);
}
.hsv-val-slider >>> .v-slider .v-slider-track .v-slider-track__background {
  background: linear-gradient(to right, #000 0%, hsl(var(--averageHue), 100%, 50%) 100%);
}
:is(.hsv-hue-slider, .hsv-sat-slider, .hsv-val-slider) >>> .v-slider .v-slider-track .v-slider-track__fill {
  height: 12% !important;
}
</style>
