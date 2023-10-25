<script setup lang="ts">
import PvSelect from "@/components/common/pv-select.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType, RobotOffsetPointMode } from "@/types/PipelineTypes";
import PvSwitch from "@/components/common/pv-switch.vue";
import { computed, getCurrentInstance } from "vue";
import { RobotOffsetType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";

const isTagPipeline = computed(
  () =>
    useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag ||
    useCameraSettingsStore().currentPipelineType === PipelineType.Aruco
);

interface MetricItem {
  header: string;
  value?: string;
}

const offsetPoints = computed<MetricItem[]>(() => {
  switch (useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode) {
    case RobotOffsetPointMode.Single:
      const value = Object.values(useCameraSettingsStore().currentPipelineSettings.offsetSinglePoint);
      return [{ header: "Offset Point", value: `(${value[0].toFixed(2)}°, ${value[1].toFixed(2)}°)` }];
    case RobotOffsetPointMode.Dual:
      const firstPoint = Object.values(useCameraSettingsStore().currentPipelineSettings.offsetDualPointA);
      const firstPointArea = useCameraSettingsStore().currentPipelineSettings.offsetDualPointAArea;
      const secondPoint = Object.values(useCameraSettingsStore().currentPipelineSettings.offsetDualPointB);
      const secondPointArea = useCameraSettingsStore().currentPipelineSettings.offsetDualPointBArea;
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

const currentPipelineSettings = useCameraSettingsStore().currentPipelineSettings;

const interactiveCols = computed(
  () =>
    (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) &&
    (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)
)
  ? 9
  : 8;
</script>

<template>
  <div>
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOffsetPointEdge"
      label="Target Offset Point"
      tooltip="Changes where the 'center' of the target is (used for calculating e.g. pitch and yaw)"
      :items="['Center', 'Top', 'Bottom', 'Left', 'Right']"
      :select-cols="interactiveCols"
      @input="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOffsetPointEdge: value }, false)
      "
    />
    <pv-select
      v-if="!isTagPipeline"
      v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOrientation"
      label="Target Orientation"
      tooltip="Used to determine how to calculate target landmarks (e.g. the top, left, or bottom of the target)"
      :items="['Portrait', 'Landscape']"
      :select-cols="interactiveCols"
      @input="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOrientation: value }, false)
      "
    />
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.outputShowMultipleTargets"
      label="Show Multiple Targets"
      tooltip="If enabled, up to five targets will be displayed and sent via PhotonLib, instead of just one"
      :disabled="isTagPipeline"
      :switch-cols="interactiveCols"
      @input="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ outputShowMultipleTargets: value }, false)
      "
    />
    <pv-switch
      v-if="
        (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
          currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
      "
      v-model="currentPipelineSettings.doMultiTarget"
      label="Do Multi-Target Estimation"
      tooltip="If enabled, all visible fiducial targets will be used to provide a single pose estimate from their combined model."
      :switch-cols="interactiveCols"
      :disabled="!isTagPipeline"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ doMultiTarget: value }, false)"
    />
    <pv-switch
      v-if="
        (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
          currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
      "
      v-model="currentPipelineSettings.doSingleTargetAlways"
      label="Always Do Single-Target Estimation"
      tooltip="If disabled, visible fiducial targets used for multi-target estimation will not also be used for single-target estimation."
      :switch-cols="interactiveCols"
      :disabled="!isTagPipeline || !currentPipelineSettings.doMultiTarget"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ doSingleTargetAlways: value }, false)"
    />
    <v-divider />
    <table
      v-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode !== RobotOffsetPointMode.None"
      class="metrics-table mt-3 mb-3"
    >
      <tr>
        <th v-for="(item, itemIndex) in offsetPoints" :key="itemIndex" class="metric-item metric-item-title">
          {{ item.header }}
        </th>
      </tr>
      <tr>
        <td v-for="(item, itemIndex) in offsetPoints" :key="itemIndex" class="metric-item">
          {{ item.value }}
        </td>
      </tr>
    </table>
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode"
      label="Robot Offset Mode"
      tooltip="Used to add an arbitrary offset to the location of the targeting crosshair"
      :items="['None', 'Single Point', 'Dual Point']"
      :select-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ offsetRobotOffsetMode: value }, false)"
    />
    <v-row
      v-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode !== RobotOffsetPointMode.None"
      align="center"
      justify="start"
    >
      <v-row
        v-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Single"
      >
        <v-col>
          <v-btn
            small
            color="accent"
            style="width: 100%"
            class="black--text"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Single)"
          >
            Take Point
          </v-btn>
        </v-col>
      </v-row>
      <v-row
        v-else-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Dual"
      >
        <v-col>
          <v-btn
            small
            color="accent"
            style="width: 100%"
            class="black--text"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.DualFirst)"
          >
            Take First Point
          </v-btn>
        </v-col>
        <v-col>
          <v-btn
            small
            color="accent"
            style="width: 100%"
            class="black--text"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.DualSecond)"
          >
            Take Second Point
          </v-btn>
        </v-col>
      </v-row>
      <v-col>
        <v-btn
          small
          color="yellow darken-3"
          style="width: 100%"
          @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Clear)"
        >
          Clear All Points
        </v-btn>
      </v-col>
    </v-row>
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
  text-decoration-color: #ffd843;
}
</style>
