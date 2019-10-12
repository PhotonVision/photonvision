<template>
    <div>
        <CVslider name="Exposure" v-model="value.Exposure" :min="0" :max="100" @input="handleInput('exposure',value.Exposure)"></CVslider>
        <CVslider name="Brightness" v-model="value.Brightness" :min="0" :max="100" @input="handleInput('brightness',value.Brightness)"></CVslider>
        <CVselect name="Orientation" v-model="value.Orientation" :list="['Normal','Inverted']" @input="handleInput('orientation',value.Orientation)"></CVselect>
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