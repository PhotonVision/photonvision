<script setup lang="ts">
import {ref} from "vue";
import axios from "axios";
import {useStateStore} from "@/stores/StateStore";
import PvIcon from "@/components/common/pv-icon.vue";

interface ImageData {
  imgName: string;
  imgSrc: string;
}

const imgData = ref<ImageData[]>([]);
const fetchSnapshots = () => {
  axios
      .get("/utils/getImageSnapshots")
      .then((response) => {
        imgData.value = Object.entries(response.data as Record<string, string>).map(([k, v]) => {
          return {
            imgName: k,
            imgSrc: "data:image/jpg;base64," + v
          };
        });
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
const selectedImg = ref(-1);
const showSnapshotViewer = () => {
  fetchSnapshots();
  showSnapshotViewerDialog.value = true;
};
const showSpecificSnapshotDialog = ref(false);
</script>


<template>
  <v-card dark class="pr-6 pb-3" style="background-color: #006492">
    <v-card-title>Camera Control</v-card-title>
    <v-row class="pl-6">
      <v-col>
        <v-btn color="secondary" @click="showSnapshotViewer">
          <v-icon left> mdi-folder </v-icon>
          Show Saved Snapshots
        </v-btn>
      </v-col>
    </v-row>
    <v-dialog v-model="showSnapshotViewerDialog">
      <v-card dark class="pt-3 pl-5 pr-5" color="primary" flat>
        <v-card-title> View Saved Frame Snapshots </v-card-title>
        <v-divider />
        <v-card-text v-if="imgData.length === 0" style="font-size: 18px; font-weight: 600" class="pt-4">
          There are no snapshots saved
        </v-card-text>
        <div v-else>
          <div style="display: flex; justify-content: space-around; font-size: 18px" class="pb-2 pt-2">
            <span>Image Index</span>
            <span>Image Name</span>
            <span>View Image</span>
          </div>
          <v-virtual-scroll :items="imgData" item-height="50" height="600" class="mt-2">
            <template #default="{ item, index }">
              <div class="img-row">
                <div style="display: flex; justify-content: center; width: 33%">
                  {{ index }}
                </div>
                <div style="display: flex; justify-content: center; width: 33%">
                  {{ item.imgName }}
                </div>
                <div style="display: flex; justify-content: center; width: 33%">
                  <pv-icon
                      icon-name="mdi-image"
                      @click="
                      () => {
                        selectedImg = index;
                        showSpecificSnapshotDialog = true;
                      }
                    "
                  />
                </div>
              </div>
            </template>
          </v-virtual-scroll>
        </div>
      </v-card>
    </v-dialog>
    <v-dialog v-model="showSpecificSnapshotDialog" width="800px">
      <v-card v-if="imgData[selectedImg]" dark color="primary" flat class="pa-4">
        <v-card-title style="display: flex; justify-content: center">
          {{ imgData[selectedImg].imgName }}
        </v-card-title>
        <v-divider class="pb-3" />
        <div style="display: flex; align-items: center; justify-content: center">
          <img :src="imgData[selectedImg].imgSrc" alt="snapshot-image" style="width: 100%" />
        </div>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<style scoped>
.v-divider {
  border-color: white !important;
}
.v-btn {
  width: 100%;
}
.img-row {
  display: flex;
  width: 100%;
  justify-content: space-around;
}
</style>
