<template>
    <div>
        <CVselect name="SortMode" v-model="value.sortMode"
                  :list="['Largest','Smallest','Highest','Lowest','Rightmost','Leftmost','Closest']"
                  @input="handleInput('sortMode',value.sortMode)"/>
        <span>Calibrate:</span>
        <v-divider dark color="white"/>
        <CVselect name="Calibration Mode" v-model="value.calibrationMode" :list="['Single point','Dual point']"
                  @input="handleInput('calibrationMode',value.calibrationMode)"/>
        <component :raw-point="rawPoint" :is="selectedComponent"/>
        <v-snackbar :timeout="3000" v-model="snackbar" top color="error">
            <span style="color:#000">Points are too close</span>
            <v-btn color="black" text @click="snackbar = false">Close</v-btn>
        </v-snackbar>
    </div>
</template>

<script>
import CVselect from '../../components/cv-select'
import DualCalibration from "./OutputViewes/DualCalibration";
import SingleCalibration from "./OutputViewes/SingleCalibration";
    export default {
        name: 'Output',
        props:['value'],
        components:{
            CVselect,
            SingleCalibration,
            DualCalibration,

        },
        methods:{
        },

        data() {
            return {
                snackbar: false,
            }
        },
        computed:{
            selectedComponent:{
                get(){
                    switch (this.value.calibrationMode) {
                        case 0:
                            return "SingleCalibration";
                        case 1:
                            return "DualCalibration"
                    }
                    return ""
                }
            },
            rawPoint:{
                get(){
                    return this.$store.state.point.rawPoint;
                }
            }
        }
    }
</script>

<style scoped>
</style>