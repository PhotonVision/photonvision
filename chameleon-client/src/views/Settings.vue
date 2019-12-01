<template>
    <div>
        <v-row>
            <v-col class="colsClass" cols="6">
                <v-tabs background-color="#212121" dark fixed-tabs height="50" slider-color="#4baf62"
                        v-model="selectedTab">
                    <v-tab to="">General</v-tab>
                    <v-tab to="">Cameras</v-tab>
                </v-tabs>
                <div style="padding-left:30px">
                    <component :is="selectedComponent" @update="$emit('save')"/>
                </div>
            </v-col>
            <v-col class="colsClass" v-show="selectedTab === 1 || selectedTab === 2">
                <div class="videoClass">
                    <img :src="streamAddress" alt="Camera Stream">
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
        components: {
            General,
            Cameras,
        },
        data() {
            return {
                selectedTab: 0,
            }
        },
        computed: {
            selectedComponent: {
                get() {
                    switch (this.selectedTab) {
                        case 0:
                            return "General";
                        case 1:
                            return "Cameras";
                    }
                    return "";
                }
            },
            streamAddress: {
                get: function () {
                    return "http://" + location.hostname + ":" + this.$store.state.port + "/stream.mjpg";
                }
            },
        }
    }
</script>

<style scoped>
    .videoClass {
        text-align: center;
    }

    .videoClass img {
        padding-top: 10px;
        height: auto !important;
        width: 75%;
        vertical-align: middle;
    }

    .colsClass {
        padding: 0 !important;
    }
</style>