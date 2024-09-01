<script setup lang="ts">
import PvTextbox from "@/components/common/pv-textbox.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import type { ValidationRule } from "@/types/Components";
import { ref } from "vue";
import { nameChangeRegex } from "@/lib/PhotonUtils";

const dialogOpen = ref<boolean>();
const changingName = ref<boolean>(false);

// Camera Name Edit
const bufferCameraName = ref<string>(useCameraSettingsStore().currentCameraSettings.nickname);
const cameraNameRule: ValidationRule = (name: string) => {
  if (!nameChangeRegex.test(name)) {
    return "A camera name can only contain letters, numbers, spaces, underscores, hyphens, parenthesis, and periods";
  }
  if (useCameraSettingsStore().cameraNames.some((cameraName) => cameraName === name)) {
    return "This camera name has already been used";
  }

  return true;
};
const saveCameraName = (name: string) => {
  changingName.value = true;
  useCameraSettingsStore()
    .changeCameraNickname(name, false)
    .then((response) => {
      useStateStore().showSnackbarMessage({
        color: "success",
        message: response.data.text || response.data
      });
      useCameraSettingsStore().currentCameraSettings.nickname = name;
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
      bufferCameraName.value = useCameraSettingsStore().currentCameraSettings.nickname;
    })
    .finally(() => {
      changingName.value = false;
      dialogOpen.value = false;
    });
};
</script>

<template>
  <v-dialog v-model="dialogOpen" max-width="700px">
    <template #activator="{ props }">
      <slot v-bind="{ props }" />
    </template>
    <template #default="{ isActive }">
      <v-card class="pa-3">
        <v-card-title>Edit Camera Name: {{ useCameraSettingsStore().currentCameraName }}</v-card-title>
        <v-divider />
        <pv-textbox
          v-model="bufferCameraName"
          class="pl-3 pr-3 pt-3"
          label="New Camera Name"
          :rules="[cameraNameRule]"
        />
        <v-card-actions>
          <v-btn
            color="accent"
            :disabled="cameraNameRule(bufferCameraName) != true"
            :loading="changingName"
            text="Save"
            variant="elevated"
            @click="saveCameraName(bufferCameraName)"
          />
          <v-btn
            color="error"
            :disabled="changingName"
            text="Close"
            variant="elevated"
            @click="isActive.value = false"
          />
        </v-card-actions>
      </v-card>
    </template>
  </v-dialog>
</template>
