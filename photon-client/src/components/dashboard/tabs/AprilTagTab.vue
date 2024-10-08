<script setup lang="ts">
import { AprilTagPipelineSettings } from "@/types/PipelineTypes";
import { computed } from "vue";
import { useDisplay } from "vuetify";
import PvDropdown from "@/components/common/pv-dropdown.vue";
import PvNumberSlider from "@/components/common/pv-number-slider.vue";
import PvSwitch from "@/components/common/pv-switch.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";
import { CameraConfig } from "@/types/SettingTypes";

const clientStore = useClientStore();
const serverStore = useServerStore();

const props = defineProps<{
  cameraSettings: CameraConfig,
  pipelineIndex: number
}>();

const targetPipelineSettings = computed<AprilTagPipelineSettings>(() => props.cameraSettings.pipelineSettings.find((v) => v.pipelineIndex === props.pipelineIndex) as AprilTagPipelineSettings);

const { mdAndDown } = useDisplay();
const labelCols = computed<number>(() => mdAndDown.value && (!clientStore.sidebarFolded || serverStore.isDriverMode) ? 3 : 5);
</script>

<template>
  <div>
    <pv-dropdown
      :items="
        ['AprilTag 36h11 (6.5in)', 'AprilTag 25h9 (6in)', 'AprilTag 16h5 (6in)'].map((v, i) => ({ name: v, value: i }))
      "
      label="Target Family"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.tagFamily"
      @update:model-value="
        (value: number) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { tagFamily: value }, true ,true)
      "
    />
    <pv-number-slider
      label="Decimate"
      :label-cols="labelCols"
      :max="8"
      :min="1"
      :model-value="targetPipelineSettings.decimate"
      :step="1"
      tooltip="Increases FPS at the expense of range by reducing image resolution initially"
      @update:model-value="(value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { decimate: value }, true, true)"
    />
    <pv-number-slider
      label="Blur"
      :label-cols="labelCols"
      :max="5"
      :min="0"
      :model-value="targetPipelineSettings.blur"
      :step="0.1"
      tooltip="Gaussian blur added to the image, high FPS cost for slightly decreased noise"
      @update:model-value="(value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { blur: value }, true, true)"
    />
    <pv-number-slider
      label="Threads"
      :label-cols="labelCols"
      :max="8"
      :min="1"
      :model-value="targetPipelineSettings.threads"
      :step="1"
      tooltip="Number of threads spawned by the AprilTag detector"
      @update:model-value="(value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { threads: value }, true, true)"
    />
    <pv-switch
      label="Refine Edges"
      :label-cols="labelCols"
      :model-value="targetPipelineSettings.refineEdges"
      tooltip="Further refines the AprilTag corner position initial estimate, suggested left on"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { refineEdges: value }, true, true)
      "
    />
    <pv-number-slider
      label="Decision Margin Cutoff"
      :label-cols="labelCols"
      :max="250"
      :min="0"
      :model-value="targetPipelineSettings.decisionMargin"
      :step="1"
      tooltip="Tags with a 'margin' (decoding quality score) less than this wil be rejected. Increase this to reduce the number of false positive detections"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { decisionMargin: value }, true, true)
      "
    />
    <pv-number-slider
      label="Pose Estimation Iterations"
      :label-cols="labelCols"
      :max="500"
      :min="0"
      :model-value="targetPipelineSettings.numIterations"
      :step="1"
      tooltip="Number of iterations the pose estimation algorithm will run, 50-100 is a good starting point"
      @update:model-value="
        (value) => serverStore.updatePipelineSettings(cameraSettings.cameraIndex, pipelineIndex, { numIterations: value }, true, true)
      "
    />
  </div>
</template>
