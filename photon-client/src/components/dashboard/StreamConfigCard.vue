<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";

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
    :disabled="useCameraSettingsStore().isDriverMode || useStateStore().colorPickingMode"
    class="mt-3"
    color="primary"
    style="flex-grow: 1; display: flex; flex-direction: column"
  >
    <v-row class="pa-3 pb-0 align-center">
      <v-col class="pa-4">
        <p style="color: white">Processing Mode</p>
        <v-btn-toggle v-model="processingMode" mandatory base-color="surface-variant" class="fill w-100">
          <v-btn
            color="secondary"
            :disabled="!useCameraSettingsStore().hasConnected"
            class="w-50"
            prepend-icon="mdi-square-outline"
          >
            <span>2D</span>
          </v-btn>
          <v-btn
            color="secondary"
            :disabled="
              !useCameraSettingsStore().hasConnected || !useCameraSettingsStore().isCurrentVideoFormatCalibrated
            "
            class="w-50"
            prepend-icon="mdi-cube-outline"
          >
            <span>3D</span>
          </v-btn>
        </v-btn-toggle>
      </v-col>
    </v-row>
    <v-row class="pa-3 pt-0 align-center">
      <v-col class="pa-4 pt-0">
        <p style="color: white">Stream Display</p>
        <v-btn-toggle v-model="value" :multiple="true" mandatory base-color="surface-variant" class="fill w-100">
          <v-btn color="secondary" class="fill w-50">
            <v-icon start class="mode-btn-icon">mdi-import</v-icon>
            <span class="mode-btn-label">Raw</span>
          </v-btn>
          <v-btn color="secondary" class="fill w-50">
            <v-icon start class="mode-btn-icon">mdi-export</v-icon>
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
