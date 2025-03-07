<script setup lang="ts">
import { ref } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";

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
  <v-card dark style="background-color: #006492">
    <v-card-title>Camera Control</v-card-title>
    <v-card-text>
      <v-btn color="secondary" @click="fetchSnapshots">
        <v-icon left class="open-icon"> mdi-folder </v-icon>
        <span class="open-label">Show Saved Snapshots</span>
      </v-btn>
    </v-card-text>
    <v-dialog v-model="showSnapshotViewerDialog">
      <v-card dark class="pt-3 pl-5 pr-5" color="primary" flat>
        <v-card-title> View Saved Frame Snapshots </v-card-title>
        <v-divider />
        <v-card-text v-if="imgData.length === 0" style="font-size: 18px; font-weight: 600" class="pt-4">
          There are no snapshots saved
        </v-card-text>
        <div v-else class="pb-2">
          <v-data-table
            v-model:expanded="expanded"
            :headers="[
              { text: 'Snapshot Name', value: 'snapshotShortName', sortable: false },
              { text: 'Camera Unique Name', value: 'cameraUniqueName' },
              { text: 'Camera Nickname', value: 'cameraNickname' },
              { text: 'Stream Type', value: 'streamType' },
              { text: 'Time Created', value: 'timeCreated' },
              { text: 'Actions', value: 'actions', sortable: false }
            ]"
            :items="imgData"
            group-by="cameraUniqueName"
            class="elevation-0"
            item-key="index"
            show-expand
            expand-icon="mdi-eye"
          >
            <template #expanded-item="{ headers, item }">
              <td :colspan="headers.length">
                <div style="display: flex; justify-content: center; width: 100%">
                  <img :src="item.snapshotSrc" alt="snapshot-image" class="snapshot-preview pt-2 pb-2" />
                </div>
              </td>
            </template>
            <!-- eslint-disable-next-line vue/valid-v-slot-->
            <template #item.actions="{ item }">
              <div style="display: flex; justify-content: center">
                <a :download="item.snapshotName" :href="item.snapshotSrc">
                  <v-icon small> mdi-download </v-icon>
                </a>
              </div>
            </template>
          </v-data-table>
          <span
            >Snapshot Timestamps may be incorrect as they depend on when the coprocessor was last connected to the
            internet</span
          >
        </div>
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
.v-data-table {
  text-align: center;
  background-color: #006492 !important;

  th,
  td {
    background-color: #005281 !important;
    font-size: 1rem !important;
  }

  tbody :hover tr {
    background-color: #005281 !important;
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
    background-color: #ffd843;
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
