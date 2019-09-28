<template>
    <div>
        <CVnumberinput name="Team Number"></CVnumberinput>
        <CVradio v-model="t" :list="['DHCP','Static']"></CVradio>
        <v-divider color="white"></v-divider>
        <CVinput name="IP" :disabled="isDisabled"></CVinput>
        <CVinput name="NetMask" :disabled="isDisabled"></CVinput>
        <CVinput name="Gateway" :disabled="isDisabled"></CVinput>
        <v-divider color="white"></v-divider>
        <CVinput name="Hostname" ></CVinput>
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
                t:0
            }
        },
        computed:{
            isDisabled(){
                if(this.t === 0){
                    return true;
                }
                return false;
            },
            
        }
    }
</script>

<style lang="" scoped>
    
</style>