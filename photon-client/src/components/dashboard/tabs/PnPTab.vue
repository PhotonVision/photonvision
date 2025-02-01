<script setup lang="ts">
import PvSelect from "@/components/common/pv-select.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { TargetModel } from "@/types/PipelineTypes";
import PvSlider from "@/components/common/pv-slider.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";

const interactiveCols = computed(() =>
  (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) &&
  (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)
    ? 9
    : 8
);
</script>

<template>
  <div>
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.targetModel"
      label="Target Model"
      :items="[
        { name: '2016 High Goal', value: TargetModel.StrongholdHighGoal },
        { name: '2019 Dual Target', value: TargetModel.DeepSpaceDualTarget },
        { name: '2020 High Goal Outer', value: TargetModel.InfiniteRechargeHighGoalOuter },
        { name: '2020 Power Cell (7in)', value: TargetModel.CircularPowerCell7in },
        { name: '2022 Cargo Ball (9.5in)', value: TargetModel.RapidReactCircularCargoBall },
        { name: '2023 AprilTag 6in (16h5)', value: TargetModel.AprilTag6in_16h5 },
        { name: '2024 AprilTag 6.5in (36h11)', value: TargetModel.AprilTag6p5in_36h11 },
        { name: '2025 Algae (16.25in)', value: TargetModel.ReefscapeAlgae }
      ]"
      :select-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ targetModel: value }, false)"
    />
    <pv-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cornerDetectionAccuracyPercentage"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Contour simplification Percentage"
      :min="0"
      :max="100"
      @input="
        (value) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ cornerDetectionAccuracyPercentage: value }, false)
      "
    />
  </div>
</template>
