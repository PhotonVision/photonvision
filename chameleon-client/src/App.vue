<template>
    <v-app>
        <v-app-bar app dense clipped-left dark>
            <img class="imgClass" src="./assets/logo.png">
            <v-toolbar-title id="title">Chameleon Vision</v-toolbar-title>
            <div class="flex-grow-1"></div>
            <v-toolbar-items>
                <v-tabs background-color="#272727" dark height="48" slider-color="#4baf62">
                    <v-tab to="vision">Vision</v-tab>
                    <v-tab to="settings">Settings</v-tab>
                </v-tabs>
            </v-toolbar-items>
        </v-app-bar>
        <v-content>
            <v-container fluid fill-height>
                <v-layout>
                    <v-flex>
                        <router-view @save="startTimer"/>
                        <v-snackbar :timeout="1000" v-model="saveSnackbar" top color="#4baf62">
                            <div style="text-align: center;width: 100%;">
                                <h4>Saved All changes</h4>
                            </div>
                        </v-snackbar>
                    </v-flex>
                </v-layout>
            </v-container>
        </v-content>
    </v-app>
</template>

<script>
    export default {
        name: 'App',
        components: {},
        methods: {
            handleMessage(key, value) {
                if (this.$store.state.hasOwnProperty(key)) {
                    this.$store.commit(key, value);
                } else if (this.$store.state.pipeline.hasOwnProperty(key)) {
                    this.$store.commit('setPipeValues', {[key]: value});
                } else {
                    switch (key) {
                        default: {
                            console.log(key + " : " + value);
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
            }
        },
        data: () => ({
            timer: undefined
        }),
        created() {
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
        computed: {
            saveSnackbar: {
                get() {
                    return this.$store.state.saveBar;
                },
                set(value) {
                    this.$store.commit("saveBar", value);
                }
            }
        }
    };
</script>

<style>
    html {
        overflow-y: hidden !important;
    }

    .imgClass {
        width: auto;
        height: 45px;
        vertical-align: middle;
        padding-right: 5px;
    }

    .container {
        background-color: #212121;
        padding: 0 !important;
    }

    #title {
        color: #4baf62;
    }

    span {
        color: white;
    }
</style>