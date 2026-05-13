<script setup lang="ts">
import { computed, inject, ref } from "vue";
import { useTheme } from "vuetify";
import { axiosPost } from "@/lib/PhotonUtils";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import PvSelect from "@/components/common/pv-select.vue";

// Mounted on the per-camera config view (CameraSettingsView). Surfaces the live camera's
// own recordings as replay targets: pick one, hit Replay, the live FrameProvider is swapped
// for a FileLogFrameProvider until EOF or Cancel (server-side state machine in
// VisionModule.startReplay / cancelReplay). The current pipeline + calibration runs against
// the replayed frames, and JsonResultExporter tees a results/<hash>.jsonl into the recording
// dir per tuning attempt.
//
// Scope of this commit: dropdown + Replay / Cancel / Download buttons. Results-list
// enumeration and a top-of-page progress banner driven by NT topics are intentional follow-ups
// (need a new server endpoint and an NT→websocket bridge respectively).

const theme = useTheme();
const address = inject<string>("backendHost");

const currentCamera = computed(() => useCameraSettingsStore().currentCameraSettings);
const recordings = computed<string[]>(() => currentCamera.value.recordings ?? []);

// Default selection: newest recording. Names embed a timestamp so reverse-lexicographic ==
// newest-first; the server writes them via FrameRecorder.startRecording which prefixes the
// directory name with the start-of-recording wall clock.
const selectedRecording = ref<string>("");
const sortedRecordings = computed<string[]>(() => [...recordings.value].sort().reverse());
const effectiveSelection = computed<string>(() =>
  selectedRecording.value && recordings.value.includes(selectedRecording.value)
    ? selectedRecording.value
    : (sortedRecordings.value[0] ?? "")
);

const hasRecordings = computed<boolean>(() => recordings.value.length > 0);

const startReplay = () => {
  const recording = effectiveSelection.value;
  if (!recording) return;
  void axiosPost("/recordings/replay", "replay " + recording + " on " + currentCamera.value.nickname, {
    cameraUniqueName: currentCamera.value.uniqueName,
    recording
  });
};

const cancelReplay = () => {
  void axiosPost("/recordings/replay/cancel", "cancel replay on " + currentCamera.value.nickname, {
    cameraUniqueName: currentCamera.value.uniqueName
  });
};

const downloadRecording = () => {
  const recording = effectiveSelection.value;
  if (!recording) return;
  const link = document.createElement("a");
  link.href = `http://${address}/api/recordings/exportIndividual?recording=${encodeURIComponent(recording)}&camera=${encodeURIComponent(currentCamera.value.uniqueName)}`;
  link.download = `${currentCamera.value.nickname}_${recording}_recording.zip`;
  link.click();
};
</script>

<template>
  <v-card class="mb-3 rounded-12" color="surface" dark>
    <v-card-title class="pb-0">Replay</v-card-title>
    <v-card-text class="pt-3">
      <div v-if="!hasRecordings" class="text-medium-emphasis">
        No recordings exist for this camera yet. Trigger a recording from robot code (PhotonCamera.setRecording(true))
        or via the matchData NT topic, then return here to replay it through the current pipeline.
      </div>
      <div v-else>
        <pv-select
          v-model="selectedRecording"
          label="Recording"
          tooltip="Recordings for this camera, newest first. Pick one to replay through the current pipeline + calibration."
          :items="sortedRecordings"
          :select-cols="8"
        />
        <v-row no-gutters>
          <v-col cols="12" sm="4" class="pr-sm-2 pb-2 pb-sm-0">
            <v-btn
              block
              size="small"
              color="buttonActive"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              :disabled="!effectiveSelection"
              title="Swap the live frame provider for this recording. Live camera goes offline for the duration; pipeline + calibration are reused as-is."
              @click="startReplay"
            >
              <v-icon start size="large">mdi-play</v-icon>
              Replay with current pipeline
            </v-btn>
          </v-col>
          <v-col cols="12" sm="4" class="px-sm-2 pb-2 pb-sm-0">
            <v-btn
              block
              size="small"
              color="error"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              title="Force-end the active replay on this camera. Idempotent."
              @click="cancelReplay"
            >
              <v-icon start size="large">mdi-stop</v-icon>
              Cancel replay
            </v-btn>
          </v-col>
          <v-col cols="12" sm="4" class="pl-sm-2">
            <v-btn
              block
              size="small"
              color="buttonPassive"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              :disabled="!effectiveSelection"
              title="Download a zip of the recording directory — frames, metadata.jsonl, tss.json, and every results/<hash>.jsonl produced so far."
              @click="downloadRecording"
            >
              <v-icon start size="large">mdi-download</v-icon>
              Download recording zip
            </v-btn>
          </v-col>
        </v-row>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.v-divider {
  border-color: white !important;
}
</style>
