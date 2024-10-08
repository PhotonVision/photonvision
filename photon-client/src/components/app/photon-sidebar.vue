<script setup lang="ts">
import { computed } from "vue";
import { useDisplay, useTheme } from "vuetify";
import { useClientStore } from "@/stores/ClientStore";
import { useServerStore } from "@/stores/ServerStore";

const clientStore = useClientStore();
const serverStore = useServerStore();

const { mdAndUp } = useDisplay();

const themeHandler = useTheme();
const currentThemeIcon = computed(() => themeHandler.current.value.variables.icon as string);

const themeSwitch = computed<boolean>({
  get: () => themeHandler.global.current.value.dark,
  set: (v) => (themeHandler.global.name.value = v ? "PhotonVisionDarkTheme" : "PhotonVisionClassicTheme")
});

const cycleTheme = () => {
  themeHandler.global.name.value = themeHandler.global.current.value.dark
    ? "PhotonVisionClassicTheme"
    : "PhotonVisionDarkTheme";
};
</script>

<template>
  <v-navigation-drawer permanent :rail="clientStore.sidebarFolded || !mdAndUp">
    <div class="mt-2 pb-6" style="height: 155px">
      <div>
        <v-img v-if="!clientStore.sidebarFolded && mdAndUp" alt="large logo" class="logo" src="@/assets/images/logoLarge.svg" />
        <v-img v-else alt="small logo" class="logo" src="@/assets/images/logoSmall.svg" />
      </div>
      <div class="d-flex justify-center">
        <v-switch
          v-if="!clientStore.sidebarFolded && mdAndUp"
          v-model="themeSwitch"
          append-icon="mdi-controller"
          hide-details
          prepend-icon="mdi-controller-classic"
          style="justify-items: center; max-width: 120px"
        />
        <v-btn v-else class="mt-4" :icon="currentThemeIcon" size="large" variant="plain" @click="cycleTheme" />
      </div>
    </div>

    <v-list>
      <v-list-item link to="/dashboard">
        <template #prepend>
          <v-icon icon="mdi-view-dashboard" />
        </template>
        <v-list-item-title class="pa-0 pt-3 pb-3">Dashboard</v-list-item-title>
      </v-list-item>
      <v-list-item link to="/cameras">
        <template #prepend>
          <v-icon icon="mdi-camera" />
        </template>
        <v-list-item-title class="pa-0 pt-3 pb-3">Cameras</v-list-item-title>
      </v-list-item>
      <v-list-item link to="/settings">
        <template #prepend>
          <v-icon icon="mdi-cog" />
        </template>
        <v-list-item-title class="pa-0 pt-3 pb-3">Settings</v-list-item-title>
      </v-list-item>
      <v-list-item link to="/docs">
        <template #prepend>
          <v-icon icon="mdi-bookshelf" />
        </template>
        <v-list-item-title class="pa-0 pt-3 pb-3">Documentation</v-list-item-title>
      </v-list-item>

      <v-list-item v-if="mdAndUp" link @click="() => (clientStore.sidebarFolded = !clientStore.sidebarFolded)">
        <template #prepend>
          <v-icon v-if="clientStore.sidebarFolded || !mdAndUp" icon="mdi-chevron-right" />
          <v-icon v-else icon="mdi-chevron-left" />
        </template>
        <v-list-item-title class="pa-0 pt-3 pb-3">Compact Mode</v-list-item-title>
      </v-list-item>
    </v-list>
    <v-list style="position: absolute; bottom: 0">
      <v-list-item>
        <template #prepend>
          <v-icon v-if="serverStore.settings?.network.runNTServer" icon="mdi-server" />
          <v-icon v-else-if="clientStore.ntConnectionStatus.connected" icon="mdi-robot" />
          <v-icon v-else icon="mdi-robot-off" style="border-radius: 100%" />
        </template>
        <v-list-item-title>NT Status</v-list-item-title>
        <v-list-item-subtitle v-if="serverStore.settings?.network.runNTServer">
          Server Mode: <span class="text-accent">{{ clientStore.ntConnectionStatus.clients || 0 }}</span> clients
        </v-list-item-subtitle>
        <v-list-item-subtitle
          v-else-if="clientStore.ntConnectionStatus.connected && clientStore.backendConnected"
        >
          Connected to <span class="text-accent">{{ clientStore.ntConnectionStatus.address }}</span>
        </v-list-item-subtitle>
        <v-list-item-subtitle v-else> Not connected </v-list-item-subtitle>
      </v-list-item>
      <v-list-item>
        <template #prepend>
          <v-icon v-if="clientStore.backendConnected" icon="mdi-server-network" />
          <v-icon v-else icon="mdi-server-network-off" style="border-radius: 100%" />
        </template>
        <v-list-item-title>Backend Status</v-list-item-title>
        <v-list-item-subtitle v-if="clientStore.backendConnected"> Connected </v-list-item-subtitle>
        <v-list-item-subtitle v-else> Not connected </v-list-item-subtitle>
      </v-list-item>
    </v-list>
  </v-navigation-drawer>
</template>
