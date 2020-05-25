<template>
    <div>
        <CVrangeSlider v-model="value.area" name="Area" :min="0" :max="100" :step="0.1"
                       @input="handleData('area')"/>
        <CVrangeSlider v-model="value.ratio" name="Ratio (W/H)" :min="0" :max="100" :step="0.1"
                       @input="handleData('ratio')"/>
        <CVrangeSlider v-model="value.extent" name="Extent" :min="0" :max="100"
                       @input="handleData('extent')"/>
        <CVslider name="Speckle Rejection" :min="0" :max="100" v-model="value.speckle"
                  @input="handleData('speckle')"/>
        <CVselect name="Target Group" :list="['Single','Dual']" v-model="value.targetGroup"
                  @input="handleData('targetGroup')"/>
        <CVselect name="Target Intersection" :list="['None','Up','Down','Left','Right']" :disabled="isDisabled"
                  v-model="value.targetIntersection" @input="handleData('targetIntersection')"/>

    </div>
</template>

<script>
    import CVrangeSlider from '../../components/common/cv-range-slider'
    import CVselect from '../../components/common/cv-select'
    import CVslider from '../../components/common/cv-slider'

    export default {
        name: 'Contours',
        props: ['value'],
        components: {
            CVrangeSlider,
            CVselect,
            CVslider
        },
        methods: {
        },

        data() {
            return {}
        },
        computed: {
            isDisabled() {
                return this.value.targetGroup === 0;

            }
        },
    }
</script>

<style lang="" scoped>

</style>