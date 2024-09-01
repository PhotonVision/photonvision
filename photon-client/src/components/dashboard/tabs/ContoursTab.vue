<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ActivePipelineSettings, PipelineType } from "@/types/PipelineTypes";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useDisplay } from "vuetify";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvRangeNumberSlider from "@/components/common/pv-range-number-slider.vue";

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

const { mdAndDown } = useDisplay();
const labelCols = computed(
  () => 12 - (mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8)
);
</script>

<template>
  <div>
    <pv-range-number-slider
      v-model="contourArea"
      label="Area"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :step="0.01"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourArea: value }, false)
      "
    />
    <pv-range-number-slider
      v-if="useCameraSettingsStore().currentPipelineType !== PipelineType.ColoredShape"
      v-model="contourRatio"
      label="Ratio (W/H)"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :step="0.1"
      tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourRatio: value }, false)
      "
    />
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOrientation"
      :items="['Portrait', 'Landscape'].map((v, i) => ({ name: v, value: i }))"
      label="Target Orientation"
      :label-cols="labelCols"
      tooltip="Used to determine how to calculate target landmarks, as well as aspect ratio"
      @update:model-value="
        (value: number) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOrientation: value }, false)
      "
    />
    <pv-range-number-slider
      v-if="useCameraSettingsStore().currentPipelineType === PipelineType.ColoredShape"
      v-model="contourFullness"
      label="Fullness"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :step="1"
      tooltip="Min and max ratio between a contour's area and its bounding rectangle"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourFullness: value }, false)
      "
    />
    <pv-range-number-slider
      v-if="currentPipelineSettings.pipelineType === PipelineType.ColoredShape"
      v-model="contourPerimeter"
      label="Perimeter"
      :label-cols="labelCols"
      :max="4000"
      :min="0"
      :step="1"
      tooltip="Min and max perimeter of the shape, in pixels"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourPerimeter: value }, false)
      "
    />
    <pv-number-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.contourSpecklePercentage"
      label="Speckle Rejection"
      :max="100"
      :min="0"
      :slider-cols="labelCols"
      :step="1"
      tooltip="Rejects contours whose average area is less than the given percentage of the average area of all the other contours"
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourSpecklePercentage: value }, false)
      "
    />
    <div v-if="currentPipelineSettings.pipelineType === PipelineType.Reflective">
      <pv-number-slider
        v-model="currentPipelineSettings.contourFilterRangeX"
        label="X Filter Tightness"
        :label-cols="labelCols"
        :max="4"
        :min="0.1"
        :step="0.1"
        tooltip="Rejects contours whose center X is further than X standard deviations left/right of the mean X location"
        @update:model-value="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourFilterRangeX: value }, false)
        "
      />
      <pv-number-slider
        v-model="currentPipelineSettings.contourFilterRangeY"
        label="Y Filter Tightness"
        :label-cols="labelCols"
        :max="4"
        :min="0.1"
        :step="0.1"
        tooltip="Rejects contours whose center Y is further than X standard deviations above/below the mean Y location"
        @update:model-value="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourFilterRangeY: value }, false)
        "
      />
      <pv-dropdown
        v-model="useCameraSettingsStore().currentPipelineSettings.contourGroupingMode"
        :items="['Single', 'Dual', 'Two or More'].map((v, i) => ({ name: v, value: i }))"
        label="Target Grouping"
        :label-cols="labelCols"
        tooltip="Whether or not every two targets are paired with each other (good for e.g. 2019 targets)"
        @update:model-value="
          (value: number) =>
            useCameraSettingsStore().changeCurrentPipelineSetting({ contourGroupingMode: value }, false)
        "
      />
      <pv-dropdown
        v-model="useCameraSettingsStore().currentPipelineSettings.contourIntersection"
        :disabled="useCameraSettingsStore().currentPipelineSettings.contourGroupingMode === 0"
        :items="['None', 'Up', 'Down', 'Left', 'Right'].map((v, i) => ({ name: v, value: i }))"
        label="Target Intersection"
        :label-cols="labelCols"
        tooltip="If target grouping is in dual mode it will use this dropdown to decide how targets are grouped with adjacent targets"
        @update:model-value="
          (value: number) =>
            useCameraSettingsStore().changeCurrentPipelineSetting({ contourIntersection: value }, false)
        "
      />
    </div>
    <div v-else-if="currentPipelineSettings.pipelineType === PipelineType.ColoredShape">
      <v-divider class="mt-3 mb-3" />
      <pv-dropdown
        v-model="currentPipelineSettings.contourShape"
        :items="['Circle', 'Polygon', 'Triangle', 'Quadrilateral'].map((v, i) => ({ name: v, value: i }))"
        label="Target Shape"
        :label-cols="labelCols"
        tooltip="The shape of targets to look for"
        @update:model-value="
          (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourShape: value }, false)
        "
      />
      <pv-number-slider
        v-model="currentPipelineSettings.accuracyPercentage"
        :disabled="currentPipelineSettings.contourShape < 1"
        label="Shape Simplification"
        :label-cols="labelCols"
        :max="100"
        :min="0"
        tooltip="How much we should simply the input contour before checking how many sides it has"
        @update:model-value="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ accuracyPercentage: value }, false)
        "
      />
      <pv-number-slider
        v-model="currentPipelineSettings.circleDetectThreshold"
        :disabled="currentPipelineSettings.contourShape !== 0"
        label="Circle match distance"
        :label-cols="labelCols"
        :max="100"
        :min="1"
        tooltip="How close the centroid of a contour must be to the center of a circle in order for them to be matched"
        @update:model-value="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ circleDetectThreshold: value }, false)
        "
      />
      <pv-range-number-slider
        v-model="contourRadius"
        :disabled="currentPipelineSettings.contourShape !== 0"
        label="Radius"
        :label-cols="labelCols"
        :max="100"
        :min="0"
        :step="1"
        @update:model-value="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourRadius: value }, false)
        "
      />
      <pv-number-slider
        v-model="currentPipelineSettings.maxCannyThresh"
        :disabled="currentPipelineSettings.contourShape !== 0"
        label="Max Canny Threshold"
        :label-cols="labelCols"
        :max="100"
        :min="1"
        :step="1"
        @update:model-value="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ maxCannyThresh: value }, false)
        "
      />
      <pv-number-slider
        v-model="currentPipelineSettings.circleAccuracy"
        :disabled="currentPipelineSettings.contourShape !== 0"
        label="Circle Accuracy"
        :label-cols="labelCols"
        :max="100"
        :min="1"
        :step="1"
        @update:model-value="
          (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ circleAccuracy: value }, false)
        "
      />
      <v-divider class="mt-3 mb-3" />
    </div>
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.contourSortMode"
      :items="
        ['Largest', 'Smallest', 'Highest', 'Lowest', 'Rightmost', 'Leftmost', 'Centermost'].map((v, i) => ({
          name: v,
          value: i
        }))
      "
      label="Target Sort"
      :label-cols="labelCols"
      tooltip="Chooses the sorting mode used to determine the 'best' targets to provide to user code"
      @update:model-value="
        (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourSortMode: value }, false)
      "
    />
  </div>
</template>
