<template>
  <v-app>
    <v-app-bar
      app
      dense
      clipped-left
      color="#006492"
      dark
    >
      <img
        class="imgClass"
        src="./assets/logo.png"
      >
      <div class="flex-grow-1" />
      <v-toolbar-items>
        <v-tabs
          background-color="#006492"
          dark
          height="48"
          slider-color="#ffd843"
        >
          <v-tab to="vision">
            Vision
          </v-tab>
          <v-tab to="settings">
            Settings
          </v-tab>
        </v-tabs>
      </v-toolbar-items>
    </v-app-bar>
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
              color="#ffd843"
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
    import logView from '@femessage/log-viewer'

    export default {
        name: 'App',
        components: {
            logView
        },
        data: () => ({
            timer: undefined,
            isLogger: false,
            log: ""
        }),
        computed: {
            saveSnackbar: {
                get() {
                    return this.$store.state.saveBar;
                },
                set(value) {
                    this.$store.commit("saveBar", value);
                }
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
            }
        },
        methods: {
            handleMessage(key, value) {
                if (key === "logMessage") {
                    console.log(value)
                    this.logMessage(value, 0)
                } else if (key === "updatePipelineResult") {
                    this.$store.commit('mutatePipelineResults', value)
                } else if (this.$store.state.hasOwnProperty(key)) {
                    this.$store.commit(key, value);
                } else if (this.$store.getters.currentPipelineSettings.hasOwnProperty(key)) {
                    this.$store.commit('mutatePipeline', {'key': key, 'value': value});
                } else {
                    switch (key) {
                        default: {
                            console.log(value);
                        }
                    }
                }
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
            }
        }
    };
</script>

<style lang="sass">
@import "./scss/variables.scss"
</style>

<style>
    
    .imgClass {
        width: auto;
        height: 45px;
        vertical-align: middle;
        padding-right: 5px;
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