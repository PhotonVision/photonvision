<script setup lang="ts">
import { computed, inject, ref } from "vue";
import { LogLevel, type LogMessage } from "@/types/SettingTypes";
import { useStateStore } from "@/stores/StateStore";

const selectedLogLevels = ref({
  [LogLevel.ERROR]: true,
  [LogLevel.WARN]: true,
  [LogLevel.INFO]: true,
  [LogLevel.DEBUG]: false
});

const searchQuery = ref("");

setInterval(function(){
  if (Math.random() < 0.75) useStateStore().logMessages?.push({ message: "test log entry " + useStateStore().logMessages.length, level: Math.floor(Math.random() * 5) % 4 });
  else useStateStore().logMessages?.push({ message: "[2024-07-25 02:27:26] [WebServer - Server] [DEBUG] Handled HTTP request of type POST from endpoint /api/utils/publishMetrics of req size 0 bytes & type null with return code 204 for host [0:0:0:0:0:0:0:1] in 0.591999 ms " + useStateStore().logMessages.length,level: 1 });
}, 1000);

const logs = computed<LogMessage[]>(() =>
  useStateStore().logMessages.filter((message) => 
    selectedLogLevels.value[message.level]
    && message.message.toLowerCase().includes(searchQuery.value?.toLowerCase() || ""))
);

const backendHost = inject<string>("backendHost");

const getLogColor = (level: LogLevel): string => {
  switch (level) {
    case LogLevel.ERROR:
      return "red";
    case LogLevel.WARN:
      return "yellow";
    case LogLevel.INFO:
      return "light-blue";
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

const handleLogClear = () => {
  useStateStore().logMessages = [];
}

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
    <v-card dark id="dialog-container" class="pa-6" color="primary" flat>
      <v-row class="no-gutters pb-3">
        <v-col cols="4">
          <v-card-title id="logs-title">Program Logs</v-card-title>
        </v-col>
        <v-col class="align-self-center pl-3" style="text-align: right;">
          <v-btn
            text
            color="white"
            @click="handleLogExport"
          >
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
          <v-btn
            text
            color="white"
            @click="handleLogClear"
          >
            <v-icon left class="menu-icon"> mdi-trash-can-outline </v-icon>
            <span class="menu-label">Clear</span>
          </v-btn>
          <v-btn
            text
            color="white"
            @click="() => (useStateStore().showLogModal = false)"
          >
            <v-icon left class="menu-icon"> mdi-close </v-icon>
            <span class="menu-label">Close</span>
          </v-btn>
        </v-col>
      </v-row>

      <v-divider />

      <div class="" id="dialog-data">
        <v-row class="no-gutters" id="log-options">
          <v-col cols="12" md="5" class="align-self-center">
            <v-text-field
              dark
              dense
              clearable
              hide-details="auto"
              prepend-icon="mdi-magnify"
              v-model="searchQuery"
              color="accent"
              label="Search"
            />
          </v-col>
          <v-col cols="12" md="7" >
            <v-row class="no-gutters">
              <v-col v-for="level in [0, 1, 2, 3]">
                <v-row dense align="center">
                  <v-col cols="6" md="8" style="text-align: right;">
                    {{ getLogLevelFromIndex(level) }}
                  </v-col>
                  <v-col cols="6" md="4" >
                    <v-switch
                      dark
                      v-model="selectedLogLevels[level]"
                      color="#ffd843"
                    />
                  </v-col>
                </v-row>
                <!-- <pv-switch
                  v-model="selectedLogLevels[level]"
                  :label="getLogLevelFromIndex(level)"
                  :label-cols="8"
                  reverseOrder
                /> -->
              </v-col>
            </v-row>
          </v-col>
          
        </v-row>
        <div class="virtual-scroll-container" id="log-display">
          <v-card-text v-if="!logs.length" style="font-size: 18px; font-weight: 150; height: 100%; text-align: center;">
            No available logs
          </v-card-text>
          <v-virtual-scroll v-else :items="logs" itemHeight="40">
            <template #default="{ item }">
              <div :class="[getLogColor(item.level) + '--text', 'log-item']">
                {{ item.message }}
              </div>
            </template>
          </v-virtual-scroll>
        </div>
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped lang="scss">
#logs-title {
  padding: 0;
}

#dialog-container {
  height: 90vh;
}

#dialog-data {
  /* Dialog size - dialog padding - header - divider */
  height: calc(90vh - 48px - 48px - 1px);
}

#log-display {
  /* Data size - options */
  height: calc(100% - 66px);
}

.v-dialog {
  overflow-x: hidden;
}

.v-virtual-scroll {
  background-color: #232c37 !important;
}

.virtual-scroll-container {
  padding: 10px;
  background-color: #232c37 !important;
  border-radius: 5px;
}

.v-btn-toggle.fill {
  width: 100%;
}

.v-btn-toggle.fill > .v-btn {
  width: 25%;
}

.v-text-field__slot {
  margin-top: -10px !important
}


@media only screen and (max-width: 960px) {
  #log-options {
    padding-top: 16px;
  }

  #log-display {
    /* Data size - options */
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
