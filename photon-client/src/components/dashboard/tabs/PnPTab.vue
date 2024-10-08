<script setup lang="ts">
import { TargetModel, UserPipelineSettings } from "@/types/PipelineTypes";
import { computed } from "vue";
import { useDisplay } from "vuetify";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import { CameraConfig } from "@/types/SettingTypes";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
  pipelineIndex: number
}>();

const targetPipelineSettings = computed<UserPipelineSettings>(() => props.cameraSettings.pipelineSettings.find((v) => v.pipelineIndex === props.pipelineIndex) as UserPipelineSettings);

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(() => mdAndDown.value && (!clientStore.sidebarFolded || serverStore.isDriverMode) ? 3 : 5);
</script>

<template>
  <div>
    <pv-dropdown
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
      :model-value="targetPipelineSettings.targetModel"
      @update:model-value="
        (value: number) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { targetModel: value }, true, true)
      "
    />
    <pv-number-slider
      class="pt-2"
      label="Contour Simplification Percentage"
      :label-cols="labelCols"
      :max="100"
      :min="0"
      :model-value="targetPipelineSettings.cornerDetectionAccuracyPercentage"
      :step="1"
      @update:model-value="
        (value) =>
          serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { cornerDetectionAccuracyPercentage: value }, true, true)
      "
    />
  </div>
</template>
