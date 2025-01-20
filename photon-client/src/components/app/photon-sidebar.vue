<script setup lang="ts">
import { computed, getCurrentInstance } from "vue";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { PlaceholderCameraSettings } from "@/types/SettingTypes";
import { useRoute } from "vue2-helpers/vue-router";

const compact = computed<boolean>({
  get: () => {
    return useStateStore().sidebarFolded;
  },
  set: (val) => {
    useStateStore().setSidebarFolded(val);
  }
});

// Vuetify2 doesn't yet support the useDisplay API so this is required to access the prop when using the Composition API
const mdAndUp = computed<boolean>(() => getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndUp || false);

const needsCamerasConfigured = computed<boolean>(() => {
  return (
    Object.values(useCameraSettingsStore().cameras).length === 0 ||
    useCameraSettingsStore().cameras["PlaceHolder Name"] === PlaceholderCameraSettings
  );
});
</script>

<template>
  <v-navigation-drawer dark app permanent :mini-variant="compact || !mdAndUp" color="primary">
    <v-list>
      <!-- List item for the heading; note that there are some tricks in setting padding and image width make things look right -->
      <v-list-item :class="compact || !mdAndUp ? 'pr-0 pl-0' : ''" style="display: flex; justify-content: center">
        <v-list-item-icon class="mr-0">
          <img v-if="!(compact || !mdAndUp)" class="logo" src="@/assets/images/logoLarge.svg" alt="large logo" />
          <img v-else class="logo" src="@/assets/images/logoSmall.svg" alt="small logo" />
        </v-list-item-icon>
      </v-list-item>

      <v-list-item link to="/dashboard">
        <v-list-item-icon>
          <v-icon>mdi-view-dashboard</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Dashboard</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item link to="/settings">
        <v-list-item-icon>
          <v-icon>mdi-cog</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Settings</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item ref="camerasTabOpener" link to="/cameras">
        <v-list-item-icon>
          <v-icon>mdi-camera</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Camera</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item
        link
        to="/cameraConfigs"
        :class="{ cameraicon: needsCamerasConfigured && useRoute().path !== '/cameraConfigs' }"
      >
        <v-list-item-icon>
          <v-icon :class="{ 'red--text': needsCamerasConfigured }">mdi-swap-horizontal-bold</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title :class="{ 'red--text': needsCamerasConfigured }">Camera Matching</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item link to="/docs">
        <v-list-item-icon>
          <v-icon>mdi-bookshelf</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Documentation</v-list-item-title>
        </v-list-item-content>
      </v-list-item>

      <div style="position: absolute; bottom: 0; left: 0">
        <v-list-item v-if="mdAndUp" link @click="() => (compact = !compact)">
          <v-list-item-icon>
            <v-icon v-if="compact || !mdAndUp"> mdi-chevron-right </v-icon>
            <v-icon v-else> mdi-chevron-left </v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Compact Mode</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-icon>
            <v-icon v-if="useSettingsStore().network.runNTServer"> mdi-server </v-icon>
            <v-icon v-else-if="useStateStore().ntConnectionStatus.connected"> mdi-robot </v-icon>
            <v-icon v-else style="border-radius: 100%"> mdi-robot-off </v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title v-if="useSettingsStore().network.runNTServer" class="text-wrap">
              NetworkTables server running for
              <span class="accent--text">{{ useStateStore().ntConnectionStatus.clients || 0 }}</span> clients
            </v-list-item-title>
            <v-list-item-title
              v-else-if="useStateStore().ntConnectionStatus.connected && useStateStore().backendConnected"
              class="text-wrap"
              style="flex-direction: column; display: flex"
            >
              NetworkTables Server Connected!
              <span class="accent--text">
                {{ useStateStore().ntConnectionStatus.address }}
              </span>
            </v-list-item-title>
            <v-list-item-title v-else class="text-wrap" style="flex-direction: column; display: flex">
              Not connected to NetworkTables Server!
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item>
          <v-list-item-icon>
            <v-icon v-if="useStateStore().backendConnected"> mdi-server-network </v-icon>
            <v-icon v-else style="border-radius: 100%"> mdi-server-network-off </v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title class="text-wrap">
              {{ useStateStore().backendConnected ? "Backend connected" : "Trying to connect to backend" }}
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </div>
    </v-list>
  </v-navigation-drawer>
</template>

<style scoped>
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
