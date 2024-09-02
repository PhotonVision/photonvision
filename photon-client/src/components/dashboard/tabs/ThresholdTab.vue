<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, ref } from "vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { useStateStore } from "@/stores/StateStore";
import { useDisplay } from "vuetify";
import PvRangeNumberSlider from "@/components/common/pv-range-number-slider.vue";
import PvEyedropper from "@/components/common/pv-eyedropper.vue";
import { ColorPicker, type HSV, type RGBA } from "@/lib/ColorPicker";
import type { RGBColor } from "@/types/Components";

const averageHue = computed<number>(() => {
  const { hueInverted, hsvHue } = useCameraSettingsStore().currentPipelineSettings;
  let val = Object.values(hsvHue).reduce((total, hue) => total + hue, 0);

  if (hueInverted) {
    val += 180;
  }

  return val % 360;
});

// TODO fix pv-range-slider so that store access doesn't need to be deferred
const hsvHue = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.hsvHue) as [number, number],
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.hsvHue = v)
});
const hsvSaturation = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.hsvSaturation) as [number, number],
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.hsvSaturation = v)
});
const hsvValue = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.hsvValue) as [number, number],
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.hsvValue = v)
});

enum ColorPickingModes {
  Undefined = -1,
  ShrinkRangeAroundVal = 0,
  ExpandRangeAroundVal = 1,
  AroundVal = 2
}

const colorPickerOpen = ref<boolean>(false);
const colorPickingMode = ref<ColorPickingModes>(ColorPickingModes.Undefined);
let inputShowing = true;
let outputShowing = false;
const startColorPicking = () => {
  useStateStore().colorPickingMode = true;
  inputShowing = useCameraSettingsStore().currentPipelineSettings.inputShouldShow;
  outputShowing = useCameraSettingsStore().currentPipelineSettings.outputShouldShow;
  useCameraSettingsStore().changeCurrentPipelineSetting(
    { outputShouldDraw: false, inputShouldShow: true, outputShouldShow: false },
    true
  );
};
const stopColorPicking = () => {
  useStateStore().colorPickingMode = false;
  useCameraSettingsStore().changeCurrentPipelineSetting(
    { outputShouldDraw: true, inputShouldShow: inputShowing, outputShouldShow: outputShowing },
    true
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
    const currentHue = Object.values(useCameraSettingsStore().currentPipelineSettings.hsvHue);
    const currentSaturation = Object.values(useCameraSettingsStore().currentPipelineSettings.hsvSaturation);
    const currentValue = Object.values(useCameraSettingsStore().currentPipelineSettings.hsvValue);

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

  useCameraSettingsStore().changeCurrentPipelineSetting(
    {
      hsvHue: [selectedHSVData[0][0], selectedHSVData[1][0]],
      hsvSaturation: [selectedHSVData[0][1], selectedHSVData[1][1]],
      hsvValue: [selectedHSVData[0][2], selectedHSVData[1][2]]
    },
    true
  );
};

const { mdAndDown } = useDisplay();
const labelCols = computed(
  () => 12 - (mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8)
);
</script>

<template>
  <div :style="{ '--averageHue': averageHue }">
    <pv-range-number-slider
      v-model="hsvHue"
      class="hsv-hue-slider"
      :flip-direction="useCameraSettingsStore().currentPipelineSettings.hueInverted"
      label="Hue"
      :label-cols="labelCols"
      :max="180"
      :min="0"
      :step="1"
      tooltip="Describes color"
      :track-size="8"
      @update:model-value="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ hsvHue: value }, false)"
    />
    <pv-range-number-slider
      v-model="hsvSaturation"
      class="hsv-sat-slider"
      label="Saturation"
      :label-cols="labelCols"
      :max="255"
      :min="0"
      :step="1"
      tooltip="Describes colorfulness; the smaller this value the 'whiter' the color becomes"
      :track-size="8"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ hsvSaturation: value }, false)
      "
    />
    <pv-range-number-slider
      v-model="hsvValue"
      class="hsv-val-slider"
      label="Value"
      :label-cols="labelCols"
      :max="255"
      :min="0"
      :step="1"
      tooltip="Describes lightness; the smaller this value the 'blacker' the color becomes"
      :track-size="8"
      @update:model-value="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ hsvValue: value }, false)"
    />
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.hueInverted"
      label="Invert Hue"
      :label-cols="labelCols"
      tooltip="Selects the hue range outside of the hue slider bounds instead of inside"
      @update:model-value="
        (value) => {
          useCameraSettingsStore().changeCurrentPipelineSetting({ hueInverted: value }, false);
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
          :disabled="useStateStore().colorPickingMode"
          divided
          mandatory
        >
          <v-btn append-icon="mdi-plus" class="w-33" text="Shrink Range" />
          <v-btn append-icon="mdi-plus" class="w-33" text="Expand Range" />
          <v-btn
            append-icon="mdi-plus-minus"
            class="w-33"
            :text="`${useCameraSettingsStore().currentPipelineSettings.hueInverted ? 'Exclude' : 'Set to'} Average`"
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
