<script setup lang="ts">
import PvSelect, { type SelectItem } from "@/components/common/pv-select.vue";
import PvInput from "@/components/common/pv-input.vue";
import PvNumberInput from "@/components/common/pv-number-input.vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { computed, ref, watchEffect } from "vue";
import { type CameraSettingsChangeRequest, ValidQuirks } from "@/types/SettingTypes";
import { useTheme } from "vuetify";
import { axiosPost } from "@/lib/PhotonUtils";

const theme = useTheme();

const tempSettingsStruct = ref<CameraSettingsChangeRequest>({
  fov: useCameraSettingsStore().currentCameraSettings.fov.value,
  quirksToChange: Object.assign({}, useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks)
});

const arducamSelectWrapper = computed<number>({
  get: () => {
    if (tempSettingsStruct.value.quirksToChange.ArduOV9281Controls) return 1;
    else if (tempSettingsStruct.value.quirksToChange.ArduOV2311Controls) return 2;
    else if (tempSettingsStruct.value.quirksToChange.ArduOV9782Controls) return 3;
    else return 0;
  },
  set: (v) => {
    switch (v) {
      case 1:
        tempSettingsStruct.value.quirksToChange.ArduOV9281Controls = true;
        tempSettingsStruct.value.quirksToChange.ArduOV2311Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV9782Controls = false;
        break;
      case 2:
        tempSettingsStruct.value.quirksToChange.ArduOV9281Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV2311Controls = true;
        tempSettingsStruct.value.quirksToChange.ArduOV9782Controls = false;
        break;
      case 3:
        tempSettingsStruct.value.quirksToChange.ArduOV9281Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV2311Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV9782Controls = true;
        break;
      default:
        tempSettingsStruct.value.quirksToChange.ArduOV9281Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV2311Controls = false;
        tempSettingsStruct.value.quirksToChange.ArduOV9782Controls = false;
        break;
    }
  }
});

const currentCameraIsArducam = computed<boolean>(
  () => useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks.ArduCamCamera
);

const settingsHaveChanged = (): boolean => {
  const a = tempSettingsStruct.value;
  const b = useCameraSettingsStore().currentCameraSettings;

  for (const q in ValidQuirks) {
    if (a.quirksToChange[q] != b.cameraQuirks.quirks[q]) return true;
  }

  return a.fov != b.fov.value;
};

const resetTempSettingsStruct = () => {
  tempSettingsStruct.value.fov = useCameraSettingsStore().currentCameraSettings.fov.value;
  tempSettingsStruct.value.quirksToChange = Object.assign(
    {},
    useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks
  );
};

const saveCameraSettings = () => {
  useCameraSettingsStore()
    .updateCameraSettings(tempSettingsStruct.value)
    .then((response) => {
      useStateStore().showSnackbarMessage({ color: "success", message: response.data.text || response.data });

      // Update the local settings cause the backend checked their validity. Assign is to deref value
      useCameraSettingsStore().currentCameraSettings.fov.value = tempSettingsStruct.value.fov;
      useCameraSettingsStore().currentCameraSettings.cameraQuirks.quirks = Object.assign(
        {},
        tempSettingsStruct.value.quirksToChange
      );
    })
    .catch((error) => {
      resetTempSettingsStruct();
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

watchEffect(() => {
  // Reset temp settings on remote camera settings change
  resetTempSettingsStruct();
});

const showDeleteCamera = ref(false);
const yesDeleteMySettingsText = ref("");
const deletingCamera = ref(false);
const deleteThisCamera = () => {
  if (deletingCamera.value) return;
  deletingCamera.value = true;

  const payload = { cameraUniqueName: useStateStore().currentCameraUniqueName };

  axiosPost("/utils/nukeOneCamera", "delete this camera", payload).finally(() => {
    deletingCamera.value = false;
    showDeleteCamera.value = false;
  });
};
const wrappedCameras = computed<SelectItem[]>(() =>
  Object.keys(useCameraSettingsStore().cameras).map((cameraUniqueName) => ({
    name: useCameraSettingsStore().cameras[cameraUniqueName].nickname,
    value: cameraUniqueName
  }))
);
</script>

<template>
  <v-card class="mb-3 rounded-12" color="surface" dark>
    <v-card-title class="pb-0">Camera Settings</v-card-title>
    <v-card-text class="pt-3">
      <pv-select
        v-model="useStateStore().currentCameraUniqueName"
        label="Camera"
        :items="wrappedCameras"
        :select-cols="8"
      />
      <pv-number-input
        v-model="tempSettingsStruct.fov"
        :tooltip="
          !useCameraSettingsStore().currentCameraSettings.fov.managedByVendor
            ? 'Field of view (in degrees) of the camera measured across the diagonal of the frame, in a video mode which covers the whole sensor area.'
            : 'This setting is managed by a vendor'
        "
        label="Maximum Diagonal FOV"
        :disabled="useCameraSettingsStore().currentCameraSettings.fov.managedByVendor"
        :label-cols="4"
      />
      <pv-select
        v-show="currentCameraIsArducam"
        v-model="arducamSelectWrapper"
        label="Arducam Model"
        :items="[
          { name: 'None', value: 0, disabled: true },
          { name: 'OV9281', value: 1 },
          { name: 'OV2311', value: 2 },
          { name: 'OV9782', value: 3 }
        ]"
        :select-cols="8"
      />
    </v-card-text>
    <v-card-text class="d-flex pt-0">
      <v-col cols="6" class="pa-0 pr-2">
        <v-btn
          block
          size="small"
          color="primary"
          :disabled="!settingsHaveChanged()"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="saveCameraSettings"
        >
          <v-icon start size="large"> mdi-content-save </v-icon>
          Save Changes
        </v-btn>
      </v-col>
      <v-col cols="6" class="pa-0 pl-2">
        <v-btn
          block
          size="small"
          color="error"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="() => (showDeleteCamera = true)"
        >
          <v-icon start size="large"> mdi-trash-can-outline </v-icon>
          Delete Camera
        </v-btn>
      </v-col>
    </v-card-text>

    <v-dialog v-model="showDeleteCamera" width="800">
      <v-card color="surface" flat>
        <v-card-title> Delete {{ useCameraSettingsStore().currentCameraSettings.nickname }}? </v-card-title>
        <v-card-text class="pt-0 pb-10px">
          Are you sure you want to delete "{{ useCameraSettingsStore().currentCameraSettings.nickname }}"? This cannot
          be undone.
        </v-card-text>
        <v-card-text class="pt-0 pb-10px">
          <pv-input
            v-model="yesDeleteMySettingsText"
            :label="'Type &quot;' + useCameraSettingsStore().currentCameraName + '&quot;:'"
            :label-cols="6"
            :input-cols="6"
          />
        </v-card-text>
        <v-card-actions class="pa-5 pt-0">
          <v-btn
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            color="primary"
            class="text-black"
            @click="showDeleteCamera = false"
          >
            Cancel
          </v-btn>
          <v-btn
            color="error"
            :disabled="
              yesDeleteMySettingsText.toLowerCase() !== useCameraSettingsStore().currentCameraName.toLowerCase()
            "
            :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
            :loading="deletingCamera"
            @click="deleteThisCamera"
          >
            <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
            <span class="open-label">Delete</span>
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<style scoped>
.v-divider {
  border-color: white !important;
}
</style>
