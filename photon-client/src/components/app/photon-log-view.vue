<script setup lang="ts">
import { computed, ref, inject } from "vue";
import { LogLevel, type LogMessage } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";

const selectedLogLevels = ref<LogLevel[]>([LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO]);

const logs = computed<LogMessage[]>(() => useStateStore().logMessages.filter(message => selectedLogLevels.value.includes(message.level)));

const backendHost = inject<string>("backendHost");

const getLogColor = (level: LogLevel): string => {
  switch (level) {
    case LogLevel.ERROR:
      return "red";
    case LogLevel.WARN:
      return "yellow";
    case LogLevel.INFO:
      return "green";
    case LogLevel.DEBUG:
      return "white";
  }
  return "";
};

const getLogLevelFromIndex = (index: number): string => {
  return LogLevel[index];
};

const exportLogFile = ref();

const handleLogExport = () => {
  exportLogFile.value.click();
};

document.addEventListener("keydown", e => {
  switch (e.key) {
    case "`":
      useStateStore().$patch(state => state.showLogModal = !state.showLogModal);
      break;
  }
});
</script>

<template>
  <v-dialog
    v-model="useStateStore().showLogModal"
    width="1500"
    dark
  >
    <v-card
      dark
      class="pt-3"
      color="primary"
      flat
    >
      <v-card-title>
        View Program Logs
        <v-btn
          color="secondary"
          style="margin-left: auto;"
          depressed
          @click="handleLogExport"
        >
          <v-icon left>
            mdi-download
          </v-icon>
          Download Current Log

          <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
          <a
            ref="exportLogFile"
            style="color: black; text-decoration: none; display: none"
            :href="`http://${backendHost}/api/utils/photonvision-journalctl.txt`"
            download="photonvision-journalctl.txt"
            target="_blank"
          />
        </v-btn>
      </v-card-title>

      <div class="pr-6 pl-6">
        <v-btn-toggle
          v-model="selectedLogLevels"
          dark
          multiple
          class="fill mb-4"
        >
          <v-btn
            v-for="(level) in [0, 1, 2, 3]"
            :key="level"
            color="secondary"
            class="fill"
          >
            {{ getLogLevelFromIndex(level) }}
          </v-btn>
        </v-btn-toggle>
        <v-card-text
          v-if="logs.length === 0"
          style="font-size: 18px; font-weight: 600"
        >
          There are no Logs to show
        </v-card-text>
        <v-virtual-scroll
          v-else
          :items="logs"
          item-height="50"
          height="600"
        >
          <template #default="{item}">
            <div :class="[getLogColor(item.level) + '--text', 'log-item']">
              {{ item.message }}
            </div>
          </template>
        </v-virtual-scroll>
      </div>

      <v-divider />

      <v-card-actions>
        <v-spacer />
        <v-btn
          color="white"
          text
          @click="() => useStateStore().showLogModal = false"
        >
          Close
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.v-btn-toggle.fill {
  width: 100%;
  height: 100%;
}

.v-btn-toggle.fill > .v-btn {
  width: 25%;
  height: 100%;
}
</style>
