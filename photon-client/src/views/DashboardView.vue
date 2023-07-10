<script setup lang="ts">
import {computed} from "vue";
import CamerasCard from "@/components/dashboard/CamerasCard.vue";
import PipelineConfigCard from "@/components/dashboard/CameraAndPipelineSelectCard.vue";
import StreamConfigCard from "@/components/dashboard/StreamConfigCard.vue";
import OperationConfigCard from "@/components/dashboard/PipelineConfigCard.vue";
import {useCameraSettingsStore} from "@/stores/settings/CameraSettingsStore";

const cameraViewType = computed<number[]>({
  get: (): number[] => {
    // Only show the output stream in Driver Mode
    if(useCameraSettingsStore().isDriverMode) return [1];

    const ret: number[] = [];
    if(useCameraSettingsStore().currentPipelineSettings.inputShouldShow) {
      ret.push(0);
    }
    if(useCameraSettingsStore().currentPipelineSettings.outputShouldShow) {
      ret.push(1);
    }

    return ret;
  },
  set: v => useCameraSettingsStore().changeCurrentPipelineSetting({
    inputShouldShow: v.includes(0),
    outputShouldShow: v.includes(1)
  }, true)
});
</script>

<template>
  <v-container
      class="pa-3"
      fluid
  >
    <v-row
        no-gutters
        align="center"
        justify="center"
    >
      <v-col
          cols="12"
          :class="['pb-3 ', 'pr-lg-3']"
          lg="8"
          align-self="stretch"
      >
        <CamerasCard v-model="cameraViewType"/>
      </v-col>
      <v-col
          cols="12"
          class="pb-3"
          lg="4"
          style="display: flex; flex-direction: column"
      >
        <PipelineConfigCard/>
        <StreamConfigCard v-model="cameraViewType"/>
      </v-col>
    </v-row>
    <OperationConfigCard/>
  </v-container>
</template>
