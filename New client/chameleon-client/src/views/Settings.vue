<template>
    <div>
        <v-row>
            <v-col cols="6" class="colsClass">
                <v-tabs v-model="selectedTab" background-color="#212121" dark height="50" fixed-tabs slider-color="#4baf62">
                    <v-tab to="">General</v-tab>
                    <v-tab to="">Cameras</v-tab>
                </v-tabs>

            </v-col>
            <v-col v-if="selectedTab === 1">
                <img class="videoClass" src="https://pbs.twimg.com/profile_images/846659478120366082/K-kZVvT8_400x400.jpg">
            </v-col>
      </v-row>
    </div>
</template>

<script>
    export default {
        name: 'SettingsTab',
        data() {
            return {
                selectedTab:0
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
</style>