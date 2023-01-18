<template>
  <div>
    <v-form
      ref="form"
      v-model="valid"
    >
      <CVnumberinput
        v-model="teamNumber"
        :disabled="settings.runNTServer"
        name="Team Number"
        :rules="[v => (v > 0) || 'Team number must be greater than zero', v => (v < 10000) || 'Team number must have fewer than five digits']"
        class="mb-4"
        :label-cols="$vuetify.breakpoint.mdAndUp ? undefined : 5"
      />
      <v-banner
        v-show="(teamNumber < 1 || teamNumber > 10000) && !runNTServer"
        rounded
        color="red"
        text-color="white"
      >
        Team number is unset or invalid. NetworkTables will not be able to connect.
      </v-banner>
      <CVradio
        v-show="$store.state.settings.networkSettings.shouldManage"
        v-model="connectionType"
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
      >
        This switch is intended for testing; it should be off on a robot. PhotonLib will NOT work!
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
      timeout="5000"
    >
      <span>{{ snackbar.text }}</span>
    </v-snackbar>

    <template v-if="$store.state.settings.networkSettings.shouldManage && false">

      <!-- Advanced controls for changing DHCP settings and stuff -->
      <v-divider class="mt-4 mb-4" />

      <v-title> Advanced </v-title>

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

    <!-- TEMP - RIO finder is not currently enabled
    <v-row>
      <v-col
        cols="12"
        sm="6"
      >
        <v-simple-table
          fixed-header
          height="100%"
          dense
        >
          <template v-slot:default>
            <thead style="font-size: 1.25rem;">
              <tr>
                <th>
                  Device IPs
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(value, index) in $store.state.networkInfo.deviceips"
                :key="index"
              >
                <td>{{ value }}</td>
              </tr>
            </tbody>
          </template>
        </v-simple-table>
      </v-col>
      <v-col
        cols="12"
        sm="6"
      >
        <v-simple-table
          fixed-header
          height="100%"
          dense
        >
          <template v-slot:default>
            <thead style="font-size: 1.25rem;">
              <tr>
                <th>
                  Possible RoboRIOs
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(value, index) in $store.state.networkInfo.possibleRios"
                :key="index"
              >
                <td>{{ value }}</td>
              </tr>
            </tbody>
          </template>
        </v-simple-table>
      </v-col>
    </v-row>
    -->
  </div>
</template>

<script>
import CVnumberinput from '../../components/common/cv-number-input'
import CVradio from '../../components/common/cv-radio'
import CVinput from '../../components/common/cv-input'
import CVSwitch from "@/components/common/cv-switch";

// https://stackoverflow.com/a/17871737
const ipv4Regex = /^((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])$/;
// https://stackoverflow.com/a/18494710
const hostnameRegex = /^([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*)+(\.([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*))*$/;

export default {
    name: 'Networking',
    components: {
        CVSwitch,
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
        teamNumber: {
            get() {
                return this.settings.teamNumber
            },
            set(value) {
                this.$store.commit('mutateNetworkSettings', {['teamNumber']: value || 0});
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
                    let bitValue = (octets[i] >>> j & 1) == 1;
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

            this.axios.post("http://" + this.$address + "/api/settings/general", this.settings).then(
                response => {
                    if (response.status === 200) {
                        this.snackbar = {
                            color: "success",
                            text: "Settings updated successfully"
                        };
                        this.snack = true;
                    }
                },
                error => {
                  if (error.status === 504 || changingStaticIp) {
                    this.snackbar = {
                        color: "error",
                        text: (error.response || {data: `Connection lost! Try the new static IP at ${this.staticIp}:5800 or ${this.hostname}:5800 ?`}).data
                    };
                  } else {
                    this.snackbar = {
                        color: "error",
                        text: (error.response || {data: "Couldn't save settings"}).data
                    };
                  }
                    this.snack = true;
                }
            )
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
