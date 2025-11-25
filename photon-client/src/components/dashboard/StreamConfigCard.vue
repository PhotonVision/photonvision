<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useTheme } from "vuetify";
import { PipelineType } from "@/types/PipelineTypes";

const theme = useTheme();

const value = defineModel<number[]>();

const processingMode = computed<number>({
  get: () => (useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled ? 1 : 0),
  set: (v) => {
    if (useCameraSettingsStore().isCurrentVideoFormatCalibrated) {
      useCameraSettingsStore().changeCurrentPipelineSetting({ solvePNPEnabled: v === 1 }, true);
    }
  }
});
</script>

<template>
  <v-card
    :disabled="
      useCameraSettingsStore().isDriverMode || useCameraSettingsStore().isFocusMode || useStateStore().colorPickingMode
    "
    class="mt-3 rounded-12"
    color="surface"
    style="flex-grow: 1; display: flex; flex-direction: column"
  >
    <v-row class="pa-3 pb-0 align-center">
      <v-col class="pa-4">
        <p style="color: white">Processing Mode</p>
        <v-btn-toggle v-model="processingMode" mandatory class="fill w-100">
          <v-btn
            color="buttonPassive"
            :disabled="!useCameraSettingsStore().hasConnected"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            class="w-50"
          >
            <template #prepend>
              <v-icon size="large">mdi-square-outline</v-icon>
            </template>
            <span>2D</span>
          </v-btn>
          <v-btn
            color="buttonPassive"
            :disabled="
              !useCameraSettingsStore().hasConnected ||
              !useCameraSettingsStore().isCurrentVideoFormatCalibrated ||
              useCameraSettingsStore().currentPipelineSettings.pipelineType == PipelineType.ObjectDetection ||
              useCameraSettingsStore().currentPipelineSettings.pipelineType == PipelineType.ColoredShape
            "
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            class="w-50"
          >
            <template #prepend>
              <v-icon size="large">mdi-cube-outline</v-icon>
            </template>
            <span>3D</span>
          </v-btn>
        </v-btn-toggle>
      </v-col>
    </v-row>
    <v-row class="pa-3 pt-0 align-center">
      <v-col class="pa-4 pt-0">
        <p style="color: white">Stream Display</p>
        <v-btn-toggle v-model="value" :multiple="true" mandatory class="fill w-100">
          <v-btn
            color="buttonPassive"
            class="fill w-50"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          >
            <v-icon start class="mode-btn-icon" size="large">mdi-import</v-icon>
            <span class="mode-btn-label">Raw</span>
          </v-btn>
          <v-btn
            color="buttonPassive"
            class="fill w-50"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          >
            <v-icon start class="mode-btn-icon" size="large">mdi-export</v-icon>
            <span class="mode-btn-label">Processed</span>
          </v-btn>
        </v-btn-toggle>
      </v-col>
    </v-row>
  </v-card>
</template>

<style scoped>
.v-btn--disabled {
  background-color: #191919 !important;
}

th {
  width: 80px;
  text-align: center;
}

@media only screen and (max-width: 351px) {
  .mode-btn-icon {
    margin: 0 !important;
  }
  .mode-btn-label {
    display: none;
  }
}
</style>
