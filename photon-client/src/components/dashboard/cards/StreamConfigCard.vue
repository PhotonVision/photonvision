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
  <v-card class="fill-height d-flex flex-column" :disabled="useCameraSettingsStore().isDriverMode">
    <v-row align="center" class="pa-3 pb-0">
      <v-col>
        <span class="text-white">Processing Mode</span>
        <v-btn-toggle v-model="processingMode" base-color="surface-variant" class="w-100 mt-2" mandatory>
          <v-btn class="w-50" prepend-icon="mdi-square-outline" text="2D" />
          <v-btn
            class="w-50"
            :disabled="!useCameraSettingsStore().isCurrentVideoFormatCalibrated"
            prepend-icon="mdi-cube-outline"
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
          class="mt-2 w-100"
          mandatory
          :multiple="true"
        >
          <v-btn class="w-50" prepend-icon="mdi-import" text="Raw" />
          <v-btn class="w-50" prepend-icon="mdi-export" text="Processed" />
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
