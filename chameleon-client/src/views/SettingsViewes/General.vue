<template>
    <div>
        <CVnumberinput v-model="settings.teamNumber" name="Team Number"/>
        <CVradio v-model="settings.connectionType" :list="['DHCP','Static']"/>
        <v-divider color="white"/>
        <CVinput name="IP" v-model="settings.ip" :disabled="isDisabled"/>
        <CVinput name="NetMask" v-model="settings.netmask" :disabled="isDisabled"/>
        <CVinput name="Gateway" v-model="settings.gateway" :disabled="isDisabled"/>
        <v-divider color="white"/>
        <CVinput name="Hostname" v-model="settings.hostname"/>
        <v-btn style="margin-top:10px" small color="#4baf62" @click="sendGeneralSettings">Save General Settings</v-btn>
    </div>
</template>

<script>
    import CVnumberinput from '../../components/cv-number-input'
    import CVradio from '../../components/cv-radio'
    import CVinput from '../../components/cv-input'

    export default {
        name: 'General',
        components: {
            CVnumberinput,
            CVradio,
            CVinput
        },
        data() {
            return {}
        },
        methods: {
            sendGeneralSettings() {
                const self = this;
                this.axios.post("http://" + this.$address + "/api/settings/general", this.settings).then(
                    function (response) {
                        if (response.status === 200){
                            self.$store.state.saveBar = true;
                        }
                    }
                )
            }
        },
        computed: {
            isDisabled() {
                if (this.settings.connectionType === 0) {
                    return true;
                }
                return false;
            },
            settings: {
                get() {
                    return this.$store.state.settings;
                }
            }
        }
    }
</script>

<style lang="" scoped>

</style>