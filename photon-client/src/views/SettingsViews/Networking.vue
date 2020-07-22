<template>
  <div>
    <CVnumberinput
      v-model="settings.teamNumber"
      name="Team Number"
    />
    <template v-if="$store.state.settings.networking.supported">
      <CVradio
        v-model="settings.connectionType"
        :list="['DHCP','Static']"
      />
      <template v-if="!isDHCP">
        <CVinput
          v-model="settings.ip"
          :input-cols="inputCols"
          name="IP"
        />
        <CVinput
          v-model="settings.netmask"
          :input-cols="inputCols"
          name="Netmask"
        />
        <CVinput
          v-model="settings.gateway"
          :input-cols="inputCols"
          name="Gateway"
        />
      </template>
    </template>
    <CVinput
      v-model="settings.hostname"
      :input-cols="inputCols"
      name="Hostname"
    />
  </div>
</template>

<script>
    import CVnumberinput from '../../components/common/cv-number-input'
    import CVradio from '../../components/common/cv-radio'
    import CVinput from '../../components/common/cv-input'

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
                return this.$store.state.settings.networking;
            }
        },
    }
</script>

<style lang="" scoped>

</style>