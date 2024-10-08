<script setup lang="ts">
import {
  AprilTagPipelineSettings, ArucoPipelineSettings,
  PipelineType, RobotOffsetPointMode, UserPipelineSettings
} from "@/types/PipelineTypes";
import PvSwitch from "@/components/common/pv-switch.vue";
import { computed } from "vue";
import { CameraConfig, RobotOffsetOperationMode, VideoFormat } from "@/types/SettingTypes";
import { useDisplay } from "vuetify";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import { resolutionsAreEqual } from "@/lib/PhotonUtils";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
  pipelineIndex: number
}>();

const targetPipelineSettings = computed<UserPipelineSettings>(() => props.cameraSettings.pipelineSettings.find((v) => v.pipelineIndex === props.pipelineIndex) as UserPipelineSettings);

const isTagPipeline = computed(() => [PipelineType.AprilTag, PipelineType.Aruco].includes(targetPipelineSettings.value.pipelineType));
const isCalibrated = computed(() => {
  const targetIndex = targetPipelineSettings.value.cameraVideoModeIndex;
  const targetVideoMode = props.cameraSettings.videoFormats.find((v) => v.sourceIndex === targetIndex) as VideoFormat;
  return props.cameraSettings.calibrations.some((v) => resolutionsAreEqual(v.resolution, targetVideoMode.resolution));
});

const offsetData = computed(() => {
  if (targetPipelineSettings.value.offsetRobotOffsetMode === RobotOffsetPointMode.Single) {
    const value = Object.values(targetPipelineSettings.value.offsetSinglePoint);
    return [{ "Offset Point": `(${value[0].toFixed(2)}°, ${value[1].toFixed(2)}°)` }];
  } else {
    const firstPoint = Object.values(targetPipelineSettings.value.offsetDualPointA);
    const firstPointArea = targetPipelineSettings.value.offsetDualPointAArea;
    const secondPoint = Object.values(targetPipelineSettings.value.offsetDualPointB);
    const secondPointArea = targetPipelineSettings.value.offsetDualPointBArea;
    return [
      {
        "First Offset Point": `(${firstPoint[0].toFixed(2)}°, ${firstPoint[1].toFixed(2)}°)`,
        "First Offset Point Area": `${firstPointArea.toFixed(2)}%`,
        "Second Offset Point": `(${secondPoint[0].toFixed(2)}°, ${secondPoint[1].toFixed(2)}°)`,
        "Second Offset Point Area": `${secondPointArea.toFixed(2)}%`
      }
    ];
  }
});

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(() => mdAndDown.value && (!clientStore.sidebarFolded || serverStore.isDriverMode) ? 3 : 5);
</script>

<template>
  <div>
    <pv-dropdown
      :items="['Center', 'Top', 'Bottom', 'Left', 'Right'].map((v, i) => ({ name: v, value: i }))"
      label="Target Offset Point"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.contourTargetOffsetPointEdge"
      tooltip="Changes where the 'center' of the target is (used for calculating e.g. pitch and yaw)"
      @update:model-value="
        (value: number) =>
          serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourTargetOffsetPointEdge: value }, true, true)
      "
    />
    <pv-dropdown
      v-if="!isTagPipeline"
      :items="['Portrait', 'Landscape'].map((v, i) => ({ name: v, value: i }))"
      label="Target Orientation"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.contourTargetOrientation"
      tooltip="Used to determine how to calculate target landmarks (e.g. the top, left, or bottom of the target)"
      @update:model-value="
        (value: number) =>
          serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { contourTargetOrientation: value }, true, true)
      "
    />
    <pv-switch
      :disabled="isTagPipeline"
      label="Show Multiple Targets"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.outputShowMultipleTargets"
      tooltip="If enabled, up to five targets will be displayed and sent via PhotonLib, instead of just one"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { outputShowMultipleTargets: value }, true, true)
      "
    />
    <pv-switch
      v-if="
        isTagPipeline &&
          isCalibrated &&
          targetPipelineSettings.solvePNPEnabled
      "
      :disabled="!isTagPipeline"
      label="Do Multi-Target Estimation"
      :label-cols="labelCols"
      :model-value="(targetPipelineSettings as AprilTagPipelineSettings | ArucoPipelineSettings).doMultiTarget"
      tooltip="If enabled, all visible fiducial targets will be used to provide a single pose estimate from their combined model."
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { doMultiTarget: value }, true, true)
      "
    />
    <pv-switch
      v-if="
        isTagPipeline &&
          isCalibrated &&
          targetPipelineSettings.solvePNPEnabled
      "
      :disabled="!isTagPipeline || !(targetPipelineSettings as AprilTagPipelineSettings | ArucoPipelineSettings).doMultiTarget"
      label="Always Do Single-Target Estimation"
      :label-cols="labelCols"
      :model-value="(targetPipelineSettings as AprilTagPipelineSettings | ArucoPipelineSettings).doSingleTargetAlways"
      tooltip="If disabled, visible fiducial targets used for multi-target estimation will not also be used for single-target estimation."
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { doSingleTargetAlways: value }, true, true)
      "
    />
    <v-divider class="mt-3 mb-3" />
    <v-data-table
      v-show="targetPipelineSettings.offsetRobotOffsetMode !== RobotOffsetPointMode.None"
      class="mt-3 mb-3"
      :items="offsetData"
    />

    <pv-dropdown
      :items="[{name:'None', value: RobotOffsetPointMode.None}, {name:'Single Point',value:RobotOffsetPointMode.Single}, {name:'Dual Point',value:RobotOffsetPointMode.Dual}]"
      label="Robot Offset Mode"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.offsetRobotOffsetMode"
      tooltip="Used to add an arbitrary offset to the location of the targeting crosshair"
      @update:model-value="
        (value: number) =>
          serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { offsetRobotOffsetMode: value }, true, true)
      "
    />
    <v-row
      v-if="targetPipelineSettings.offsetRobotOffsetMode !== RobotOffsetPointMode.None"
      align="center"
      justify="start"
    >
      <v-row
        v-if="targetPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Single"
      >
        <v-col>
          <v-btn
            class="w-100"
            color="accent"
            small
            text="Take Point"
            @click="serverStore.takeRobotOffsetPoint(RobotOffsetOperationMode.Single, cameraSettings.cameraIndex)"
          />
        </v-col>
      </v-row>
      <v-row
        v-else-if="targetPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Dual"
      >
        <v-col>
          <v-btn
            class="w-100"
            color="accent"
            small
            text="Take First Point"
            @click="serverStore.takeRobotOffsetPoint(RobotOffsetOperationMode.DualFirst, cameraSettings.cameraIndex)"
          />
        </v-col>
        <v-col>
          <v-btn
            class="w-100"
            color="accent"
            small
            text="Take Second Point"
            @click="serverStore.takeRobotOffsetPoint(RobotOffsetOperationMode.DualSecond, cameraSettings.cameraIndex)"
          />
        </v-col>
      </v-row>
      <v-col>
        <v-btn
          class="w-100"
          small
          text="Clear All Points"
          @click="serverStore.takeRobotOffsetPoint(RobotOffsetOperationMode.Clear, cameraSettings.cameraIndex)"
        />
      </v-col>
    </v-row>
  </div>
</template>
