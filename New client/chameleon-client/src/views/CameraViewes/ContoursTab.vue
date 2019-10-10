<template>
    <div>
        <CVrangeSlider v-model="value.Area" name="Area" :min="0" :max="100" :step="0.1"></CVrangeSlider>
        <CVrangeSlider v-model="value.Ratio" name="Ratio (W/H)" :min="0" :max="100" :step="0.1"></CVrangeSlider>
        <CVrangeSlider v-model="value.Extent" name="Extent" :min="0" :max="100"></CVrangeSlider>
        <CVselect name="Target Group" :list="['Single','Dual','Triple','Quadruple','Quintuple']" v-model="value.TargetGrouping"></CVselect>
        <CVselect name="Target Intersection" :list="['None','Up','Down','Left','Right']" :disabled="isDisabled" v-model="value.TargetIntersection"></CVselect>

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
                if(this.value.TargetGrouping === 0){
                    return true;
                }
                return false;
            }
        },
    }
</script>

<style lang="" scoped>
    
</style>