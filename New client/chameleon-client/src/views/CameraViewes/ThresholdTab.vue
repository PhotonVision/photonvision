<template>
    <div>
        <CVrangeSlider v-model="value.hue" name="Hue" :min="0" :max="180" @input="handleInput('hue',value.hue)"></CVrangeSlider>
        <CVrangeSlider v-model="value.saturation" name="Saturation" :min="0" :max="255" @input="handleInput('saturation',value.saturation)"></CVrangeSlider>
        <CVrangeSlider v-model="value.value" name="Value" :min="0" :max="255" @input="handleInput('value',value.value)"></CVrangeSlider>
        <CVswitch v-model="value.erode" name="Erode" @input="handleInput('erode',value.erode)"></CVswitch>
        <CVswitch v-model="value.dilate" name="Dilate" @input="handleInput('dilate',value.dilate)"></CVswitch>
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