<script setup lang="ts">
import { computed } from "vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PlaceholderCameraSettings } from "@/types/SettingTypes";
import { useRoute } from "vue-router";
import { useDisplay } from "vuetify";

const compact = computed<boolean>({
  get: () => {
    return useStateStore().sidebarFolded;
  },
  set: (val) => {
    useStateStore().setSidebarFolded(val);
  }
});
const { mdAndUp } = useDisplay();

const renderCompact = computed<boolean>(() => compact.value || !mdAndUp.value);

const needsCamerasConfigured = computed<boolean>(() => {
  return (
    Object.values(useCameraSettingsStore().cameras).length === 0 ||
    useCameraSettingsStore().cameras["PlaceHolder Name"] === PlaceholderCameraSettings
  );
});
</script>

<template>
  <v-navigation-drawer permanent :rail="renderCompact" color="primary">
    <v-list nav>
      <!-- List item for the heading; note that there are some tricks in setting padding and image width make things look right -->
      <v-list-item :class="renderCompact ? 'pr-0 pl-0' : ''" style="display: flex; justify-content: center">
        <template #prepend>
          <img v-if="!renderCompact" class="logo" src="@/assets/images/logoLarge.svg" alt="large logo" />
          <img v-else class="logo" src="@/assets/images/logoSmall.svg" alt="small logo" />
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
        :class="{ cameraicon: needsCamerasConfigured && useRoute().path !== '/cameraConfigs' }"
      >
        <template #prepend>
          <v-icon :class="{ 'text-red': needsCamerasConfigured }">mdi-swap-horizontal-bold</v-icon>
        </template>
        <v-list-item-title :class="{ 'text-red': needsCamerasConfigured }">Camera Matching</v-list-item-title>
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
          <v-list-item-title>Compact Mode</v-list-item-title>
        </v-list-item>
        <v-list-item
          :prepend-icon="
            useSettingsStore().network.runNTServer
              ? 'mdi-server'
              : useStateStore().ntConnectionStatus.connected
                ? 'mdi-robot'
                : 'mdi-robot-off'
          "
        >
          <v-list-item-title v-if="useSettingsStore().network.runNTServer" v-show="!renderCompact" class="text-wrap">
            NetworkTables server running for
            <span class="text-accent">{{ useStateStore().ntConnectionStatus.clients || 0 }}</span> clients
          </v-list-item-title>
          <v-list-item-title
            v-else-if="useStateStore().ntConnectionStatus.connected && useStateStore().backendConnected"
            v-show="!renderCompact"
            class="text-wrap"
            style="flex-direction: column; display: flex"
          >
            NetworkTables Server Connected!
            <span class="text-accent">
              {{ useStateStore().ntConnectionStatus.address }}
            </span>
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

        <v-list-item :prepend-icon="useStateStore().backendConnected ? 'mdi-server-network' : 'mdi-server-network-off'">
          <v-list-item-title v-show="!renderCompact" class="text-wrap">
            {{ useStateStore().backendConnected ? "Backend connected" : "Trying to connect to backend" }}
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </template>
  </v-navigation-drawer>
</template>

<style scoped>
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
