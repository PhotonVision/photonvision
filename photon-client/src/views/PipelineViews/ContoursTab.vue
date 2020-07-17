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
      min="0"
      max="100"
      step="0.1"
      @input="handlePipelineData('contourRatio')"
      @rollback="e=> rollback('contourRatio',e)"
    />
    <CVrangeSlider
      v-model="contourFullness"
      name="Fullness"
      min="0"
      max="100"
      @input="handlePipelineData('contourFullness')"
      @rollback="e=> rollback('contourFullness',e)"
    />
    <CVslider
      v-model="contourSpecklePercentage"
      name="Speckle Rejection"
      min="0"
      max="100"
      :slider-cols="largeBox"
      @input="handlePipelineData('contourSpecklePercentage')"
      @rollback="e=> rollback('contourSpecklePercentage',e)"
    />
    <CVselect
      v-model="contourGroupingMode"
      name="Target Group"
      :select-cols="largeBox"
      :list="['Single','Dual']"
      @input="handlePipelineData('targetGroup')"
      @rollback="e=> rollback('targetGroup',e)"
    />
    <CVselect
      v-model="contourIntersection"
      name="Target Intersection"
      :select-cols="largeBox"
      :list="['None','Up','Down','Left','Right']"
      :disabled="contourGroupingMode === 0"
      @input="handlePipelineData('contourIntersection')"
      @rollback="e=> rollback('contourIntersection',e)"
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