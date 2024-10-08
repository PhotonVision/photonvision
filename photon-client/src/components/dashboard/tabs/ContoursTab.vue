<script setup lang="ts">
import {
  ColoredShapePipelineSettings,
  PipelineType, ReflectivePipelineSettings
} from "@/types/PipelineTypes";
import { computed } from "vue";
import { useDisplay } from "vuetify";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvRangeNumberSlider from "@/components/common/pv-range-number-slider.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import { CameraConfig } from "@/types/SettingTypes";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
  pipelineIndex: number
}>();

const targetPipelineSettings = computed<ColoredShapePipelineSettings | ReflectivePipelineSettings>(() => props.cameraSettings.pipelineSettings.find((v) => v.pipelineIndex === props.pipelineIndex) as ColoredShapePipelineSettings | ReflectivePipelineSettings);

const contourArea = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.contourArea) as [number, number]);
const contourRatio = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.contourRatio) as [number, number]);
const contourFullness = computed<[number, number]>(() => Object.values(targetPipelineSettings.value.contourFullness) as [number, number]);
const contourPerimeter = computed<[number, number]>(() => Object.values((targetPipelineSettings.value as ColoredShapePipelineSettings).contourPerimeter) as [number, number]);
const contourRadius = computed<[number, number]>(() => Object.values((targetPipelineSettings.value as ColoredShapePipelineSettings).contourRadius) as [number, number]);

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(() => mdAndDown.value && (!clientStore.sidebarFolded || serverStore.isDriverMode) ? 3 : 5);
</script>

<template>
  <div>
    <pv-range-number-slider
      label="Area"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="contourArea"
      :step="0.01"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourArea: value }, true, true)
      "
    />
    <pv-range-number-slider
      v-if="targetPipelineSettings.pipelineType !== PipelineType.ColoredShape"
      label="Ratio (W/H)"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="contourRatio"
      :step="0.1"
      tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourRatio: value }, true, true)
      "
    />
    <pv-dropdown
      :items="['Portrait', 'Landscape'].map((v, i) => ({ name: v, value: i }))"
      label="Target Orientation"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.contourTargetOrientation"
      tooltip="Used to determine how to calculate target landmarks, as well as aspect ratio"
      @update:model-value="
        (value: number) =>
          serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourTargetOrientation: value }, true, true)
      "
    />
    <pv-range-number-slider
      v-if="targetPipelineSettings.pipelineType === PipelineType.ColoredShape"
      label="Fullness"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="contourFullness"
      :step="1"
      tooltip="Min and max ratio between a contour's area and its bounding rectangle"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourFullness: value }, true, true)
      "
    />
    <pv-range-number-slider
      v-if="targetPipelineSettings.pipelineType === PipelineType.ColoredShape"
      label="Perimeter"
      :label-cols="labelCols"
      :max="4000"
      :min="0"
      :model-value="contourPerimeter"
      :step="1"
      tooltip="Min and max perimeter of the shape, in pixels"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourPerimeter: value }, true, true)
      "
    />
    <pv-number-slider
      label="Speckle Rejection"
      :max="100"
      :min="0"
      :model-value="targetPipelineSettings.contourSpecklePercentage"
      :slider-cols="labelCols"
      :step="1"
      tooltip="Rejects contours whose average area is less than the given percentage of the average area of all the other contours"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourSpecklePercentage: value }, true, true)
      "
    />
    <div v-if="targetPipelineSettings.pipelineType === PipelineType.Reflective">
      <pv-number-slider
        label="X Filter Tightness"
        :label-cols="labelCols"
        :max="4"
        :min="0.1"
        :model-value="targetPipelineSettings.contourFilterRangeX"
        :step="0.1"
        tooltip="Rejects contours whose center X is further than X standard deviations left/right of the mean X location"
        @update:model-value="
          (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourFilterRangeX: value }, true, true)
        "
      />
      <pv-number-slider
        label="Y Filter Tightness"
        :label-cols="labelCols"
        :max="4"
        :min="0.1"
        :model-value="targetPipelineSettings.contourFilterRangeY"
        :step="0.1"
        tooltip="Rejects contours whose center Y is further than X standard deviations above/below the mean Y location"
        @update:model-value="
          (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourFilterRangeY: value }, true, true)
        "
      />
      <pv-dropdown
        :items="['Single', 'Dual', 'Two or More'].map((v, i) => ({ name: v, value: i }))"
        label="Target Grouping"
        :label-cols="labelCols"
        :model-value="targetPipelineSettings.contourGroupingMode"
        tooltip="Whether or not every two targets are paired with each other (good for e.g. 2019 targets)"
        @update:model-value="
          (value: number) =>
            serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourGroupingMode: value }, true, true)
        "
      />
      <pv-dropdown
        :disabled="targetPipelineSettings.contourGroupingMode === 0"
        :items="['None', 'Up', 'Down', 'Left', 'Right'].map((v, i) => ({ name: v, value: i }))"
        label="Target Intersection"
        :label-cols="labelCols"
        :model-value="targetPipelineSettings.contourIntersection"
        tooltip="If target grouping is in dual mode it will use this dropdown to decide how targets are grouped with adjacent targets"
        @update:model-value="
          (value: number) =>
            serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourIntersection: value }, true, true)
        "
      />
    </div>
    <div v-else-if="targetPipelineSettings.pipelineType === PipelineType.ColoredShape">
      <v-divider class="mt-3 mb-3" />
      <pv-dropdown
        :items="['Circle', 'Polygon', 'Triangle', 'Quadrilateral'].map((v, i) => ({ name: v, value: i }))"
        label="Target Shape"
        :label-cols="labelCols"
        :model-value="targetPipelineSettings.contourShape"
        tooltip="The shape of targets to look for"
        @update:model-value="
          (value: number) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourShape: value }, true, true)
        "
      />
      <pv-number-slider
        :disabled="targetPipelineSettings.contourShape < 1"
        label="Shape Simplification"
        :label-cols="labelCols"
        :max="100"
        :min="0"
        :model-value="targetPipelineSettings.accuracyPercentage"
        tooltip="How much we should simply the input contour before checking how many sides it has"
        @update:model-value="
          (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { accuracyPercentage: value }, true, true)
        "
      />
      <pv-number-slider
        :disabled="targetPipelineSettings.contourShape !== 0"
        label="Circle match distance"
        :label-cols="labelCols"
        :max="100"
        :min="1"
        :model-value="targetPipelineSettings.circleDetectThreshold"
        tooltip="How close the centroid of a contour must be to the center of a circle in order for them to be matched"
        @update:model-value="
          (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { circleDetectThreshold: value }, true, true)
        "
      />
      <pv-range-number-slider
        :disabled="targetPipelineSettings.contourShape !== 0"
        label="Radius"
        :label-cols="labelCols"
        :max="100"
        :min="0"
        :model-value="contourRadius"
        :step="1"
        @update:model-value="
          (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourRadius: value }, true, true)
        "
      />
      <pv-number-slider
        :disabled="targetPipelineSettings.contourShape !== 0"
        label="Max Canny Threshold"
        :label-cols="labelCols"
        :max="100"
        :min="1"
        :model-value="targetPipelineSettings.maxCannyThresh"
        :step="1"
        @update:model-value="
          (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { maxCannyThresh: value }, true, true)
        "
      />
      <pv-number-slider
        :disabled="targetPipelineSettings.contourShape !== 0"
        label="Circle Accuracy"
        :label-cols="labelCols"
        :max="100"
        :min="1"
        :model-value="targetPipelineSettings.circleAccuracy"
        :step="1"
        @update:model-value="
          (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { circleAccuracy: value }, true, true)
        "
      />
      <v-divider class="mt-3 mb-3" />
    </div>
    <pv-dropdown
      :items="
        ['Largest', 'Smallest', 'Highest', 'Lowest', 'Rightmost', 'Leftmost', 'Centermost'].map((v, i) => ({
          name: v,
          value: i
        }))
      "
      label="Target Sort"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.contourSortMode"
      tooltip="Chooses the sorting mode used to determine the 'best' targets to provide to user code"
      @update:model-value="
        (value: number) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourSortMode: value }, true, true)
      "
    />
  </div>
</template>
