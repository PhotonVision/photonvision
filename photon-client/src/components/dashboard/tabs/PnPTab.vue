<script setup lang="ts">
import CvSelect from "@/components/common/cv-select.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { TargetModel } from "@/types/PipelineTypes";
import CvSlider from "@/components/common/cv-slider.vue";
import { computed, getCurrentInstance } from "vue";
import { useStateStore } from "@/stores/StateStore";

const interactiveCols = computed(() => (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)) ? 9 : 8;
</script>

<template>
  <div>
    <cv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.targetModel"
      label="Target Model"
      :items="[
        {name: '2020 High Goal Outer', value: TargetModel.InfiniteRechargeHighGoalOuter},
        {name: '2020 High Goal Inner', value: TargetModel.InfiniteRechargeHighGoalInner},
        {name: '2019 Dual Target', value: TargetModel.DeepSpaceDualTarget},
        {name: '2020 Power Cell (7in)', value: TargetModel.CircularPowerCell7in},
        {name: '2022 Cargo Ball (9.5in)', value: TargetModel.RapidReactCircularCargoBall},
        {name: '2016 High Goal', value: TargetModel.StrongholdHighGoal},
        {name: '200mm AprilTag', value: TargetModel.Apriltag_200mm},
        {name: '6in (16h5) Aruco', value: TargetModel.Aruco6in_16h5},
        {name: '6in (16h5) AprilTag', value: TargetModel.Apriltag6in_16h5}
      ]"
      :select-cols="interactiveCols"
      @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({targetModel: value}, false)"
    />
    <cv-slider
      v-model="useCameraSettingsStore().currentPipelineSettings.cornerDetectionAccuracyPercentage"
      class="pt-2"
      :slider-cols="interactiveCols"
      label="Contour simplification Percentage"
      :min="0"
      :max="100"
      @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({cornerDetectionAccuracyPercentage: value}, false)"
    />
  </div>
</template>
