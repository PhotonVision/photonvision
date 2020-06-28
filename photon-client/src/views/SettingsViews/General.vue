<template>
  <div>
    <div style="margin-top: 15px">
      <span>General Settings:</span>
      <v-divider color="white" />
    </div>
    <CVnumberinput
      v-model="settings.teamNumber"
      name="Team Number"
    />
    <CVradio
      v-model="settings.connectionType"
      :list="['DHCP','Static']"
    />
    <v-divider color="white" />
    <CVinput
      v-model="settings.ip"
      name="IP"
      :disabled="isDisabled"
    />
    <CVinput
      v-model="settings.netmask"
      name="NetMask"
      :disabled="isDisabled"
    />
    <CVinput
      v-model="settings.gateway"
      name="Gateway"
      :disabled="isDisabled"
    />
    <v-divider color="white" />
    <CVinput
      v-model="settings.hostname"
      name="Hostname"
    />
    <v-btn
      style="margin-top:10px"
      small
      color="#ffd843"
      @click="sendGeneralSettings"
    >
      Save General Settings
    </v-btn>
    <div style="margin-top: 20px">
      <span>Install or Update:</span>
      <v-divider color="white" />
    </div>
    <div v-if="!isLoading">
      <v-row
        dense
        align="center"
      >
        <v-col :cols="3">
          <span>Choose a newer version: </span>
        </v-col>
        <v-col :cols="6">
          <v-file-input
            v-model="file"
            accept=".jar"
            dark
          />
        </v-col>
      </v-row>
      <v-btn
        small
        @click="installOrUpdate"
      >
        {{ fileUploadText }}
      </v-btn>
    </div>
    <div
      v-else
      style="text-align: center; margin-top: 20px"
    >
      <v-progress-circular
        color="white"
        :indeterminate="true"
        size="32"
        width="4"
      />
      <br>
      <span>Please wait this may take a while</span>
    </div>
    <v-snackbar
      v-model="snack"
      top
      :color="snackbar.color"
    >
      <span>{{ snackbar.text }}</span>
    </v-snackbar>
  </div>
</template>

<script>
    import CVnumberinput from '../../components/common/cv-number-input'
    import CVradio from '../../components/common/cv-radio'
    import CVinput from '../../components/common/cv-input'

    export default {
        name: 'General',
        components: {
            CVnumberinput,
            CVradio,
            CVinput
        },
        data() {
            return {
                file: undefined,
                snackbar: {
                    color: "success",
                    text: ""
                },
                snack: false,
                isLoading: false
            }
        },
        computed: {
            fileUploadText() {
                if (this.file !== undefined) {
                    return "Update and run at startup"
                } else {
                    return "Run current version at startup"
                }
            },
            isDisabled() {
                return this.settings.connectionType === 0;
            },
            settings: {
                get() {
                    return this.$store.state.settings;
                }
            }
        },
        methods: {
            sendGeneralSettings() {
                const self = this;
                this.axios.post("http://" + this.$address + "/api/settings/general", this.settings).then(
                    function (response) {
                        if (response.status === 200) {
                            self.snackbar = {
                                color: "success",
                                text: "Save successful, Please restart for changes to take action"
                            };
                            self.snack = true;
                        }
                    },
                    function (error) {
                        self.snackbar = {
                            color: "error",
                            text: error.response.data
                        };
                        self.snack = true;
                    }
                )
            },
            installOrUpdate() {
                let formData = new FormData();
                formData.append('file', this.file);
                if (this.file !== undefined) {
                    this.isLoading = true;
                }
                this.axios.post("http://" + this.$address + "/api/install", formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }).then(() => {
                    this.snackbar = {
                        color: "success",
                        text: "Installation successful"
                    };
                    this.isLoading = false;
                    this.snack = true;
                }).catch(error => {
                    this.snackbar = {
                        color: "error",
                        text: error.response.data
                    };
                    this.isLoading = false;
                    this.snack = true;
                })
            }
        }
    }
</script>

<style lang="" scoped>

</style>