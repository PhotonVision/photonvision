<script setup lang="ts">
import { computed } from "vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useDisplay, useTheme } from "vuetify";

const { mdAndUp } = useDisplay();

const compact = computed<boolean>({
  get: () => {
    return useStateStore().sidebarFolded;
  },
  set: (val) => {
    useStateStore().setSidebarFolded(val);
  }
});

const themeHandler = useTheme();
const currentThemeIcon = computed(() => themeHandler.current.value.variables.icon);

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
  <v-navigation-drawer permanent :rail="compact || !mdAndUp">
    <div class="mt-2 pb-6" style="height: 155px">
      <div>
        <v-img v-if="!compact && mdAndUp" alt="large logo" class="logo" src="@/assets/images/logoLarge.svg" />
        <v-img v-else alt="small logo" class="logo" src="@/assets/images/logoSmall.svg" />
      </div>
      <div style="display: flex; justify-content: center">
        <v-switch
          v-if="!compact && mdAndUp"
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
        <v-list-item-title style="padding: 12px 0">Dashboard</v-list-item-title>
      </v-list-item>
      <v-list-item link to="/cameras">
        <template #prepend>
          <v-icon icon="mdi-camera" />
        </template>
        <v-list-item-title style="padding: 12px 0">Cameras</v-list-item-title>
      </v-list-item>
      <v-list-item link to="/settings">
        <template #prepend>
          <v-icon icon="mdi-cog" />
        </template>
        <v-list-item-title style="padding: 12px 0">Settings</v-list-item-title>
      </v-list-item>
      <v-list-item link to="/docs">
        <template #prepend>
          <v-icon icon="mdi-bookshelf" />
        </template>
        <v-list-item-title style="padding: 12px 0">Documentation</v-list-item-title>
      </v-list-item>

      <v-list-item v-if="mdAndUp" link @click="() => (compact = !compact)">
        <template #prepend>
          <v-icon v-if="compact || !mdAndUp" icon="mdi-chevron-right" />
          <v-icon v-else icon="mdi-chevron-left" />
        </template>
        <v-list-item-title style="padding: 12px 0">Compact Mode</v-list-item-title>
      </v-list-item>
    </v-list>
    <v-list style="position: absolute; bottom: 0">
      <v-list-item>
        <template #prepend>
          <v-icon v-if="useSettingsStore().network.runNTServer" icon="mdi-server" />
          <v-icon v-else-if="useStateStore().ntConnectionStatus.connected" icon="mdi-robot" />
          <v-icon v-else icon="mdi-robot-off" style="border-radius: 100%" />
        </template>
        <v-list-item-title>NT Status</v-list-item-title>
        <v-list-item-subtitle v-if="useSettingsStore().network.runNTServer">
          Server Mode: <span class="text-accent">{{ useStateStore().ntConnectionStatus.clients || 0 }}</span> clients
        </v-list-item-subtitle>
        <v-list-item-subtitle
          v-else-if="useStateStore().ntConnectionStatus.connected && useStateStore().backendConnected"
        >
          Connected to <span class="text-accent">{{ useStateStore().ntConnectionStatus.address }}</span>
        </v-list-item-subtitle>
        <v-list-item-subtitle v-else> Not connected </v-list-item-subtitle>
      </v-list-item>
      <v-list-item>
        <template #prepend>
          <v-icon v-if="useStateStore().backendConnected" icon="mdi-server-network" />
          <v-icon v-else icon="mdi-server-network-off" style="border-radius: 100%" />
        </template>
        <v-list-item-title>Backend Status</v-list-item-title>
        <v-list-item-subtitle v-if="useStateStore().backendConnected"> Connected </v-list-item-subtitle>
        <v-list-item-subtitle v-else> Not connected </v-list-item-subtitle>
      </v-list-item>
    </v-list>
  </v-navigation-drawer>
</template>
