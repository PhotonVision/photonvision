<template>
    <div>
        <CVslider
                v-model="exposure"
                name="Exposure"
                :min="0"
                :max="100"
                @input="handlePipelineData('exposure')"
                @rollback="e => rollback('exposure', e)"
        />
        <CVslider
                v-model="brightness"
                name="Brightness"
                :min="0"
                :max="100"
                @input="handlePipelineData('brightness')"
                @rollback="e => rollback('brightness', e)"
        />
        <CVslider
                v-if="gain !== -1"
                v-model="gain"
                name="Gain"
                :min="0"
                :max="100"
                @input="handlePipelineData('gain')"
                @rollback="e => rollback('gain', e)"
        />
        <CVselect
                v-model="orientationMode"
                name="Orientation"
                :list="['Normal','90° CW','180°','90° CCW']"
                @input="handlePipelineData('rotationMode')"
                @rollback="e => e => rollback('rotationMode',e)"
        />
        <CVselect
                v-model="videoModeIndex"
                name="Resolution"
                :list="resolutionList"
                @input="handlePipelineData('videoModeIndex')"
                @rollback="e => rollback('videoModeIndex', e)"
        />
        <CVselect
                v-model="streamDivisor"
                name="Stream Resolution"
                :list="streamResolutionList"
                @input="handlePipelineData('streamDivisor')"
                @rollback="e => rollback('streamDivisor', e)"
        />
    </div>
</template>

<script>
    import CVslider from '../../components/common/cv-slider'
    import CVselect from '../../components/common/cv-select'

    export default {
        name: 'Input',
        components: {
            CVslider,
            CVselect,
        },
        // eslint-disable-next-line vue/require-prop-types
        props: ['value'],
        data() {
            return {}
        },
        computed: {
            exposure: {
                get() {
                    return this.$store.getters.currentPipelineSettings.exposure
                },
                set(val) {
                    this.$store.commit("exposure", val);
                }
            },
            brightness: {
                get() {
                    return this.$store.getters.currentPipelineSettings.brightness
                },
                set(val) {
                    this.$store.commit("brightness", val);
                }
            },
            gain: {
                get() {
                    return this.$store.getters.currentPipelineSettings.gain
                },
                set(val) {
                    this.$store.commit("gain", val);
                }
            },
            orientationMode: {
                get() {
                    return this.$store.getters.currentPipelineSettings.rotationMode
                },
                set(val) {
                    this.$store.commit("rotationMode", val);
                }
            },
            videoModeIndex: {
                get() {
                    return this.$store.getters.currentPipelineSettings.videoModeIndex
                },
                set(val) {
                    this.$store.commit("videoModeIndex", val);
                }
            },
            streamDivisor: {
                get() {
                    return this.$store.getters.currentPipelineSettings.streamDivisor
                },
                set(val) {
                    this.$store.commit("streamDivisor", val);
                }
            },

            resolutionList: {
                get() {
                    let tmp_list = [];
                    for (let i of this.$store.getters.videoFormatList) {
                        tmp_list.push(`${i['width']} X ${i['height']} at ${i['fps']} FPS, ${i['pixelFormat']}`)
                    }
                    return tmp_list;
                }
            },

            streamResolutionList: {
                get() {
                    let cam_res = this.$store.getters.videoFormatList[
                        this.$store.getters.currentCameraSettings.currentPipelineSettings.cameraVideoModeIndex]
                    let tmp_list = [];
                    tmp_list.push(`${Math.floor(cam_res['width'])} X ${Math.floor(cam_res['height'])}`);
                    for (let x = 2; x <= 6; x += 2) {
                        tmp_list.push(`${Math.floor(cam_res['width'] / x)} X ${Math.floor(cam_res['height'] / x)}`);
                    }
                    return tmp_list;
                }
            }
        },
        methods: {}
    }
</script>

<style scoped>

</style>