<script setup lang="ts">
import type { Component } from "vue";
import { computed, getCurrentInstance, onBeforeUpdate, ref } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import InputTab from "@/components/dashboard/tabs/InputTab.vue";
import ThresholdTab from "@/components/dashboard/tabs/ThresholdTab.vue";
import ContoursTab from "@/components/dashboard/tabs/ContoursTab.vue";
import AprilTagTab from "@/components/dashboard/tabs/AprilTagTab.vue";
import ArucoTab from "@/components/dashboard/tabs/ArucoTab.vue";
import OutputTab from "@/components/dashboard/tabs/OutputTab.vue";
import TargetsTab from "@/components/dashboard/tabs/TargetsTab.vue";
import PnPTab from "@/components/dashboard/tabs/PnPTab.vue";
import Map3DTab from "@/components/dashboard/tabs/Map3DTab.vue";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";

interface ConfigOption {
  tabName: string;
  component: Component;
}

const allTabs = Object.freeze({
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
});

const selectedTabs = ref([0, 0, 0, 0]);
const getTabGroups = (): ConfigOption[][] => {
  const smAndDown = getCurrentInstance()?.proxy.$vuetify.breakpoint.smAndDown || false;
  const mdAndDown = getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndDown || false;
  const lgAndDown = getCurrentInstance()?.proxy.$vuetify.breakpoint.lgAndDown || false;
  const xl = getCurrentInstance()?.proxy.$vuetify.breakpoint.xl || false;

  if (smAndDown || useCameraSettingsStore().isDriverMode || (mdAndDown && !useStateStore().sidebarFolded)) {
    return [Object.values(allTabs)];
  } else if (mdAndDown || !useStateStore().sidebarFolded) {
    return [
      [
        allTabs.inputTab,
        allTabs.thresholdTab,
        allTabs.contoursTab,
        allTabs.apriltagTab,
        allTabs.arucoTab,
        allTabs.outputTab
      ],
      [allTabs.targetsTab, allTabs.pnpTab, allTabs.map3dTab]
    ];
  } else if (lgAndDown) {
    return [
      [allTabs.inputTab],
      [allTabs.thresholdTab, allTabs.contoursTab, allTabs.apriltagTab, allTabs.arucoTab, allTabs.outputTab],
      [allTabs.targetsTab, allTabs.pnpTab, allTabs.map3dTab]
    ];
  } else if (xl) {
    return [
      [allTabs.inputTab],
      [allTabs.thresholdTab],
      [allTabs.contoursTab, allTabs.apriltagTab, allTabs.arucoTab, allTabs.outputTab],
      [allTabs.targetsTab, allTabs.pnpTab, allTabs.map3dTab]
    ];
  }

  return [];
};
const tabGroups = computed<ConfigOption[][]>(() => {
  // Just return the input tab because we know that is always the case in driver mode
  if (useCameraSettingsStore().isDriverMode) return [[allTabs.inputTab]];

  const allow3d = useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled;
  const isAprilTag = useCameraSettingsStore().currentWebsocketPipelineType === WebsocketPipelineType.AprilTag;
  const isAruco = useCameraSettingsStore().currentWebsocketPipelineType === WebsocketPipelineType.Aruco;

  var ret = getTabGroups().map((tabGroup) =>
    tabGroup.filter(
      (tabConfig) =>
        !(!allow3d && tabConfig.tabName === "3D") && //Filter out 3D tab any time 3D isn't calibrated
        !((!allow3d || isAprilTag || isAruco) && tabConfig.tabName === "PnP") && //Filter out the PnP config tab if 3D isn't available, or we're doing AprilTags
        !((isAprilTag || isAruco) && tabConfig.tabName === "Threshold") && //Filter out threshold tab if we're doing AprilTags
        !((isAprilTag || isAruco) && tabConfig.tabName === "Contours") && //Filter out contours if we're doing AprilTags
        !(!isAprilTag && tabConfig.tabName === "AprilTag") && //Filter out apriltag unless we actually are doing AprilTags
        !(!isAruco && tabConfig.tabName === "Aruco") //Filter out aruco unless we actually are doing Aruco
    )
  );
  // remove empty tab groups
  ret = ret.filter((it) => it.length);
  return ret;
});

onBeforeUpdate(() => {
  // Force the current tab to the input tab on driver mode change
  if (useCameraSettingsStore().isDriverMode) {
    selectedTabs.value[0] = 0;
  }
});
</script>

<template>
  <v-row no-gutters class="tabGroups">
    <v-col
      v-for="(tabGroupData, tabGroupIndex) in tabGroups"
      :key="tabGroupIndex"
      :class="tabGroupIndex !== tabGroups.length - 1 && 'pr-3'"
    >
      <v-card color="primary" height="100%" class="pr-4 pl-4">
        <v-tabs
          v-model="selectedTabs[tabGroupIndex]"
          grow
          background-color="primary"
          dark
          height="48"
          slider-color="accent"
        >
          <v-tab v-for="(tabConfig, index) in tabGroupData" :key="index">
            {{ tabConfig.tabName }}
          </v-tab>
        </v-tabs>
        <div class="pl-4 pr-4 pt-4 pb-2">
          <KeepAlive>
            <Component :is="tabGroupData[selectedTabs[tabGroupIndex]].component" />
          </KeepAlive>
        </div>
      </v-card>
    </v-col>
  </v-row>
</template>

<style>
.v-slide-group__next--disabled,
.v-slide-group__prev--disabled {
  display: none !important;
}
</style>
