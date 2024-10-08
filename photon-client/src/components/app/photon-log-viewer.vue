<script setup lang="ts">
import { inject, reactive, ref } from "vue";
import { useClientStore } from "@/stores/ClientStore";
import { LogLevel } from "@/types/SettingTypes";
import { useDisplay } from "vuetify";

const clientStore = useClientStore();

const backendHost = inject<string>("backendHost");

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
const filterValues = reactive<{search: string, selectedLogLevels: LogLevel[]}>({ search: "", selectedLogLevels: [LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO] });
const { xs } = useDisplay();

const exportLogFile = ref();

document.addEventListener("keydown", (e) => {
  if (e.key === "`") {
    // Disable opening the log viewer if any other modal is open
    const collection = document.getElementsByClassName("v-overlay v-overlay--active");
      for (let i = 0; i < collection.length; i++) {
        if (collection.item(i)!.id !== "log-viewer") {
          return;
        }
    }

    clientStore.showLogModal = !clientStore.showLogModal;
  }
});
</script>

<template>
  <v-dialog id="log-viewer" v-model="clientStore.showLogModal" max-width="1500">
    <v-card class="pt-3">
      <v-row no-gutters class="pb-3 pb-sm-0 pl-6 pr-6 flex-nowrap">
        <v-col>
          <v-card-title>View Program Logs</v-card-title>
        </v-col>
        <v-col class="d-flex align-center flex-grow-0 ml-auto">
          <v-btn
            color="white"
            prepend-icon="mdi-download"
            variant="text"
            @click="exportLogFile.click()"
          >
            <div class="action-label">
              Download Log
            </div>
          </v-btn>
          <!-- Special hidden link that gets 'clicked' when the user exports journalctl logs -->
          <a
            ref="exportLogFile"
            class="d-none"
            download="photonvision-journalctl.txt"
            :href="`http://${backendHost}/api/utils/photonvision-journalctl.txt`"
            target="_blank"
          />
        </v-col>
        <v-col class="d-flex align-center flex-grow-0">
          <v-btn
            color="white"
            prepend-icon="mdi-trash-can-outline"
            :disabled="!clientStore.logMessages.length"
            variant="text"
            @click="clientStore.clearLogs()"
          >
            <div class="action-label">
              Clear Client Logs
            </div>
          </v-btn>
        </v-col>
      </v-row>

      <v-data-table-virtual
        class="pl-6 pr-6 pb-3 pt-4"
        :headers="[
          {title: 'Timestamp', key: 'timestamp', sortable: true, maxWidth: '120px', width: '120px'},
          {title: 'Log Level', key: 'level', sortable: false, maxWidth: '120px', width: '120px', filter: (v) => filterValues.selectedLogLevels.includes(v as unknown as LogLevel) },
          {title: 'Message', key: 'message', sortable: false},
        ]"
        height="400px"
        :items="clientStore.logMessages"
        :search="filterValues.search"
      >
        <template #top>
          <v-row no-gutters>
            <v-col class="pr-0 pr-md-3 pb-1 pb-md-0" cols="12" md="8">
              <v-text-field
                v-model="filterValues.search"
                hide-details
                label="Search"
                base-color="accent"
                prepend-inner-icon="mdi-magnify"
                single-line
                variant="outlined"
              />
            </v-col>
            <v-col cols="12" md="4" style="min-height: 35px;">
              <v-btn-toggle
                v-model="filterValues.selectedLogLevels"
                base-color="surface-variant"
                class="w-100 fill-height"
                divided
                multiple
              >
                <v-btn v-for="level in [0, 1, 2, 3]" :key="level" class="w-25" :text="getLogLevelFromIndex(level)" />
              </v-btn-toggle>
            </v-col>
          </v-row>
        </template>

        <template v-if="xs" #headers>
          <tr>
            <th>
              Message
            </th>
          </tr>
        </template>
        <template v-if="xs" #item="{ item }">
          <tr>
            <td class="pt-1 pb-1">
              [{{ item.timestamp.toTimeString().split(" ")[0] }}] [<span :class="'text-' + getLogColor(item.level)">{{ getLogLevelFromIndex(item.level) }}</span>] {{ item.message }}
            </td>
          </tr>
        </template>

        <template #item.timestamp="{ value }">
          {{ value.toTimeString().split(" ")[0] }}
        </template>
        <template #item.level="{ value }">
          <div :class="'text-' + getLogColor(value)">{{ getLogLevelFromIndex(value) }}</div>
        </template>
        <template #item.message="{ value }">
          <div class="pt-1 pb-1">{{ value }}</div>
        </template>

        <template #no-data>
          There are no logs to show
        </template>
      </v-data-table-virtual>

      <v-divider />

      <v-card-actions>
        <v-spacer />
        <v-btn color="white" text="Close" @click="() => (clientStore.showLogModal = false)" />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
@media only screen and (max-width: 700px) {
  .action-label {
    display: none;
  }
}
</style>
