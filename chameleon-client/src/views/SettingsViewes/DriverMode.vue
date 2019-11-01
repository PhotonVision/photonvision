<template>
    <div>
        <CVselect name="Camera" :list="cameraList" v-model="currentCameraIndex"/>
        <CVswitch v-model="driverState.isDriver" name="Driver Mode" @input="sendDriverMode"/>
        <CVslider name="Exposure" v-model="driverState.driverExposure" :min="0" :max="100" @input="sendDriverMode"/>
        <CVslider name="Brightness" v-model="driverState.driverBrightness" :min="0" :max="100"
                  @input="sendDriverMode"/>

    </div>
</template>

<script>
    import CVselect from '../../components/cv-select'
    import CVswitch from '../../components/cv-switch'
    import CVslider from '../../components/cv-slider'

    export default {
        name: "DriverMode",
        components: {
            CVselect,
            CVswitch,
            CVslider
        },
        methods: {
            sendDriverMode() {
                this.handleInput('driverMode', this.driverState);
                this.$emit("update");
            }
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
            driverState: {
                get() {
                    return this.$store.state.driverMode;
                },
                set(value) {
                    this.$store.commit("driverMode", value);
                }
            }
        }
    }
</script>

<style scoped>

</style>