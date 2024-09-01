<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { TargetModel } from "@/types/PipelineTypes";
import { computed } from "vue";
import { useStateStore } from "@/stores/StateStore";
import { useDisplay } from "vuetify";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvDropdown from "@/components/common/pv-dropdown.vue";

const { mdAndDown } = useDisplay();
const labelCols = computed(
  () => 12 - (mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8)
);
</script>

<template>
  <div>
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.targetModel"
      :items="[
        { name: '2016 High Goal', value: TargetModel.StrongholdHighGoal },
        { name: '2019 Dual Target', value: TargetModel.DeepSpaceDualTarget },
        { name: '2020 High Goal Outer', value: TargetModel.InfiniteRechargeHighGoalOuter },
        { name: '2020 Power Cell (7in)', value: TargetModel.CircularPowerCell7in },
        { name: '2022 Cargo Ball (9.5in)', value: TargetModel.RapidReactCircularCargoBall },
        { name: '2023 AprilTag 6in (16h5)', value: TargetModel.AprilTag6in_16h5 },
        { name: '2024 AprilTag 6.5in (36h11)', value: TargetModel.AprilTag6p5in_36h11 }
      ]"
      label="Target Model"
      :label-cols="labelCols"
      @update:model-value="
        (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ targetModel: value }, false)
      "
    />
    <pv-number-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cornerDetectionAccuracyPercentage"
      class="pt-2"
      label="Contour Simplification Percentage"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :step="1"
      @update:model-value="
        (value) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ cornerDetectionAccuracyPercentage: value }, false)
      "
    />
  </div>
</template>
