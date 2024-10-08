<script setup lang="ts">
import { inject, ref } from "vue";
import OfflineUpdateModal from "@/components/modals/OfflineUpdateModal.vue";
import SettingsImportModal from "@/components/modals/SettingsImportModal.vue";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";

const clientStore = useClientStore();
const serverStore = useServerStore();

const address = inject<string>("backendHost");

const exportLogFile = ref();
const exportSettings = ref();
</script>

<template>
  <v-card :disabled="!clientStore.backendConnected">
    <v-card-title class="mb-3 mt-2">Device Control</v-card-title>
    <v-row class="pl-4 pr-4" no-gutters>
      <v-col class="pb-3 pb-md-0 pr-md-1" cols="12" md="4">
        <v-btn
          class="w-100"
          color="red"
          prepend-icon="mdi-restart"
          text="Restart PhotonVision"
          @click="serverStore.restartProgram()"
        />
      </v-col>
      <v-col class="pb-3 pb-md-0 pr-md-1 pl-md-1" cols="12" md="4">
        <v-btn
          class="w-100"
          color="red"
          prepend-icon="mdi-restart-alert"
          text="Restart Device"
          @click="serverStore.restartDevice()"
        />
      </v-col>
      <v-col class="pb-3 pb-md-0 pl-md-1" cols="12" md="4">
        <OfflineUpdateModal>
          <template #activator="{ props }">
            <v-btn
              class="w-100"
              color="secondary"
              prepend-icon="mdi-upload"
              text="Offline Update"
              v-bind="props"
            />
          </template>
        </OfflineUpdateModal>
      </v-col>
    </v-row>
    <v-divider class="mt-2 mb-2 ml-4 mr-4" />
    <v-row class="pl-4 pr-4 pt-2 mb-4" no-gutters>
      <v-col class="pb-3 pb-sm-0 pr-sm-1 pb-sm-1" cols="12" sm="6">
        <SettingsImportModal>
          <template #activator="{ props }">
            <v-btn
              class="w-100"
              color="secondary"
              prepend-icon="mdi-import"
              text="Import Settings"
              v-bind="props"
            />
          </template>
        </SettingsImportModal>
      </v-col>
      <v-col class="pb-3 pb-sm-0 pl-sm-1 pb-sm-1" cols="12" sm="6">
        <v-btn
          class="w-100"
          color="secondary"
          prepend-icon="mdi-export"
          text="Export Settings"
          @click="exportSettings.click()"
        />
        <a
          ref="exportSettings"
          class="d-none"
          download="photonvision-settings.zip"
          :href="`http://${address}/api/settings/photonvision_config.zip`"
          target="_blank"
        />
      </v-col>
      <v-col class="pb-3 pb-sm-0 pr-sm-1 pt-sm-1" cols="12" sm="6">
        <v-btn
          class="w-100"
          color="secondary"
          prepend-icon="mdi-download"
          text="Download Current Log"
          @click="exportLogFile.click()"
        />
        <a
          ref="exportLogFile"
          class="d-none"
          download="photonvision-journalctl.txt"
          :href="`http://${address}/api/utils/photonvision-journalctl.txt`"
          target="_blank"
        />
      </v-col>
      <v-col class="pb-3 pb-sm-0 pl-sm-1 pt-sm-1" cols="12" sm="6">
        <v-btn
          class="w-100"
          color="secondary"
          prepend-icon="mdi-eye"
          text="Show log viewer"
          @click="clientStore.showLogModal = true"
        />
      </v-col>
    </v-row>
  </v-card>
</template>
