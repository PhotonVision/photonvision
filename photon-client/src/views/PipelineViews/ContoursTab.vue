<template>
  <div>
    <CVrangeSlider
      v-model="area"
      name="Area"
      :min="0"
      :max="100"
      :step="0.1"
      @input="handleData('area')"
      @rollback="e=> rollback('area',e)"
    />
    <CVrangeSlider
      v-model="ratio"
      name="Ratio (W/H)"
      :min="0"
      :max="100"
      :step="0.1"
      @input="handleData('ratio')"
      @rollback="e=> rollback('ratio',e)"
    />
    <CVrangeSlider
      v-model="extent"
      name="Extent"
      :min="0"
      :max="100"
      @input="handleData('extent')"
      @rollback="e=> rollback('extent',e)"
    />
    <CVslider
      v-model="speckle"
      name="Speckle Rejection"
      :min="0"
      :max="100"
      @input="handleData('speckle')"
      @rollback="e=> rollback('speckle',e)"
    />
    <CVselect
      v-model="contourGroupingMode"
      name="Target Group"
      :list="['Single','Dual']"
      @input="handleData('targetGroup')"
      @rollback="e=> rollback('targetGroup',e)"
    />
    <CVselect
      v-model="targetIntersection"
      name="Target Intersection"
      :list="['None','Up','Down','Left','Right']"
      :disabled="isDisabled"
      @input="handleData('targetIntersection')"
      @rollback="e=> rollback('targetIntersection',e)"
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
        props: ['value'],

        data() {
            return {}
        },
        computed: {
            area: {
                get() {
                    return this.$store.getters.currentPipelineSettings.area
                },
                set(val) {
                    this.$store.commit("area", val);
                }
            },
            ratio: {
                get() {
                    return this.$store.getters.currentPipelineSettings.ratio
                },
                set(val) {
                    this.$store.commit("ratio", val);
                }
            },
            extent: {
                get() {
                    return this.$store.getters.currentPipelineSettings.extent
                },
                set(val) {
                    this.$store.commit("extent", val);
                }
            },
            speckle: {
                get() {
                    return this.$store.getters.currentPipelineSettings.speckle
                },
                set(val) {
                    this.$store.commit("speckle", val);
                }
            },
            contourGroupingMode: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourGroupingMode
                },
                set(val) {
                    this.$store.commit("contourGroupingMode", val);
                }
            },
            targetIntersection: {
                get() {
                    return this.$store.getters.currentPipelineSettings.targetIntersection
                },
                set(val) {
                    this.$store.commit("targetIntersection", val);
                }
            },

            isDisabled() {
                return this.value.targetGroup === 0;

            }
        },
        methods: {},
    }
</script>

<style lang="" scoped>

</style>