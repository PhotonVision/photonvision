<script setup lang="ts">
import { computed } from "vue";
import { useServerStore } from "@/stores/ServerStore";
import { AdvancedPipelineSettings } from "@/types/PipelineTypes";

const serverStore = useServerStore();

const processingMode = computed<number>({
  get: () => {
    const currentPipelineSettings = serverStore.currentPipelineSettings;
    if (!currentPipelineSettings || !Object.prototype.hasOwnProperty.call(currentPipelineSettings, "solvePNPEnabled")) {
      return 0;
    } else {
      return (currentPipelineSettings as AdvancedPipelineSettings).solvePNPEnabled ? 1 : 0;
    }
  },
  set: (v) => {
    if (serverStore.isCurrentVideoFormatCalibrated) {
      serverStore.updateCurrentPipelineSettings({ solvePNPEnabled: v === 1 }, true, true);
    }
  }
});

const model = defineModel<number[]>({ required: true });
</script>

<template>
  <v-card class="fill-height d-flex flex-column" :disabled="serverStore.isDriverMode">
    <v-row align="center" class="pa-3 pb-0">
      <v-col>
        <span class="text-white">Processing Mode</span>
        <v-btn-toggle v-model="processingMode" base-color="surface-variant" class="w-100 mt-2" mandatory :disabled="!serverStore.currentPipelineSettings">
          <v-btn class="w-50" prepend-icon="mdi-square-outline" text="2D" />
          <v-btn
            class="w-50"
            :disabled="!serverStore.isCurrentVideoFormatCalibrated"
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
          :disabled="!serverStore.currentPipelineSettings"
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
