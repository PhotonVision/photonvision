<template>
  <div>
    <CVrangeSlider
      v-model="contourArea"
      name="Area"
      min="0"
      max="100"
      step="0.1"
      @input="handlePipelineData('contourArea')"
      @rollback="e=> rollback('contourArea',e)"
    />
    <CVrangeSlider
      v-model="contourRatio"
      name="Ratio (W/H)"
      tooltip="Min and max ratio between the width and height of a contour's bounding rectangle"
      min="0"
      max="100"
      step="0.1"
      @input="handlePipelineData('contourRatio')"
      @rollback="e=> rollback('contourRatio',e)"
    />
    <CVrangeSlider
      v-model="contourFullness"
      name="Fullness"
      tooltip="Min and max ratio between a contour's area and its bounding rectangle"
      min="0"
      max="100"
      @input="handlePipelineData('contourFullness')"
      @rollback="e=> rollback('contourFullness',e)"
    />
    <CVslider
      v-model="contourSpecklePercentage"
      name="Speckle Rejection"
      tooltip="Rejects contours whose average area is less than the given percentage of the average area of all the other contours"
      min="0"
      max="100"
      :slider-cols="largeBox"
      @input="handlePipelineData('contourSpecklePercentage')"
      @rollback="e=> rollback('contourSpecklePercentage',e)"
    />
    <CVselect
      v-model="contourGroupingMode"
      name="Target Grouping"
      tooltip="Whether or not every two targets are paired with each other (good for e.g. 2019 targets)"
      :select-cols="largeBox"
      :list="['Single','Dual']"
      @input="handlePipelineData('contourGroupingMode')"
      @rollback="e=> rollback('contourGroupingMode',e)"
    />
    <CVselect
      v-model="contourIntersection"
      name="Target Intersection"
      tooltip="If target grouping is in dual mode it will use this dropdown to decide how targets are grouped with adjacent targets"
      :select-cols="largeBox"
      :list="['None','Up','Down','Left','Right']"
      :disabled="contourGroupingMode === 0"
      @input="handlePipelineData('contourIntersection')"
      @rollback="e=> rollback('contourIntersection',e)"
    />
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
            contourFullness: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourFullness
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"contourFullness": val});
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
            contourIntersection: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourIntersection
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"contourIntersection": val});
                }
            }
        },
        methods: {},
    }
</script>

<style lang="" scoped>

</style>