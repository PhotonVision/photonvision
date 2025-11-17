<script setup lang="ts">
import type { Component } from "vue";
import { computed, ref } from "vue";
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
import { WebsocketPipelineType } from "@/types/WebsocketDataTypes";
import { useDisplay } from "vuetify/lib/composables/display";
import { useTheme } from "vuetify";

const theme = useTheme();

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
const { smAndDown, mdAndDown, lgAndDown, xl } = useDisplay();

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

const onBeforeTabUpdate = () => {
  // Force the current tab to the input tab on driver mode change
  if (useCameraSettingsStore().isDriverMode) {
    selectedTabs.value[0] = 0;
  }
};
</script>

<template>
  <v-row no-gutters class="tabGroups">
    <template v-if="!useCameraSettingsStore().hasConnected">
      <v-alert
        color="error"
        density="compact"
        text="Camera is not connected. Please check your connection and try again."
        icon="mdi-alert-circle-outline"
        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
      />
    </template>
    <template v-else>
      <v-col
        v-for="(tabGroupData, tabGroupIndex) in tabGroups"
        :key="tabGroupIndex"
        :cols="tabGroupIndex == 1 && useCameraSettingsStore().currentPipelineSettings.doMultiTarget ? 7 : ''"
        :class="tabGroupIndex !== tabGroups.length - 1 && 'pr-3'"
        @vue:before-update="onBeforeTabUpdate"
      >
        <v-card color="surface" height="100%" class="pr-5 pl-5 rounded-12">
          <v-tabs v-model="selectedTabs[tabGroupIndex]" grow bg-color="surface" height="48" slider-color="buttonActive">
            <v-tab v-for="(tabConfig, index) in tabGroupData" :key="index">
              {{ tabConfig.tabName }}
            </v-tab>
          </v-tabs>
          <div class="pt-10px pb-10px">
            <KeepAlive>
              <Component :is="tabGroupData[selectedTabs[tabGroupIndex]].component" />
            </KeepAlive>
          </div>
        </v-card>
      </v-col>
    </template>
  </v-row>
</template>

<style>
.v-slide-group {
  transition-duration: 0.28s;
  transition-property: box-shadow, opacity, background;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}
.v-slide-group__next--disabled,
.v-slide-group__prev--disabled {
  display: none !important;
}
</style>
