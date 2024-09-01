<script setup lang="ts">
import PvTooltippedIcon from "@/components/common/pv-tooltipped-icon.vue";
import PipelineConfigModal from "@/components/modals/PipelineConfigModal.vue";
import RenameCameraModal from "@/components/modals/RenameCameraModal.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed } from "vue";
import type { DropdownSelectItem } from "@/types/Components";
import PvDropdown from "@/components/common/pv-dropdown.vue";

const cameraNames = computed<DropdownSelectItem<number>[]>(() =>
  useCameraSettingsStore().cameraNames.map((v, i) => ({ name: v, value: i }))
);
const pipelineNames = computed<DropdownSelectItem<number>[]>(() =>
  useCameraSettingsStore().currentCameraSettings.pipelineNicknames.map((v, i) => ({ name: v, value: i }))
);
</script>

<template>
  <v-card class="fill-height pa-3">
    <v-row class="pt-2 pb-3 flex-nowrap" no-gutters>
      <v-col cols="10">
        <pv-dropdown
          v-model="useStateStore().currentCameraIndex"
          :items="cameraNames"
          label="Camera"
          :label-cols="3"
          tooltip="Select the Camera to Configure"
          @update:model-value="(v: number) => useCameraSettingsStore().setCurrentCameraIndex(v, true)"
        />
      </v-col>
      <v-col class="pl-2">
        <RenameCameraModal v-slot="{ props }">
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
      <v-col cols="10">
        <pv-dropdown
          v-model="useCameraSettingsStore().currentCameraSettings.currentPipelineIndex"
          :disabled="useCameraSettingsStore().isDriverMode || useCameraSettingsStore().isCalibrationMode"
          :items="pipelineNames"
          label="Pipeline"
          :label-cols="3"
          tooltip="Each pipeline runs on a camera output and stores a unique set of processing settings"
          @update:model-value="(args: number) => useCameraSettingsStore().changeCurrentPipelineIndex(args, true)"
        />
      </v-col>
      <v-col class="pl-2">
        <PipelineConfigModal v-slot="{ props }">
          <pv-tooltipped-icon
            class="mt-4 ml-2"
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
