<template>
    <div>        
        <CVswitch v-model="value.isColorPick" name="Colorpick Calibration"/>
        <v-divider color="darkgray "/>
        <CVrangeSlider v-model="value.hue" name="Hue" :min="0" :max="180" @input="handleData('hue')" :disabled="isAutomaticHSV"/>
        <CVrangeSlider v-model="value.saturation" name="Saturation" :min="0" :max="255" @input="handleData('saturation')" :disabled="isAutomaticHSV"/>
        <CVrangeSlider v-model="value.value" name="Value" :min="0" :max="255" @input="handleData('value')" :disabled="isAutomaticHSV"/>
        <v-divider color="darkgray "/>
        <v-btn style="margin: 20px;" tile color="#4baf62" :disabled="isManualHSV" @click="openPickerTab">
                    <v-icon>colorize</v-icon>
                    Colorpick Calibration
                </v-btn>
        <v-divider color="darkgray "/>
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
        computed: {            
            isAutomaticHSV() {
                return this.pipeline.isColorPick;
            },
            pipeline: {
                get() {
                    return this.$store.state.pipeline;
                }
            },
            isManualHSV()
            {
                return !this.pipeline.isColorPick;
            },
        },
        methods:{  
            openPickerTab()
            {
                console.log("opening picker tab");
                this.$router.push("picker");
            },
            handleData(val){
                this.handleInput(val,this.value[val]);
                this.$emit('update')
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>