<template>
    <div>
        <div>
            <v-row align="center">
                <v-col :cols="3" class="colsClass">
                    <div style="padding-left:30px">
                        <CVselect name="Camera" :list="[1,2,3]"></CVselect>
                    </div>
                </v-col>
                <v-col :cols="3" class="colsClass">
                    <CVselect name="Pipeline" :list="[1,2,3]"></CVselect>
                </v-col>
                 <v-col :cols="2" class="colsClass">
                     <v-icon color="white" @click="test">add</v-icon>
                     <v-icon color="red darken-2" @click="test">delete</v-icon>
                </v-col>
            </v-row>
        </div>
        <v-row>
            <v-col cols="6" class="colsClass">
                <v-tabs fixed-tabs background-color="#212121" dark height="50" slider-color="#4baf62" v-model="selectedTab">
                    <v-tab>Input</v-tab>
                    <v-tab>Threshold</v-tab>
                    <v-tab>Contours</v-tab>
                    <v-tab>Output</v-tab>
                </v-tabs>
                <div style="padding-left:30px">
                    <component v-model="pipeline" :is="selectedComponent"></component>
                </div>
            </v-col>
            <v-col cols="6" class="colsClass">
                <div>
                    <v-tabs background-color="#212121" dark height="50" slider-color="#4baf62" centered style="padding-bottom:10px">
                        <v-tab>Normal</v-tab>
                        <v-tab>Threshold</v-tab>
                    </v-tabs>
                    <img class="videoClass" src="https://pbs.twimg.com/profile_images/846659478120366082/K-kZVvT8_400x400.jpg">
                    <h5 id="Point">Yaw: 222 Pitch: 111 FPS: 15</h5>
                </div>
            </v-col>
      </v-row>
    </div>
</template>

<script>
import InputTab from './CameraViewes/InputTab'
import ThresholdTab from './CameraViewes/ThresholdTab'
import ContoursTab from './CameraViewes/ContoursTab'
import OutputTab from './CameraViewes/OutputTab'
import CVselect from '../components/cv-select'
    export default {
        name: 'CameraTab',
        components:{
            InputTab,
            ThresholdTab,
            ContoursTab,
            OutputTab,
            CVselect
        },
        methods:{
            test(){

            }
        },
        data() {
            return {
                selectedTab:0,
                pipeline:{
                    Exposure:0,
                    Brightness:0,
                    Orientation:0,
                    Hue:[0,15],
                    Saturation:[0,15],
                    Value:[0,25],
                    Erode:false,
                    Dilate:false,
                    Area:[0,12],
                    Ratio:[0,12],
                    Extent:[0,12],
                    TargetGrouping:0,
                    TargetIntersection:0,
                    SortMode:0
                }
            }
        },
        computed:{
            selectedComponent(){
                switch(this.selectedTab){
                    case 0:
                        return "InputTab";
                    case 1:
                        return "ThresholdTab";
                    case 2:
                        return "ContoursTab";
                    case 3:
                        return "OutputTab";
                }
            }
        }
    }
</script>

<style scoped>
    .colsClass{
        padding: 0 !important;
        
    }
    .videoClass{
        display: block;
        margin-right: auto;
        margin-left: auto;
    }
    #Point{
        padding-top: 5px;
        text-align: center;
        color: #f4f4f4;
    }
</style>