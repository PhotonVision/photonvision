<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, onBeforeUnmount, onMounted } from "vue";
import PvRangeSlider from "@/components/common/pv-range-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { useStateStore } from "@/stores/StateStore";
import { ColorPicker, type HSV } from "@/lib/ColorPicker";
import { useDisplay } from "vuetify";
import { useTheme } from "vuetify";

const theme = useTheme();

const averageHue = computed<number>(() => {
  const isHueInverted = useCameraSettingsStore().currentPipelineSettings.hueInverted;
  let val = Object.values(useCameraSettingsStore().currentPipelineSettings.hsvHue).reduce((a, b) => a + b, 0);

  if (isHueInverted) val += 180;
  if (val > 360) val -= 360;

  return val;
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

let selectedEventMode: 0 | 1 | 2 | 3 = 0;
const handleStreamClick = (event: MouseEvent) => {
  if (!useStateStore().colorPickingMode || selectedEventMode === 0) return;

  const cameraStream = document.getElementById("input-camera-stream");
  if (cameraStream === null) return;

  const canvas = document.createElement("canvas");
  canvas.width = cameraStream.clientWidth;
  canvas.height = cameraStream.clientHeight;

  // Get the (x, y) position of the click with (0, 0) in the top left corner
  const rect = cameraStream.getBoundingClientRect();
  const x = Math.round(((event.clientX - rect.left) / rect.width) * cameraStream.clientWidth);
  const y = Math.round(((event.clientY - rect.top) / rect.height) * cameraStream.clientHeight);

  const context = canvas.getContext("2d");
  if (context === null) return;

  context.drawImage(cameraStream as CanvasImageSource, 0, 0, cameraStream.clientWidth, cameraStream.clientHeight);
  const colorPicker = new ColorPicker(context.getImageData(x, y, 1, 1).data);

  // Calculate HSV values based on the mode
  let selectedHSVData: [HSV, HSV] = [
    [0, 0, 0],
    [0, 0, 0]
  ];
  if (selectedEventMode === 1) {
    selectedHSVData = colorPicker.selectedColorRange();
  } else {
    const currentHue = Object.values(useCameraSettingsStore().currentPipelineSettings.hsvHue);
    const currentSaturation = Object.values(useCameraSettingsStore().currentPipelineSettings.hsvSaturation);
    const currentValue = Object.values(useCameraSettingsStore().currentPipelineSettings.hsvValue);

    const currentData: [HSV, HSV] = [
      [currentHue[0], currentSaturation[0], currentValue[0]],
      [currentHue[1], currentSaturation[1], currentValue[1]]
    ];

    if (selectedEventMode === 2) {
      selectedHSVData = colorPicker.expandColorRange(currentData);
    } else if (selectedEventMode === 3) {
      selectedHSVData = colorPicker.shrinkColorRange(currentData);
    }
  }

  // Update the store and backend with the new HSV values
  useCameraSettingsStore().changeCurrentPipelineSetting(
    {
      hsvHue: [selectedHSVData[0][0], selectedHSVData[1][0]],
      hsvSaturation: [selectedHSVData[0][1], selectedHSVData[1][1]],
      hsvValue: [selectedHSVData[0][2], selectedHSVData[1][2]]
    },
    true
  );

  disableColorPicking();
};

// Put some default values in case color picking was enabled before the enableColorPicking method is called
let inputShowing = true;
let outputShowing = false;
const enableColorPicking = (mode: 1 | 2 | 3) => {
  useStateStore().colorPickingMode = true;
  inputShowing = useCameraSettingsStore().currentPipelineSettings.inputShouldShow;
  outputShowing = useCameraSettingsStore().currentPipelineSettings.outputShouldShow;
  useCameraSettingsStore().changeCurrentPipelineSetting(
    { outputShouldDraw: false, inputShouldShow: true, outputShouldShow: false },
    true
  );
  selectedEventMode = mode;
};
const disableColorPicking = () => {
  useStateStore().colorPickingMode = false;
  useCameraSettingsStore().changeCurrentPipelineSetting(
    { outputShouldDraw: true, inputShouldShow: inputShowing, outputShouldShow: outputShowing },
    true
  );
  selectedEventMode = 0;
};

onMounted(() => {
  const cameraStream = document.getElementById("input-camera-stream");
  if (cameraStream === null) return;

  cameraStream.addEventListener("click", handleStreamClick);
});
onBeforeUnmount(() => {
  const cameraStream = document.getElementById("input-camera-stream");
  if (cameraStream === null) return;

  cameraStream.removeEventListener("click", handleStreamClick);
});
const { mdAndDown } = useDisplay();

const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8
);
</script>

<template>
  <div class="threshold-modifiers" :style="{ '--averageHue': averageHue }">
    <pv-range-slider
      id="hue-slider"
      v-model="hsvHue"
      :class="useCameraSettingsStore().currentPipelineSettings.hueInverted ? 'inverted-slider' : 'normal-slider'"
      label="Hue"
      tooltip="Describes color"
      :min="0"
      :max="180"
      :slider-cols="interactiveCols"
      :inverted="useCameraSettingsStore().currentPipelineSettings.hueInverted"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ hsvHue: value }, false)"
    />
    <pv-range-slider
      id="sat-slider"
      v-model="hsvSaturation"
      class="normal-slider"
      label="Saturation"
      tooltip="Describes colorfulness; the smaller this value the 'whiter' the color becomes"
      :min="0"
      :max="255"
      :slider-cols="interactiveCols"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ hsvSaturation: value }, false)
      "
    />
    <pv-range-slider
      id="value-slider"
      v-model="hsvValue"
      class="normal-slider"
      label="Value"
      tooltip="Describes lightness; the smaller this value the 'blacker' the color becomes"
      :min="0"
      :max="255"
      :slider-cols="interactiveCols"
      @update:modelValue="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ hsvValue: value }, false)"
    />
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.hueInverted"
      label="Invert Hue"
      :switch-cols="interactiveCols"
      tooltip="Selects the hue range outside of the hue slider bounds instead of inside"
      @update:modelValue="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ hueInverted: value }, false)
      "
    />
    <div>
      <div class="text-white pt-3">Color Picker</div>
      <div class="d-flex pt-3">
        <template v-if="!useStateStore().colorPickingMode">
          <v-col cols="4" class="pl-0 pr-2">
            <v-btn
              size="small"
              block
              color="primary"
              class="text-black"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="enableColorPicking(useCameraSettingsStore().currentPipelineSettings.hueInverted ? 2 : 3)"
            >
              <v-icon start size="large"> mdi-minus </v-icon>
              Shrink Range
            </v-btn>
          </v-col>
          <v-col cols="4" class="pl-0 pr-0">
            <v-btn
              color="primary"
              class="text-black"
              size="small"
              block
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="enableColorPicking(1)"
            >
              <v-icon start size="large"> mdi-plus-minus </v-icon>
              {{ useCameraSettingsStore().currentPipelineSettings.hueInverted ? "Exclude" : "Set to" }} Average
            </v-btn>
          </v-col>
          <v-col cols="4" class="pl-2 pr-0">
            <v-btn
              size="small"
              block
              color="primary"
              class="text-black"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="enableColorPicking(useCameraSettingsStore().currentPipelineSettings.hueInverted ? 3 : 2)"
            >
              <v-icon start size="large"> mdi-plus </v-icon>
              Expand Range
            </v-btn>
          </v-col>
        </template>
        <template v-else>
          <v-card-text class="pa-0 pt-3 pb-3">
            <v-btn
              block
              color="primary"
              class="text-black"
              size="small"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="disableColorPicking"
            >
              Cancel
            </v-btn>
          </v-card-text>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped lang="css">
.threshold-modifiers {
  --averageHue: 0;
}
#hue-slider:deep(.v-slider__container) {
  background: linear-gradient(to right, #f00 0%, #ff0 17%, #0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100%);
  border-radius: 10px;
  /* prettier-ignore */
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
}
#sat-slider:deep(.v-slider__container) {
  background: linear-gradient(to right, #fff 0%, hsl(var(--averageHue), 100%, 50%) 100%);
  border-radius: 10px;
  /* prettier-ignore */
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
}
#value-slider:deep(.v-slider__container) {
  background: linear-gradient(to right, #000 0%, hsl(var(--averageHue), 100%, 50%) 100%);
  border-radius: 10px;
  /* prettier-ignore */
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
}
:deep(.v-slider__thumb) {
  outline: black solid thin;
}
.normal-slider:deep(.v-slider__track-fill) {
  outline: black solid thin;
}

.inverted-slider:deep(.v-slider__track-background) {
  outline: black solid thin;
}
</style>
