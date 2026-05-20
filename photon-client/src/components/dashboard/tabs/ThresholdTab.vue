<script setup lang="ts">
import IconMinus from "~icons/mdi/minus";
import IconPlusMinus from "~icons/mdi/plus-minus";
import IconPlus from "~icons/mdi/plus";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed, onBeforeUnmount, onMounted } from "vue";

import { useStateStore } from "@/stores/StateStore";
import { ColorPicker, type HSV } from "@/lib/ColorPicker";
import { useCustomBreakpoints } from "@/lib/Breakpoints";
import type { WebsocketNumberPair } from "@/types/WebsocketDataTypes";
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

const normalizeNumberPair = (value: WebsocketNumberPair | [number, number]): [number, number] =>
  Array.isArray(value) ? value : [value.first, value.second];

let selectedEventMode: 0 | 1 | 2 | 3 = 0;
const handleStreamClick = (event: MouseEvent) => {
  if (!useStateStore().colorPickingMode || selectedEventMode === 0) return;

  const cameraStream = document.getElementById("input-camera-stream");
  console.log(cameraStream);
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
  console.log(cameraStream);
  if (cameraStream === null) return;
  //
  cameraStream.addEventListener("click", handleStreamClick);
});
onBeforeUnmount(() => {
  const cameraStream = document.getElementById("input-camera-stream");
  if (cameraStream === null) return;

  cameraStream.removeEventListener("click", handleStreamClick);
});
const breakpoints = useCustomBreakpoints();
const mdAndDown = breakpoints.smallerOrEqual("md");

const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8
);
</script>

<template>
  <div class="threshold-modifiers" :style="{ '--averageHue': averageHue }">
    <pv-range-slider
      id="hue-slider"
      class="hue-slider"
      v-model="hsvHue"
      :class="useCameraSettingsStore().currentPipelineSettings.hueInverted ? 'inverted-slider' : 'normal-slider'"
      label="Hue"
      tooltip="Describes color"
      :min="0"
      :max="180"
      :slider-cols="interactiveCols"
      :inverted="useCameraSettingsStore().currentPipelineSettings.hueInverted"
      @update:modelValue="
        (value: WebsocketNumberPair | [number, number]) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ hsvHue: normalizeNumberPair(value) }, false)
      "
    />
    <pv-range-slider
      id="sat-slider"
      v-model="hsvSaturation"
      class="normal-slider sat-slider"
      label="Saturation"
      tooltip="Describes colorfulness; the smaller this value the 'whiter' the color becomes"
      :min="0"
      :max="255"
      :slider-cols="interactiveCols"
      @update:modelValue="
        (value: WebsocketNumberPair | [number, number]) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ hsvSaturation: normalizeNumberPair(value) }, false)
      "
    />
    <pv-range-slider
      id="value-slider"
      v-model="hsvValue"
      class="normal-slider value-slider"
      label="Value"
      tooltip="Describes lightness; the smaller this value the 'blacker' the color becomes"
      :min="0"
      :max="255"
      :slider-cols="interactiveCols"
      @update:modelValue="
        (value: WebsocketNumberPair | [number, number]) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ hsvValue: normalizeNumberPair(value) }, false)
      "
    />
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.hueInverted"
      label="Invert Hue"
      :switch-cols="interactiveCols"
      tooltip="Selects the hue range outside of the hue slider bounds instead of inside"
      @update:modelValue="
        (value: boolean | undefined) =>
          value !== undefined && useCameraSettingsStore().changeCurrentPipelineSetting({ hueInverted: value }, false)
      "
    />
    <div>
      <div class="text-pv-on-surface pt-3">Color Picker</div>
      <div class="flex pt-3">
        <template v-if="!useStateStore().colorPickingMode">
          <div class="w-1/3 pr-2 pl-0">
            <pv-button
              size="sm"
              variant="primary"
              :icon="IconMinus"
              block
              @click="enableColorPicking(useCameraSettingsStore().currentPipelineSettings.hueInverted ? 2 : 3)"
            >
              Shrink Range
            </pv-button>
          </div>
          <div class="w-1/3 pr-0 pl-0">
            <pv-button size="sm" variant="primary" :icon="IconPlusMinus" block @click="enableColorPicking(1)">
              {{ useCameraSettingsStore().currentPipelineSettings.hueInverted ? "Exclude" : "Set to" }} Average
            </pv-button>
          </div>
          <div class="w-1/3 pr-0 pl-2">
            <pv-button
              size="sm"
              variant="primary"
              :icon="IconPlus"
              block
              @click="enableColorPicking(useCameraSettingsStore().currentPipelineSettings.hueInverted ? 3 : 2)"
            >
              Expand Range
            </pv-button>
          </div>
        </template>
        <template v-else>
          <div class="p-0 pt-3 pb-3">
            <pv-button size="sm" variant="primary" block @click="disableColorPicking"> Cancel </pv-button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped lang="css">
.threshold-modifiers {
  --averageHue: 0;
}
.hue-slider:deep(.pv-slider-track) {
  background: linear-gradient(to right, #f00 0%, #ff0 17%, #0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100%);
  border-radius: 10px;
  /* prettier-ignore */
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
}
.sat-slider:deep(.pv-slider-track) {
  background: linear-gradient(to right, #fff 0%, hsl(var(--averageHue), 100%, 50%) 100%);
  border-radius: 10px;
  /* prettier-ignore */
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
}
.value-slider:deep(.pv-slider-track) {
  background: linear-gradient(to right, #000 0%, hsl(var(--averageHue), 100%, 50%) 100%);
  border-radius: 10px;
  /* prettier-ignore */
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
}

.normal-slider:deep(.pv-slider-range) {
  background: transparent;
  outline: black solid thin;
}

.inverted-slider:deep(.pv-slider-range) {
  outline: black solid thin;
}
</style>
