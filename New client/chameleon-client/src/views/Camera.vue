<template>
    <div>
        <div>
            <v-row align="center">
                <v-col :cols="3" class="colsClass">
                    <div style="padding-left:30px">
                        <CVselect name="Camera" v-model="currentCameraIndex" :list="cameraList"></CVselect>
                    </div>
                </v-col>
                <v-col :cols="1">
                    <CVicon color="#c5c5c5" hover text="edit" @click="test" tooltip="Edit camera name"></CVicon>
                </v-col>
                <v-col :cols="3" class="colsClass">
                    <CVselect name="Pipeline" :list="pipelineList" v-model="currentPipelineIndex"></CVselect>
                </v-col>
                 <v-col :cols="1" class="colsClass">
                        <v-menu offset-y dark auto>
                            <template v-slot:activator="{ on }">
                                <v-icon color="white" @click="test" v-on="on">menu</v-icon>
                            </template>
                            <v-list dense>
                                <v-list-item @click="test">
                                    <v-list-item-title>
                                        <CVicon color="#c5c5c5" :right="true" text="edit" tooltip="Edit pipeline name"></CVicon>
                                    </v-list-item-title>
                                </v-list-item>
                                      <v-list-item @click="test">
                                    <v-list-item-title>
                                        <CVicon color="#c5c5c5" :right="true" text="add" tooltip="Add new pipeline"></CVicon>
                                    </v-list-item-title>
                                </v-list-item>
                                <v-list-item @click="test">
                                    <v-list-item-title>
                                        <CVicon color="red darken-2" :right="true" text="delete" tooltip="Delete pipeline"></CVicon>
                                    </v-list-item-title>
                                </v-list-item>
                                <v-list-item @click="test">
                                    <v-list-item-title>
                                        <CVicon color="#c5c5c5" :right="true" text="mdi-content-copy" tooltip="Duplicate pipeline"></CVicon>
                                    </v-list-item-title>
                                </v-list-item>
                            </v-list>
                        </v-menu>
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
                    <div class="videoClass">
                        <img v-if="cameraList.length > 0" :src="steamAdress">
                        <span v-else>No Cameras Are connected</span>
                    </div>
                    <h5 id="Point">{{point}}</h5>
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
import CVicon from '../components/cv-icon'
    export default {
        name: 'CameraTab',
        components:{
            InputTab,
            ThresholdTab,
            ContoursTab,
            OutputTab,
            CVselect,
            CVicon
        },
        methods:{
        },
        data() {
            return {
                selectedTab:0,
            }
        },
        computed:{
            selectedComponent:{
                get(){
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
            },
            point:{
                 get:function(){
                    let p = this.$store.state.point.calulated;
                    if(p !== undefined){
                        return ("Pitch: " + parseFloat(p['pitch']).toFixed(2) + " Yaw: " + parseFloat(p['yaw']).toFixed(2) + " FPS: " + parseFloat(p['fps']).toFixed(2))
                    } else{
                        return undefined;
                    }
                }
            },
            currentCameraIndex:{
                get(){
                    return this.$store.state.currentCameraIndex;
                },
                set(value){
                    this.$store.commit('currentCameraIndex',value);
                }
            },
            currentPipelineIndex:{
                get(){
                    return this.$store.state.currentPipelineIndex;
                },
                set(value){
                    this.$store.commit('currentPipelineIndex',value);
                }
            },
            cameraList:{
                get(){
                    return this.$store.state.cameraList;
                },
                set(value){
                    this.$store.commit('cameraList',value);
                }
            },
            pipelineList:{
                get(){
                    return this.$store.state.pipelinelist;
                },
                set(value){
                    this.$store.commit('pipelinelist',value);
                }
            },
            pipeline:{
                get(){
                    return this.$store.state.pipeline;
                },
                set(value){
                    this.$store.commit('pipeline',value);
                }
            },
            steamAdress: {
                get: function(){
                    return "http://"+location.hostname + ":"+ this.$store.state.port +"/stream.mjpg";
                }
            },
        }
    }
</script>

<style scoped>
    .colsClass{
        padding: 0 !important;
        
    }
    .videoClass{
        text-align: center;
    }
    .videoClass img{
        height: auto !important;
        width: 75%;
        vertical-align: middle;
    }
    #Point{
        padding-top: 5px;
        text-align: center;
        color: #f4f4f4;
    }
</style>