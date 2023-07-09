<script setup lang="ts">
import {computed, getCurrentInstance, ref} from "vue";
import {useSettingsStore} from "@/stores/settings/GeneralSettingsStore";
import {useStateStore} from "@/stores/StateStore";

const getSidebarFoldedFromLocalStorage = () => {
  const localValue = localStorage.getItem("sidebarFolded");
  return localValue === null ? false : localValue === "true";
};

const sidebarFolded = ref<boolean>(getSidebarFoldedFromLocalStorage());

const setSidebarFolded = (folded: boolean) => {
  sidebarFolded.value = folded;
  localStorage.setItem("sidebarFolded", Boolean(folded).toString());
};

const stateStore = useStateStore();
const settingsStore = useSettingsStore();

// Vuetify2 doesn't yet support the useDisplay API so this is required to access the prop when using the Composition API
const mdAndUp = computed<boolean>(() => getCurrentInstance()?.proxy.$vuetify.breakpoint.mdAndUp || false);
</script>

<template>
  <v-navigation-drawer
      dark
      app
      permanent
      :mini-variant="!mdAndUp || sidebarFolded"
      color="primary"
  >
    <v-list>
      <!-- List item for the heading; note that there are some tricks in setting padding and image width make things look right -->
      <v-list-item
          :class="!mdAndUp || sidebarFolded ? 'pr-0 pl-0' : ''"
          style="display: flex; justify-content: center"
      >
        <v-list-item-icon class="mr-0">
          <img
              v-if="mdAndUp && !sidebarFolded"
              class="logo"
              src="@/assets/images/logoLarge.svg"
              alt="large logo"
          >
          <img
              v-else
              class="logo"
              src="@/assets/images/logoSmall.svg"
              alt="small logo"
          >
        </v-list-item-icon>
      </v-list-item>

      <v-list-item
          link
          to="/dashboard"
      >
        <v-list-item-icon>
          <v-icon>mdi-view-dashboard</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Dashboard</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item
          ref="camerasTabOpener"
          link
          to="/cameras"
      >
        <v-list-item-icon>
          <v-icon>mdi-camera</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Cameras</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item
          link
          to="/settings"
      >
        <v-list-item-icon>
          <v-icon>mdi-cog</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Settings</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item
          link
          to="/docs"
      >
        <v-list-item-icon>
          <v-icon>mdi-bookshelf</v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Documentation</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item
          v-if="mdAndUp"
          link
          @click="setSidebarFolded(!sidebarFolded)"
      >
        <v-list-item-icon>
          <v-icon v-if="sidebarFolded">
            mdi-chevron-right
          </v-icon>
          <v-icon v-else>
            mdi-chevron-left
          </v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>Compact Mode</v-list-item-title>
        </v-list-item-content>
      </v-list-item>

      <div style="position: absolute; bottom: 0; left: 0;">
        <v-list-item>
          <v-list-item-icon>
            <v-icon v-if="settingsStore.network.runNTServer">
              mdi-server
            </v-icon>
            <v-icon v-else-if="stateStore.ntConnectionStatus.connected">
              mdi-robot
            </v-icon>
            <v-icon
                v-else
                style="border-radius: 100%"
            >
              mdi-robot-off
            </v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title
                v-if="settingsStore.network.runNTServer"
                class="text-wrap"
            >
              NetworkTables server running for <span class="accent--text">{{ stateStore.ntConnectionStatus.clients }}</span> clients
            </v-list-item-title>
            <v-list-item-title
                v-else-if="stateStore.ntConnectionStatus.connected && stateStore.backendConnected"
                class="text-wrap"
                style="flex-direction: column; display: flex"
            >
              NetworkTables Server Connected!
              <span
                  class="accent--text"
              >
                  {{ stateStore.ntConnectionStatus.address }}
                </span>
            </v-list-item-title>
            <v-list-item-title
                v-else
                class="text-wrap"
                style="flex-direction: column; display: flex"
            >
              Not connected to NetworkTables Server!
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item>
          <v-list-item-icon>
            <v-icon v-if="stateStore.backendConnected">
              mdi-server-network
            </v-icon>
            <v-icon
                v-else
                style="border-radius: 100%;"
            >
              mdi-server-network-off
            </v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title class="text-wrap">
              {{ stateStore.backendConnected ? "Backend Connected" : "Trying to connect to Backend" }}
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
</style>
