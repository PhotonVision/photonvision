<script setup lang="ts">
import type {Component} from "vue";
import {computed, ref} from "vue";
import {useCameraSettingsStore} from "@/stores/settings/CameraSettingsStore";

import InputTab from "@/components/Dashboard/PipelineConfigTabs/InputTab.vue";
import ThresholdTab from "@/components/Dashboard/PipelineConfigTabs/ThresholdTab.vue";
import ContoursTab from "@/components/Dashboard/PipelineConfigTabs/ContoursTab.vue";
import AprilTagTab from "@/components/Dashboard/PipelineConfigTabs/AprilTagTab.vue";
import ArucoTab from "@/components/Dashboard/PipelineConfigTabs/ArucoTab.vue";
import OutputTab from "@/components/Dashboard/PipelineConfigTabs/OutputTab.vue";
import TargetsTab from "@/components/Dashboard/PipelineConfigTabs/TargetsTab.vue";
import PnPTab from "@/components/Dashboard/PipelineConfigTabs/PnPTab.vue";
import Map3DTab from "@/components/Dashboard/PipelineConfigTabs/Map3DTab.vue";
import {WebsocketPipelineType} from "@/types/WebsocketDataTypes";

interface ConfigOption {
  tabName: string,
  component: Component
}

const allTabs = {
  inputTab: {
    tabName: "Input",
    component: InputTab
  },
  thresholdTab: {
    tabName: "Threshold",
    component: ThresholdTab
  },
  contoursTab: {
    tabName: "Contours",
    component: ContoursTab
  },
  apriltagTab: {
    tabName: "AprilTag",
    component: AprilTagTab
  },
  arucoTab: {
    tabName: "Aruco",
    component: ArucoTab
  },
  outputTab: {
    tabName: "Output",
    component: OutputTab
  },
  targetsTab: {
    tabName: "Targets",
    component: TargetsTab
  },
  pnpTab: {
    tabName: "PnP",
    component: PnPTab
  },
  map3dTab: {
    tabName: "3D",
    component: Map3DTab
  }
};

const selectedTab = ref(0);
const filteredTabs = computed<ConfigOption[]>(() => {
  if(useCameraSettingsStore().isDriverMode) return [allTabs.inputTab];

  const allow3d = useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled;
  const isAprilTag = useCameraSettingsStore().currentWebsocketPipelineType === WebsocketPipelineType.AprilTag;
  const isAruco = useCameraSettingsStore().currentWebsocketPipelineType === WebsocketPipelineType.Aruco;

  return Object.values(allTabs).filter((tabConfig: ConfigOption) =>
      !(!allow3d && tabConfig.tabName === "3D") //Filter out 3D tab any time 3D isn't calibrated
      && !((!allow3d || isAprilTag || isAruco) && tabConfig.tabName === "PnP") //Filter out the PnP config tab if 3D isn't available, or we're doing AprilTags
      && !((isAprilTag || isAruco) && (tabConfig.tabName === "Threshold")) //Filter out threshold tab if we're doing AprilTags
      && !((isAprilTag || isAruco) && (tabConfig.tabName === "Contours")) //Filter out contours if we're doing AprilTags
      && !(!isAprilTag && tabConfig.tabName === "AprilTag") //Filter out apriltag unless we actually are doing AprilTags
      && !(!isAruco && tabConfig.tabName === "Aruco")
  );
});
</script>

<template>
  <v-row no-gutters>
    <v-col :cols="12" align-self="stretch">
      <v-card
          color="primary"
          height="100%"
          class="pr-4 pl-4"
      >
        <v-tabs
            v-model="selectedTab"
            grow
            background-color="primary"
            dark
            height="48"
            slider-color="accent"
        >
          <v-tab
            v-for="(tabConfig, index) in filteredTabs"
            :key="index"
          >
            {{tabConfig.tabName}}
          </v-tab>
        </v-tabs>
        <div class="pl-4 pr-4 pt-4 pb-2">
          <KeepAlive>
            <Component :is="filteredTabs[selectedTab].component"/>
          </KeepAlive>
        </div>
      </v-card>
    </v-col>
  </v-row>
</template>
