<template>
  <div :style="{'--averageHue': averageHue}">
    <CVrangeSlider
      id="hue-slider"
      v-model="hsvHue"
      :class="hueInverted ? 'inverted-slider' : 'normal-slider'"
      name="Hue"
      tooltip="Describes color"
      :min="0"
      :max="180"
      :inverted="hueInverted"
      @input="handlePipelineData('hsvHue')"
      @rollback="e => rollback('hue',e)"
    />
    <CVrangeSlider
      id="sat-slider"
      v-model="hsvSaturation"
      class="normal-slider"
      name="Saturation"
      tooltip="Describes colorfulness; the smaller this value the 'whiter' the color becomes"
      :min="0"
      :max="255"
      @input="handlePipelineData('hsvSaturation')"
      @rollback="e => rollback('saturation',e)"
    />
    <CVrangeSlider
      id="value-slider"
      v-model="hsvValue"
      class="normal-slider"
      name="Value"
      tooltip="Describes lightness; the smaller this value the 'blacker' the color becomes"
      :min="0"
      :max="255"
      @input="handlePipelineData('hsvValue')"
      @rollback="e => rollback('value',e)"
    />
    <CVSwitch
      v-model="hueInverted"
      name="Invert hue"
      tooltip="Selects the hue range outside of the hue slider bounds instead of inside"
      @input="handlePipelineData('hueInverted')"
      @rollback="e => rollback('hueInverted',e)"
    />
    <template v-if="currentPipelineType() === 3">
      <CVSwitch
        v-model="erode"
        name="Erode"
        tooltip="Removes pixels around the edges of white areas in the thresholded image"
        @input="handlePipelineData('erode')"
        @rollback="e => rollback('erode',e)"
      />
      <CVSwitch
        v-model="dilate"
        class="mb-0"
        name="Dilate"
        tooltip="Adds pixels around the edges of white areas in the thresholded image"
        @input="handlePipelineData('dilate')"
        @rollback="e => rollback('dilate',e)"
      />
    </template>
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
          @click="setFunction(hueInverted ? 2 : 3)"
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
          {{ hueInverted ? "Exclude" : "Set to" }} Average
        </v-btn>
        <v-btn
          color="accent"
          class="ma-2 black--text"
          small
          @click="setFunction(hueInverted ? 3: 2)"
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
    <v-divider class="mb-3" />
  </div>
</template>

<script>
import CVrangeSlider from '../../components/common/cv-range-slider'
import CVSwitch from "@/components/common/cv-switch";

export default {
  name: 'Threshold',
  components: {
    CVSwitch,
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
    averageHue: {
      get() {
        var isInverted = this.$store.getters.currentPipelineSettings.hueInverted;
        const arr = this.$store.getters.currentPipelineSettings.hsvHue;
        var retVal = 0;

        if (Array.isArray(arr)) {
          retVal = (arr[0] + arr[1]);
        } else {
          retVal = (arr.first + arr.second);
        }

        if(isInverted){
          retVal += 180;
        }

        if(retVal > 360){
          retVal -= 360;
        }

        return retVal;

      },
    },
    hueInverted: {
      get() {
        return this.$store.getters.currentPipelineSettings.hueInverted;
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"hueInverted": val});
      }
    },
    hsvSaturation: {
      get() {
        return this.$store.getters.currentPipelineSettings.hsvSaturation;
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"hsvSaturation": val})
      }
    },
    hsvValue: {
      get() {
        return this.$store.getters.currentPipelineSettings.hsvValue;
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"hsvValue": val});
      }
    },
    erode: {
      get() {
        return this.$store.getters.currentPipelineSettings.erode;
      },
      set(val) {
        this.$store.commit("mutatePipeline", {"erode": val});
      }
    },
    dilate: {
      get() {
        return this.$store.getters.currentPipelineSettings.dilate;
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
        this.$store.state.websocket.ws.send(msg);
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
      this.$store.commit("mutatePipeline", {"inputShouldShow": true});
      this.handlePipelineUpdate("inputShouldShow", true);
    }
  }
}

</script>

<style lang="css" scoped>
#hue-slider >>> .v-slider {
  background: linear-gradient( to right, #f00 0%, #ff0 17%, #0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100% );
  border-radius: 10px;
  box-shadow: 0px 0px 5px #333, inset 0px 0px 3px #333;
}
#sat-slider >>> .v-slider {
  background: linear-gradient( to right, #fff 0%, hsl(var(--averageHue), 100%, 50%) 100% );
  border-radius: 10px;
  box-shadow: 0px 0px 5px #333, inset 0px 0px 3px #333;
}
#value-slider >>> .v-slider {
  background: linear-gradient( to right, #000 0%, hsl(var(--averageHue), 100%, 50%) 100% );
  border-radius: 10px;
  box-shadow: 0px 0px 5px #333, inset 0px 0px 3px #333;
}
>>> .v-slider__thumb {
  outline: black solid thin;
}
.normal-slider >>> .v-slider__track-fill {
    outline: black solid thin;
}

.inverted-slider >>> .v-slider__track-background {
  outline: black solid thin;
}
</style>
