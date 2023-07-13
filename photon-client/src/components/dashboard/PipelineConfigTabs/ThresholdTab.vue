<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { computed } from "vue";
import CvRangeSlider from "@/components/common/cv-range-slider.vue";
import CvSwitch from "@/components/common/cv-switch.vue";

const averageHue = computed<number>(() => {
  const isHueInverted = useCameraSettingsStore().currentPipelineSettings.hueInverted;
  let val = Object.values(useCameraSettingsStore().currentPipelineSettings.hsvHue).reduce((a, b) => a + b, 0);
  
  if(isHueInverted) val += 180;
  if (val > 360) val -= 360;
  
  return val;
});

// TODO fix cv-range-slider so that store access doesn't need to be deferred
const hsvHue = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.hsvHue) as [number, number],
  set: v => useCameraSettingsStore().currentPipelineSettings.hsvHue = v
});
const hsvSaturation = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.hsvSaturation) as [number, number],
  set: v => useCameraSettingsStore().currentPipelineSettings.hsvSaturation = v
});
const hsvValue = computed<[number, number]>({
  get: () => Object.values(useCameraSettingsStore().currentPipelineSettings.hsvValue) as [number, number],
  set: v => useCameraSettingsStore().currentPipelineSettings.hsvValue = v
});
</script>

<template>
  <div class="threshold-modifiers" :style="{'--averageHue': averageHue}">
    <cv-range-slider
      id="hue-slider"
      v-model="hsvHue"
      :class="useCameraSettingsStore().currentPipelineSettings.hueInverted ? 'inverted-slider' : 'normal-slider'"
      label="Hue"
      tooltip="Describes color"
      :min="0"
      :max="180"
      :inverted="useCameraSettingsStore().currentPipelineSettings.hueInverted"
      @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({hsvHue: value}, false)"
    />
    <cv-range-slider
        id="sat-slider"
        v-model="hsvSaturation"
        class="normal-slider"
        label="Saturation"
        tooltip="Describes colorfulness; the smaller this value the 'whiter' the color becomes"
        :min="0"
        :max="255"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({hsvSaturation: value}, false)"
    />
    <cv-range-slider
        id="value-slider"
        v-model="hsvValue"
        class="normal-slider"
        label="Value"
        tooltip="Describes lightness; the smaller this value the 'blacker' the color becomes"
        :min="0"
        :max="255"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({hsvValue: value}, false)"
    />
    <cv-switch
        v-model="useCameraSettingsStore().currentPipelineSettings.hueInverted"
        label="Invert hue"
        tooltip="Selects the hue range outside of the hue slider bounds instead of inside"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({hueInverted: value}, false)"
    />
  </div>
</template>

<style scoped lang="css">
.threshold-modifiers {
  --averageHue: 0;
}
#hue-slider >>> .v-slider {
  background: linear-gradient( to right, #f00 0%, #ff0 17%, #0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100% );
  border-radius: 10px;
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
}
#sat-slider >>> .v-slider {
  background: linear-gradient( to right, #fff 0%, hsl(var(--averageHue), 100%, 50%) 100% );
  border-radius: 10px;
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
}
#value-slider >>> .v-slider {
  background: linear-gradient( to right, #000 0%, hsl(var(--averageHue), 100%, 50%) 100% );
  border-radius: 10px;
  box-shadow: 0 0 5px #333, inset 0 0 3px #333;
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
