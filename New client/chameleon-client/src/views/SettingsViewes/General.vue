<template>
    <div>
        <CVnumberinput name="Team Number"></CVnumberinput>
        <CVradio v-model="settings.connectionType" :list="['DHCP','Static']"></CVradio>
        <v-divider color="white"></v-divider>
        <CVinput name="IP" v-model="settings.ip" :disabled="isDisabled"></CVinput>
        <CVinput name="NetMask" v-model="settings.netmask" :disabled="isDisabled"></CVinput>
        <CVinput name="Gateway" v-model="settings.gateway" :disabled="isDisabled"></CVinput>
        <v-divider color="white"></v-divider>
        <CVinput name="Hostname" v-model="settings.hostname"></CVinput>
        <v-btn style="margin-top:10px" small color="#4baf62">Save General Settings</v-btn>
    </div>
</template>

<script>
import CVnumberinput from '../../components/cv-number-input'
import CVradio from '../../components/cv-radio'
import CVinput from '../../components/cv-input'
    export default {
        name: 'General',
        components:{
            CVnumberinput,
            CVradio,
            CVinput
        },
        data() {
            return {
            }
        },
        computed:{
            isDisabled(){
                if(this.settings.connectionType === 0){
                    return true;
                }
                return false;
            },
            settings:{
                get(){
                    return this.$store.state.settings;
                },
                set(value){
                    this.$store.commit('settings',value);
                }
            }
            
        }
    }
</script>

<style lang="" scoped>
    
</style>