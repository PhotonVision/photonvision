<template>
    <div>
        <v-row align="center">
            <v-col cols="6" class="colsClass">
                <v-tabs fixed-tabs background-color="#212121" dark height="50" slider-color="#4baf62" v-model="selectedTab">
                    <v-tab to="">General</v-tab>
                    <v-tab to="">Cameras</v-tab>
                </v-tabs>
                <div style="padding-left:30px">
                    <component :is="selectedComponent"></component>
                </div>
            </v-col>
            <v-col v-show="selectedTab === 1" class="colsClass">
                <div class="videoClass">
                    <img :src="steamAdress">
                </div>
            </v-col>
      </v-row>
    </div>
</template>

<script>
import General from './SettingsViewes/General'
import Cameras from './SettingsViewes/Cameras'

    export default {
        name: 'SettingsTab',
        components:{
            General,
            Cameras,
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
                        return "General";
                    case 1:
                        return "Cameras";
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
    .videoClass{
        text-align: center;
    }
    .videoClass img{
        height: auto !important;
        width: 75%;
        vertical-align: middle;
    }
    .colsClass{
        padding: 0 !important;
    }
</style>