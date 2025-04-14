<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";

const props = defineProps<{
  // TODO fully update v-model usage in custom components on Vue3 update
  value: number[];
}>();

const emit = defineEmits<{
  (e: "input", value: number[]): void;
}>();

const localValue = computed({
  get: () => props.value,
  set: (v) => emit("input", v)
});

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
    style="height: 100%; display: flex; flex-direction: column"
  >
    <v-row class="pa-3 pb-0 align-center">
      <v-col class="pa-4">
        <p style="color: white">Processing Mode</p>
        <v-btn-toggle v-model="processingMode" mandatory dark class="fill">
          <v-btn color="secondary" :disabled="!useCameraSettingsStore().hasConnected">
            <v-icon left>mdi-square-outline</v-icon>
            <span>2D</span>
          </v-btn>
          <v-btn
            color="secondary"
            :disabled="
              !useCameraSettingsStore().hasConnected || !useCameraSettingsStore().isCurrentVideoFormatCalibrated
            "
          >
            <v-icon left>mdi-cube-outline</v-icon>
            <span>3D</span>
          </v-btn>
        </v-btn-toggle>
      </v-col>
    </v-row>
    <v-row class="pa-3 pt-0 align-center">
      <v-col class="pa-4 pt-0">
        <p style="color: white">Stream Display</p>
        <v-btn-toggle v-model="localValue" :multiple="true" mandatory dark class="fill">
          <v-btn color="secondary" class="fill">
            <v-icon left class="mode-btn-icon">mdi-import</v-icon>
            <span class="mode-btn-label">Raw</span>
          </v-btn>
          <v-btn color="secondary" class="fill">
            <v-icon left class="mode-btn-icon">mdi-export</v-icon>
            <span class="mode-btn-label">Processed</span>
          </v-btn>
        </v-btn-toggle>
      </v-col>
    </v-row>
  </v-card>
</template>

<style scoped>
.v-btn-toggle.fill {
  width: 100%;
  height: 100%;
}

.v-btn-toggle.fill > .v-btn {
  width: 50%;
  height: 100%;
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
