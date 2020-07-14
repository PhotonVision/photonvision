<template>
  <div>
    <CVrangeSlider
      v-model="hsvHue"
      name="Hue"
      :min="0"
      :max="180"
      @input="handlePipelineData('hsvHue')"
      @rollback="e => rollback('hue',e)"
    />
    <CVrangeSlider
      v-model="hsvSaturation"
      name="Saturation"
      :min="0"
      :max="255"
      @input="handlePipelineData('hsvSaturation')"
      @rollback="e => rollback('saturation',e)"
    />
    <CVrangeSlider
      v-model="hsvValue"
      name="Value"
      :min="0"
      :max="255"
      @input="handlePipelineData('hsvValue')"
      @rollback="e => rollback('value',e)"
    />
    <v-divider
      class="mt-3"
    />
    <v-row justify="center">
      <v-btn
        color="accent"
        class="ma-5 black--text"
        small
        @click="setFunction(1)"
      >
        <v-icon>colorize</v-icon>
        Eye drop
      </v-btn>
      <v-btn
        color="accent"
        class="ma-5 black--text"
        small
        @click="setFunction(2)"
      >
        <v-icon>add</v-icon>
        Expand Selection
      </v-btn>
      <v-btn
        color="accent"
        class="ma-5 black--text"
        small
        @click="setFunction(3)"
      >
        <v-icon>remove</v-icon>
        Shrink Selection
      </v-btn>
    </v-row>
    <v-divider />
    <CVswitch
      v-model="erode"
      name="Erode"
      @input="handlePipelineData('erode')"
      @rollback="e => rollback('erode',e)"
    />
    <CVswitch
      v-model="dilate"
      name="Dilate"
      @input="handlePipelineData('dilate')"
      @rollback="e => rollback('dilate',e)"
    />
  </div>
</template>

<script>
    import CVrangeSlider from '../../components/common/cv-range-slider'
    import CVswitch from '../../components/common/cv-switch'

    export default {
        name: 'Threshold',
        components: {
            CVrangeSlider,
            CVswitch
        },
        // eslint-disable-next-line vue/require-prop-types
        props: ['value'],
        data() {
            return {
                currentFunction: undefined,
                colorPicker: undefined,
                showThresholdState: 0
            }
        },
        computed: {
            hsvHue: {
                get() {
                    return this.$store.getters.currentPipelineSettings.hsvHue
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"hsvHue": val})
                }
            },
            hsvSaturation: {
                get() {
                    return this.$store.getters.currentPipelineSettings.hsvSaturation
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"hsvSaturation": val})
                }
            },
            hsvValue: {
                get() {
                    return this.$store.getters.currentPipelineSettings.hsvValue
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"hsvValue": val})
                }
            },
            erode: {
                get() {
                    return this.$store.getters.currentPipelineSettings.erode
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"erode": val});
                }
            },
            dilate: {
                get() {
                    return this.$store.getters.currentPipelineSettings.dilate
                },
                set(val) {
                    this.$store.commit("mutatePipeline", {"dilate": val});
                }
            },
        },
        mounted: function () {
            const self = this;
            this.colorPicker = require('../../plugins/ColorPicker').default;
            this.$nextTick(() => {
                self.colorPicker.initColorPicker();
            });
        },
        methods: {
            onClick(event) {
                if (this.currentFunction !== undefined) {
                    let hsvArray = this.colorPicker.colorPickerClick(event, this.currentFunction,
                        [[this.value.hue[0], this.value.saturation[0], this.value.value[0]], [this.value.hue[1], this.value.saturation[1], this.value.value[1]]]);
                    this.currentFunction = undefined;
                    let msg = this.$msgPack.encode({
                        "changePipelineSetting": {
                            'hsvHue': [hsvArray[0][0], hsvArray[1][0]],
                            'hsvSaturation': [hsvArray[0][1], hsvArray[1][1]],
                            'hsvValue': [hsvArray[0][2], hsvArray[1][2]],
                            'outputShowThresholded': this.showThresholdState,
                            'cameraIndex': this.$store.state.currentCameraIndex
                        }
                    });
                    this.$socket.send(msg);
                    this.$emit('update');
                }
            },
            setFunction(index) {
                this.showThresholdState = this.value.outputShowThresholded;
                if (this.showThresholdState === true) {
                    this.value.outputShowThresholded = false;
                    this.handlePipelineData('outputShowThresholded')
                }
                switch (index) {
                    case 0:
                        this.currentFunction = undefined;
                        break;
                    case 1:
                        this.currentFunction = this.colorPicker.eyeDrop;
                        break;
                    case 2:
                        this.currentFunction = this.colorPicker.expand;
                        break;
                    case 3:
                        this.currentFunction = this.colorPicker.shrink;
                        break;
                }
            }
        }
    }

</script>