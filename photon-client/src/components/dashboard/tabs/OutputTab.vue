<script setup lang="ts">
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { type ActivePipelineSettings, PipelineType, RobotOffsetPointMode } from "@/types/PipelineTypes";
import PvSwitch from "@/components/common/pv-switch.vue";
import { computed } from "vue";
import { RobotOffsetType } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import { useDisplay } from "vuetify";
import PvDropdown from "@/components/common/pv-dropdown.vue";

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
    // eslint-disable-next-line default-case-last
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

const { mdAndDown } = useDisplay();
const labelCols = computed(
  () => 12 - (mdAndDown.value && (!useStateStore().sidebarFolded || useCameraSettingsStore().isDriverMode) ? 9 : 8)
);
</script>

<template>
  <div>
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOffsetPointEdge"
      :items="['Center', 'Top', 'Bottom', 'Left', 'Right'].map((v, i) => ({ name: v, value: i }))"
      label="Target Offset Point"
      :label-cols="labelCols"
      tooltip="Changes where the 'center' of the target is (used for calculating e.g. pitch and yaw)"
      @update:model-value="
        (value: number) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOffsetPointEdge: value }, false)
      "
    />
    <pv-dropdown
      v-if="!isTagPipeline"
      v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOrientation"
      :items="['Portrait', 'Landscape'].map((v, i) => ({ name: v, value: i }))"
      label="Target Orientation"
      :label-cols="labelCols"
      tooltip="Used to determine how to calculate target landmarks (e.g. the top, left, or bottom of the target)"
      @update:model-value="
        (value: number) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ contourTargetOrientation: value }, false)
      "
    />
    <pv-switch
      v-model="useCameraSettingsStore().currentPipelineSettings.outputShowMultipleTargets"
      :disabled="isTagPipeline"
      label="Show Multiple Targets"
      :label-cols="labelCols"
      tooltip="If enabled, up to five targets will be displayed and sent via PhotonLib, instead of just one"
      @update:model-value="
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
      :disabled="!isTagPipeline"
      label="Do Multi-Target Estimation"
      :label-cols="labelCols"
      tooltip="If enabled, all visible fiducial targets will be used to provide a single pose estimate from their combined model."
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ doMultiTarget: value }, false)
      "
    />
    <pv-switch
      v-if="
        (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
          currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
        useCameraSettingsStore().isCurrentVideoFormatCalibrated &&
        useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled
      "
      v-model="currentPipelineSettings.doSingleTargetAlways"
      :disabled="!isTagPipeline || !currentPipelineSettings.doMultiTarget"
      label="Always Do Single-Target Estimation"
      :label-cols="labelCols"
      tooltip="If disabled, visible fiducial targets used for multi-target estimation will not also be used for single-target estimation."
      @update:model-value="
        (value) => useCameraSettingsStore().changeCurrentPipelineSetting({ doSingleTargetAlways: value }, false)
      "
    />
    <v-divider class="mt-3 mb-3" />
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
    <pv-dropdown
      v-model="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode"
      :items="['None', 'Single Point', 'Dual Point'].map((v, i) => ({ name: v, value: i }))"
      label="Robot Offset Mode"
      :label-cols="labelCols"
      tooltip="Used to add an arbitrary offset to the location of the targeting crosshair"
      @update:model-value="
        (value: number) =>
          useCameraSettingsStore().changeCurrentPipelineSetting({ offsetRobotOffsetMode: value }, false)
      "
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
            color="accent"
            small
            style="width: 100%"
            text="Take Point"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Single)"
          />
        </v-col>
      </v-row>
      <v-row
        v-else-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Dual"
      >
        <v-col>
          <v-btn
            color="accent"
            small
            style="width: 100%"
            text="Take First Point"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.DualFirst)"
          />
        </v-col>
        <v-col>
          <v-btn
            color="accent"
            small
            style="width: 100%"
            text="Take Second Point"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.DualSecond)"
          />
        </v-col>
      </v-row>
      <v-col>
        <v-btn
          small
          style="width: 100%"
          text="Clear All Points"
          @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Clear)"
        />
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
