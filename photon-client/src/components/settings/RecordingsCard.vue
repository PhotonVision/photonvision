<script setup lang="ts">
import axios from "axios";
import { ref, inject, computed, watch } from "vue";
import { useTheme } from "vuetify";
import { axiosPost } from "@/lib/PhotonUtils";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import PvSelect from "@/components/common/pv-select.vue";
import PvDeleteModal from "@/components/common/pv-delete-modal.vue";
import PhotonCameraStream from "@/components/app/photon-camera-stream.vue";
import { useReplayStatus, type ActiveReplay } from "@/composables/useReplayStatus";
import type { UiCameraConfiguration } from "@/types/SettingTypes";

interface ResultInfo {
  hash: string;
  sizeBytes: number;
  resultCount: number;
  pipelineType: string;
  tssActiveAtRecord: boolean;
  mtimeMillis: number;
}

const AUTO_DOWNLOAD_KEY = "photonvision.recordings.autoDownloadResults";

const theme = useTheme();

const address = inject<string>("backendHost");

// Initialize selected recordings for each camera. The computed below seeds an entry for
// every camera with at least one recording before the table renders, so the lookup at v-model
// time is always a valid string — declaring undefined-in-value just produced a v-model type
// mismatch against pv-select's `T extends string | number`.
const selectedRecordings = ref<Record<string, string>>({});

const camerasWithRecordings = computed(() => {
  const cameras = useCameraSettingsStore().camerasWithRecordings;
  // Initialize selectedRecordings for any new cameras
  cameras.forEach((camera) => {
    if (!(camera.uniqueName in selectedRecordings.value) && camera.recordings.length > 0) {
      selectedRecordings.value[camera.uniqueName] = camera.recordings[0];
    }
  });
  return cameras;
});

const { active: activeReplays } = useReplayStatus();
const isReplayingHere = (cameraUniqueName: string) =>
  activeReplays.value.some((r) => r.cameraUniqueName === cameraUniqueName);

const resultsKey = (cameraUniqueName: string, recording: string) => `${cameraUniqueName}::${recording}`;
const expandedResults = ref<Record<string, boolean>>({});
const resultsCache = ref<Record<string, ResultInfo[]>>({});
const resultsLoading = ref<Record<string, boolean>>({});

const autoDownloadResults = ref<boolean>(window.localStorage.getItem(AUTO_DOWNLOAD_KEY) !== "false");
watch(autoDownloadResults, (v) => window.localStorage.setItem(AUTO_DOWNLOAD_KEY, String(v)));

const fetchResults = async (cameraUniqueName: string, recording: string): Promise<ResultInfo[]> => {
  const key = resultsKey(cameraUniqueName, recording);
  resultsLoading.value[key] = true;
  try {
    const res = await axios.get<ResultInfo[]>("/recordings/results", {
      params: { camera: cameraUniqueName, recording }
    });
    const sorted = [...res.data].sort((a, b) => b.mtimeMillis - a.mtimeMillis);
    resultsCache.value[key] = sorted;
    return sorted;
  } catch {
    resultsCache.value[key] = [];
    return [];
  } finally {
    resultsLoading.value[key] = false;
  }
};

const toggleResults = (camera: UiCameraConfiguration) => {
  const recording = selectedRecordings.value[camera.uniqueName];
  if (!recording) return;
  const key = resultsKey(camera.uniqueName, recording);
  const next = !expandedResults.value[key];
  expandedResults.value[key] = next;
  // Re-fetch on every open so the dropdown reflects results created since the
  // last expand (e.g. another replay landed while this row was collapsed).
  if (next) void fetchResults(camera.uniqueName, recording);
};

const resultDownloadHref = (cameraUniqueName: string, recording: string, hash: string) =>
  `http://${address}/api/recordings/result?camera=${encodeURIComponent(cameraUniqueName)}&recording=${encodeURIComponent(recording)}&hash=${encodeURIComponent(hash)}`;

const triggerDownload = (href: string) => {
  const link = document.createElement("a");
  link.href = href;
  // Server sets Content-Disposition; no need to set link.download.
  link.click();
};

const previousActive = ref<ActiveReplay[]>([]);
watch(
  activeReplays,
  (current) => {
    const ended = previousActive.value.filter(
      (prev) => !current.some((c) => c.cameraUniqueName === prev.cameraUniqueName)
    );
    previousActive.value = [...current];
    if (!autoDownloadResults.value) return;
    for (const entry of ended) {
      void (async () => {
        const list = await fetchResults(entry.cameraUniqueName, entry.recordingName);
        if (list.length === 0) return;
        triggerDownload(resultDownloadHref(entry.cameraUniqueName, entry.recordingName, list[0].hash));
      })();
    }
  },
  { deep: true }
);

const confirmDeleteDialog = ref({ show: false, recordings: [] as string[], cameraUniqueName: "" });

const deleteRecordings = (recordingsToDelete: string[], cameraUniqueName: string) => {
  void axiosPost("/recordings/delete", "delete " + recordingsToDelete.join(", "), {
    recordings: recordingsToDelete,
    cameraUniqueName: cameraUniqueName
  });
};

const showNukeDialog = ref(false);
const nukeRecordings = () => {
  void axiosPost("/recordings/nuke", "clear and reset all recordings");
};

const downloadIndividualRecording = (camera: UiCameraConfiguration) => {
  const recording = selectedRecordings.value[camera.uniqueName];
  const link = document.createElement("a");
  link.href = `http://${address}/api/recordings/exportIndividual?recording=${encodeURIComponent(recording ?? "")}&camera=${encodeURIComponent(camera.uniqueName)}`;
  link.download = `${camera.nickname}_${recording}_recording.zip`;
  link.click();
};

const replayRecording = (camera: UiCameraConfiguration) => {
  const recording = selectedRecordings.value[camera.uniqueName];
  if (!recording) return;
  void axiosPost("/recordings/replay", "replay " + recording + " on " + camera.nickname, {
    cameraUniqueName: camera.uniqueName,
    recording
  });
};

const downloadCameraRecordings = (camera: UiCameraConfiguration) => {
  const link = document.createElement("a");
  link.href = `http://${address}/api/recordings/exportCamera?camera=${encodeURIComponent(camera.uniqueName)}`;
  link.download = `${camera.nickname}_recordings.zip`;
  link.click();
};

const downloadAllRecordings = () => {
  const link = document.createElement("a");
  link.href = `http://${address}/api/recordings/export`;
  link.download = "photonvision-recordings-export.zip";
  link.click();
};
</script>

<template>
  <v-card class="mb-3" color="surface">
    <v-card-title>Recordings</v-card-title>
    <div class="pa-5 pt-0">
      <div v-if="camerasWithRecordings.length > 0">
        <v-row>
          <v-col cols="12" sm="6">
            <v-btn
              color="buttonPassive"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="downloadAllRecordings()"
            >
              <v-icon start class="open-icon"> mdi-export </v-icon>
              <span class="open-label">Export Recordings</span>
            </v-btn>
          </v-col>
          <v-col cols="12" sm="6">
            <v-btn
              color="error"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="() => (showNukeDialog = true)"
            >
              <v-icon left class="open-icon"> mdi-trash </v-icon>
              <span class="open-label">Clear all recordings</span>
            </v-btn>
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="12">
            <v-checkbox
              v-model="autoDownloadResults"
              label="Auto-download results when replay finishes"
              density="compact"
              hide-details
            />
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="">
            <v-table fixed-header height="100%" density="compact" dark>
              <thead style="font-size: 1.25rem">
                <tr>
                  <th>Camera</th>
                  <th>Selected Recording</th>
                  <th>Replay Selected</th>
                  <th>Delete Selected</th>
                  <th>Export Selected</th>
                  <th>Results</th>
                  <th>Delete All</th>
                  <th>Export All</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="camera in camerasWithRecordings" :key="camera.uniqueName">
                  <tr>
                    <td>{{ camera.nickname }}</td>
                    <td>
                      <pv-select v-model="selectedRecordings[camera.uniqueName]" :items="camera.recordings" />
                    </td>
                    <td class="text-right">
                      <v-btn
                        icon
                        small
                        color="buttonPassive"
                        title="Replay selected recording through this camera's pipeline"
                        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                        @click="replayRecording(camera)"
                      >
                        <v-icon size="large">mdi-play</v-icon>
                      </v-btn>
                    </td>
                    <td class="text-right">
                      <v-btn
                        icon
                        small
                        color="error"
                        title="Delete Selected Recording"
                        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                        @click="
                          () =>
                            (confirmDeleteDialog = {
                              show: true,
                              recordings: [selectedRecordings[camera.uniqueName] || ''],
                              cameraUniqueName: camera.uniqueName
                            })
                        "
                      >
                        <v-icon size="large">mdi-trash-can-outline</v-icon>
                      </v-btn>
                    </td>
                    <td class="text-right">
                      <v-btn
                        icon
                        small
                        color="buttonPassive"
                        title="Export Selected Recording"
                        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                        @click="downloadIndividualRecording(camera)"
                      >
                        <v-icon size="large">mdi-export</v-icon>
                      </v-btn>
                    </td>
                    <td class="text-right">
                      <v-btn
                        icon
                        small
                        color="buttonPassive"
                        title="Show per-tuning results for the selected recording"
                        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                        @click="toggleResults(camera)"
                      >
                        <v-icon size="large">
                          {{
                            expandedResults[resultsKey(camera.uniqueName, selectedRecordings[camera.uniqueName] || "")]
                              ? "mdi-chevron-up"
                              : "mdi-chevron-down"
                          }}
                        </v-icon>
                      </v-btn>
                    </td>
                    <td class="text-right">
                      <v-btn
                        icon
                        small
                        color="error"
                        title="Delete Recordings"
                        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                        @click="
                          () =>
                            (confirmDeleteDialog = {
                              show: true,
                              recordings: camera.recordings,
                              cameraUniqueName: camera.uniqueName
                            })
                        "
                      >
                        <v-icon size="large">mdi-trash-can-outline</v-icon>
                      </v-btn>
                    </td>
                    <td class="text-right">
                      <v-btn
                        icon
                        small
                        color="buttonPassive"
                        title="Export Recordings"
                        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                        @click="downloadCameraRecordings(camera)"
                      >
                        <v-icon size="large">mdi-export</v-icon>
                      </v-btn>
                    </td>
                  </tr>
                  <tr v-if="isReplayingHere(camera.uniqueName)" class="replay-preview-row">
                    <td colspan="8">
                      <div class="replay-preview-container">
                        <photon-camera-stream
                          :id="`replay-preview-${camera.uniqueName}`"
                          stream-type="Processed"
                          :camera-settings="camera"
                        />
                      </div>
                    </td>
                  </tr>
                  <tr
                    v-if="
                      selectedRecordings[camera.uniqueName] &&
                      expandedResults[resultsKey(camera.uniqueName, selectedRecordings[camera.uniqueName])]
                    "
                    class="results-detail-row"
                  >
                    <td colspan="8">
                      <div class="results-detail">
                        <div
                          v-if="resultsLoading[resultsKey(camera.uniqueName, selectedRecordings[camera.uniqueName])]"
                          class="results-empty"
                        >
                          Loading…
                        </div>
                        <div
                          v-else-if="
                            (resultsCache[resultsKey(camera.uniqueName, selectedRecordings[camera.uniqueName])] || [])
                              .length === 0
                          "
                          class="results-empty"
                        >
                          No exported results yet for this recording.
                        </div>
                        <table v-else class="results-table">
                          <thead>
                            <tr>
                              <th>Hash</th>
                              <th>Pipeline</th>
                              <th>Result Count</th>
                              <th>TSS</th>
                              <th>Download</th>
                            </tr>
                          </thead>
                          <tbody>
                            <tr
                              v-for="info in resultsCache[
                                resultsKey(camera.uniqueName, selectedRecordings[camera.uniqueName])
                              ]"
                              :key="info.hash"
                            >
                              <td>{{ info.hash.substring(0, 8) }}</td>
                              <td>{{ info.pipelineType }}</td>
                              <td>{{ info.resultCount }}</td>
                              <td>
                                <v-chip v-if="!info.tssActiveAtRecord" size="x-small" color="warning" variant="flat">
                                  no TSS
                                </v-chip>
                                <v-chip v-else size="x-small" color="success" variant="flat">TSS</v-chip>
                              </td>
                              <td>
                                <a
                                  :href="
                                    resultDownloadHref(
                                      camera.uniqueName,
                                      selectedRecordings[camera.uniqueName],
                                      info.hash
                                    )
                                  "
                                  download
                                >
                                  <v-icon color="primary">mdi-download</v-icon>
                                </a>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </td>
                  </tr>
                </template>
              </tbody>
            </v-table>
          </v-col>
        </v-row>
      </div>
    </div>
  </v-card>

  <pv-delete-modal
    v-model="showNukeDialog"
    title="Clear All Recordings"
    description="This will permanently delete all recordings from all cameras. This action cannot be undone."
    delete-text="Delete Recordings"
    :backup="() => downloadAllRecordings()"
    @confirm="nukeRecordings"
  />

  <pv-delete-modal
    v-model="confirmDeleteDialog.show"
    title="Confirm Delete Recordings"
    :description="'Are you sure you want to delete the recording(s) ' + confirmDeleteDialog.recordings.join(', ') + '?'"
    @confirm="() => deleteRecordings(confirmDeleteDialog.recordings, confirmDeleteDialog.cameraUniqueName)"
  />
</template>

<style scoped lang="scss">
.v-col-12 > .v-btn {
  width: 100%;
}

.pt-10px {
  padding-top: 10px !important;
}

@media only screen and (max-width: 351px) {
  .open-icon {
    margin: 0 !important;
  }
  .open-label {
    display: none;
  }
}
.replay-preview-row td {
  padding: 0.5rem !important;
  background-color: rgba(0, 0, 0, 0.2);
}
.replay-preview-container {
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  aspect-ratio: 16 / 9;
}

.results-detail-row td {
  padding: 0.5rem !important;
  background-color: rgba(0, 0, 0, 0.15);
}
.results-detail {
  padding: 0.25rem 0.75rem;
}
.results-empty {
  font-style: italic;
  opacity: 0.7;
  padding: 0.5rem 0;
}
.results-table {
  width: 100%;
  border-collapse: collapse;
  font-family: monospace !important;
}
.results-table th,
.results-table td {
  padding: 0.25rem 0.5rem;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.results-table th {
  font-weight: 600;
  opacity: 0.8;
}
.results-table tr:last-child td {
  border-bottom: none;
}

.v-table {
  width: 100%;
  height: 100%;
  text-align: center;

  th,
  td {
    font-size: 1rem !important;
    color: white !important;
    text-align: center !important;
  }

  td {
    font-family: monospace !important;
  }

  ::-webkit-scrollbar {
    width: 0;
    height: 0.55em;
    border-radius: 5px;
  }

  ::-webkit-scrollbar-track {
    -webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
    border-radius: 10px;
  }

  ::-webkit-scrollbar-thumb {
    background-color: rgb(var(--v-theme-accent));
    border-radius: 10px;
  }
}
</style>
