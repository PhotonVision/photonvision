<template>
    <div>
        <CVselect name="Camera" :list="[1,2,3]"></CVselect>
        <CVselect name="Resulotion" :list="[1,2,3]"></CVselect>
        <CVselect name="Stream Resulotion" :list="['1:1','1:2','1:4','1:6']"></CVselect>
        <CVnumberinput name="Diagonal FOV"></CVnumberinput>
        <v-btn style="margin-top:10px" small color="#4baf62">Save Camera Settings</v-btn>
    </div>
</template>

<script>
import CVselect from '../../components/cv-select'
import CVnumberinput from '../../components/cv-number-input'
    export default {
        name: 'CameraSettings',
        components:{
            CVselect,
            CVnumberinput
        },
        data() {
            return {
                
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>