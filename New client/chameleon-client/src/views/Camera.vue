<template>
    <div>
        <v-row >
            <v-col cols="6" class="colsClass">
                <v-tabs fixed-tabs background-color="#212121" dark height="50" slider-color="#4baf62">
                    <v-tab to="">Input</v-tab>
                    <v-tab to="">Threshold</v-tab>
                    <v-tab to="">Contours</v-tab>
                    <v-tab to="">Output</v-tab>
                </v-tabs>
                <div>
                    
                </div>
            </v-col>
            <v-col cols="6">
                <img class="videoClass" src="https://pbs.twimg.com/profile_images/846659478120366082/K-kZVvT8_400x400.jpg">
            </v-col>
      </v-row>
    </div>
</template>

<script>
    export default {
        name: '',
        data() {
            return {
                
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
</style>