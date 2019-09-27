<template>
    <div>
        <CVrangeSlider v-model="t" name="Hue" :min="0" :max="180"></CVrangeSlider>
        <CVrangeSlider v-model="t" name="Saturation" :min="0" :max="255"></CVrangeSlider>
        <CVrangeSlider v-model="t" name="Value" :min="0" :max="255"></CVrangeSlider>
        <CVswitch name="Erode"></CVswitch>
        <CVswitch name="Dilate"></CVswitch>
    </div>
</template>

<script>
import CVrangeSlider from '../../components/cv-range-slider'
import CVswitch from '../../components/cv-switch'
    export default {
        name: 'Threshold',
        components:{
            CVrangeSlider,
            CVswitch
        },
        data() {
            return {
                t:[0,5]
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>