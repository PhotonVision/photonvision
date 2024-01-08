<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType } from "@/types/PipelineTypes";
import PvSelect from "@/components/common/pv-select.vue";
import PvSlider from "@/components/common/pv-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = useCameraSettingsStore().currentPipelineSettings;

const interactiveCols = computed(
  () =>
    (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) &&
    (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)
)
  ? 9
  : 8;

const models = () => {
  return ["a", "bcd"]
}
</script>

<template>
  <div v-if="currentPipelineSettings.pipelineType === PipelineType.Rknn">
    <pv-slider
      v-model="currentPipelineSettings.cameraBrightness"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Confidence"
      tooltip="asdf"
      :min="0"
      :max="1"
      :step="-1"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ cameraExposure: value }, false)"
    />
  </div>
</template>