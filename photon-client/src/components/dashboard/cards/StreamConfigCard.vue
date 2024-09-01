<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";

const processingMode = computed<number>({
  get: () => (useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled ? 1 : 0),
  set: (v) => {
    if (useCameraSettingsStore().isCurrentVideoFormatCalibrated) {
      useCameraSettingsStore().changeCurrentPipelineSetting({ solvePNPEnabled: v === 1 }, true);
    }
  }
});

const model = defineModel<number[]>({ required: true });
</script>

<template>
  <v-card
    class="fill-height"
    :disabled="useCameraSettingsStore().isDriverMode"
    style="display: flex; flex-direction: column"
  >
    <v-row align="center" class="pa-3 pb-0">
      <v-col>
        <span class="text-white">Processing Mode</span>
        <v-btn-toggle
          v-model="processingMode"
          base-color="surface-variant"
          class="mt-2"
          mandatory
          style="width: 100%"
        >
          <v-btn prepend-icon="mdi-square-outline" style="width: 50%" text="2D" />
          <v-btn
            :disabled="!useCameraSettingsStore().isCurrentVideoFormatCalibrated"
            prepend-icon="mdi-cube-outline"
            style="width: 50%"
            text="3D"
          />
        </v-btn-toggle>
      </v-col>
    </v-row>
    <v-row align="center" class="pa-3 pt-0">
      <v-col>
        <span class="text-white">Stream Display</span>
        <v-btn-toggle
          v-model="model"
          base-color="surface-variant"
          class="mt-2"
          mandatory
          :multiple="true"
          style="width: 100%"
        >
          <v-btn prepend-icon="mdi-import" style="width: 50%" text="Raw" />
          <v-btn prepend-icon="mdi-export" style="width: 50%" text="Processed" />
        </v-btn-toggle>
      </v-col>
    </v-row>
  </v-card>
</template>

<style scoped>
th {
  width: 80px;
  text-align: center;
}

.v-btn:not(.v-btn--active).v-theme--PhotonVisionClassicTheme {
  opacity: 0.8;
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
