<template>
    <div>
        <CVselect name="Camera" :list="cameraList" v-model="currentCameraIndex"/>
        <CVselect name="Resolution" v-model="cameraSettings.resolution" :list="resolutionList"/>
        <CVselect name="Stream Resolution" v-model="cameraSettings.streamDivisor"
                  :list="['1:1','1:2','1:4','1:6']"/>
        <CVnumberinput name="Diagonal FOV" v-model="cameraSettings.fov"/>
        <v-btn style="margin-top:10px" small color="#4baf62" @click="sendCameraSettings">Save Camera Settings</v-btn>
    </div>
</template>

<script>
    import CVselect from '../../components/cv-select'
    import CVnumberinput from '../../components/cv-number-input'

    export default {
        name: 'CameraSettings',
        components: {
            CVselect,
            CVnumberinput
        },
        data() {
            return {}
        },
        methods: {
            sendCameraSettings() {
                this.handleInput('cameraSettings', this.cameraSettings);
            },

        },
        computed: {

            currentCameraIndex: {
                get() {
                    return this.$store.state.currentCameraIndex;
                },
                set(value) {
                    this.$store.commit('currentCameraIndex', value);
                }
            },
            cameraList: {
                get() {
                    return this.$store.state.cameraList;
                },
                set(value) {
                    this.$store.commit('cameraList', value);
                }
            },
            resolutionList: {
                get() {
                    return this.$store.state.resolutionList;
                }
            },
            cameraSettings: {
                get() {
                    return this.$store.state.cameraSettings;
                },
                set(value) {
                    this.$store.commit('cameraSettings', value);
                }
            },

        }
    }
</script>

<style lang="" scoped>

</style>