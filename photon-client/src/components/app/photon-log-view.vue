<script setup lang="ts">
import { computed, inject, ref, useTemplateRef, watch } from "vue";
import { LogLevel, type LogMessage } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";
import LogEntry from "@/components/app/photon-log-entry.vue";
import IconDownload from "~icons/mdi/download";
import IconTrashCanOutline from "~icons/mdi/trash-can-outline";
import IconClose from "~icons/mdi/close";
import IconMagnify from "~icons/mdi/magnify";

import VirtualList from "vue3-virtual-scroll-list";

const backendHost = inject<string>("backendHost");

const searchQuery = ref("");
const timeInput = ref<string>("");
const autoScroll = ref(true);
const logList = useTemplateRef<InstanceType<typeof VirtualList>>("logList"); // this needs to be typed in the definition since vue has trouble inferring it
const logKeeps = ref(40);
const exportLogFile = useTemplateRef("exportLogFile");
const selectedLogLevels = ref<Record<number, boolean>>({
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
          !timeInput.value ||
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

  if (autoScroll.value) logList.value?.scrollToBottom();
});

const getLogLevelFromIndex = (index: number): string => {
  return LogLevel[index].charAt(0).toUpperCase() + LogLevel[index].slice(1).toLowerCase();
};

const handleLogExport = () => {
  exportLogFile.value?.click();
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
  <pv-dialog v-model="useStateStore().showLogModal" :width="1500">
    <pv-card class="dialog-container p-5">
      <!-- Logs header -->
      <div class="flex flex-wrap pb-3">
        <div class="w-1/3">
          <div class="text-lg font-semibold">Program Logs</div>
        </div>
        <div class="flex-1 self-center pl-3 text-right">
          <pv-button variant="text" size="sm" :icon="IconDownload" @click="handleLogExport">
            <span class="menu-label">Download</span>

            <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
            <a
              ref="exportLogFile"
              style="color: black; text-decoration: none; display: none"
              :href="`http://${backendHost}/api/utils/photonvision-journalctl.txt`"
              download="photonvision-journalctl.txt"
              target="_blank"
            />
          </pv-button>
          <pv-button variant="text" size="sm" :icon="IconTrashCanOutline" @click="handleLogClear">
            <span class="menu-label">Clear Client Logs</span>
          </pv-button>
          <pv-button
            variant="text"
            size="sm"
            :icon="IconClose"
            @click="() => (useStateStore().showLogModal = false)"
          >
            <span class="menu-label">Close</span>
          </pv-button>
        </div>
      </div>

      <hr class="w-full border-t border-white/10" />

      <div class="dialog-data">
        <!-- Log view options -->
        <div class="flex flex-wrap justify-between pt-4 md:pt-0">
          <div class="flex flex-1 items-center gap-4 pr-3 justify-start">
            <pv-input
              v-model="searchQuery"
              clearable
              hide-details
              :prepend-icon="IconMagnify"
              label="Search"
              :input-cols="9"
            />
            <pv-input
              label="Minimum Time"
              v-model="timeInput"
              type="time"
              step="1"
              class="text-white pl-3"
              :clearable="true"
              :input-cols="7"
            />
          </div>
          <div class="flex gap-1">
            <div v-for="level in [0, 1, 2, 3]" :key="level" class="flex-1 pr-1 flex items-center justify-center">
              <div class="pb-0 pt-0 flex items-center basis-[min-content]">
                <pv-switch
                  v-model="selectedLogLevels[level]"
                  class="pl-2"
                  hide-details
                  color="primary"
                  :label="getLogLevelFromIndex(level)"
                ></pv-switch>
              </div>
            </div>
          </div>
        </div>

        <!-- Log entry list display -->
        <div class="log-display">
          <div
            v-if="!logs.length"
            class="flex h-full items-center justify-center text-center"
            style="font-size: 18px; font-weight: 150"
          >
            No available logs
          </div>
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
    </pv-card>
  </pv-dialog>
</template>

<style scoped >
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
  height: calc(100% - 56px);
  padding: 10px;
  background-color: var(--color-pv-logs-background) !important;
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
