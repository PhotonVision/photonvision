<template>
  <v-app>
    <!-- Although most of the app runs with the "light" theme, the navigation drawer needs to have white text and icons so it uses the dark theme-->
    <v-navigation-drawer dark app permanent :mini-variant="compact" color="primary">
      <v-list>
        <!-- List item for the heading; note that there are some tricks in setting padding and image width make things look right -->
        <v-list-item :class="compact ? 'pr-0 pl-0' : ''">
          <v-list-item-icon class="mr-0">
            <img v-if="!compact" class="logo" src="./assets/logoLarge.png">
            <img v-else class="logo" src="./assets/logoSmall.png">
          </v-list-item-icon>
        </v-list-item>

        <v-list-item link to="dashboard" @click="rollbackPipelineIndex()">
          <v-list-item-icon>
            <v-icon>mdi-view-dashboard</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Dashboard</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item ref="camerasTabOpener" link to="cameras" @click="switchToDriverMode()">
          <v-list-item-icon>
            <v-icon>mdi-camera</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Cameras</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link to="settings" @click="switchToSettingsTab()">
          <v-list-item-icon>
            <v-icon>mdi-settings</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Settings</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link to="docs">
          <v-list-item-icon>
            <v-icon>mdi-bookshelf</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Documentation</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item v-if="this.$vuetify.breakpoint.mdAndUp" link @click.stop="toggleCompactMode">
          <v-list-item-icon>
            <v-icon v-if="compact">
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
              <v-icon v-if="$store.state.settings.networkSettings.runNTServer">
                mdi-server
              </v-icon>
              <img v-else-if="$store.state.ntConnectionInfo.connected" src="@/assets/robot.svg" alt="">
              <img v-else class="pulse" style="border-radius: 100%" src="@/assets/robot-off.svg" alt="">
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title v-if="$store.state.settings.networkSettings.runNTServer" class="text-wrap">
                NetworkTables server running for {{ $store.state.ntConnectionInfo.clients ?
                    $store.state.ntConnectionInfo.clients : 'zero'
                }} clients!
              </v-list-item-title>
              <v-list-item-title v-else-if="$store.state.ntConnectionInfo.connected && $store.state.backendConnected"
                class="text-wrap">
                Robot connected! {{ $store.state.ntConnectionInfo.address }}
              </v-list-item-title>
              <v-list-item-title v-else class="text-wrap">
                Not connected to robot!
              </v-list-item-title>
              <router-link v-if="!$store.state.settings.networkSettings.runNTServer" to="settings" class="accent--text"
                @click="switchToSettingsTab">
                Team number is {{ $store.state.settings.networkSettings.teamNumber }}
              </router-link>
            </v-list-item-content>
          </v-list-item>

          <v-list-item>
            <v-list-item-icon>
              <v-icon v-if="$store.state.backendConnected">
                mdi-wifi
              </v-icon>
              <v-icon v-else class="pulse" style="border-radius: 100%;">
                mdi-wifi-off
              </v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title class="text-wrap">
                {{ $store.state.backendConnected ? "Backend Connected" : "Trying to connect..." }}
              </v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </div>
      </v-list>
    </v-navigation-drawer>
    <v-main>
      <v-container fluid fill-height>
        <v-layout>
          <v-flex>
            <router-view @switch-to-cameras="switchToDriverMode" />
          </v-flex>
        </v-layout>
      </v-container>
    </v-main>

    <v-dialog v-model="$store.state.logsOverlay" width="1500" dark>
      <logs />
    </v-dialog>
    <v-dialog v-model="needsTeamNumberSet" width="500" dark persistent>
      <v-card dark color="primary" flat>
        <v-card-title>No team number set!</v-card-title>
        <v-card-text>
          PhotonVision cannot connect to your robot! Please
          <router-link to="settings" class="accent--text" @click="switchToSettingsTab">
            visit the settings tab
          </router-link>
          and set your team number.
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script>
import Logs from "./views/LogsView"
import { ReconnectingWebsocket } from "./plugins/ReconnectingWebsocket.js"

export default {
  name: 'App',
  components: {
    Logs
  },
  data: () => ({
    // Used so that we can switch back to the previously selected pipeline after camera calibration
    previouslySelectedIndices: [],
    timer: undefined,
    teamNumberDialog: true,
    websocket: null,
  }),
  computed: {
    needsTeamNumberSet: {
      get() {
        return this.$store.state.settings.networkSettings.teamNumber < 1
          && this.teamNumberDialog && this.$store.state.backendConnected
          && !this.$route.name.toLowerCase().includes("settings");
      }
    },
    compact: {
      get() {
        if (this.$store.state.compactMode === undefined) {
          return this.$vuetify.breakpoint.smAndDown;
        } else {
          return this.$store.state.compactMode || this.$vuetify.breakpoint.smAndDown;
        }
      },
      set(value) {
        // compactMode is the user's preference for compact mode; it overrides screen size
        this.$store.commit("compactMode", value);
        localStorage.setItem("compactMode", value);
      },
    },
  },
  created() {
    document.addEventListener("keydown", e => {
      switch (e.key) {
        case "`":
          this.$store.state.logsOverlay = !this.$store.state.logsOverlay;
          break;
        case "z":
          if (e.ctrlKey && this.$store.getters.canUndo) {
            this.$store.dispatch('undo', { vm: this });
          }
          break;
        case "y":
          if (e.ctrlKey && this.$store.getters.canRedo) {
            this.$store.dispatch('redo', { vm: this });
          }
          break;

      }
    });

    const wsDataURL = 'ws://' + this.$address + '/websocket_data';
    this.websocket = new ReconnectingWebsocket(
      wsDataURL,

      // On data in
      (event) => {
        try {
          let message = this.$msgPack.decode(event.data);
          for (let prop in message) {
            if (message.hasOwnProperty(prop)) {
              this.handleMessage(prop, message[prop]);
            }
          }
        } catch (error) {
          console.log(event)
          console.error('error: ' + JSON.stringify(event.data) + " , " + error);
        }
      },

      // on connect
      (event) => {
        event; this.$store.commit("backendConnected", true);
        this.$store.state.connectedCallbacks.forEach(it => it());
       },

      // on disconnect
      (event) => { event; this.$store.commit("backendConnected", false) }
    );

    this.$store.commit("websocket", this.websocket);
  },
  methods: {
    handleMessage(key, value) {
      if (key === "logMessage") {
        this.logMessage(value["logMessage"], value["logLevel"]);
      } else if (key === "log") {
        this.logMessage(value["logMessage"]["logMessage"], value["logMessage"]["logLevel"]);
      } else if (key === "updatePipelineResult") {
        this.$store.commit('mutatePipelineResults', value)
      } else if (this.$store.state.hasOwnProperty(key)) {
        this.$store.commit(key, value);
      } else if (this.$store.getters.currentPipelineSettings.hasOwnProperty(key)) {
        this.$store.commit('mutatePipeline', { [key]: value });
      } else if (this.$store.state.settings.hasOwnProperty(key)) {
        this.$store.commit('mutateSettings', { [key]: value });
      } else {
        console.error("Unknown message from backend: " + value);
      }
    },
    toggleCompactMode() {
      this.compact = !this.compact;
    },
    // eslint-disable-next-line no-unused-vars
    logMessage(message, levelInt) {
      this.$store.commit('logString', {
        ['level']: levelInt,
        ['message']: message
      })
    },
    switchToDriverMode() {
      if (!this.previouslySelectedIndices) this.previouslySelectedIndices = [];

      for (const [i, cameraSettings] of this.$store.state.cameraSettings.entries()) {
        this.previouslySelectedIndices[i] = cameraSettings.currentPipelineIndex;
        this.handleInputWithIndex('currentPipeline', -1, i);
      }
    },
    rollbackPipelineIndex() {
      if (this.previouslySelectedIndices !== null) {
        for (const [i] of this.$store.state.cameraSettings.entries()) {
          this.handleInputWithIndex('currentPipeline', this.previouslySelectedIndices[i] || 0, i);
        }
      }
      this.previouslySelectedIndices = null;
    },
    switchToSettingsTab() {
      this.axios.post('http://' + this.$address + '/api/sendMetrics', {})
    }
  }
};
</script>

<style lang="sass">
    @import "./scss/variables.scss"
</style>

<style>
.pulse {
  animation: pulse-animation 2s infinite;
}

@keyframes pulse-animation {
  0% {
    box-shadow: 0 0 0 0px rgba(0, 0, 0, 0.2);
    background-color: rgba(0, 0, 0, 0.2);
  }

  100% {
    box-shadow: 0 0 0 20px rgba(0, 0, 0, 0);
    background-color: rgba(0, 0, 0, 0);
  }
}

.logo {
  width: 100%;
  height: 70px;
  object-fit: contain;
}

::-webkit-scrollbar {
  width: 0.5em;
  border-radius: 5px;
}

::-webkit-scrollbar-track {
  -webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
  border-radius: 10px;
}


::-webkit-scrollbar-thumb {
  background-color: #ffd843;
  border-radius: 10px;
}

.container {
  background-color: #232c37;
  padding: 0 !important;
}

#title {
  color: #ffd843;
}
</style>

<style>
/* Hacks */

.v-divider {
  border-color: white !important;
}

.v-input {
  font-size: 1rem !important;
}

/* This is unfortunately the only way to override table background color */
.theme--dark.v-data-table>.v-data-table__wrapper>table>tbody>tr:hover:not(.v-data-table__expanded__content):not(.v-data-table__empty-wrapper) {
  background: #005281 !important;
}
</style>

<style lang="scss">
@import '~vuetify/src/styles/settings/_variables';

@media #{map-get($display-breakpoints, 'md-and-down')} {
  html {
    font-size: 14px !important;
  }
}
</style>
