<template>
    <div id="SystemTab">
        <div>
            <row type="flex" justify="start" align="middle" :gutter="10" >
                <Col span="6">
                    <h4>Team Number:</h4>
                </Col>
                <col span="4">
                    <InputNumber :min="0" v-model="team_number" size="small"></InputNumber>
                </col>
          </row>
        </div>
        <Divider class="divdiv" orientation="left">Networking</Divider>
        <div>
            <RadioGroup v-model="connection_type" style="display: flex;">
                <Radio label="DHCP"></Radio>
                <Radio label="Static"></Radio>
            </RadioGroup>
                <div class="ipSettings">
                    <row type="flex" justify="start" align="middle" class="spacing">
                        <Col span="4">
                            <h4>IP:</h4>
                        </Col>
                        <Col span="10">
                            <Input v-model="ip" size="small" :disabled="isConnection"></Input>
                        </Col>
                    </row>
                    <row type="flex" justify="start" align="middle" class="spacing">
                        <Col span="4">
                            <h4>Netmask:</h4>
                        </Col>
                        <Col span="10">
                            <Input v-model="netmask" size="small" :disabled="isConnection"></Input>
                        </Col>
                    </row>
                    <row type="flex" justify="start" align="middle" class="spacing">
                        <Col span="4">
                            <h4>Gateway:</h4>
                        </Col>
                        <Col span="10">
                            <Input v-model="gateway" size="small" :disabled="isConnection"></Input>
                        </Col>
                    </row>
                    <row type="flex" justify="start" align="middle" class="spacing">
                        <Col span="4">
                        <h4>Hostname:</h4>
                        </Col>
                        <Col span="10">
                            <Input v-model="hostname" size="small">
                                <span slot="prepend">http://Chameleon-Vision-</span>
                                <span slot="append">.local</span>
                            </Input>
                        </Col>
                    </row>
            </div>
            <Divider class="divdiv" orientation="left"></Divider>
            <row type="flex" justify="start" align="middle" style="margin-top:20px">
                 <Button type="primary" size="small" v-on:click="socketSendAll">Save Changes and Restart</Button>
            </row>
        </div> 
    </div>
</template>

<script>
    import chInputNumber from './ch-inputNumber.vue'
    import chSelect from './ch-select.vue'
    export default {
        name: 'SystemTab',
        data() {
            return {
                lan:0            
            }
        },
        components:{
            chInputNumber,
            chSelect
        },
        methods: {
            socketSendAll: function(){
                this.$socket.sendObj(
                    {'change_general_settings_values':{
                        'team_number':this.team_number,
                        'connection_type':this.connection_type,
                        'ip':this.ip,
                        'netmask':this.netmask,
                        'gateway':this.gateway,
                        'hostname':this.hostname}});
            }
        },
        computed: {
            team_number:{
                get: function(){
                    return this.$store.state.team_number;
                },
                set: function(value){
                    this.$store.commit('team_number',value);
                }
            },
            connection_type:{
                get: function(){
                    return this.$store.state.connection_type;
                },
                set: function(value){
                    this.$store.commit('connection_type',value);
                }
            },
            ip:{
                get: function(){
                    return this.$store.state.ip;
                },
                set: function(value){
                    this.$store.commit('ip',value);
                }
            },
            netmask:{
                get: function(){
                    return this.$store.state.netmask;
                },
                set: function(value){
                    this.$store.commit('netmask',value);
                }
            },
            gateway:{
                get: function(){
                    return this.$store.state.gateway;
                },
                set: function(value){
                    this.$store.commit('gateway',value);
                }
            },
            hostname:{
                get: function(){
                    return this.$store.state.hostname;
                },
                set: function(value){
                    this.$store.commit('hostname',value);
                }
            },
            isConnection: function(){
                if(this.connection_type == "DHCP"){
                    return true
                } else{
                    return false
                }
            }
        }
    }
</script>            
<style>
    .ivu-divider-inner-text{
        color: aliceblue
    }
    .ivu-radio-group {
        display: flex;
        text-align: left;
        color: aliceblue;
    }
    .ipSettings{
        margin-top: 10px;
    }
    .spacing{
        margin-top: 10px;
    }
</style>