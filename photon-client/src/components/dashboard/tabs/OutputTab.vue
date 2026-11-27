<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import {
  FiducialAlgorithm,
  type AprilTagPipelineSettings,
  PipelineType,
  RobotOffsetPointMode,
  ContourTargetOrientation,
  ContourTargetOffsetPointEdge
} from "@/types/PipelineTypes";

import { computed } from "vue";
import { RobotOffsetType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import { useCustomBreakpoints } from "@/lib/Breakpoints";

const settings = computed<AprilTagPipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings as AprilTagPipelineSettings
);
const isTagPipeline = computed(
  () => settings.value.pipelineType === PipelineType.AprilTag
);
const isAruco = computed(() => settings.value.algorithm === FiducialAlgorithm.Aruco);

interface MetricItem {
  header: string;
  value?: string;
}

const offsetPoints = computed<MetricItem[]>(() => {
  switch (settings.value.offsetRobotOffsetMode) {
    case RobotOffsetPointMode.Single:
      const value = Object.values(settings.value.offsetSinglePoint);
      return [{ header: "Offset Point", value: `(${value[0].toFixed(2)}°, ${value[1].toFixed(2)}°)` }];
    case RobotOffsetPointMode.Dual:
      const firstPoint = Object.values(settings.value.offsetDualPointA);
      const firstPointArea = settings.value.offsetDualPointAArea;
      const secondPoint = Object.values(settings.value.offsetDualPointB);
      const secondPointArea = settings.value.offsetDualPointBArea;
      return [
        { header: "First Offset Point", value: `(${firstPoint[0].toFixed(2)}°, ${firstPoint[1].toFixed(2)}°)` },
        { header: "First Offset Point Area", value: `${firstPointArea.toFixed(2)}%` },
        { header: "Second Offset Point", value: `(${secondPoint[0].toFixed(2)}°, ${secondPoint[1].toFixed(2)}°)` },
        { header: "Second Offset Point Area", value: `${secondPointArea.toFixed(2)}%` }
      ];
    default:
    case RobotOffsetPointMode.None:
      return [];
  }
});

const breakpoints = useCustomBreakpoints();
const mdAndDown = breakpoints.smallerOrEqual("md");
const interactiveCols = computed(() =>
  mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 8 : 7
);
</script>

<template>
  <div>
    <pv-slider
      v-model="settings.outputMaximumTargets"
      label="Maximum Targets"
      tooltip="The maximum number of targets to display and send."
      :hidden="isTagPipeline"
      :min="1"
      :max="127"
      :step="1"
      :slider-cols="interactiveCols"
      @update:modelValue="
        (value: number) => useCameraSettingsStore().changeCurrentPipelineSetting({ outputMaximumTargets: value }, false)
      "
    />
    <pv-switch
      v-if="
        settings.pipelineType === PipelineType.AprilTag &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        settings.solvePNPEnabled
      "
      v-model="settings.doMultiTarget"
      label="Do Multi-Target Estimation"
      tooltip="If enabled, all visible fiducial targets will be used to provide a single pose estimate from their combined model."
      :switch-cols="interactiveCols"
      :disabled="!isTagPipeline"
      @update:modelValue="
        (value: boolean | undefined) =>
          value !== undefined && useCameraSettingsStore().changeCurrentPipelineSetting({ doMultiTarget: value }, false)
      "
    />
    <pv-switch
      v-if="
        settings.pipelineType === PipelineType.AprilTag &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        settings.solvePNPEnabled
      "
      v-model="settings.doSingleTargetAlways"
      label="Always Do Single-Target Estimation"
      tooltip="If disabled, visible fiducial targets used for multi-target estimation will not also be used for single-target estimation."
      :switch-cols="interactiveCols"
      :disabled="!isTagPipeline || !settings.doMultiTarget"
      @update:modelValue="
        (value: boolean | undefined) =>
          value !== undefined &&
          useCameraSettingsStore().changeCurrentPipelineSetting({ doSingleTargetAlways: value }, false)
      "
    />
    <pv-select
      v-model="settings.contourTargetOffsetPointEdge"
      label="Target Offset Point"
      tooltip="Changes where the 'center' of the target is (used for calculating e.g. pitch and yaw)"
      :items="[
        { value: ContourTargetOffsetPointEdge.Center, name: 'Center' },
        { value: ContourTargetOffsetPointEdge.Top, name: 'Top' },
        { value: ContourTargetOffsetPointEdge.Bottom, name: 'Bottom' },
        { value: ContourTargetOffsetPointEdge.Left, name: 'Left' },
        { value: ContourTargetOffsetPointEdge.Right, name: 'Right' }
      ]"
      :select-cols="interactiveCols"
      @update:modelValue="
        (value: ContourTargetOffsetPointEdge) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOffsetPointEdge: value }, false)
      "
    />
    <pv-select
      v-if="!isTagPipeline"
      v-model="settings.contourTargetOrientation"
      label="Target Orientation"
      tooltip="Used to determine how to calculate target landmarks (e.g. the top, left, or bottom of the target)"
      :items="[
        { value: ContourTargetOrientation.Portrait, name: 'Portrait' },
        { value: ContourTargetOrientation.Landscape, name: 'Landscape' }
      ]"
      :select-cols="interactiveCols"
      @update:modelValue="
        (value: ContourTargetOrientation) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOrientation: value }, false)
      "
    />
    <pv-select
      v-model="settings.offsetRobotOffsetMode"
      label="Robot Offset Mode"
      tooltip="Used to add an arbitrary offset to the location of the targeting crosshair"
      :items="[
        { value: RobotOffsetPointMode.None, name: 'None' },
        { value: RobotOffsetPointMode.Single, name: 'Single Point' },
        { value: RobotOffsetPointMode.Dual, name: 'Dual Point' }
      ]"
      :select-cols="interactiveCols"
      @update:modelValue="
        (value: RobotOffsetPointMode) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ offsetRobotOffsetMode: value }, false)
      "
    />
    <table
      v-if="settings.offsetRobotOffsetMode !== RobotOffsetPointMode.None"
      class="metrics-table mt-3 mb-3"
    >
      <thead>
        <tr>
          <th v-for="(item, itemIndex) in offsetPoints" :key="itemIndex" class="metric-item metric-item-title">
            {{ item.header }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td v-for="(item, itemIndex) in offsetPoints" :key="itemIndex" class="metric-item">
            {{ item.value }}
          </td>
        </tr>
      </tbody>
    </table>
    <div
      v-if="settings.offsetRobotOffsetMode !== RobotOffsetPointMode.None"
      class="flex items-center"
    >
      <div
        v-if="settings.offsetRobotOffsetMode === RobotOffsetPointMode.Single"
        class="flex flex-wrap p-0"
      >
        <div class="w-1/2 pl-0">
          <pv-button
            size="sm"
            variant="primary"
            block
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Single)"
          >
            Take Point
          </pv-button>
        </div>
        <div class="w-1/2 pr-0">
          <pv-button
            size="sm"
            variant="danger"
            block
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Clear)"
          >
            Clear All Points
          </pv-button>
        </div>
      </div>
      <div
        v-else-if="settings.offsetRobotOffsetMode === RobotOffsetPointMode.Dual"
        class="flex flex-wrap p-0"
      >
        <div class="w-1/2 pr-2 pl-0 lg:w-1/3">
          <pv-button
            size="sm"
            variant="primary"
            block
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.DualFirst)"
          >
            Take First Point
          </pv-button>
        </div>
        <div class="w-1/2 pr-0 pl-2 lg:w-1/3 lg:pr-2">
          <pv-button
            size="sm"
            variant="primary"
            block
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.DualSecond)"
          >
            Take Second Point
          </pv-button>
        </div>
        <div class="w-full pr-0 pl-0 lg:w-1/3 lg:pl-2">
          <pv-button
            size="sm"
            variant="danger"
            block
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Clear)"
          >
            Clear All Points
          </pv-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.metrics-table {
  border-collapse: separate;
  border-spacing: 0;
  border-radius: 5px;
  border: 1px solid white;
  width: 100%;
  text-align: center;
}

.metric-item {
  padding: 1px 15px 1px 10px;
  border-right: 1px solid;
  font-weight: normal;
  color: white;
}

.metric-item-title {
  font-size: 18px;
  text-decoration: underline;
  text-decoration-color: var(--color-pv-primary);
}
</style>
