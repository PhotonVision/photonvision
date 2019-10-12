<template>
    <div>
        <CVrangeSlider v-model="value.Hue" name="Hue" :min="0" :max="180" @input="handleInput('hue',value.Hue)"></CVrangeSlider>
        <CVrangeSlider v-model="value.Saturation" name="Saturation" :min="0" :max="255" @input="handleInput('saturation',value.Saturation)"></CVrangeSlider>
        <CVrangeSlider v-model="value.Value" name="Value" :min="0" :max="255" @input="handleInput('value',value.Value)"></CVrangeSlider>
        <CVswitch v-model="value.Erode" name="Erode" @input="handleInput('erode',value.Erode)"></CVswitch>
        <CVswitch v-model="value.Dilate" name="Dilate" @input="handleInput('dilate',value.Dilate)"></CVswitch>
    </div>
</template>

<script>
import CVrangeSlider from '../../components/cv-range-slider'
import CVswitch from '../../components/cv-switch'
    export default {
        name: 'Threshold',
        props:['value'],
        components:{
            CVrangeSlider,
            CVswitch
        },
        data() {
            return {
            }
        },
        computed:{
        },
        methods:{
        }
    }
</script>

<style lang="" scoped>
    
</style>