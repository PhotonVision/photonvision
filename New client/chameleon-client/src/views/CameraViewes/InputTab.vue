<template>
    <div>
        <CVslider name="Exposure" v-model="value.exposure" @input="handleInput('exposure',value.exposure)" :min="0" :max="100"></CVslider>
        <CVslider name="Brightness" v-model="value.brightness" :min="0" :max="100" @input="handleInput('brightness',value.brightness)"></CVslider>
        <CVselect name="Orientation" v-model="value.orientation" :list="['Normal','Inverted']" @input="handleInput('orientation',value.orientation)"></CVselect>
    </div>
</template>

<script>
import CVslider from '../../components/cv-slider'
import CVselect from '../../components/cv-select'
    export default {
        name: 'Input',
        props:['value'],
        components:{
            CVslider,
            CVselect,
        },
        data() {
            return {
                t:0,
                a:1
            }
        },
        methods:{

        },
        computed:{}
    }
</script>

<style scoped>
    
</style>