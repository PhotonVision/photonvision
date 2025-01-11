<script setup lang="ts">
import PvSelect from "@/components/common/pv-select.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ActivePipelineSettings, PipelineType, RobotOffsetPointMode } from "@/types/PipelineTypes";
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

// TODO fix pipeline typing in order to fix this, the store settings call should be able to infer that only valid pipeline type settings are exposed based on pre-checks for the entire config section
// Defer reference to store access method
const currentPipelineSettings = computed<ActivePipelineSettings>(
  () => useCameraSettingsStore().currentPipelineSettings
);

const interactiveCols = computed(() =>
  (getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false) &&
  (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode)
    ? 8
    : 7
);
</script>

<template>
  <div>
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
    <pv-select
      v-model="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode"
      label="Robot Offset Mode"
      tooltip="Used to add an arbitrary offset to the location of the targeting crosshair"
      :items="['None', 'Single Point', 'Dual Point']"
      :select-cols="interactiveCols"
      @input="(value) => useCameraSettingsStore().changeCurrentPipelineSetting({ offsetRobotOffsetMode: value }, false)"
    />
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
    <div
      v-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode !== RobotOffsetPointMode.None"
      class="d-flex align-center"
    >
      <v-card-text
        v-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Single"
        class="d-flex pa-0 flex-wrap"
      >
        <v-col cols="6" class="pl-0">
          <v-btn
            small
            block
            color="accent"
            class="black--text"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Single)"
          >
            Take Point
          </v-btn>
        </v-col>
        <v-col cols="6" class="pr-0">
          <v-btn
            small
            block
            color="yellow darken-3"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Clear)"
          >
            Clear All Points
          </v-btn>
        </v-col>
      </v-card-text>
      <v-card-text
        v-else-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Dual"
        class="d-flex pa-0 flex-wrap"
      >
        <v-col cols="6" lg="4" class="pl-0 pr-2">
          <v-btn
            small
            block
            color="accent"
            class="black--text"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.DualFirst)"
          >
            Take First Point
          </v-btn>
        </v-col>
        <v-col cols="6" lg="4" class="pl-2 pr-0 pr-lg-2">
          <v-btn
            small
            block
            color="accent"
            class="black--text"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.DualSecond)"
          >
            Take Second Point
          </v-btn>
        </v-col>
        <v-col cols="12" lg="4" class="pl-0 pl-lg-2 pr-0">
          <v-btn
            small
            block
            color="yellow darken-3"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Clear)"
          >
            Clear All Points
          </v-btn>
        </v-col>
      </v-card-text>
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
  text-decoration-color: #ffd843;
}
</style>
