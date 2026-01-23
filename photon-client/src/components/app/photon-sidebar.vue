<script setup lang="ts">
import { computed } from "vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useRoute } from "vue-router";
import { useDisplay, useTheme } from "vuetify";
import { toggleTheme } from "@/lib/ThemeManager";

const compact = computed<boolean>({
  get: () => {
    return useStateStore().sidebarFolded;
  },
  set: (val) => {
    useStateStore().setSidebarFolded(val);
  }
});
const { mdAndUp } = useDisplay();

const theme = useTheme();

const renderCompact = computed<boolean>(() => compact.value || !mdAndUp.value);
</script>

<template>
  <v-navigation-drawer permanent :rail="renderCompact" color="sidebar">
    <v-list nav color="primary">
      <v-list-item class="pr-0 pl-0" style="display: flex; justify-content: center">
        <template #prepend>
          <img v-if="!renderCompact" class="logo" src="@/assets/images/logoLarge.svg" alt="large logo" />
          <img v-else class="logo" src="@/assets/images/logoSmallTransparent.svg" alt="small logo" />
        </template>
      </v-list-item>

      <v-list-item link to="/dashboard" prepend-icon="mdi-view-dashboard">
        <v-list-item-title>Dashboard</v-list-item-title>
      </v-list-item>
      <v-list-item link to="/settings" prepend-icon="mdi-cog">
        <v-list-item-title>Settings</v-list-item-title>
      </v-list-item>
      <v-list-item ref="camerasTabOpener" link to="/cameras" prepend-icon="mdi-camera">
        <v-list-item-title>Camera</v-list-item-title>
      </v-list-item>
      <v-list-item
        link
        to="/cameraConfigs"
        :class="{
          cameraicon: useCameraSettingsStore().needsCameraConfiguration && useRoute().path !== '/cameraConfigs'
        }"
      >
        <template #prepend>
          <v-icon :class="{ 'text-red': useCameraSettingsStore().needsCameraConfiguration }"
            >mdi-swap-horizontal-bold</v-icon
          >
        </template>
        <v-list-item-title :class="{ 'text-red': useCameraSettingsStore().needsCameraConfiguration }"
          >Camera Matching</v-list-item-title
        >
      </v-list-item>
      <v-list-item link to="/docs" prepend-icon="mdi-bookshelf">
        <v-list-item-title>Documentation</v-list-item-title>
      </v-list-item>
    </v-list>
    <template #append>
      <v-list nav>
        <v-list-item
          v-if="mdAndUp"
          link
          :prepend-icon="`mdi-chevron-${compact || !mdAndUp ? 'right' : 'left'}`"
          @click="() => (compact = !compact)"
        >
          <v-list-item-title>Compact</v-list-item-title>
        </v-list-item>
        <v-list-item
          link
          :prepend-icon="theme.global.name.value === 'LightTheme' ? 'mdi-white-balance-sunny' : 'mdi-weather-night'"
          @click="() => toggleTheme(theme)"
        >
          <v-list-item-title>Theme</v-list-item-title>
        </v-list-item>
        <v-list-item>
          <template #prepend>
            <v-icon
              :icon="
                useSettingsStore().network.runNTServer
                  ? 'mdi-server'
                  : useStateStore().ntConnectionStatus.connected
                    ? 'mdi-robot'
                    : 'mdi-robot-off'
              "
              :color="
                useSettingsStore().network.runNTServer || useStateStore().ntConnectionStatus.connected
                  ? '#00ff00'
                  : '#ff0000'
              "
            />
          </template>
          <v-list-item-title v-if="useSettingsStore().network.runNTServer" v-show="!renderCompact" class="text-wrap">
            NetworkTables server running for
            <span class="text-primary">{{ useStateStore().ntConnectionStatus.clients || 0 }}</span> clients
          </v-list-item-title>
          <v-list-item-title
            v-else-if="useStateStore().ntConnectionStatus.connected && useStateStore().backendConnected"
            v-show="!renderCompact"
            class="text-wrap"
            style="flex-direction: column; display: flex"
          >
            NetworkTables Server Connected!
            <span class="text-primary"> {{ useStateStore().ntConnectionStatus.address }} </span>
          </v-list-item-title>
          <v-list-item-title
            v-else
            v-show="!renderCompact"
            class="text-wrap"
            style="flex-direction: column; display: flex"
          >
            Not connected to NetworkTables Server!
          </v-list-item-title>
        </v-list-item>
        <v-list-item>
          <template #prepend>
            <v-icon
              :icon="useStateStore().backendConnected ? 'mdi-server-network' : 'mdi-server-network-off'"
              :color="useStateStore().backendConnected ? '#00ff00' : '#ff0000'"
            />
          </template>
          <v-list-item-title v-show="!renderCompact" class="text-wrap">
            {{ useStateStore().backendConnected ? "Backend connected" : "Trying to connect to backend..." }}
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </template>
  </v-navigation-drawer>
</template>

<style scoped>
.v-navigation-drawer {
  border: none;
}

.v-navigation-drawer--rail {
  border: none;
}

.v-list-item-title {
  font-size: 1rem !important;
  line-height: 1.2rem !important;
}

.logo {
  width: 100%;
  height: 70px;
  object-fit: contain;
}

.cameraicon {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(0.95);
  }
  50% {
    transform: scale(1.05);
  }
}
</style>
