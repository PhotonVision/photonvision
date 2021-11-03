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
      />
      <span v-if="parseInt(teamNumber) < 1 && !runNTServer" class="red font-weight-bold">Team number not set! NetworkTables cannot connect.</span>
      <CVradio
        v-model="connectionType"
        :list="['DHCP','Static']"
        :disabled="!$store.state.settings.networkSettings.supported"
      />
      <template v-if="!isDHCP">
        <CVinput
          v-model="staticIp"
          :input-cols="inputCols"
          :rules="[v => isIPv4(v) || 'Invalid IPv4 address']"
          name="IP"
        />
      </template>
      <CVinput
        v-model="hostname"
        :input-cols="inputCols"
        :rules="[v => isHostname(v) || 'Invalid hostname']"
        name="Hostname"
      />
      Advanced
      <v-divider/>
      <CVSwitch
          v-model="runNTServer"
          name="Run NetworkTables Server (Debugging Only!)"
          tooltip="If enabled, this device will create a NT server. This is useful for home debugging, but should be disabled on-robot."
      />
      <span v-if="runNTServer" class="red font-weight-bold">Disable this switch if you're on a robot! Photonlib will NOT work.</span>
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
            return this.$vuetify.breakpoint.smAndUp ? 10 : 7;
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
            this.axios.post("http://" + this.$address + "/api/settings/general", this.settings).then(
                function (response) {
                    if (response.status === 200) {
                        this.snackbar = {
                            color: "success",
                            text: "Settings updated successfully"
                        };
                        this.snack = true;
                    }
                },
                function (error) {
                    this.snackbar = {
                        color: "error",
                        text: (error.response || {data: "Couldn't save settings"}).data
                    };
                    this.snack = true;
                }
            )
        },

    },
}
</script>

<style lang="" scoped>

</style>
