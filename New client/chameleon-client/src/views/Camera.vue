<template>
    <div>
        <v-row >
            <v-col cols="6" class="colsClass">
                <v-tabs fixed-tabs background-color="#212121" dark height="50" slider-color="#4baf62" v-model="selectedTab">
                    <v-tab>Input</v-tab>
                    <v-tab>Threshold</v-tab>
                    <v-tab>Contours</v-tab>
                    <v-tab>Output</v-tab>
                </v-tabs>
                <div>
                    <component :is="selectedComponent"></component>
                </div>
            </v-col>
            <v-col cols="6">
                <div>
                    <img class="videoClass" src="https://pbs.twimg.com/profile_images/846659478120366082/K-kZVvT8_400x400.jpg">
                    <h5 id="Point">Yaw: 222 Pitch: 111 FPS: 15</h5>
                </div>
            </v-col>
      </v-row>
    </div>
</template>

<script>
import InputTab from './CameraViewes/InputTab'
    export default {
        name: 'CameraTab',
        components:{
            InputTab
        },
        data() {
            return {
                selectedTab:0,
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