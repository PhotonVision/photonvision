<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import CvRangeSlider from "@/components/common/cv-range-slider.vue";
import CvSelect from "@/components/common/cv-select.vue";
import CvSlider from "@/components/common/cv-slider.vue";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = useCameraSettingsStore().currentPipelineSettings;
</script>

<template>
  <div>
    <cv-range-slider
        v-model="useCameraSettingsStore().currentPipelineSettings.contourArea"
        label="Area"
        :min="0"
        :max="100"
        :step="0.01"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourArea: value}, false)"
    />
    <cv-range-slider
        v-if="useCameraSettingsStore().currentPipelineType !== PipelineType.ColoredShape"
        v-model="useCameraSettingsStore().currentPipelineSettings.contourRatio"
        label="Ratio (W/H)"
        tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
        :min="0"
        :max="100"
        :step="0.1"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourRatio: value}, false)"
    />
    <cv-select
        v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOrientation"
        label="Target Orientation"
        tooltip="Used to determine how to calculate target landmarks, as well as aspect ratio"
        :items="['Portrait', 'Landscape']"
        :select-cols="10"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourTargetOrientation: value}, false)"
    />
    <cv-range-slider
        v-if="useCameraSettingsStore().currentPipelineType === PipelineType.ColoredShape"
        v-model="useCameraSettingsStore().currentPipelineSettings.contourFullness"
        label="Fullness"
        tooltip="Min and max ratio between a contour's area and its bounding rectangle"
        :min="0"
        :max="100"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourFullness: value}, false)"
    />
    <cv-range-slider
        v-if="currentPipelineSettings.pipelineType === PipelineType.ColoredShape"
        v-model="currentPipelineSettings.contourPerimeter"
        label="Perimeter"
        tooltip="Min and max perimeter of the shape, in pixels"
        min="0"
        max="4000"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourPerimeter: value}, false)"
    />
    <cv-slider
        v-model="useCameraSettingsStore().currentPipelineSettings.contourSpecklePercentage"
        label="Speckle Rejection"
        tooltip="Rejects contours whose average area is less than the given percentage of the average area of all the other contours"
        :min="0"
        :max="100"
        :slider-cols="10"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourSpecklePercentage: value}, false)"
    />
    <template v-if="currentPipelineSettings.pipelineType === PipelineType.Reflective">
      <cv-slider
          v-model="currentPipelineSettings.contourFilterRangeX"
          label="X Filter Tightness"
          tooltip="Rejects contours whose center X is further than X standard deviations above/below the mean X location"
          :min="0.1"
          :max="4"
          :step="0.1"
          :slider-cols="10"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourFilterRangeX: value}, false)"
      />
      <cv-slider
          v-model="currentPipelineSettings.contourFilterRangeY"
          label="Y Filter Tightness"
          tooltip="Rejects contours whose center Y is further than X standard deviations above/below the mean Y location"
          :min="0.1"
          :max="4"
          :step="0.1"
          :slider-cols="10"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourFilterRangeY: value}, false)"
      />
      <cv-select
          v-model="useCameraSettingsStore().currentPipelineSettings.contourGroupingMode"
          label="Target Grouping"
          tooltip="Whether or not every two targets are paired with each other (good for e.g. 2019 targets)"
          :select-cols="10"
          :items="['Single','Dual','Two or More']"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourGroupingMode: value}, false)"
      />
      <cv-select
          v-model="useCameraSettingsStore().currentPipelineSettings.contourIntersection"
          label="Target Intersection"
          tooltip="If target grouping is in dual mode it will use this dropdown to decide how targets are grouped with adjacent targets"
          :select-cols="10"
          :items="['None','Up','Down','Left','Right']"
          :disabled="useCameraSettingsStore().currentPipelineSettings.contourGroupingMode === 0"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourIntersection: value}, false)"
      />
    </template>
    <template v-else-if="currentPipelineSettings.pipelineType === PipelineType.ColoredShape">
      <v-divider class="mt-3" />
      <cv-select
          v-model="currentPipelineSettings.contourShape"
          label="Target Shape"
          tooltip="The shape of targets to look for"
          :select-cols="10"
          :items="['Circle', 'Polygon', 'Triangle', 'Quadrilateral']"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourShape: value}, false)"
      />
      <cv-slider
        v-model="currentPipelineSettings.accuracyPercentage"
        :disabled="currentPipelineSettings.contourShape < 1"
        label="Shape Simplification"
        tooltip="How much we should simply the input contour before checking how many sides it has"
        :min="0"
        :max="100"
        :slider-cols="10"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({accuracyPercentage: value}, false)"
      />
      <cv-slider
          v-model="currentPipelineSettings.circleDetectThreshold"
          :disabled="currentPipelineSettings.contourShape !== 0"
          label="Circle match distance"
          tooltip="How close the centroid of a contour must be to the center of a circle in order for them to be matched"
          :min="1"
          :max="100"
          :slider-cols="10"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({circleDetectThreshold: value}, false)"
      />
      <cv-range-slider
          v-model="currentPipelineSettings.contourRadius"
          :disabled="currentPipelineSettings.contourShape !== 0"
          label="Radius"
          :min="0"
          :max="100"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourRadius: value}, false)"
      />
      <cv-slider
          v-model="currentPipelineSettings.maxCannyThresh"
          :disabled="currentPipelineSettings.contourShape !== 0"
          label="Max Canny Threshold"
          :min="1"
          :max="100"
          :slider-cols="10"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({maxCannyThresh: value}, false)"
      />
      <cv-slider
          v-model="currentPipelineSettings.circleAccuracy"
          :disabled="currentPipelineSettings.contourShape !== 0"
          label="Circle Accuracy"
          :min="1"
          :max="100"
          :slider-cols="10"
          @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({circleAccuracy: value}, false)"
      />
      <v-divider class="mt-3" />
    </template>
    <cv-select
        v-model="useCameraSettingsStore().currentPipelineSettings.contourSortMode"
        label="Target Sort"
        tooltip="Chooses the sorting mode used to determine the 'best' targets to provide to user code"
        :select-cols="10"
        :items="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Centermost']"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourSortMode: value}, false)"
    />
  </div>
</template>

<style scoped>

</style>
