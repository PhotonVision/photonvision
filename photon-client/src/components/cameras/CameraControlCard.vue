<script setup lang="ts">
import { ref } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import IconFolder from "~icons/mdi/folder";
import IconInformationOutline from "~icons/mdi/information-outline";
import IconEye from "~icons/mdi/eye";
import IconDownload from "~icons/mdi/download";

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
        (snapshotData: { snapshotName: string; cameraUniqueName: string; snapshotData: string }, index: number) => {
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
  <pv-card>
    <div class="pb-2 text-base font-semibold">Camera Control</div>
    <div class="pt-1">
      <pv-button variant="passive" :icon="IconFolder" block @click="fetchSnapshots">
        <span class="open-label">Show Saved Snapshots</span>
      </pv-button>
    </div>
    <pv-dialog v-model="showSnapshotViewerDialog" :width="1500">
      <pv-card padding="none" class="p-5">
        <div class="pb-2 text-lg font-semibold">Saved Frame Snapshots</div>
        <div v-if="imgData.length === 0" class="pt-0">
          <pv-alert
            color="buttonPassive"
            density="compact"
            text="There are currently no saved snapshots."
            :icon="IconInformationOutline"
            variant="tonal"
          />
        </div>
        <div v-else class="pt-0">
          <pv-alert
            closable
            color="buttonPassive"
            density="compact"
            text="Snapshot timestamps depend on when the coprocessor was last connected to the internet."
            :icon="IconInformationOutline"
            variant="tonal"
          />
          <pv-data-table
            v-model:expanded="expanded"
            :columns="[
              { header: 'Snapshot Name', accessorKey: 'snapshotShortName', sortable: false },
              { header: 'Camera Unique Name', accessorKey: 'cameraUniqueName' },
              { header: 'Camera Nickname', accessorKey: 'cameraNickname' },
              { header: 'Stream Type', accessorKey: 'streamType' },
              { header: 'Time Created', accessorKey: 'timeCreated' },
              { header: 'Actions', accessorKey: 'actions', sortable: false }
            ]"
            :data="imgData"
            :grouping="['cameraUniqueName']"
            class="elevation-0"
            item-value="index"
            show-expand
          >
            <template #item.data-table-expand="{ internalItem, toggleExpand }">
              <pv-button
                size="icon"
                variant="ghost"
                :icon="IconEye"
                class="text-white/70 hover:text-white"
                @click="toggleExpand(internalItem)"
              />
            </template>

            <template #expanded-row="{ item, columns }">
              <td :colspan="columns.length">
                <div style="display: flex; justify-content: center; width: 100%">
                  <img :src="item.snapshotSrc" alt="snapshot-image" class="snapshot-preview pt-2 pb-2" />
                </div>
              </td>
            </template>
            <template #item.actions="{ item }">
              <div style="display: flex; justify-content: center">
                <a :download="item.snapshotName" :href="item.snapshotSrc">
                  <pv-icon size="small" :icon="IconDownload" />
                </a>
              </div>
            </template>
          </pv-data-table>
        </div>
      </pv-card>
    </pv-dialog>
  </pv-card>
</template>

<style scoped>
.v-divider {
  border-color: white !important;
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
    background-color: var(--color-pv-accent);
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
