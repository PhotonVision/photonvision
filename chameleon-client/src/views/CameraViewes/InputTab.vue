<template>
    <div>
        <CVslider name="Exposure" v-model="value.exposure" :min="0" :max="100" @input="handleData('exposure')"/>
        <CVslider name="Brightness" v-model="value.brightness" :min="0" :max="100" @input="handleData('brightness')"/>
        <CVselect name="Orientation" v-model="value.rotationMode" :list="['Normal','90° CW','180°','90° CCW']"
                  @input="handleData('rotationMode')"/>
        <CVselect name="Resolution" v-model="value.VideoModeIndex" :list="resolutionList" @input="handleData('VideoModeIndex')"/>
        <CVselect name="Stream Resolution" v-model="value.streamDivisor"
                  :list="streamResolutionList" @input="handleData('streamDivisor')"/>
    </div>
</template>

<script>
    import CVslider from '../../components/cv-slider'
    import CVselect from '../../components/cv-select'

    export default {
        name: 'Input',
        props: ['value'],
        components: {
            CVslider,
            CVselect,
        },
        methods: {
            handleData(val) {
                this.handleInput(val, this.value[val]);
                this.$emit('update')
            }
        },
        data() {
            return {
                t: 0,
                a: 1
            }
        },
        computed: {
            resolutionList: {
                get() {
                    let tmp_list = [];
                    for (let i of this.$store.state.resolutionList) {
                        tmp_list.push(`${i['width']} X ${i['height']} at ${i['fps']} FPS, ${i['pixelFormat']}`)
                    }
                    return tmp_list;
                }
            },
            streamResolutionList: {
                get() {
                    let cam_res = this.$store.state.resolutionList[this.value.VideoModeIndex];
                    let tmp_list = [];
                    let x = 1;
                    for (let i = 0; i < 4; i++) {
                        tmp_list.push(`${cam_res['width'] / x} X ${cam_res['height'] / x}`);
                        x *= 2;
                    }
                    return tmp_list;
                }
            }
        }
    }
</script>

<style scoped>

</style>