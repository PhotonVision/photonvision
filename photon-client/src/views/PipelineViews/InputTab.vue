<template>
  <div>
    <CVslider
      v-model="cameraExposure"
      name="Exposure"
      :min="0"
      :max="100"
      @input="handlePipelineData('cameraExposure')"
      @rollback="e => rollback('cameraExposure', e)"
    />
    <CVslider
      v-model="cameraBrightness"
      name="Brightness"
      :min="0"
      :max="100"
      @input="handlePipelineData('cameraBrightness')"
      @rollback="e => rollback('cameraBrightness', e)"
    />
    <CVslider
      v-if="cameraGain !== -1"
      v-model="cameraGain"
      name="Gain"
      :min="0"
      :max="100"
      @input="handlePipelineData('cameraGain')"
      @rollback="e => rollback('cameraGain', e)"
    />
    <CVselect
      v-model="inputImageRotationMode"
      name="Orientation"
      :list="['Normal','90° CW','180°','90° CCW']"
      @input="handlePipelineData('inputImageRotationMode')"
      @rollback="e => rollback('inputImageRotationMode',e)"
    />
    <CVselect
      v-model="cameraVideoModeIndex"
      name="Resolution"
      :list="resolutionList"
      @input="handlePipelineData('cameraVideoModeIndex')"
      @rollback="e => rollback('cameraVideoModeIndex', e)"
    />
    <CVselect
      v-model="outputFrameDivisor"
      name="Stream Resolution"
      :list="streamResolutionList"
      @input="handlePipelineData('outputFrameDivisor')"
      @rollback="e => rollback('outputFrameDivisor', e)"
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
            cameraExposure: {
                get() {
                    return parseInt(this.$store.getters.currentPipelineSettings.cameraExposure);
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"cameraExposure": parseInt(val)});
                }
            },
            cameraBrightness: {
                get() {
                    return parseInt(this.$store.getters.currentPipelineSettings.cameraBrightness)
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"cameraBrightness": parseInt(val)});
                }
            },
            cameraGain: {
                get() {
                    return parseInt(this.$store.getters.currentPipelineSettings.cameraGain)
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"cameraGain": parseInt(val)});
                }
            },
            inputImageRotationMode: {
                get() {
                    return this.$store.getters.currentPipelineSettings.inputImageRotationMode
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"inputImageRotationMode": val});
                }
            },
            cameraVideoModeIndex: {
                get() {
                    return this.$store.getters.currentPipelineSettings.cameraVideoModeIndex
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"cameraVideoModeIndex": val});
                }
            },
            outputFrameDivisor: {
                get() {
                    return this.$store.getters.currentPipelineSettings.outputFrameDivisor
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"outputFrameDivisor": val});
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