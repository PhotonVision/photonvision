<script setup lang="ts">
import { computed } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";

const props = defineProps<{
  // TODO fully update v-model usage in custom components on Vue3 update
  value: number[]
}>();

const emit = defineEmits<{
  (e: "input", value: number[]): void
}>();


const localValue = computed({
  get: () => props.value,
  set: v => emit("input", v)
});

const processingMode = computed<number>({
  get: () => useCameraSettingsStore().currentPipelineSettings.solvePNPEnabled ? 1: 0,
  set: v => {
    if(useCameraSettingsStore().isCurrentVideoFormatCalibrated) {
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
  >
    <v-row
      align="center"
      class="pa-3"
    >
      <v-col lg="12">
        <p style="color: white;">
          Processing Mode
        </p>
        <v-btn-toggle
          v-model="processingMode"
          mandatory
          dark
          class="fill"
        >
          <v-btn
            color="secondary"
          >
            <v-icon>mdi-square-outline</v-icon>
            <span>2D</span>
          </v-btn>
          <v-btn
            color="secondary"
            :disabled="!useCameraSettingsStore().isCurrentVideoFormatCalibrated"
          >
            <v-icon>mdi-cube-outline</v-icon>
            <span>3D</span>
          </v-btn>
        </v-btn-toggle>
      </v-col>
      <v-col lg="12">
        <p style="color: white;">
          Stream Display
        </p>
        <v-btn-toggle
          v-model="localValue"
          :multiple="true"
          mandatory
          dark
          class="fill"
        >
          <v-btn
            color="secondary"
            class="fill"
          >
            <v-icon>mdi-import</v-icon>
            <span>Raw</span>
          </v-btn>
          <v-btn
            color="secondary"
            class="fill"
          >
            <v-icon>mdi-export</v-icon>
            <span>Processed</span>
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
</style>
