<script setup lang="ts">
import type { Component } from "vue";
import { computed, ref } from "vue";
import IconAlertCircleOutline from "~icons/mdi/alert-circle-outline";
import type { PvTabItem } from "@/components/common/base/pv-tabs.vue";

import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import InputTab from "@/components/dashboard/tabs/InputTab.vue";
import ThresholdTab from "@/components/dashboard/tabs/ThresholdTab.vue";
import ContoursTab from "@/components/dashboard/tabs/ContoursTab.vue";
import AprilTagTab from "@/components/dashboard/tabs/AprilTagTab.vue";
import ArucoTab from "@/components/dashboard/tabs/ArucoTab.vue";
import ObjectDetectionTab from "@/components/dashboard/tabs/ObjectDetectionTab.vue";
import OutputTab from "@/components/dashboard/tabs/OutputTab.vue";
import TargetsTab from "@/components/dashboard/tabs/TargetsTab.vue";
import PnPTab from "@/components/dashboard/tabs/PnPTab.vue";
import Map3DTab from "@/components/dashboard/tabs/Map3DTab.vue";
import { PipelineType } from "@/types/PipelineTypes";
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import { useCustomBreakpoints } from "@/lib/Breakpoints";

interface ConfigOption {
  tabName: string;
  component: Component;
}

const allTabs = Object.freeze({
  inputTab: { tabName: "Input", component: InputTab },
  thresholdTab: { tabName: "Threshold", component: ThresholdTab },
  contoursTab: { tabName: "Contours", component: ContoursTab },
  apriltagTab: { tabName: "AprilTag", component: AprilTagTab },
  arucoTab: { tabName: "ArUco", component: ArucoTab },
  objectDetectionTab: { tabName: "Object Detection", component: ObjectDetectionTab },
  outputTab: { tabName: "Output", component: OutputTab },
  targetsTab: { tabName: "Targets", component: TargetsTab },
  pnpTab: { tabName: "PnP", component: PnPTab },
  map3dTab: { tabName: "3D", component: Map3DTab }
});

const selectedTabs = ref([0, 0, 0, 0]);
const breakpoints = useCustomBreakpoints();
const smAndDown = breakpoints.smallerOrEqual("sm");
const mdAndDown = breakpoints.smallerOrEqual("md");
const lgAndDown = breakpoints.smallerOrEqual("lg");
const xl = breakpoints.greaterOrEqual("xl");

const getTabGroups = (): ConfigOption[][] => {
  if (smAndDown.value || useCameraSettingsStore().isDriverMode) {
    return [Object.values(allTabs)];
  } else if (mdAndDown.value || !useStateStore().sidebarFolded) {
    return [
      [
        allTabs.inputTab,
        allTabs.thresholdTab,
        allTabs.contoursTab,
        allTabs.apriltagTab,
        allTabs.arucoTab,
        allTabs.objectDetectionTab,
        allTabs.outputTab
      ],
      [allTabs.targetsTab, allTabs.pnpTab, allTabs.map3dTab]
    ];
  } else if (lgAndDown.value) {
    return [
      [allTabs.inputTab],
      [
        allTabs.thresholdTab,
        allTabs.contoursTab,
        allTabs.apriltagTab,
        allTabs.arucoTab,
        allTabs.objectDetectionTab,
        allTabs.outputTab
      ],
      [allTabs.targetsTab, allTabs.pnpTab, allTabs.map3dTab]
    ];
  } else if (xl.value) {
    return [
      [allTabs.inputTab],
      [allTabs.thresholdTab],
      [allTabs.contoursTab, allTabs.apriltagTab, allTabs.arucoTab, allTabs.objectDetectionTab, allTabs.outputTab],
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
  const isObjectDetection =
    useCameraSettingsStore().currentWebsocketPipelineType === WebsocketPipelineType.ObjectDetection;

  return getTabGroups()
    .map((tabGroup) =>
      tabGroup.filter(
        (tabConfig) =>
          !(!allow3d && tabConfig.tabName === "3D") && //Filter out 3D tab any time 3D isn't calibrated
          !((!allow3d || isAprilTag || isAruco || isObjectDetection) && tabConfig.tabName === "PnP") && //Filter out the PnP config tab if 3D isn't available, or we're doing AprilTags
          !((isAprilTag || isAruco || isObjectDetection) && tabConfig.tabName === "Threshold") && //Filter out threshold tab if we're doing AprilTags
          !((isAprilTag || isAruco || isObjectDetection) && tabConfig.tabName === "Contours") && //Filter out contours if we're doing AprilTags
          !(!isAprilTag && tabConfig.tabName === "AprilTag") && //Filter out apriltag unless we actually are doing AprilTags
          !(!isAruco && tabConfig.tabName === "ArUco") &&
          !(!isObjectDetection && tabConfig.tabName === "Object Detection") //Filter out ArUco unless we actually are doing ArUco
      )
    )
    .filter((it) => it.length); // Remove empty tab groups
});

// This boolean is used to satisfy type-checking requirements.
const shouldUseWideSecondTabGroup = computed(() => {
  const currentPipelineSettings = useCameraSettingsStore().currentPipelineSettings;

  return (
    (currentPipelineSettings.pipelineType === PipelineType.AprilTag ||
      currentPipelineSettings.pipelineType === PipelineType.Aruco) &&
    currentPipelineSettings.doMultiTarget
  );
});

const onBeforeTabUpdate = () => {
  // Force the current tab to the input tab on driver mode change
  if (useCameraSettingsStore().isDriverMode) {
    selectedTabs.value[0] = 0;
  }
};

const getSelectedComponent = (tabGroupData: ConfigOption[], selectedTabName: number): Component => {
  return tabGroupData[selectedTabName].component;
};

const getTabItems = (tabGroupData: ConfigOption[]): PvTabItem<string>[] =>
  tabGroupData.map((tabConfig) => ({ label: tabConfig.tabName, value: tabConfig.tabName }));
</script>

<template>
  <div class="tabGroups flex flex-wrap">
    <template v-if="!useCameraSettingsStore().hasConnected">
      <pv-alert
        color="error"
        density="compact"
        text="Camera is not connected. Please check your connection and try again."
        :icon="IconAlertCircleOutline"
      />
    </template>
    <template v-else>
      <div
        v-for="(tabGroupData, tabGroupIndex) in tabGroups"
        :key="tabGroupIndex"
        :class="[
          tabGroupIndex === 1 && shouldUseWideSecondTabGroup ? 'w-7/12' : 'flex-1',
          tabGroupIndex !== tabGroups.length - 1 && 'pr-3'
        ]"
        @vue:before-update="onBeforeTabUpdate"
      >
        <pv-card padding="none" class="h-full pr-5 pl-5">
          <pv-tabs v-model="selectedTabs[tabGroupIndex]" :items="getTabItems(tabGroupData)" class="mt-2" />
          <div class="pt-10px pb-10px">
            <KeepAlive>
              <Component :is="getSelectedComponent(tabGroupData, selectedTabs[tabGroupIndex])" />
            </KeepAlive>
          </div>
        </pv-card>
      </div>
    </template>
  </div>
</template>
