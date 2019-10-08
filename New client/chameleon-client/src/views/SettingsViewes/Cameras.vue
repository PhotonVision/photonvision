<template>
    <div>
        <CVselect name="Camera" :list="cameraList"></CVselect>
        <CVselect name="Resulotion" v-model="cameraSettings.resolution" :list="resolutionList"></CVselect>
        <CVselect name="Stream Resulotion" v-model="cameraSettings.streamResolution"  :list="['1:1','1:2','1:4','1:6']"></CVselect>
        <CVnumberinput name="Diagonal FOV" v-model="cameraSettings.fov" ></CVnumberinput>
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
        },
        computed:{
            currentCameraIndex:{
                get(){
                    return this.$store.state.currentCameraIndex;
                },
                set(value){
                    this.$store.commit('currentCameraIndex',value);
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
            resolutionList:{
                get(){
                    return this.$store.state.resolutionList;
                }
            },
            cameraSettings:{
                get(){
                    return this.$store.state.cameraSettings;
                },
                set(value){
                    this.$store.commit('cameraSettings',value);
                }
            },
            
        }
    }
</script>

<style lang="" scoped>
    
</style>