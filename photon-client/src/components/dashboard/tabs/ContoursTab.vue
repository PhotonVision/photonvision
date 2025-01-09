<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ActivePipelineSettings, PipelineType } from "@/types/PipelineTypes";
import PvRangeSlider from "@/components/common/pv-range-slider.vue";
import PvSelect from "@/components/common/pv-select.vue";
import PvSlider from "@/components/common/pv-slider.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ActivePipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings
);

// TODO fix pv-range-slider so that store access doesn't need to be deferred
const contourArea = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.contourArea) as [number, number],
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.contourArea = v)
});
const contourRatio = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.contourRatio) as [number, number],
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.contourRatio = v)
});
const contourFullness = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.contourFullness) as [number, number],
  set: (v) => (useCameraSettingsStore().currentPipelineSettings.contourFullness = v)
});
const contourPerimeter = computed<[number, number]>({
  get: () =>
    currentPipelineSettings.value.pipelineType === PipelineType.ColoredShape
      ? (Object.values(currentPipelineSettings.value.contourPerimeter) as [number, number])
      : ([0, 0] as [number, number]),
  set: (v) => {
    if (currentPipelineSettings.value.pipelineType === PipelineType.ColoredShape) {
      currentPipelineSettings.value.contourPerimeter = v;
    }
  }
});
const contourRadius = computed<[number, number]>({
  get: () =>
    currentPipelineSettings.value.pipelineType === PipelineType.ColoredShape
      ? (Object.values(currentPipelineSettings.value.contourRadius) as [number, number])
      : ([0, 0] as [number, number]),
  set: (v) => {
    if (currentPipelineSettings.value.pipelineType === PipelineType.ColoredShape) {
      currentPipelineSettings.value.contourRadius = v;
    }
  }
});

const interactiveCols = computed(() =>
  (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) &&
  (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)
    ? 8
    : 7
);
</script>

<template>
  <div>
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOrientation"
      label="Target Orientation"
      tooltip="Used to determine how to calculate target landmarks, as well as aspect ratio"
      :items="['Portrait', 'Landscape']"
      :select-cols="interactiveCols"
      @input="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOrientation: value }, false)
      "
    />
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.contourSortMode"
      label="Target Sort"
      tooltip="Chooses the sorting mode used to determine the 'best' targets to provide to user code"
      :select-cols="interactiveCols"
      :items="['Largest', 'Smallest', 'Highest', 'Lowest', 'Rightmost', 'Leftmost', 'Centermost']"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourSortMode: value }, false)"
    />
    <pv-range-slider
      v-model="contourArea"
      label="Area"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      :step="0.01"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourArea: value }, false)"
    />
    <pv-range-slider
      v-if="useCameraSettingsStore().currentPipelineType !== PipelineType.ColoredShape"
      v-model="contourRatio"
      label="Ratio (W/H)"
      tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      :step="0.1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourRatio: value }, false)"
    />
    <pv-range-slider
      v-if="useCameraSettingsStore().currentPipelineType === PipelineType.ColoredShape"
      v-model="contourFullness"
      label="Fullness"
      tooltip="Min and max ratio between a contour's area and its bounding rectangle"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourFullness: value }, false)"
    />
    <pv-range-slider
      v-if="currentPipelineSettings.pipelineType === PipelineType.ColoredShape"
      v-model="contourPerimeter"
      label="Perimeter"
      tooltip="Min and max perimeter of the shape, in pixels"
      :min="0"
      :max="4000"
      :slider-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourPerimeter: value }, false)"
    />
    <pv-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.contourSpecklePercentage"
      label="Speckle Rejection"
      tooltip="Rejects contours whose average area is less than the given percentage of the average area of all the other contours"
      :min="0"
      :max="100"
      :slider-cols="interactiveCols"
      @input="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourSpecklePercentage: value }, false)
      "
    />
    <template v-if="currentPipelineSettings.pipelineType === PipelineType.Reflective">
      <pv-slider
        v-model="currentPipelineSettings.contourFilterRangeX"
        label="X Filter Tightness"
        tooltip="Rejects contours whose center X is further than X standard deviations left/right of the mean X location"
        :min="0.1"
        :max="4"
        :step="0.1"
        :slider-cols="interactiveCols"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourFilterRangeX: value }, false)"
      />
      <pv-slider
        v-model="currentPipelineSettings.contourFilterRangeY"
        label="Y Filter Tightness"
        tooltip="Rejects contours whose center Y is further than X standard deviations above/below the mean Y location"
        :min="0.1"
        :max="4"
        :step="0.1"
        :slider-cols="interactiveCols"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourFilterRangeY: value }, false)"
      />
      <pv-select
        v-model="useCameraSettingsStore().currentPipelineSettings.contourGroupingMode"
        label="Target Grouping"
        tooltip="Whether or not every two targets are paired with each other (good for e.g. 2019 targets)"
        :select-cols="interactiveCols"
        :items="['Single', 'Dual', 'Two or More']"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourGroupingMode: value }, false)"
      />
      <pv-select
        v-model="useCameraSettingsStore().currentPipelineSettings.contourIntersection"
        label="Target Intersection"
        tooltip="If target grouping is in dual mode it will use this dropdown to decide how targets are grouped with adjacent targets"
        :select-cols="interactiveCols"
        :items="['None', 'Up', 'Down', 'Left', 'Right']"
        :disabled="useCameraSettingsStore().currentPipelineSettings.contourGroupingMode === 0"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourIntersection: value }, false)"
      />
    </template>
    <template v-else-if="currentPipelineSettings.pipelineType === PipelineType.ColoredShape">
      <pv-select
        v-model="currentPipelineSettings.contourShape"
        label="Target Shape"
        tooltip="The shape of targets to look for"
        :select-cols="interactiveCols"
        :items="['Circle', 'Polygon', 'Triangle', 'Quadrilateral']"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourShape: value }, false)"
      />
      <pv-slider
        v-if="currentPipelineSettings.contourShape >= 1"
        v-model="currentPipelineSettings.accuracyPercentage"
        :disabled="currentPipelineSettings.contourShape < 1"
        label="Shape Simplification"
        tooltip="How much we should simply the input contour before checking how many sides it has"
        :min="0"
        :max="100"
        :slider-cols="interactiveCols"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ accuracyPercentage: value }, false)"
      />
      <pv-slider
        v-if="currentPipelineSettings.contourShape === 0"
        v-model="currentPipelineSettings.circleDetectThreshold"
        :disabled="currentPipelineSettings.contourShape !== 0"
        label="Circle match distance"
        tooltip="How close the centroid of a contour must be to the center of a circle in order for them to be matched"
        :min="1"
        :max="100"
        :slider-cols="interactiveCols"
        @input="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ circleDetectThreshold: value }, false)
        "
      />
      <pv-slider
        v-if="currentPipelineSettings.contourShape === 0"
        v-model="currentPipelineSettings.maxCannyThresh"
        :disabled="currentPipelineSettings.contourShape !== 0"
        label="Max Canny Threshold"
        :min="1"
        :max="100"
        :slider-cols="interactiveCols"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ maxCannyThresh: value }, false)"
      />
      <pv-slider
        v-if="currentPipelineSettings.contourShape === 0"
        v-model="currentPipelineSettings.circleAccuracy"
        :disabled="currentPipelineSettings.contourShape !== 0"
        label="Circle Accuracy"
        :min="1"
        :max="100"
        :slider-cols="interactiveCols"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ circleAccuracy: value }, false)"
      />
      <pv-range-slider
        v-if="currentPipelineSettings.contourShape === 0"
        v-model="contourRadius"
        :disabled="currentPipelineSettings.contourShape !== 0"
        label="Radius"
        :min="0"
        :max="100"
        :slider-cols="interactiveCols"
        @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourRadius: value }, false)"
      />
    </template>
  </div>
</template>
