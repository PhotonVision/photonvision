<template>
    <div>
        <CVrangeSlider v-model="value.area" name="Area" :min="0" :max="100" :step="0.1" @input="handleInput('area',value.area)"></CVrangeSlider>
        <CVrangeSlider v-model="value.ratio" name="Ratio (W/H)" :min="0" :max="100" :step="0.1" @input="handleInput('ratio',value.ratio)"></CVrangeSlider>
        <CVrangeSlider v-model="value.extent" name="Extent" :min="0" :max="100" @input="handleInput('extent',value.extent)"></CVrangeSlider>
        <CVslider name="Speckle Rejection" :min="0" :max="100" v-model="value.speckle" @input="handleInput('speckle',value.speckle)"></CVslider>
        <CVselect name="Target Group" :list="['Single','Dual']" v-model="value.targetGroup" @input="handleInput('targetGroup',value.targetGroup)"></CVselect>
        <CVselect name="Target Intersection" :list="['None','Up','Down','Left','Right']" :disabled="isDisabled" v-model="value.targetIntersection" @input="handleInput('targetIntersection',value.targetIntersection)"></CVselect>

    </div>
</template>

<script>
import CVrangeSlider from '../../components/cv-range-slider'
import CVselect from '../../components/cv-select'
import CVslider from '../../components/cv-slider'
    export default {
        name: 'Contours',
        props:['value'],
        components:{
            CVrangeSlider,
            CVselect,
            CVslider
        },

        data() {
            return {
            }
        },
        computed:{
            isDisabled(){
                if(this.value.targetGroup === 0){
                    return true;
                }
                return false;
            }
        },
    }
</script>

<style lang="" scoped>
    
</style>