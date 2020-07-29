<template>
  <v-app>
    <!-- Although most of the app runs with the "light" theme, the navigation drawer needs to have white text and icons so it uses the dark theme-->
    <v-navigation-drawer
      dark
      app
      permanent
      :mini-variant="compact"
      color="primary"
    >
      <v-list>
        <!-- List item for the heading; note that there are some tricks in setting padding and image width make things look right -->
        <v-list-item :class="compact ? 'pr-0 pl-0' : ''">
          <v-list-item-icon class="mr-0">
            <img
              v-if="!compact"
              class="logo"
              src="./assets/logoLarge.png"
            >
            <img
              v-else
              class="logo"
              src="./assets/logoSmall.png"
            >
          </v-list-item-icon>
        </v-list-item>

        <v-list-item
          link
          to="dashboard"
          @click="rollbackPipelineIndex()"
        >
          <v-list-item-icon>
            <v-icon>mdi-view-dashboard</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Dashboard</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item
          link
          to="cameras"
          @click="switchToDriverMode()"
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
          to="settings"
        >
          <v-list-item-icon>
            <v-icon>mdi-settings</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Settings</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item
          link
          to="docs"
        >
          <v-list-item-icon>
            <v-icon>mdi-bookshelf</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Documentation</v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item
          v-if="this.$vuetify.breakpoint.mdAndUp"
          link
          @click.stop="toggleCompactMode"
        >
          <v-list-item-icon>
            <v-icon v-if="compact">
              mdi-chevron-right
            </v-icon>
            <v-icon v-else>
              mdi-chevron-left
            </v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>Advanced Mode</v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item style="position: absolute; bottom: 0; left: 0;">
          <v-list-item-icon>
            <v-icon v-if="$store.state.backendConnected">
              mdi-wifi
            </v-icon>
            <v-icon
              v-else
              class="pulse"
              style="border-radius: 100%;"
            >
              mdi-wifi-off
            </v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>{{ $store.state.backendConnected ? "Connected" : "Trying to connect..." }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>
    <v-content>
      <v-container
        fluid
        fill-height
      >
        <v-layout>
          <v-flex>
            <router-view @save="startTimer" />
            <v-snackbar
              v-model="saveSnackbar"
              :timeout="1000"
              top
              color="accent"
            >
              <div style="text-align: center;width: 100%;">
                <h4>Saved All changes</h4>
              </div>
            </v-snackbar>
            <div v-if="isLogger">
              <keep-alive>
                <log-view
                  class="loggerClass"
                  :log="log"
                />
              </keep-alive>
            </div>
          </v-flex>
        </v-layout>
      </v-container>
    </v-content>
  </v-app>
</template>

<script>
    import logView from '@femessage/log-viewer';

    export default {
        name: 'App',
        components: {
            logView
        },
        data: () => ({
            // Used so that we can switch back to the previously selected pipeline after camera calibration
            previouslySelectedIndex: null,
            timer: undefined,
            isLogger: false,
            log: "",
        }),
        computed: {
            saveSnackbar: {
                get() {
                    return this.$store.state.saveBar;
                },
                set(value) {
                    this.$store.commit("saveBar", value);
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
            }
        },
        created() {
            document.addEventListener("keydown", e => {
                switch (e.key) {
                    case '`' :
                        this.isLogger = !this.isLogger;
                        break;
                    case "z":
                        if (e.ctrlKey && this.$store.getters.canUndo) {
                            this.$store.dispatch('undo', {vm: this});
                        }
                        break;
                    case "y":
                        if (e.ctrlKey && this.$store.getters.canRedo) {
                            this.$store.dispatch('redo', {vm: this});
                        }
                        break;

                }
            });

            this.$options.sockets.onmessage = (data) => {
                try {
                    let message = this.$msgPack.decode(data.data);
                    for (let prop in message) {
                        if (message.hasOwnProperty(prop)) {
                            this.handleMessage(prop, message[prop]);
                        }
                    }
                } catch (error) {
                    console.error('error: ' + data.data + " , " + error);
                }
            };
            this.$options.sockets.onopen = () => {
              this.$store.state.backendConnected = true;
            };

            let closed = () => {
              this.$store.state.backendConnected = false;
            }
            this.$options.sockets.onclose = closed;
            this.$options.sockets.onerror = closed;

            this.$connect();
        },
        methods: {
            handleMessage(key, value) {
                if (key === "logMessage") {
                    console.log("[FROM BACKEND]" + value);
                    this.logMessage(value, 0);
                } else if (key === "updatePipelineResult") {
                    this.$store.commit('mutatePipelineResults', value)
                } else if (this.$store.state.hasOwnProperty(key)) {
                    this.$store.commit(key, value);
                } else if (this.$store.getters.currentPipelineSettings.hasOwnProperty(key)) {
                    this.$store.commit('mutatePipeline', {[key]: value});
                } else {
                    switch (key) {
                        default: {
                            console.error("Unknown message from backend: " + value);
                        }
                    }
                }
            },
            toggleCompactMode() {
                this.compact = !this.compact;
            },
            saveSettings() {
                clearInterval(this.timer);
                this.saveSnackbar = true;
                this.handleInput("command", "save");
            },
            startTimer() {
                if (this.timer !== undefined) {
                    clearInterval(this.timer);
                }
                this.timer = setInterval(this.saveSettings, 4000);
            },
            logMessage(message, level) {
                const colors = ["\u001b[31m", "\u001b[32m", "\u001b[33m", "\u001b[34m"]
                const reset = "\u001b[0m"
                this.log += `${colors[level]}${message}${reset}\n`
            },
            switchToDriverMode() {
                this.previouslySelectedIndex = this.$store.getters.currentPipelineIndex;
                this.handleInputWithIndex('currentPipeline', -1)
            },
            rollbackPipelineIndex() {
                if (this.previouslySelectedIndex !== null) {
                  this.handleInputWithIndex('currentPipeline', this.previouslySelectedIndex)
                }
                this.previouslySelectedIndex = null;
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

    .loggerClass {
        position: absolute;
        bottom: 0;
        height: 25% !important;
        left: 0;
        right: 0;
        box-shadow: #282828 0 0 5px 1px;
        background-color: #2b2b2b;
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

    span {
        color: white;
    }
</style>

<style>
  /* Hack */
  .v-divider {
    border-color: white !important;
  }

  .v-input {
    font-size: 1rem !important;
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