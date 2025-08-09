<script setup lang="ts">
import { ref } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import { useTheme } from "vuetify";

const theme = useTheme();

interface SnapshotMetadata {
  snapshotName: string;
  cameraNickname: string;
  streamType: "input" | "output";
  timeCreated: Date;
}
const getSnapshotMetadataFromName = (snapshotName: string): SnapshotMetadata => {
  snapshotName = snapshotName.replace(/\.[^/.]+$/, "");

  const data = snapshotName.split("_");

  const cameraName = data.slice(0, data.length - 2).join("_");
  const streamType = data[data.length - 2] as "input" | "output";
  const dateStr = data[data.length - 1];

  const year = parseInt(dateStr.substring(0, 4), 10);
  const month = parseInt(dateStr.substring(5, 7), 10) - 1; // Months are zero-based
  const day = parseInt(dateStr.substring(8, 10), 10);
  const hours = parseInt(dateStr.substring(11, 13), 10);
  const minutes = parseInt(dateStr.substring(13, 15), 10);
  const seconds = parseInt(dateStr.substring(15, 17), 10);
  const milliseconds = parseInt(dateStr.substring(17), 10);

  return {
    snapshotName: snapshotName,
    cameraNickname: cameraName,
    streamType: streamType,
    timeCreated: new Date(year, month, day, hours, minutes, seconds, milliseconds)
  };
};

interface Snapshot {
  index: number;
  snapshotName: string;
  snapshotShortName: string;
  cameraUniqueName: string;
  cameraNickname: string;
  streamType: "input" | "output";
  timeCreated: Date;
  snapshotSrc: string;
}
const imgData = ref<Snapshot[]>([]);
const fetchSnapshots = () => {
  axios
    .get("/utils/getImageSnapshots")
    .then((response) => {
      imgData.value = response.data.map(
        (snapshotData: { snapshotName: string; cameraUniqueName: string; snapshotData: string }, index) => {
          const metadata = getSnapshotMetadataFromName(snapshotData.snapshotName);

          return {
            index: index,
            snapshotName: snapshotData.snapshotName,
            snapshotShortName: metadata.snapshotName,
            cameraUniqueName: snapshotData.cameraUniqueName,
            cameraNickname: metadata.cameraNickname,
            streamType: metadata.streamType,
            timeCreated: metadata.timeCreated,
            snapshotSrc: "data:image/jpg;base64," + snapshotData.snapshotData
          };
        }
      );
      showSnapshotViewerDialog.value = true;
    })
    .catch((error) => {
      if (error.response) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: error.response.data.text || error.response.data
        });
      } else if (error.request) {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "Error while trying to process the request! The backend didn't respond."
        });
      } else {
        useStateStore().showSnackbarMessage({
          color: "error",
          message: "An error occurred while trying to process the request."
        });
      }
    });
};
const showSnapshotViewerDialog = ref(false);
const expanded = ref([]);
</script>

<template>
  <v-card color="surface" class="rounded-12">
    <v-card-title>Camera Control</v-card-title>
    <v-card-text class="pt-0">
      <v-btn
        color="buttonPassive"
        :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
        @click="fetchSnapshots"
      >
        <v-icon start class="open-icon" size="large"> mdi-folder </v-icon>
        <span class="open-label">Show Saved Snapshots</span>
      </v-btn>
    </v-card-text>
    <v-dialog v-model="showSnapshotViewerDialog">
      <v-card color="surface" flat>
        <v-card-title> Saved Frame Snapshots </v-card-title>
        <v-card-text v-if="imgData.length === 0" class="pt-0">
          <v-alert
            color="buttonPassive"
            density="compact"
            text="There are currently no saved snapshots."
            icon="mdi-information-outline"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
          />
        </v-card-text>
        <v-card-text v-else class="pt-0">
          <v-alert
            closable
            color="buttonPassive"
            density="compact"
            text="Snapshot timestamps depend on when the coprocessor was last connected to the internet."
            icon="mdi-information-outline"
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
          />
          <v-data-table
            v-model:expanded="expanded"
            :headers="[
              { title: 'Snapshot Name', key: 'snapshotShortName', sortable: false },
              { title: 'Camera Unique Name', key: 'cameraUniqueName' },
              { title: 'Camera Nickname', key: 'cameraNickname' },
              { title: 'Stream Type', key: 'streamType' },
              { title: 'Time Created', key: 'timeCreated' },
              { title: 'Actions', key: 'actions', sortable: false }
            ]"
            :items="imgData"
            :group-by="[{ key: 'cameraUniqueName' }]"
            class="elevation-0"
            item-value="index"
            show-expand
          >
            <template #item.data-table-expand="{ internalItem, toggleExpand }">
              <v-btn
                icon="mdi-eye"
                class="text-none"
                color="medium-emphasis"
                size="small"
                variant="text"
                slim
                @click="toggleExpand(internalItem)"
              ></v-btn>
            </template>

            <template #expanded-row="{ item, columns }">
              <td :colspan="columns.length">
                <div style="display: flex; justify-content: center; width: 100%">
                  <img :src="item.snapshotSrc" alt="snapshot-image" class="snapshot-preview pt-2 pb-2" />
                </div>
              </td>
            </template>
            <!-- eslint-disable-next-line vue/valid-v-slot-->
            <template #item.actions="{ item }">
              <div style="display: flex; justify-content: center">
                <a :download="item.snapshotName" :href="item.snapshotSrc">
                  <v-icon size="small"> mdi-download </v-icon>
                </a>
              </div>
            </template>
          </v-data-table>
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<style scoped lang="scss">
.v-divider {
  border-color: white !important;
}
.v-btn {
  width: 100%;
}
.v-table {
  text-align: center;

  th,
  td {
    font-size: 1rem !important;
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

.snapshot-preview {
  max-width: 55%;
}

@media only screen and (max-width: 512px) {
  .snapshot-preview {
    max-width: 100%;
  }
}
@media only screen and (max-width: 351px) {
  .open-icon {
    margin: 0 !important;
  }
  .open-label {
    display: none;
  }
}
</style>
