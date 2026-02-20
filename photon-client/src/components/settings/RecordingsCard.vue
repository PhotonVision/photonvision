<script setup lang="ts">
import { ref, inject, computed } from "vue";
import { useTheme } from "vuetify";
import { axiosPost } from "@/lib/PhotonUtils";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import PvSelect from "@/components/common/pv-select.vue";
import PvDeleteModal from "@/components/common/pv-delete-modal.vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";

const theme = useTheme();

const address = inject<string>("backendHost");

// Initialize selected recordings for each camera
const selectedRecordings = ref<Record<string, string | undefined>>({});

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

const confirmDeleteDialog = ref({ show: false, recordings: [] as string[], cameraUniqueName: "" });

const deleteRecordings = async (recordingsToDelete: string[], cameraUniqueName: string) => {
  axiosPost("/recordings/delete", "delete " + recordingsToDelete.join(", "), {
    recordings: recordingsToDelete,
    cameraUniqueName: cameraUniqueName
  });
};

const showNukeDialog = ref(false);
const nukeRecordings = () => {
  axiosPost("/recordings/nuke", "clear and reset all recordings");
};

const downloadIndividualRecording = (camera: any) => {
  const recording = selectedRecordings.value[camera.uniqueName];
  const link = document.createElement("a");
  link.href = `http://${address}/api/recordings/exportIndividual?recording=${recording}&camera=${camera.uniqueName}`;
  link.download = `${camera.nickname}_${recording}_recording.mp4`;
  link.click();
};

const downloadCameraRecordings = (camera: any) => {
  const link = document.createElement("a");
  link.href = `http://${address}/api/recordings/exportCamera?camera=${camera.uniqueName}`;
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
      <pv-select
        v-model="useSettingsStore().general.recordingStrategy"
        label="Recording Strategy"
        :items="useSettingsStore().general.supportedRecordingStrategies"
        @update:modelValue="(args) => useSettingsStore().setRecordingStrategy(String(args ?? ''))"
      />
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
            <a
              ref="exportRecordings"
              style="color: black; text-decoration: none; display: none"
              :href="`http://${address}/api/recordings/export`"
              download="photonvision-recordings-export.zip"
              target="_blank"
            />
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
          <v-col cols="">
            <v-table fixed-header height="100%" density="compact" dark>
              <thead style="font-size: 1.25rem">
                <tr>
                  <th>Camera</th>
                  <th>Selected Recording</th>
                  <th>Delete Selected</th>
                  <th>Export Selected</th>
                  <th>Delete All</th>
                  <th>Export All</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="camera in camerasWithRecordings" :key="camera.uniqueName">
                  <td>{{ camera.nickname }}</td>
                  <td>
                    <pv-select v-model="selectedRecordings[camera.uniqueName]" :items="camera.recordings" />
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
    :description="'This will permanently delete all recordings from all cameras. This action cannot be undone.'"
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
