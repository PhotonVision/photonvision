<script setup lang="ts">
import { computed } from "vue";
import { Icon } from "@iconify/vue";
import { useRoute } from "vue-router";
import { useSettingsStore } from "@/stores/settings/GeneralSettingsStore";
import { useStateStore } from "@/stores/StateStore";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { useTheme } from "vuetify";
import { toggleTheme } from "@/lib/ThemeManager";
import PvIcon from "@/components/common/pv-icon.vue";
import { NavigationMenuItem, NavigationMenuLink, NavigationMenuList, NavigationMenuRoot } from "reka-ui";
import { useCustomBreakpoints } from "@/lib/Breakpoints";

const compact = computed<boolean>({
  get: () => useStateStore().sidebarFolded,
  set: (val) => useStateStore().setSidebarFolded(val)
});
const breakpoints = useCustomBreakpoints();
const mdAndUp = breakpoints.greaterOrEqual("md");
const theme = useTheme();
const route = useRoute();

const renderCompact = computed<boolean>(() => compact.value || !mdAndUp.value);

const navItems = [
  { title: "Dashboard", to: "/dashboard", icon: "mdi-view-dashboard" },
  { title: "Settings", to: "/settings", icon: "mdi-cog" },
  { title: "Camera", to: "/cameras", icon: "mdi-camera" },
  { title: "Documentation", to: "/docs", icon: "mdi-bookshelf" }
];

const baseItemClass = "sidebar-item group flex items-center gap-3 rounded-12 px-3 py-2  text-white/80";
const activeItemClass = "bg-white/5 text-white font-semibold";
</script>

<template>
  <aside
    class="flex flex-col text-white transition-[width] duration-200 h-screen sticky top-0"
    :class="renderCompact ? 'w-20' : 'w-64'"
  >
    <div class="flex items-center justify-center px-3 py-4">
      <img
        v-if="!renderCompact"
        class="h-16 w-full object-contain"
        src="@/assets/images/logoLarge.svg"
        alt="large logo"
      />
      <img v-else class="h-16 w-full object-contain" src="@/assets/images/logoSmallTransparent.svg" alt="small logo" />
    </div>

    <NavigationMenuRoot class="flex flex-1 flex-col" orientation="vertical">
      <NavigationMenuList class="flex flex-1 flex-col gap-1 px-3">
        <NavigationMenuItem v-for="item in navItems" :key="item.to">
          <NavigationMenuLink as-child :active="route.path === item.to">
            <RouterLink
              :to="item.to"
              :class="[baseItemClass, renderCompact ? 'justify-center px-2' : '']"
              :active-class="activeItemClass"
            >
              <Icon :icon="item.icon" class="text-lg text-white/80 transition group-hover:text-white size-6" />
              <span
                class="transition-opacity duration-200"
                :class="renderCompact ? 'opacity-0 w-0 h-0 overflow-hidden absolute' : 'opacity-100'"
              >
                {{ item.title }}
              </span>
            </RouterLink>
          </NavigationMenuLink>
        </NavigationMenuItem>

        <NavigationMenuItem>
          <NavigationMenuLink as-child :active="route.path === '/cameraConfigs'">
            <RouterLink
              to="/cameraConfigs"
              :class="[
                baseItemClass,
                renderCompact ? 'justify-center px-2' : '',
                route.path === '/cameraConfigs' ? activeItemClass : '',
                useCameraSettingsStore().needsCameraConfiguration && route.path !== '/cameraConfigs' ? 'pulse' : ''
              ]"
            >
              <Icon
                icon="mdi-swap-horizontal-bold"
                class="text-lg text-white/80 transition group-hover:text-white size-6"
                :class="{ 'text-red-400': useCameraSettingsStore().needsCameraConfiguration }"
              />
              <span
                class="transition-opacity duration-200"
                :class="[
                  renderCompact ? 'opacity-0 w-0 h-0 overflow-hidden absolute' : 'opacity-100',
                  { 'text-red-400': useCameraSettingsStore().needsCameraConfiguration }
                ]"
              >
                Camera Matching
              </span>
            </RouterLink>
          </NavigationMenuLink>
        </NavigationMenuItem>
      </NavigationMenuList>
    </NavigationMenuRoot>

    <div class="border-t border-white/10 px-3 py-3">
      <button
        v-if="mdAndUp"
        type="button"
        class="sidebar-item mb-2 flex w-full items-center gap-3 rounded-12 px-3 py-2 text-white/80 justify-between"
        :class="renderCompact ? 'justify-center px-2' : ''"
        @click="() => (compact = !compact)"
      >
        <Icon
          :icon="`mdi-chevron-${compact || !mdAndUp ? 'right' : 'left'}`"
          class="text-lg text-white/80 transition group-hover:text-white size-6"
        />
        <span
          class="transition-opacity duration-200"
          :class="renderCompact ? 'opacity-0 w-0 h-0 overflow-hidden absolute' : 'opacity-100'"
        >
          Compact
        </span>
      </button>
      <button
        type="button"
        class="sidebar-item mb-3 flex w-full items-center gap-3 rounded-12 px-3 py-2 text-white/80 justify-between"
        :class="renderCompact ? 'justify-center px-2' : ''"
        @click="() => toggleTheme(theme)"
      >
        <Icon
          :icon="theme.global.current.value.dark ? 'mdi-weather-night' : 'mdi-white-balance-sunny'"
          class="text-lg text-white/80 transition group-hover:text-white size-6"
        />
        <span
          class="transition-opacity duration-200"
          :class="renderCompact ? 'opacity-0 w-0 h-0 overflow-hidden absolute' : 'opacity-100'"
        >
          Theme
        </span>
      </button>

      <div
        class="flex items-center gap-3 rounded-12 px-3 py-2 text-white/70"
        :class="renderCompact ? 'justify-center' : 'justify-between'"
      >
        <Icon
          :icon="
            useSettingsStore().network.runNTServer
              ? 'mdi-server'
              : useStateStore().ntConnectionStatus.connected
                ? 'mdi-robot'
                : 'mdi-robot-off'
          "
          :class="
            useSettingsStore().network.runNTServer || useStateStore().ntConnectionStatus.connected
              ? 'text-green-400'
              : 'text-red-400'
          "
          class="size-6 shrink-0"
        />
        <div
          class="leading-snug text-end transition-opacity duration-200"
          :class="renderCompact ? 'opacity-0 w-0 h-0 overflow-hidden absolute absolute' : 'opacity-100'"
        >
          <span v-if="useSettingsStore().network.runNTServer">
            NetworkTables server running for
            <span class="text-primary">{{ useStateStore().ntConnectionStatus.clients || 0 }}</span> clients
          </span>
          <span v-else-if="useStateStore().ntConnectionStatus.connected && useStateStore().backendConnected">
            NetworkTables Server Connected!
            <span class="text-primary">{{ useStateStore().ntConnectionStatus.address }}</span>
          </span>
          <span v-else>Not connected to NetworkTables Server!</span>
        </div>
      </div>

      <div
        class="mt-2 flex items-start gap-3 rounded-12 px-3 py-2 text-white/70"
        :class="renderCompact ? 'justify-center' : 'justify-between'"
      >
        <Icon
          :icon="useStateStore().backendConnected ? 'mdi-server-network' : 'mdi-server-network-off'"
          :class="useStateStore().backendConnected ? 'text-green-400' : 'text-red-400'"
          class="size-6 shrink-0"
        />
        <div
          class="leading-snug transition-opacity duration-200"
          :class="renderCompact ? 'opacity-0 w-0 h-0 overflow-hidden absolute' : 'opacity-100'"
        >
          {{ useStateStore().backendConnected ? "Backend connected" : "Trying to connect to backend..." }}
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar-item {
  transition:
    background-color 150ms ease,
    color 150ms ease,
    box-shadow 150ms ease,
    transform 150ms ease;
}

.sidebar-item:hover {
  background-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12);
  transform: translateY(-1px);
}

.pulse {
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
