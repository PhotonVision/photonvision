<template>
    <div>
        <CVselect name="Camera" :list="cameraList" v-model="currentCameraIndex"/>
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
                const self = this;
                this.axios.post("http://" + this.$address + "/api/settings/camera", this.cameraSettings).then(
                    function (response) {
                        if (response.status === 200) {
                            self.$store.state.saveBar = true;
                        }
                    }
                )
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