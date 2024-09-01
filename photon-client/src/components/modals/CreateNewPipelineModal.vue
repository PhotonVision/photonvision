<script setup lang="ts">
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvTextbox from "@/components/common/pv-textbox.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import type { ValidationRule } from "@/types/Components";
import { WebsocketPipelineType } from "@/types/WebsocketTypes";
import { computed, ref } from "vue";
import { nameChangeRegex } from "@/lib/PhotonUtils";

const dialogOpen = ref<boolean>();
const backendBusy = ref<boolean>(false);
const bufferPipelineName = ref<string>("");
const bufferPipelineType = ref<WebsocketPipelineType>();

const checkPipelineName: ValidationRule = (name: string) => {
  if (!nameChangeRegex.test(name)) {
    return "A pipeline name can only contain letters, numbers, spaces, underscores, hyphens, parenthesis, and periods";
  }
  if (useCameraSettingsStore().pipelineNames.some((pipelineName) => pipelineName === name)) {
    return "This pipeline name has already been used";
  }

  return true;
};
const validNewPipelineTypes = computed(() => {
  const pipelineTypes = [
    { name: "Reflective", value: WebsocketPipelineType.Reflective },
    { name: "Colored Shape", value: WebsocketPipelineType.ColoredShape },
    { name: "AprilTag", value: WebsocketPipelineType.AprilTag },
    { name: "Aruco", value: WebsocketPipelineType.Aruco }
  ];
  if (useSettingsStore().general.rknnSupported) {
    pipelineTypes.push({ name: "Object Detection", value: WebsocketPipelineType.ObjectDetection });
  }
  return pipelineTypes;
});
const createNewPipeline = () => {
  backendBusy.value = true;
  const type = bufferPipelineType.value;
  if (type !== WebsocketPipelineType.DriverMode && type !== WebsocketPipelineType.Calib3d) {
    useCameraSettingsStore().createNewPipeline(bufferPipelineName.value, type as any);
  }

  dialogOpen.value = false;
  backendBusy.value = false;
  bufferPipelineName.value = "";
  bufferPipelineType.value = undefined;
};
</script>

<template>
  <v-dialog v-model="dialogOpen" max-width="600px">
    <template #activator="{ props }">
      <slot v-bind="{ props }" />
    </template>
    <template #default="{ isActive }">
      <v-card class="pa-3">
        <v-card-title>Create New Pipeline for {{ useCameraSettingsStore().currentCameraName }}</v-card-title>
        <v-divider class="pb-4" />
        <pv-textbox
          v-model="bufferPipelineName"
          class="pl-4 pr-4"
          label="Name"
          :label-cols="3"
          placeholder="New Pipeline Name"
          :rules="[checkPipelineName]"
          tooltip="Name for the new pipeline"
        />
        <pv-dropdown
          v-model="bufferPipelineType"
          class="pl-4 pr-4"
          :items="validNewPipelineTypes"
          label="Type"
          :label-cols="3"
          tooltip="Pipeline type, which changes the type of processing that will happen on input frames"
        />
        <v-card-actions class="mt-2">
          <v-btn
            color="accent"
            :disabled="checkPipelineName(bufferPipelineName) != true || bufferPipelineType === undefined"
            :loading="backendBusy"
            text="Create New Pipeline"
            variant="elevated"
            @click="createNewPipeline"
          />
          <v-btn
            color="error"
            :disabled="backendBusy"
            text="Cancel"
            variant="elevated"
            @click="isActive.value = false"
          />
        </v-card-actions>
      </v-card>
    </template>
  </v-dialog>
</template>
