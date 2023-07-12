<script setup lang="ts">
import CvSelect from "@/components/common/cv-select.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { TargetModel } from "@/types/PipelineTypes";
import CvSlider from "@/components/common/cv-slider.vue";
</script>

<template>
  <div>
    <cv-select
        v-model="useCameraSettingsStore().currentPipelineSettings.targetModel"
        label="Target Model"
        :items="[
            {name: '2020 High Goal Outer', value: TargetModel.k2020HighGoalOuter},
            {name: '2020 High Goal Inner', value: TargetModel.k2020HighGoalInner},
            {name: '2019 Dual Target', value: TargetModel.k2019DualTarget},
            {name: '2020 Power Cell (7in)', value: TargetModel.kCircularPowerCell7in},
            {name: '2022 Cargo Ball (9.5in)', value: TargetModel.k2022CircularCargoBall},
            {name: '2016 High Goal', value: TargetModel.k2016HighGoal},
            {name: '200mm AprilTag', value: TargetModel.k200mmAprilTag},
            {name: '6in (16h5) Aruco', value: TargetModel.kAruco6in_16h5},
            {name: '6in (16h5) AprilTag', value: TargetModel.k6in_16h5}
        ]"
        :select-cols="10"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({targetModel: value})"
    />
    <cv-slider
        v-model="useCameraSettingsStore().currentPipelineSettings.cornerDetectionAccuracyPercentage"
        class="pt-2"
        :slider-cols="10"
        label="Contour simplification Percentage"
        :min="0"
        :max="100"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({cornerDetectionAccuracyPercentage: value})"
    />
  </div>
</template>
