<template>
    <div>
        <CVselect name="Camera" :list="cameraList" v-model="currentCameraIndex"/>
        <CVselect name="Resolution" v-model="cameraSettings.resolution" :list="resolutionList"/>
        <CVselect name="Stream Resolution" v-model="cameraSettings.streamDivisor"
                  :list="streamResolutionList"/>
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
                    let tmp_list = [];
                    for (let i of this.$store.state.resolutionList){
                        tmp_list.push(`${i['width']} X ${i['height']} at ${i['fps']} FPS, ${i['pixelFormat']}`)
                    }
                    return tmp_list;
                }
            },
            streamResolutionList:{
                get(){
                    let cam_res = this.$store.state.resolutionList[this.cameraSettings.resolution];
                    let tmp_list = [];
                    let x = 1;
                    for (let i = 0; i < 4; i++){
                        tmp_list.push(`${cam_res['width']/x} X ${cam_res['height']/x}`);
                        x *= 2;
                    }
                    return tmp_list;
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