<template>
  <div>
    <CVrangeSlider
      v-model="contourArea"
      name="Area"
      min="0"
      max="100"
      step="0.01"
      @input="handlePipelineData('contourArea')"
    />
    <CVrangeSlider
      v-if="currentPipelineType() !== 3"
      v-model="contourRatio"
      name="Ratio (W/H)"
      tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
      min="0"
      max="100"
      step="0.1"
      @input="handlePipelineData('contourRatio')"
    />
    <CVselect
      v-model="contourTargetOrientation"
      name="Target Orientation"
      tooltip="Used to determine how to calculate target landmarks, as well as aspect ratio"
      :list="['Portrait', 'Landscape']"
      @input="handlePipelineData('contourTargetOrientation')"
      @rollback="e=> rollback('contourTargetOrientation', e)"
    />
    <CVrangeSlider
      v-if="currentPipelineType() !== 3"
      v-model="contourFullness"
      name="Fullness"
      tooltip="Min and max ratio between a contour's area and its bounding rectangle"
      min="0"
      max="100"
      @input="handlePipelineData('contourFullness')"
    />
    <CVrangeSlider
      v-if="currentPipelineType() === 3"
      v-model="contourPerimeter"
      name="Perimeter"
      tooltip="Min and max perimeter of the shape, in pixels"
      min="0"
      max="4000"
      @input="handlePipelineData('contourPerimeter')"
    />
    <CVslider
      v-model="contourSpecklePercentage"
      name="Speckle Rejection"
      tooltip="Rejects contours whose average area is less than the given percentage of the average area of all the other contours"
      min="0"
      max="100"
      :slider-cols="largeBox"
      @input="handlePipelineData('contourSpecklePercentage')"
    />
    <template v-if="currentPipelineType() !== 3">
      <CVslider
        v-model="contourFilterRangeX"
        name="X filter tightness"
        tooltip="Rejects contours whose center X is further than X standard deviations above/below the mean X location"
        min="0.1"
        max="4"
        step="0.1"
        :slider-cols="largeBox"
        @input="handlePipelineData('contourFilterRangeX')"
      />
      <CVslider
        v-model="contourFilterRangeY"
        name="Y filter tightness"
        tooltip="Rejects contours whose center Y is further than X standard deviations above/below the mean Y location"
        min="0.1"
        max="4"
        step="0.1"
        :slider-cols="largeBox"
        @input="handlePipelineData('contourFilterRangeY')"
      />
      <CVselect
        v-model="contourGroupingMode"
        name="Target Grouping"
        tooltip="Whether or not every two targets are paired with each other (good for e.g. 2019 targets)"
        :select-cols="largeBox"
        :list="['Single','Dual','2orMore']"
        @input="handlePipelineData('contourGroupingMode')"
      />
      <CVselect
        v-model="contourIntersection"
        name="Target Intersection"
        tooltip="If target grouping is in dual mode it will use this dropdown to decide how targets are grouped with adjacent targets"
        :select-cols="largeBox"
        :list="['None','Up','Down','Left','Right']"
        :disabled="contourGroupingMode === 0"
        @input="handlePipelineData('contourIntersection')"
      />
    </template>
    <!-- If we arent not a shape, we are a shape-->
    <template v-else>
      <v-divider class="mt-3" />
      <CVselect
        v-model="contourShape"
        name="Target Shape"
        tooltip="The shape of targets to look for"
        :select-cols="largeBox"
        :list="['Circle', 'Polygon', 'Triangle', 'Quadrilateral']"
        @input="handlePipelineData('contourShape')"
      />

      <!-- Accuracy % is only for polygons-->
      <CVslider
        v-model="accuracyPercentage"
        :disabled="currentPipelineSettings().contourShape < 1"
        name="Shape Simplification"
        tooltip="How much we should simply the input contour before checking how many sides it has"
        min="0"
        max="100"
        :slider-cols="largeBox"
        @input="handlePipelineData('accuracyPercentage')"
      />
      <!-- Similarly, the threshold is only for circles -->
      <CVslider
        v-model="circleDetectThreshold"
        :disabled="currentPipelineSettings().contourShape !== 0"
        name="Circle match distance"
        tooltip="How close the centroid of a contour must be to the center of a circle in order for them to be matched"
        min="1"
        max="100"
        :slider-cols="largeBox"
        @input="handlePipelineData('circleDetectThreshold')"
      />
      <CVrangeSlider
        v-model="contourRadius"
        :disabled="currentPipelineSettings().contourShape !== 0"
        name="Radius"
        min="0"
        max="100"
        step="1"
        label-cols="3"
        @input="handlePipelineData('contourRadius')"
      />
      <CVslider
        v-model="maxCannyThresh"
        :disabled="currentPipelineSettings().contourShape !== 0"
        name="Max Canny Threshold"
        min="1"
        max="100"
        :slider-cols="largeBox"
        @input="handlePipelineData('maxCannyThresh')"
      />
      <CVslider
        v-model="circleAccuracy"
        :disabled="currentPipelineSettings().contourShape !== 0"
        name="Circle Accuracy"
        min="1"
        max="100"
        :slider-cols="largeBox"
        @input="handlePipelineData('circleAccuracy')"
      />
      <v-divider class="mt-3" />
    </template>
    <CVselect
      v-model="contourSortMode"
      name="Target Sort"
      tooltip="Chooses the sorting mode used to determine the 'best' targets to provide to user code"
      :select-cols="largeBox"
      :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Centermost']"
      @input="handlePipelineData('contourSortMode')"
      @rollback="e => rollback('contourSortMode', e)"
    />
  </div>
</template>

<script>
import CVrangeSlider from '../../components/common/cv-range-slider'
import CVselect from '../../components/common/cv-select'
import CVslider from '../../components/common/cv-slider'

export default {
  name: 'Contours',
  components: {
    CVrangeSlider,
    CVselect,
    CVslider
  },
  // eslint-disable-next-line vue/require-prop-types
  props: ['value'],

  data() {
    return {}
  },
  computed: {
    largeBox: {
      get() {
        // Sliders and selectors should be fuller width if we're on screen size medium and
        // up and either not in compact mode (because the tab will be 100% screen width),
        // or in driver mode (where the card will also be 100% screen width).
        return this.$vuetify.breakpoint.mdAndUp && (!this.$store.state.compactMode || this.$store.getters.isDriverMode) ? 10 : 8;
      }
    },
    contourArea: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourArea
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourArea": val});
      }
    },
    contourRatio: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourRatio
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourRatio": val});
      }
    },
    contourTargetOrientation: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourTargetOrientation
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourTargetOrientation": val});
      }
    },
    contourFullness: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourFullness
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourFullness": val});
      }
    },
    contourPerimeter: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourPerimeter
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourPerimeter": val});
      }
    },
    contourSpecklePercentage: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourSpecklePercentage
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourSpecklePercentage": val});
      }
    },
    contourFilterRangeX: {
      get() {
        console.log(this.$store.getters.currentPipelineSettings.contourFilterRangeX)
        return this.$store.getters.currentPipelineSettings.contourFilterRangeX
      },
      set(val) {
        console.log("set")
        console.log(val)
        this.$store.commit("mutatePipeline", {"contourFilterRangeX": val});
      }
    },
    contourFilterRangeY: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourFilterRangeY
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourFilterRangeY": val});
      }
    },
    contourGroupingMode: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourGroupingMode
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourGroupingMode": val});
      }
    },
    contourSortMode: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourSortMode
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourSortMode": val});
      }
    },
    contourShape: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourShape
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourShape": val});
      }
    },
    contourIntersection: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourIntersection
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourIntersection": val});
      }
    },
    accuracyPercentage: {
      get() {
        return this.$store.getters.currentPipelineSettings.accuracyPercentage
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"accuracyPercentage": val});
      }
    },
    contourRadius: {
      get() {
        return this.$store.getters.currentPipelineSettings.contourRadius
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"contourRadius": val});
      }
    },
    circleDetectThreshold: {
      get() {
        return this.$store.getters.currentPipelineSettings.circleDetectThreshold
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"circleDetectThreshold": val});
      }
    },
    maxCannyThresh: {
      get() {
        return this.$store.getters.currentPipelineSettings.maxCannyThresh
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"maxCannyThresh": val});
      }
    },
    circleAccuracy: {
      get() {
        return this.$store.getters.currentPipelineSettings.circleAccuracy
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"circleAccuracy": val});
      }
    },
  },
  methods: {},
}
</script>

<style lang="" scoped>

</style>
