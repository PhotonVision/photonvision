<template>
    <div>
        <CVrangeSlider
                v-model="contourArea"
                name="Area"
                :min="0"
                :max="100"
                :step="0.1"
                @input="handlePipelineData('contourArea')"
                @rollback="e=> rollback('contourArea',e)"
        />
        <CVrangeSlider
                v-model="contourRatio"
                name="Ratio (W/H)"
                :min="0"
                :max="100"
                :step="0.1"
                @input="handlePipelineData('contourRatio')"
                @rollback="e=> rollback('contourRatio',e)"
        />
        <CVrangeSlider
                v-model="contourExtent"
                name="Extent"
                :min="0"
                :max="100"
                @input="handlePipelineData('contourExtent')"
                @rollback="e=> rollback('contourExtent',e)"
        />
        <CVslider
                v-model="contourSpecklePercentage"
                name="Speckle Rejection"
                :min="0"
                :max="100"
                @input="handlePipelineData('contourSpecklePercentage')"
                @rollback="e=> rollback('contourSpecklePercentage',e)"
        />
        <CVselect
                v-model="contourGroupingMode"
                name="Target Group"
                :list="['Single','Dual']"
                @input="handlePipelineData('targetGroup')"
                @rollback="e=> rollback('targetGroup',e)"
        />
        <CVselect
                v-model="contourIntersection"
                name="Target Intersection"
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
            contourArea: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourArea
                },
                set(val) {
                    this.$store.commit("contourArea", val);
                }
            },
            contourRatio: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourRatio
                },
                set(val) {
                    this.$store.commit("contourRatio", val);
                }
            },
            contourExtent: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourExtent
                },
                set(val) {
                    this.$store.commit("contourExtent", val);
                }
            },
            contourSpecklePercentage: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourSpecklePercentage
                },
                set(val) {
                    this.$store.commit("contourSpecklePercentage", val);
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
            contourIntersection: {
                get() {
                    return this.$store.getters.currentPipelineSettings.contourIntersection
                },
                set(val) {
                    this.$store.commit("contourIntersection", val);
                }
            }
        },
        methods: {},
    }
</script>

<style lang="" scoped>

</style>