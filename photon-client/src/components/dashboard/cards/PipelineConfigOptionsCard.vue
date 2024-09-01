<script setup lang="ts">
import type { Component } from "vue";
import { useDisplay } from "vuetify";
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
import { WebsocketPipelineType } from "@/types/WebsocketTypes";

interface ConfigOption {
  tabName: string;
  component: Component;
  index: number;
}

const allTabs: Record<string, Omit<ConfigOption, "index">> = {
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
  objectDetectionTab: {
    tabName: "Object Detection",
    component: ObjectDetectionTab
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

const { smAndDown, mdAndDown, lgAndDown, xl } = useDisplay();
const tabGroups = computed<ConfigOption[][]>(() => {
  let initialGroups: Omit<ConfigOption, "index">[][];

  if (useCameraSettingsStore().isDriverMode) {
    initialGroups = [[allTabs.inputTab]];
  } else if (smAndDown.value || (mdAndDown.value && !useStateStore().sidebarFolded)) {
    initialGroups = [Object.values(allTabs)];
  } else if (mdAndDown.value && useStateStore().sidebarFolded) {
    initialGroups = [
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
    initialGroups = [
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
    initialGroups = [
      [allTabs.inputTab],
      [allTabs.thresholdTab],
      [allTabs.contoursTab, allTabs.apriltagTab, allTabs.arucoTab, allTabs.objectDetectionTab, allTabs.outputTab],
      [allTabs.targetsTab, allTabs.pnpTab, allTabs.map3dTab]
    ];
  } else {
    initialGroups = [Object.values(allTabs)];
  }

  const allow3d = useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled;
  const isAprilTag = useCameraSettingsStore().currentWebsocketPipelineType === WebsocketPipelineType.AprilTag;
  const isAruco = useCameraSettingsStore().currentWebsocketPipelineType === WebsocketPipelineType.Aruco;
  const isObjectDetection =
    useCameraSettingsStore().currentWebsocketPipelineType === WebsocketPipelineType.ObjectDetection;

  return initialGroups
    .map((tabGroup) =>
      tabGroup
        .filter(
          (tabConfig) =>
            !(!allow3d && tabConfig.tabName === "3D") && // Filter out 3D tab any time 3D isn't calibrated
            !((!allow3d || isAprilTag || isAruco || isObjectDetection) && tabConfig.tabName === "PnP") && // Filter out the PnP config tab if 3D isn't available, or we're doing AprilTags
            !((isAprilTag || isAruco || isObjectDetection) && tabConfig.tabName === "Threshold") && // Filter out threshold tab if we're doing AprilTags
            !((isAprilTag || isAruco || isObjectDetection) && tabConfig.tabName === "Contours") && // Filter out contours if we're doing AprilTags
            !(!isAprilTag && tabConfig.tabName === "AprilTag") && // Filter out apriltag unless we actually are doing AprilTags
            !(!isAruco && tabConfig.tabName === "Aruco") &&
            !(!isObjectDetection && tabConfig.tabName === "Object Detection") // Filter out aruco unless we actually are doing Aruco
        )
        .map<ConfigOption>((tabConfig, i) => ({ ...tabConfig, index: i }))
    )
    .filter((it) => it.length); // Remove empty tab groups
});

const selectedTabIndex = ref([0, 0, 0, 0]);
const getTabGroupIndex = (tabGroupData: ConfigOption[], tabGroupIndex: number): number => {
  if (selectedTabIndex.value[tabGroupIndex] >= tabGroupData.length) {
    selectedTabIndex.value[tabGroupIndex] = 0; // Reset the tab index to 0 if tabGroup was resized. Technically we could track previously active tabs.
  }

  return selectedTabIndex.value[tabGroupIndex];
};
const changeTabGroupIndex = (tabGroupIndex: number, newTabIndex: number) => {
  selectedTabIndex.value[tabGroupIndex] = newTabIndex;
};
</script>

<template>
  <v-row no-gutters style="display: flex; gap: 12px">
    <v-col v-for="(tabGroup, tabGroupIndex) in tabGroups" :key="tabGroupIndex">
      <v-card class="pr-4 pl-4 fill-height">
        <v-tabs
          grow
          height="48"
          :items="tabGroup"
          :model-value="getTabGroupIndex(tabGroup, tabGroupIndex)"
          slider-color="accent"
          @update:modelValue="(e) => changeTabGroupIndex(tabGroupIndex, e as number)"
        >
          <template #tab="{ item }">
            <v-tab :text="(item as ConfigOption).tabName" :value="(item as ConfigOption).index" />
          </template>

          <!--          TODO make the 3D viewer work with slot API -->
          <!--          <template #item="{ item }">-->
          <!--            <v-tabs-window-item-->
          <!--              class="pa-4 pb-2"-->
          <!--              :value="item.index"-->
          <!--              :transition="false"-->
          <!--              :reverse-transition="false"-->
          <!--            >-->
          <!--              <KeepAlive>-->
          <!--                <Component :is="item.component" />-->
          <!--              </KeepAlive>-->
          <!--            </v-tabs-window-item>-->
          <!--          </template>-->
        </v-tabs>

        <div class="pa-4 pb-2">
          <KeepAlive>
            <Component :is="tabGroup[getTabGroupIndex(tabGroup, tabGroupIndex)].component" />
          </KeepAlive>
        </div>
      </v-card>
    </v-col>
  </v-row>
</template>
