<template>
    <div>
        <CVslider name="Exposure" v-model="t" :min="0" :max="100"></CVslider>
        <CVslider name="Brightness" v-model="t" :min="0" :max="100"></CVslider>
        <CVselect name="Orientation" v-model="a" :list="['Normal','Inverted']"></CVselect>
    </div>
</template>

<script>
import CVslider from '../../components/cv-slider'
import rangeSlider from '../../components/cv-range-slider'
import CVselect from '../../components/cv-select'
import CVswitch from '../../components/cv-switch'
    export default {
        name: 'Input',
        components:{
            CVslider,
            rangeSlider,
            CVselect,
            CVswitch
        },
        data() {
            return {
                t:0,
                a:1
            }
        },
        methods:{
        }
    }
</script>

<style scoped>
    
</style>