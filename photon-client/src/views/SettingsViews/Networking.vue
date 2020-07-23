<template>
  <div>
    <CVnumberinput
      v-model="settings.teamNumber"
      name="Team Number"
      :rules="[v => (v > 0) || 'Team number must be greater than zero', v => (v < 10000) || 'Team number must have fewer than five digits']"
    />
    <template v-if="$store.state.settings.networkSettings.supported">
      <CVradio
        v-model="settings.connectionType"
        :list="['DHCP','Static']"
      />
      <template v-if="!isDHCP">
        <CVinput
          v-model="settings.ip"
          :input-cols="inputCols"
          :rules="[v => isIPv4(v) || 'Invalid IPv4 address']"
          name="IP"
        />
        <CVinput
          v-model="settings.netmask"
          :input-cols="inputCols"
          :rules="[v => isSubnetMask(v) || 'Invalid subnet mask']"
          name="Subnet Mask"
        />
      </template>
    </template>
    <CVinput
      v-model="settings.hostname"
      :input-cols="inputCols"
      :rules="[v => isHostname(v) || 'Invalid hostname']"
      name="Hostname"
    />
  </div>
</template>

<script>
    import CVnumberinput from '../../components/common/cv-number-input'
    import CVradio from '../../components/common/cv-radio'
    import CVinput from '../../components/common/cv-input'

    // https://stackoverflow.com/a/17871737
    const ipv4Regex = /^((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])$/;
    // https://stackoverflow.com/a/18494710
    const hostnameRegex = /^([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*)+(\.([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*))*$/;

    export default {
        name: 'Networking',
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
            inputCols() {
              return this.$vuetify.breakpoint.smAndUp ? 10 : 7;
            },
            isDHCP() {
                return this.settings.connectionType === 0;
            },
            settings() {
                return this.$store.state.settings.networkSettings;
            }
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
            }
        },
    }
</script>

<style lang="" scoped>

</style>
