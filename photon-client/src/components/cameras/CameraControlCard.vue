<script setup lang="ts">
import { ref } from "vue";
import axios from "axios";
import { useStateStore } from "@/stores/StateStore";
import IconFolder from "~icons/mdi/folder";
import IconInformationOutline from "~icons/mdi/information-outline";
import IconEye from "~icons/mdi/eye";
import IconDownload from "~icons/mdi/download";
import { getSnapshotMetadataFromName } from "./snapshotMetadata";

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
const emptyGroupedCell = () => "";
const imgData = ref<Snapshot[]>([]);
const fetchSnapshots = () => {
  axios
    .get("/utils/getImageSnapshots")
    .then((response) => {
      imgData.value = response.data.snapshots.map(
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
      <pv-card>
        <div class="pb-2 text-lg font-semibold">Saved Frame Snapshots</div>
        <div v-if="imgData.length === 0" class="pt-0">
          <pv-alert
            color="buttonPassive"
            text="There are currently no saved snapshots."
            :icon="IconInformationOutline"
            variant="tonal"
          />
        </div>
        <div v-else class="pt-0">
          <pv-alert
            closable
            color="buttonPassive"
            text="Snapshot timestamps depend on when the coprocessor was last connected to the internet."
            :icon="IconInformationOutline"
            variant="tonal"
            class="mb-2"
          />
          <pv-data-table
            v-model:expanded="expanded"
            :columns="[
              { header: 'Snapshot Name', accessorKey: 'snapshotShortName', enableSorting: false, aggregatedCell: emptyGroupedCell },
              { header: 'Camera Nickname', accessorKey: 'cameraNickname' },
              { header: 'Stream Type', accessorKey: 'streamType', aggregatedCell: emptyGroupedCell },
              { header: 'Time Created', accessorKey: 'timeCreated', aggregatedCell: emptyGroupedCell },
              { header: 'Actions', accessorKey: 'actions', enableSorting: false, aggregatedCell: emptyGroupedCell }
            ]"
            :data="imgData"
            :grouping="['cameraNickname']"
            item-value="index"
            show-expand
          >
            <template #item.data-table-expand="{ toggleExpand }">
              <pv-button
                size="icon"
                variant="ghost"
                :icon="IconEye"
                class="text-pv-on-surface/70 hover:text-pv-on-surface"
                @click="toggleExpand()"
              />
            </template>

            <template #expanded-row="{ item, columns }">
              <td :colspan="columns.length">
                <div style="display: flex; justify-content: center; width: 100%">
                  <img :src="(item as Snapshot).snapshotSrc" alt="snapshot-image" class="snapshot-preview pt-2 pb-2" />
                </div>
              </td>
            </template>
            <template #item.actions="{ item }">
              <div style="display: flex; justify-content: center">
                <a :download="(item as Snapshot).snapshotName" :href="(item as Snapshot).snapshotSrc">
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
.pv-table {
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
