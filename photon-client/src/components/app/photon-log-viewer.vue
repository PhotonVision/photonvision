<script setup lang="ts">
import { computed, inject, ref } from "vue";
import { LogLevel, type LogMessage } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";

const backendHost = inject<string>("backendHost");

const selectedLogLevels = ref<LogLevel[]>([LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO]);

const logs = computed<LogMessage[]>(() =>
  useStateStore().logMessages.filter((message) => selectedLogLevels.value.includes(message.level))
);

const getLogColor = (level: LogLevel): string => {
  switch (level) {
    case LogLevel.ERROR:
      return "error";
    case LogLevel.WARN:
      return "warning";
    case LogLevel.INFO:
      return "info";
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

document.addEventListener("keydown", (e) => {
  if (e.key === "`") {
    useStateStore().$patch((state) => (state.showLogModal = !state.showLogModal));
  }
});
</script>

<template>
  <v-dialog v-model="useStateStore().showLogModal" width="1500">
    <v-card class="pt-3">
      <v-row class="heading-container pl-6 pr-6">
        <v-col>
          <v-card-title>View Program Logs</v-card-title>
        </v-col>
        <v-col class="align-self-center" style="display: flex">
          <v-btn
            color="secondary"
            depressed
            prepend-icon="mdi-download"
            style="margin-left: auto; max-width: 500px"
            text="Download Current Log"
            @click="handleLogExport"
          />
          <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
          <a
            ref="exportLogFile"
            download="photonvision-journalctl.txt"
            :href="`http://${backendHost}/api/utils/photonvision-journalctl.txt`"
            style="color: black; text-decoration: none; display: none"
            target="_blank"
          />
        </v-col>
      </v-row>

      <div class="pr-6 pl-6 pt-3">
        <v-btn-toggle
          v-model="selectedLogLevels"
          base-color="surface-variant"
          class="mb-4 overflow-x-auto"
          multiple
          style="width: 100%"
        >
          <v-btn v-for="level in [0, 1, 2, 3]" :key="level" style="width: 25%" :text="getLogLevelFromIndex(level)" />
        </v-btn-toggle>
        <v-card-text v-if="logs.length === 0" style="font-size: 18px; font-weight: 600">
          There are no logs to show
        </v-card-text>
        <v-virtual-scroll v-else height="600" :items="logs">
          <template #default="{ item }">
            <div :class="'text-' + getLogColor(item.level)">
              [{{ (item.timestamp as Date).toTimeString().split(" ")[0] }}] {{ item.message }}
            </div>
          </template>
        </v-virtual-scroll>
      </div>

      <v-divider />

      <v-card-actions>
        <v-spacer />
        <v-btn color="white" text="Close" @click="() => (useStateStore().showLogModal = false)" />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
@media only screen and (max-width: 512px) {
  .heading-container {
    flex-direction: column;
    padding-bottom: 14px;
  }
}
@media only screen and (max-width: 312px) {
  .open-icon {
    margin: 0 !important;
  }
  .open-label {
    display: none;
  }
}
</style>
