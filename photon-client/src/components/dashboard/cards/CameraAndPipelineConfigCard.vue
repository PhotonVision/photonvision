<script setup lang="ts">
import PvTooltippedIcon from "@/components/common/pv-tooltipped-icon.vue";
import PipelineConfigModal from "@/components/modals/PipelineConfigModal.vue";
import RenameCameraModal from "@/components/modals/RenameCameraModal.vue";
import { computed } from "vue";
import type { DropdownSelectItem } from "@/types/Components";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";

const clientStore = useClientStore();
const serverStore = useServerStore();

const cameraNames = computed<DropdownSelectItem<number>[]>(() => {
  if (!serverStore.cameras?.length) {
    return [{ name: "No Cameras", value: 0 }];
  } else {
    return serverStore.cameraNames.map((v, i) => ({ name: v, value: i }));
  }
});
const pipelineNames = computed<DropdownSelectItem<number>[]>(() => {
  if (serverStore.isDriverMode) {
    return [{ name: "Driver Mode", value: -1 }];
  } else if (serverStore.isCalibMode) {
   return [{ name: "Calibration Mode", value: -2 }];
  } else if (!serverStore.currentCameraSettings?.pipelineSettings?.length) {
    return [{ name: "No Pipelines", value: 0 }];
  } else {
    return serverStore.pipelineNames.map((v, i) => ({ name: v, value: i }));
  }
});
</script>

<template>
  <v-card class="fill-height pa-3">
    <v-row class="pt-2 pb-3 flex-nowrap" no-gutters>
      <v-col>
        <pv-dropdown
          v-model="clientStore.currentCameraIndex"
          :disabled="!serverStore.cameras?.length"
          :items="cameraNames"
          label="Camera"
          :label-cols="3"
          tooltip="Select the Camera to Configure"
        />
      </v-col>
      <v-col cols="2" v-if="serverStore.currentCameraSettings" class="pl-2 d-flex justify-center align-center">
        <RenameCameraModal v-slot="{ props }" :camera-index="clientStore.currentCameraIndex">
          <pv-tooltipped-icon
            class="mt-4 ml-2"
            clickable
            icon-name="mdi-pencil"
            tooltip="Edit Camera Name"
            tooltip-location="bottom"
            :vuetify-props="props"
          />
        </RenameCameraModal>
      </v-col>
    </v-row>
    <v-row class="mt-0 pb-2 flex-nowrap" no-gutters>
      <v-col>
        <pv-dropdown
          :disabled="serverStore.isDriverMode || serverStore.isCalibMode || !serverStore.currentCameraSettings?.pipelineSettings?.length"
          :items="pipelineNames"
          label="Pipeline"
          :label-cols="3"
          :model-value="serverStore.currentCameraSettings?.activePipelineIndex || 0"
          tooltip="Each pipeline runs on a camera output and stores a unique set of processing settings"
          @update:model-value="(args: number) => serverStore.changeActivePipeline(args)"
        />
      </v-col>
      <v-col
        cols="2"
        v-if="!serverStore.isDriverMode && !serverStore.isCalibMode && serverStore.cameras?.length"
        class="pl-2 d-flex justify-center align-center"
      >
        <PipelineConfigModal v-slot="{ props }" :camera-index="clientStore.currentCameraIndex">
          <pv-tooltipped-icon
            clickable
            icon-name="mdi-open-in-new"
            tooltip="Open Pipeline Config Menu"
            tooltip-location="bottom"
            :vuetify-props="props"
          />
        </PipelineConfigModal>
      </v-col>
    </v-row>
  </v-card>
</template>
