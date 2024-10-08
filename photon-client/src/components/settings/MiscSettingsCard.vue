<script setup lang="ts">
import PvSwitch from "@/components/common/pv-switch.vue";
import { useServerStore } from "@/stores/ServerStore";
import { computed, ref, watch } from "vue";
import { MiscellaneousSettings } from "@/types/SettingTypes";
import _ from "lodash";

const serverStore = useServerStore();

const miscSettingsBuffer = ref<MiscellaneousSettings>();

const settingsValid = ref<boolean | null>(true);

const settingsHaveChanged = computed<boolean>(() => !_.isEqual(miscSettingsBuffer.value, serverStore.settings!.misc));

const saveChanges = () => {
  if (!settingsValid.value || !miscSettingsBuffer.value) return;

  serverStore.updateMiscSettings(miscSettingsBuffer.value);
};

watch(() => serverStore.settings?.misc, (newVal) => {
  miscSettingsBuffer.value = newVal;
}, { deep: true });
</script>

<template>
  <v-card>
    <v-card-title class="mb-3 mt-2">Miscellaneous Settings</v-card-title>
    <v-card-text v-if="!miscSettingsBuffer">No Miscellaneous Settings Found</v-card-text>
    <v-form v-else v-model="settingsValid" class="pl-4 pr-4" @submit.prevent="saveChanges">
      <pv-switch
        v-model="miscSettingsBuffer.matchCamerasOnlyByPath"
        label="Strictly match ONLY known cameras"
        :label-cols="4"
        tooltip="ONLY match cameras by the USB port they're plugged into + (basename or USB VID/PID), and never only by the device product string. Also disables automatic detection of new cameras."
      />
      <v-alert v-show="miscSettingsBuffer.matchCamerasOnlyByPath" density="compact" rounded type="info">
        <ul>
          <li>
            Physical cameras will be strictly matched to camera configurations using physical USB port they are plugged
            into, in addition to device name and other USB metadata. Additionally, no new cameras are allowed to be
            added. This setting is useful for guaranteeing that an already known and configured camera can never be
            matched as an "unknown"/"new" camera, which resets pipelines and calibration data.
          </li>
          <li>
            Cameras will NOT be matched if they change USB ports, and new cameras plugged into this coprocessor will NOT
            be automatically recognized or configured for vision processing.
          </li>
          <li>To add a new camera to this coprocessor, disable this setting, connect the camera, and re-enable.</li>
        </ul>
      </v-alert>
      <v-card-actions class="pl-0 pr-0">
        <v-btn
          class="w-100"
          color="accent"
          :disabled="!settingsHaveChanged"
          text="Save"
          type="submit"
          variant="elevated"
        />
      </v-card-actions>
    </v-form>
  </v-card>
</template>
