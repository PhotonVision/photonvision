<template>
    <div>
        <v-row>
            <v-col cols="6" class="colsClass">
                <v-tabs fixed-tabs background-color="#212121" dark height="50" slider-color="#4baf62" v-model="selectedTab">
                    <v-tab to="">General</v-tab>
                    <v-tab to="">Cameras</v-tab>
                </v-tabs>
                <div style="padding-left:30px">
                    <component :is="selectedComponent"></component>
                </div>
            </v-col>
            <v-col v-if="selectedTab === 1" class="colsClass">
                <img class="videoClass" src="https://pbs.twimg.com/profile_images/846659478120366082/K-kZVvT8_400x400.jpg">
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
            Cameras
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
            }
        }
    }
</script>

<style scoped>
    .videoClass{
        display: block;
        margin-right: auto;
        margin-left: auto;
    }
    .colsClass{
        padding: 0 !important;
    }
</style>