<template>
  <div>
    <CVrangeSlider
      v-model="hsvHue"
      name="Hue"
      tooltip="Describes color"
      :min="0"
      :max="180"
      @input="handlePipelineData('hsvHue')"
      @rollback="e => rollback('hue',e)"
    />
    <CVrangeSlider
      v-model="hsvSaturation"
      name="Saturation"
      tooltip="Describes colorfulness; the smaller this value the 'whiter' the color becomes"
      :min="0"
      :max="255"
      @input="handlePipelineData('hsvSaturation')"
      @rollback="e => rollback('saturation',e)"
    />
    <CVrangeSlider
      v-model="hsvValue"
      name="Value"
      tooltip="Describes lightness; the smaller this value the 'blacker' the color becomes"
      :min="0"
      :max="255"
      @input="handlePipelineData('hsvValue')"
      @rollback="e => rollback('value',e)"
    />
    <div class="pt-3 white--text">
      Color Picker
    </div>
    <v-divider
      class="mt-3"
    />
    <v-row
      justify="center"
      class="mt-3 mb-3"
    >
      <template v-if="!$store.state.colorPicking">
        <v-btn
          color="accent"
          class="ma-2 black--text"
          small
          @click="setFunction(3)"
        >
          <v-icon left>
            mdi-minus
          </v-icon>
          Shrink Range
        </v-btn>
        <v-btn
          color="accent"
          class="ma-2 black--text"
          small
          @click="setFunction(1)"
        >
          <v-icon left>
            mdi-plus-minus
          </v-icon>
          Set To Average
        </v-btn>
        <v-btn
          color="accent"
          class="ma-2 black--text"
          small
          @click="setFunction(2)"
        >
          <v-icon left>
            mdi-plus
          </v-icon>
          Expand Range
        </v-btn>
      </template>
      <template v-else>
        <v-btn
          color="accent"
          class="ma-2 black--text"
          style="width: 30%;"
          small
          @click="setFunction(0)"
        >
          Cancel
        </v-btn>
      </template>
    </v-row>
  </div>
</template>

<script>
    import CVrangeSlider from '../../components/common/cv-range-slider'
    
    export default {
        name: 'Threshold',
        components: {
            CVrangeSlider
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
            }
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
                    this.colorPicker.initColorPicker();

                    let s = this.$store.getters.currentPipelineSettings;
                    let hsvArray = this.colorPicker.colorPickerClick(event, this.currentFunction,
                        [
                                [s.hsvHue[0], s.hsvSaturation[0], s.hsvValue[0]],
                                [s.hsvHue[1], s.hsvSaturation[1], s.hsvValue[1]]
                        ].map(hsv => hsv.map(it => it || 0)));
                    // That `map` calls are to make sure that we don't let any undefined/null values slip in
                    this.currentFunction = undefined;
                    this.$store.state.colorPicking = false;
                    this.handlePipelineUpdate("outputShouldDraw", true);

                    s.hsvHue = [hsvArray[0][0], hsvArray[1][0]];
                    s.hsvSaturation = [hsvArray[0][1], hsvArray[1][1]];
                    s.hsvValue = [hsvArray[0][2], hsvArray[1][2]];

                    let msg = this.$msgPack.encode({
                        "changePipelineSetting": {
                            'hsvHue': s.hsvHue,
                            'hsvSaturation': s.hsvSaturation,
                            'hsvValue': s.hsvValue,
                            'cameraIndex': this.$store.state.currentCameraIndex
                        }
                    });
                    this.$socket.send(msg);
                    this.$emit('update');
                }
            },
            setFunction(index) {
                switch (index) {
                    case 0:
                        this.currentFunction = undefined;
                        this.$store.state.colorPicking = false;
                        this.handlePipelineUpdate("outputShouldDraw", true);
                        return;
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
                this.$store.state.colorPicking = true;
                this.handlePipelineUpdate("outputShouldDraw", false);
            }
        }
    }

</script>