<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
    >
      <CVinput
        v-model="ntServerAddress"
        :input-cols="inputCols - 1"
        :label-cols="3"
        :disabled="settings.runNTServer"
        name="Team Number/NetworkTables Server Address"
        tooltip="Enter the Team Number or the IP address of the NetworkTables Server"
        :rules="[v => isValidTeamNumber(v) || 'The NetworkTables Server Address must be a non blank team number, IP address, or hostname']"
      />
      <v-banner
        v-show="!isValidTeamNumber(ntServerAddress) && !runNTServer"
        rounded
        color="red"
        text-color="white"
        style="margin: 8px 0"
        icon="mdi-alert-circle-outline"
      >
        NetworkTables Server Address is unset or invalid. NetworkTables is unable to connect
      </v-banner>
      <CVSwitch
        v-model="shouldManage"
        name="Manage networking"
        tooltip="If enabled, Photon will manage device hostname and network settings"
        class="mt-3 mb-3"
        :text-cols="$vuetify.breakpoint.mdAndUp ? undefined : 5"
      />
      <CVradio
        v-model="connectionType"
        :disabled="!$store.state.settings.networkSettings.shouldManage"
        :input-cols="inputCols"
        name="IP Assignment Mode"
        tooltip="DHCP will make the radio (router) automatically assign an IP address; this may result in an IP address that changes across reboots. Static IP assignment means that you pick the IP address and it won't change."
        :list="['DHCP','Static']"
      />
      <CVinput
        v-if="!isDHCP"
        v-model="staticIp"
        :input-cols="inputCols"
        :rules="[v => isIPv4(v) || 'Invalid IPv4 address']"
        name="IP"
      />
      <CVinput
        v-model="hostname"
        :disabled="!$store.state.settings.networkSettings.shouldManage"
        :input-cols="inputCols"
        :rules="[v => isHostname(v) || 'Invalid hostname']"
        name="Hostname"
      />
      <CVSwitch
        v-model="runNTServer"
        name="Run NetworkTables Server (Debugging Only)"
        tooltip="If enabled, this device will create a NT server. This is useful for home debugging, but should be disabled on-robot."
        class="mt-3 mb-3"
        :text-cols="$vuetify.breakpoint.mdAndUp ? undefined : 5"
      />
      <v-banner
        v-show="runNTServer"
        rounded
        color="red"
        text-color="white"
        icon="mdi-information-outline"
      >
        This mode is intended for debugging; it should be off for proper usage. PhotonLib will NOT work!
      </v-banner>
    </v-form>
    <v-btn
      color="accent"
      :class="runNTServer ? 'mt-3' : ''"
      style="color: black; width: 100%;"
      :disabled="!valid && !runNTServer"
      @click="sendGeneralSettings()"
    >
      Save
    </v-btn>
    <v-snackbar
      v-model="snack"
      top
      :color="snackbar.color"
      timeout="2000"
    >
      <span>{{ snackbar.text }}</span>
    </v-snackbar>

    <template v-if="$store.state.settings.networkSettings.shouldManage && false">
      <!-- Advanced controls for changing DHCP settings and stuff -->
      <v-divider class="mt-4 mb-4" />
      <v-card-title> Advanced </v-card-title>
      <CVinput
        :input-cols="inputCols"
        name="Set DHCP command"
      />
      <CVinput
        :input-cols="inputCols"
        name="Set static command"
      />
      <CVinput
        :input-cols="inputCols"
        name="NetworkManager interface"
      />
      <CVinput
        :input-cols="inputCols"
        name="Physical interface"
      />
    </template>
  </div>
</template>

<script>
import CVradio from '../../components/common/cv-radio'
import CVinput from '../../components/common/cv-input'
import CVSwitch from "@/components/common/cv-switch";

// https://stackoverflow.com/a/17871737
const ipv4Regex = /^((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])$/;
// https://stackoverflow.com/a/18494710
const hostnameRegex = /^([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*)+(\.([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*))*$/;
const teamNumberRegex = /^[1-9][0-9]{0,3}$/;
const badTeamNumberRegex = /^[0-9]{5,}$/;

export default {
    name: 'Networking',
    components: {
        CVSwitch,
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
            isLoading: false,
            valid: true, // Are all settings valid
        }
    },
    computed: {
        inputCols() {
            return this.$vuetify.breakpoint.mdAndUp ? 10 : 7;
        },
        isDHCP() {
            return this.settings.connectionType === 0;
        },
        settings() {
            return this.$store.state.settings.networkSettings;
        },
        ntServerAddress: {
            get() {
                return this.settings.ntServerAddress
            },
            set(value) {
                this.$store.commit('mutateNetworkSettings', {['ntServerAddress']: value || ""});
           }
        },
        runNTServer: {
            get() {
                return this.settings.runNTServer
            },
            set(value) {
                this.$store.commit('mutateNetworkSettings', {['runNTServer']: value});
            }
        },
        connectionType: {
            get() {
                return this.settings.connectionType
            },
            set(value) {
                this.$store.commit('mutateNetworkSettings', {['connectionType']: value});
            }
        },
        shouldManage: {
            get() {
                return this.settings.shouldManage
            },
            set(value) {
                this.$store.commit('mutateNetworkSettings', {['shouldManage']: value});
            }
        },
        staticIp: {
            get() {
                return this.settings.staticIp
            },
            set(value) {
                this.$store.commit('mutateNetworkSettings', {['staticIp']: value});
            }
        },
        hostname: {
            get() {
                return this.settings.hostname
            },
            set(value) {
                this.$store.commit('mutateNetworkSettings', {['hostname']: value});
            }
        },
    },
    methods: {
        isValidTeamNumber(v) {
            if (teamNumberRegex.test(v)) return true;
            if (ipv4Regex.test(v)) return true;
            // need to check these before the hostname. "0" and "99999" are valid hostnames,
            // but we don't want to allow then
            if (v == '0') return false;
            if (badTeamNumberRegex.test(v)) return false;
            if (hostnameRegex.test(v)) return true;
            return false;
        },
        isIPv4(v) {
            return ipv4Regex.test(v);
        },
        isHostname(v) {
            return hostnameRegex.test(v);
        },
        // https://www.freesoft.org/CIE/Course/Subnet/6.htm
        // https://stackoverflow.com/a/13957228
        isSubnetMask(v) {
            // Has to be valid IPv4 so we'll start here
            if (!this.isIPv4(v)) return false;

            let octets = v.split(".").map(it => Number(it));
            let restAreOnes = false;
            for (let i = 3; i >= 0; i--) {
                for (let j = 0; j < 8; j++) {
                    let bitValue = (octets[i] >>> j & 1) === 1;
                    if (restAreOnes && !bitValue)
                        return false;
                    restAreOnes = bitValue;
                }
            }
            return true;
        },
        sendGeneralSettings() {
            const changingStaticIp = !this.isDHCP;

            this.snackbar = {
                color: "secondary",
                text: "Updating settings..."
            };
            this.snack = true;

            this.axios.post("http://" + this.$address + "/api/settings/general", this.settings)
                .then(response => {
                  this.snackbar = {
                    color: "success",
                    text: response.data.text || response.data
                  }
                  this.snack = true;
                })
                .catch(error => {
                  if(error.response) {
                    if (error.status === 504 || changingStaticIp) {
                      this.snackbar = {
                        color: "error",
                        text: `Connection lost! Try the new static IP at ${this.staticIp}:5800 or ${this.hostname}:5800?`
                      };
                    } else {
                      this.snackbar = {
                        color: "error",
                        text: error.response.data.text || error.response.data
                      }
                    }
                  } else if(error.request) {
                    this.snackbar = {
                      color: "error",
                      text: "Error while trying to process the request! The backend didn't respond.",
                    };
                  } else {
                    this.snackbar = {
                      color: "error",
                      text: "An error occurred while trying to process the request.",
                    };
                  }
                  this.snack = true;
                })
        },

    },
}
</script>

<style scoped>
    .v-data-table {
      /*text-align: center;*/
      background-color: transparent !important;
      width: 100%;
      height: 100%;
      overflow-y: auto;
    }

    .v-data-table th {
      background-color: #006492 !important;
    }

    .v-data-table th, td {
      font-size: 1rem !important;
    }

    .v-data-table td {
      font-family: monospace !important;
    }
</style>
<style>
.v-banner__wrapper {
  padding: 6px !important;
}
</style>
