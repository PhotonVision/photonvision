<script setup lang="ts">
import { inject, ref } from "vue";
import axios from "axios";

const modalOpen = ref(false);

const offlineUpdateConfig = ref<{
  selectedJar: File[] | undefined;
  exportSettings: boolean;
}>({
  selectedJar: undefined,
  exportSettings: false
});
const offlineUpdateState = ref<{
  uploadingJar: boolean;
  startedUpload: boolean;
  uploadPercentage: number;
}>({
  uploadingJar: false,
  startedUpload: false,
  uploadPercentage: 0
});

const address = inject<string>("backendHost");
const exportSettings = ref();

const startOfflineUpdate = () => {
  const selectedJar = offlineUpdateConfig.value.selectedJar;
  if (!selectedJar) return;

  if (offlineUpdateConfig.value.exportSettings) {
    exportSettings.value.click();
  }

  const formData = new FormData();
  formData.append("jarData", selectedJar[0]);

  offlineUpdateState.value.uploadingJar = true;
  axios
    .post("/utils/offlineUpdate", formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: ({ progress }) => {
        const uploadPercentage = (progress || 0) * 100.0;

        if (uploadPercentage > 0.0) {
          offlineUpdateState.value.startedUpload = true;
        }

        offlineUpdateState.value.uploadPercentage = uploadPercentage;
      }
    })
    .then((response) => {
      // TODO handle this
      // useStateStore().showSnackbarMessage({
      //   message: response.data.text || response.data,
      //   color: "success"
      // });
    })
    .catch((error) => {
      // TODO handle this
      // if (error.response) {
      //   useStateStore().showSnackbarMessage({
      //     color: "error",
      //     message: error.response.data.text || error.response.data
      //   });
      // } else if (error.request) {
      //   useStateStore().showSnackbarMessage({
      //     color: "error",
      //     message: "Error while trying to process the request! The backend didn't respond."
      //   });
      // } else {
      //   useStateStore().showSnackbarMessage({
      //     color: "error",
      //     message: "An error occurred while trying to process the request."
      //   });
      // }
    })
    .finally(() => {
      offlineUpdateConfig.value = {
        selectedJar: undefined,
        exportSettings: false
      };
      offlineUpdateState.value = {
        uploadingJar: false,
        startedUpload: false,
        uploadPercentage: 0
      };

      // Close modal after offline update
      setTimeout(() => (modalOpen.value = false), 500);

      // TODO force websocket reconnect after offline update (same as restart program and restart device)
    });
};
</script>

<template>
  <v-dialog v-model="modalOpen" max-width="700px" :persistent="offlineUpdateState.uploadingJar">
    <template #activator="{ props }">
      <slot name="activator" v-bind="{ props }" />
    </template>
    <template #default="{ isActive }">
      <v-card class="pa-3">
        <v-progress-linear
          v-show="offlineUpdateState.uploadingJar"
          color="accent"
          :indeterminate="!offlineUpdateState.startedUpload"
          :model-value="offlineUpdateState.uploadPercentage"
          :stream="offlineUpdateState.startedUpload"
        />
        <v-card-title>Photonvision Offline Update</v-card-title>
        <v-card-subtitle>Offline update by replacing the program Jar</v-card-subtitle>

        <v-file-input
          v-model="offlineUpdateConfig.selectedJar"
          accept=".jar"
          class="pt-6 pr-3 pl-3"
          hide-details
          label="Select Jar"
        />

        <div v-show="offlineUpdateConfig.selectedJar" class="pr-3 pl-3">
          <v-divider class="mt-3 mb-3" />
          <v-checkbox v-model="offlineUpdateConfig.exportSettings" hide-details label="Export Settings Before Update" />
          <v-alert
            v-show="offlineUpdateConfig.selectedJar && !offlineUpdateConfig.exportSettings"
            density="compact"
            rounded
            text="It is recommended that you always export settings before an offline update in case the update leads to setting loss."
            type="warning"
          />
          <a
            ref="exportSettings"
            class="d-none"
            download="photonvision-settings.zip"
            :href="`http://${address}/api/settings/photonvision_config.zip`"
            target="_blank"
          />
        </div>

        <v-card-actions class="mt-3">
          <v-btn
            color="accent"
            :disabled="!offlineUpdateConfig.selectedJar"
            :loading="offlineUpdateState.uploadingJar"
            text="Start Offline Update"
            variant="elevated"
            @click="startOfflineUpdate"
          />
          <v-btn
            color="secondary"
            :disabled="offlineUpdateState.uploadingJar"
            text="Cancel"
            variant="elevated"
            @click="() => (isActive.value = false)"
          />
        </v-card-actions>
      </v-card>
    </template>
  </v-dialog>
</template>
