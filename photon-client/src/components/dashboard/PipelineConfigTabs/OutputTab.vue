<script setup lang="ts">

import CvSelect from "@/components/common/cv-select.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PipelineType, RobotOffsetPointMode } from "@/types/PipelineTypes";
import CvSwitch from "@/components/common/cv-switch.vue";
import { computed } from "vue";
import { RobotOffsetType } from "@/types/SettingTypes";

const isTagPipeline = computed(() => useCameraSettingsStore().currentPipelineType === PipelineType.AprilTag || useCameraSettingsStore().currentPipelineType === PipelineType.Aruco);
</script>

<template>
  <div>
    <cv-select
        v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOffsetPointEdge"
        label="Target Offset Point"
        tooltip="Changes where the 'center' of the target is (used for calculating e.g. pitch and yaw)"
        :items="['Center','Top','Bottom','Left','Right']"
        :select-cols="10"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourTargetOffsetPointEdge: value})"
    />
    <cv-select
        v-if="!isTagPipeline"
        v-model="useCameraSettingsStore().currentPipelineSettings.contourTargetOrientation"
        label="Target Orientation"
        tooltip="Used to determine how to calculate target landmarks (e.g. the top, left, or bottom of the target)"
        :items="['Portrait', 'Landscape']"
        :select-cols="10"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({contourTargetOrientation: value})"
    />
    <cv-switch
        v-model="useCameraSettingsStore().currentPipelineSettings.outputShowMultipleTargets"
        label="Show Multiple Targets"
        tooltip="If enabled, up to five targets will be displayed and sent to user code, instead of just one"
        :disabled="isTagPipeline"
        :label-cols="2"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({outputShowMultipleTargets: value})"
    />
    <cv-select
        v-model="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode"
        label="Robot Offset Mode"
        tooltip="Used to add an arbitrary offset to the location of the targeting crosshair"
        :items="['None','Single Point','Dual Point']"
        :select-cols="10"
        @input="value => useCameraSettingsStore().changeCurrentPipelineSetting({offsetRobotOffsetMode: value})"
    />
    <v-row
        v-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode !== RobotOffsetPointMode.None"
        align="center"
        justify="start"
    >
      <v-row v-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Single">
        <v-col>
          <v-btn
              small
              color="accent"
              style="width: 100%;"
              class="black--text"
              @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Single)"
          >
            Take Point
          </v-btn>
        </v-col>
      </v-row>
      <v-row v-else-if="useCameraSettingsStore().currentPipelineSettings.offsetRobotOffsetMode === RobotOffsetPointMode.Dual">
        <v-col>
          <v-btn
              small
              color="accent"
              style="width: 100%;"
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
              style="width: 100%;"
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
            style="width: 100%;"
            @click="useCameraSettingsStore().takeRobotOffsetPoint(RobotOffsetType.Clear)"
        >
          Clear All Points
        </v-btn>
      </v-col>
    </v-row>
  </div>
</template>
