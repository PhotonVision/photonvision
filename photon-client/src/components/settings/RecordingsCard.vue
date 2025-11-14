<script setup lang="ts">
import { ref, inject, computed } from "vue";
import pvInput from "@/components/common/pv-input.vue";
import { useTheme } from "vuetify";
import { axiosPost } from "@/lib/PhotonUtils";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import PvSelect from "@/components/common/pv-select.vue";

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
  console.log(selectedRecordings);
  return cameras;
});

const confirmDeleteDialog = ref({ show: false, recordings: {} as string[], cameraUniqueName: "" });

const deleteRecordings = async (recordingsToDelete: string[], cameraUniqueName: string) => {
  axiosPost("/recordings/delete", "delete " + recordingsToDelete.join(", "), {
    recordings: recordingsToDelete,
    cameraUniqueName: cameraUniqueName
  });

  confirmDeleteDialog.value.show = false;
};

const exportRecordings = ref();
const exportCameraRecordings = ref();
const exportIndividualRecording = ref();

const showNukeDialog = ref(false);
const expected = "Delete Recordings";
const yesDeleteMyRecordingsText = ref("");
const nukeRecordings = () => {
  axiosPost("/recordings/nuke", "clear and reset all recordings");
  showNukeDialog.value = false;
};
</script>

<template>
  <v-card class="mb-3" color="surface">
    <v-card-title>Recordings</v-card-title>
    <div class="pa-5 pt-0">
      <v-row>
        <v-col cols="12" sm="6">
          <v-btn
            color="buttonPassive"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="() => exportRecordings.value.click()"
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
                  <pv-select
                    v-model="selectedRecordings[camera.uniqueName]"
                    :items="camera.recordings"
                    :select-cols="8"
                  />
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
                    @click="() => exportIndividualRecording.value.click()"
                  >
                    <v-icon size="large">mdi-export</v-icon>
                  </v-btn>
                  <a
                    ref="exportIndividualRecording"
                    style="color: black; text-decoration: none; display: none"
                    :href="`http://${address}/api/recordings/exportIndividual?recording=${selectedRecordings[camera.uniqueName]}?camera=${camera.uniqueName}`"
                    :download="`${camera.nickname}_${camera.recordings[0].slice(camera.recordings[0].lastIndexOf('/'))}_recording.zip`"
                    target="_blank"
                  />
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
                    @click="() => exportCameraRecordings.value.click()"
                  >
                    <v-icon size="large">mdi-export</v-icon>
                  </v-btn>
                  <a
                    ref="exportCameraRecordings"
                    style="color: black; text-decoration: none; display: none"
                    :href="`http://${address}/api/recordings/exportCamera?cameraPath=${camera.uniqueName}`"
                    :download="`${camera.nickname}_recordings.zip`"
                    target="_blank"
                  />
                </td>
              </tr>
            </tbody>
          </v-table>

          <v-dialog v-model="confirmDeleteDialog.show" width="600">
            <v-card color="surface" dark>
              <v-card-title>Delete Recording</v-card-title>
              <v-card-text class="pt-0">
                Are you sure you want to delete the recording(s) {{ confirmDeleteDialog.recordings.join(", ") }}?
                <v-card-actions class="pt-5 pb-0 pr-0" style="justify-content: flex-end">
                  <v-btn
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    color="buttonPassive"
                    @click="confirmDeleteDialog.show = false"
                  >
                    Cancel
                  </v-btn>
                  <v-btn
                    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                    color="error"
                    @click="deleteRecordings(confirmDeleteDialog.recordings, confirmDeleteDialog.cameraUniqueName)"
                  >
                    Delete
                  </v-btn>
                </v-card-actions>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
      </v-row>
    </div>

    <v-dialog v-model="showNukeDialog" width="800" dark>
      <v-card color="surface" flat>
        <v-card-title style="display: flex; justify-content: center">
          <span class="open-label">
            <v-icon end color="error" class="open-icon ma-1" size="large">mdi-alert-outline</v-icon>
            Delete All Recordings
            <v-icon end color="error" class="open-icon ma-1" size="large">mdi-alert-outline</v-icon>
          </span>
        </v-card-title>
        <v-card-text class="pt-0 pb-10px">
          <v-row class="align-center text-white">
            <v-col cols="12" md="6">
              <span> This will delete ALL OF YOUR RECORDINGS. </span>
            </v-col>
            <v-col cols="12" md="6">
              <v-btn
                color="buttonActive"
                style="float: right"
                :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
                @click="() => exportRecordings.click()"
              >
                <v-icon start class="open-icon" size="large"> mdi-export </v-icon>
                <span class="open-label">Backup Recordings</span>
              </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text class="pt-0 pb-0">
          <pv-input
            v-model="yesDeleteMyRecordingsText"
            :label="'Type &quot;' + expected + '&quot;:'"
            :label-cols="6"
            :input-cols="6"
          />
        </v-card-text>
        <v-card-text class="pt-10px">
          <v-btn
            color="error"
            width="100%"
            :disabled="yesDeleteMyRecordingsText.toLowerCase() !== expected.toLowerCase()"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            @click="nukeRecordings"
          >
            <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
            <span class="open-label">
              {{ $vuetify.display.mdAndUp ? "Delete recordings, I have backed up what I need" : "Delete Recordings" }}
            </span>
          </v-btn>
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-card>
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
