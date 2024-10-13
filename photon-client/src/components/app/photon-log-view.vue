<script setup lang="ts">
import { computed, inject, ref, watch } from "vue";
import { LogLevel, type LogMessage } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import LogEntry from "@/components/app/photon-log-entry.vue";
import VirtualList from "vue-virtual-scroll-list";

const backendHost = inject<string>("backendHost");

const searchQuery = ref("");
const timeInput = ref<string>();
const autoScroll = ref(true);
const logList = ref();
const logKeeps = ref(40);
const exportLogFile = ref();
const selectedLogLevels = ref({
  [LogLevel.ERROR]: true,
  [LogLevel.WARN]: true,
  [LogLevel.INFO]: true,
  [LogLevel.DEBUG]: false
});

const logs = computed<LogMessage[]>(() =>
  useStateStore()
    .logMessages.filter(
      (message) =>
        selectedLogLevels.value[message.level] &&
        message.message.toLowerCase().includes(searchQuery.value?.toLowerCase() || "") &&
        (timeInput.value === undefined ||
          message.timestamp.getTime() >=
            new Date().setHours(
              parseInt(timeInput.value.substring(0, 2)),
              parseInt(timeInput.value.substring(3, 5)),
              parseInt(timeInput.value.substring(6, 8))
            ))
    )
    .map((item, index) => ({ ...item, index: index }))
);

watch(logs, () => {
  if (!logList.value) return;

  // Dynamic list render size based on console size
  logKeeps.value = Math.ceil(logList.value.$el.clientHeight / 17.5) + 20;

  const bottomOffset = Math.abs(
    logList.value.$el.scrollHeight - logList.value.$el.scrollTop - logList.value.$el.clientHeight
  );
  autoScroll.value = bottomOffset < 50;

  if (autoScroll.value) logList.value.scrollToBottom();
});

const getLogLevelFromIndex = (index: number): string => {
  return LogLevel[index];
};

const handleLogExport = () => {
  exportLogFile.value.click();
};

const handleLogClear = () => {
  useStateStore().logMessages = [];
};

document.addEventListener("keydown", (e) => {
  switch (e.key) {
    case "`":
      useStateStore().$patch((state) => (state.showLogModal = !state.showLogModal));
      break;
  }
});
</script>

<template>
  <v-dialog v-model="useStateStore().showLogModal" width="1500" dark>
    <v-card dark class="dialog-container pa-6" color="primary" flat>
      <!-- Logs header -->
      <v-row class="no-gutters pb-3">
        <v-col cols="4">
          <v-card-title class="pa-0">Program Logs</v-card-title>
        </v-col>
        <v-col class="align-self-center pl-3" style="text-align: right">
          <v-btn text color="white" @click="handleLogExport">
            <v-icon left class="menu-icon"> mdi-download </v-icon>
            <span class="menu-label">Download</span>

            <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
            <a
              ref="exportLogFile"
              style="color: black; text-decoration: none; display: none"
              :href="`http://${backendHost}/api/utils/photonvision-journalctl.txt`"
              download="photonvision-journalctl.txt"
              target="_blank"
            />
          </v-btn>
          <v-btn text color="white" @click="handleLogClear">
            <v-icon left class="menu-icon"> mdi-trash-can-outline </v-icon>
            <span class="menu-label">Clear Client Logs</span>
          </v-btn>
          <v-btn text color="white" @click="() => (useStateStore().showLogModal = false)">
            <v-icon left class="menu-icon"> mdi-close </v-icon>
            <span class="menu-label">Close</span>
          </v-btn>
        </v-col>
      </v-row>

      <v-divider />

      <div class="dialog-data">
        <!-- Log view options -->
        <v-row class="pt-4 pt-md-0 no-gutters">
          <v-col cols="12" md="5" class="align-self-center">
            <v-text-field
              v-model="searchQuery"
              dark
              dense
              clearable
              hide-details="auto"
              prepend-icon="mdi-magnify"
              color="accent"
              label="Search"
            />
          </v-col>
          <v-col cols="12" md="2" style="display: flex; align-items: center">
            <input v-model="timeInput" type="time" step="1" class="white--text pl-0 pl-md-8" />
            <v-btn icon class="ml-3" @click="timeInput = undefined">
              <v-icon>mdi-close-circle-outline</v-icon>
            </v-btn>
          </v-col>
          <v-col cols="12" md="5" class="pr-3">
            <v-row class="no-gutters">
              <v-col v-for="level in [0, 1, 2, 3]" :key="level">
                <v-row dense align="center">
                  <v-col cols="6" md="8" style="text-align: right">
                    {{ getLogLevelFromIndex(level) }}
                  </v-col>
                  <v-col cols="6" md="4">
                    <v-switch v-model="selectedLogLevels[level]" dark color="#ffd843" />
                  </v-col>
                </v-row>
              </v-col>
            </v-row>
          </v-col>
        </v-row>

        <!-- Log entry list display -->
        <div class="log-display">
          <v-card-text v-if="!logs.length" style="font-size: 18px; font-weight: 150; height: 100%; text-align: center">
            No available logs
          </v-card-text>
          <virtual-list
            v-else
            ref="logList"
            style="height: 100%; overflow-y: auto"
            data-key="index"
            :data-sources="logs"
            :data-component="LogEntry"
            :estimate-size="35"
            :keeps="logKeeps"
          />
        </div>
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss">
.dialog-container {
  height: 90vh;
  min-height: 300px !important;
}

.dialog-data {
  /* Dialog size - dialog padding - header - divider */
  height: calc(max(90vh, 300px) - 48px - 48px - 1px);
}

.log-display {
  /* Dialog data size - options */
  height: calc(100% - 66px);
  padding: 10px;
  background-color: #232c37 !important;
  border-radius: 5px;
}

@media only screen and (max-width: 960px) {
  .log-display {
    /* Dialog data size - options */
    height: calc(100% - 118px);
  }
}

@media only screen and (max-width: 700px) {
  .menu-label {
    display: none;
  }

  .menu-icon {
    margin: 0 !important;
  }
}
</style>
