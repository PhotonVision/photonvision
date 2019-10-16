<template>
    <div>
        <CVrangeSlider v-model="value.area" name="Area" :min="0" :max="100" :step="0.1" @input="handleInput('area',value.area)"></CVrangeSlider>
        <CVrangeSlider v-model="value.ratio" name="Ratio (W/H)" :min="0" :max="100" :step="0.1" @input="handleInput('ratio',value.ratio)"></CVrangeSlider>
        <CVrangeSlider v-model="value.extent" name="Extent" :min="0" :max="100" @input="handleInput('extent',value.extent)"></CVrangeSlider>
        <CVselect name="Target Group" :list="['Single','Dual','Triple','Quadruple','Quintuple']" v-model="value.targetGroup" @input="handleInput('targetGroup',value.targetGrouping)"></CVselect>
        <CVselect name="Target Intersection" :list="['None','Up','Down','Left','Right']" :disabled="isDisabled" v-model="value.targetIntersection" @input="handleInput('targetIntersection',value.targetIntersection)"></CVselect>

    </div>
</template>

<script>
import CVrangeSlider from '../../components/cv-range-slider'
import CVselect from '../../components/cv-select'
    export default {
        name: 'Contours',
        props:['value'],
        components:{
            CVrangeSlider,
            CVselect
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