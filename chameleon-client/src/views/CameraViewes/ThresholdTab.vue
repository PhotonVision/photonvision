<template>
    <div>
        <CVrangeSlider v-model="value.hue" name="Hue" :min="0" :max="180" @input="handleData('hue')"/>
        <CVrangeSlider v-model="value.saturation" name="Saturation" :min="0" :max="255"
                       @input="handleData('saturation')"/>
        <CVrangeSlider v-model="value.value" name="Value" :min="0" :max="255" @input="handleData('value')"/>
        <CVswitch v-model="value.erode" name="Erode" @input="handleData('erode')"/>
        <CVswitch v-model="value.dilate" name="Dilate" @input="handleData('dilate')"/>
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
            handleData(val){
                this.handleInput(val,this.value[val])
                this.$emit('update')
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>