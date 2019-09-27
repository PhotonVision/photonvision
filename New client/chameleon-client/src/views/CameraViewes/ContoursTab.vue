<template>
    <div>
        <CVrangeSlider v-model="t" name="Area" :min="0" :max="100"></CVrangeSlider>
        <CVrangeSlider v-model="t" name="Ratio (W/H)" :min="0" :max="100" :step="0.1"></CVrangeSlider>
        <CVrangeSlider v-model="t" name="Ratio Extent" :min="0" :max="100"></CVrangeSlider>
        <CVselect name="Target Group" :list="['Single','Dual','Triple','Quadruple','Quintuple']"></CVselect>
        <CVselect name="Target Intersaction" :list="['None','Up','Down','Left','Right']" :disabled="isDisabled"></CVselect>

    </div>
</template>

<script>
import CVrangeSlider from '../../components/cv-range-slider'
import CVselect from '../../components/cv-select'
    export default {
        name: 'Contours',
        components:{
            CVrangeSlider,
            CVselect
        },
        data() {
            return {
                t:[0,25]
            }
        },
        computed:{
            isDisabled(){
                return false
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>