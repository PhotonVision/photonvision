<template>
    <div id="SystemTab">
        <div>
            <row type="flex" justify="start" align="middle" :gutter="10" >
                <Col span="6">
                    <h4>Team Number:</h4>
                </Col>
                <col span="4">
                <InputNumber :min="0" v-model="teamNum" size="small"></InputNumber>
                </col>
          </row>
        </div>
        <Divider class="divdiv" orientation="left">Networking</Divider>
        <div>
            <RadioGroup v-model="connectionType" style="display: flex;">
                <Radio label="DHCP"></Radio>
                <Radio label="Static"></Radio>
            </RadioGroup>
                <div class="ipSettings">
                    <row type="flex" justify="start" align="middle" class="spacing">
                        <Col span="4">
                        <h4>IP:</h4>
                        </Col>
                        <Col span="10">
                        <Input v-model="IP" size="small" :disabled="isConnection"></Input>
                        </Col>
                    </row>
                    <row type="flex" justify="start" align="middle" class="spacing">
                        <Col span="4">
                        <h4>Gateway:</h4>
                        </Col>
                        <Col span="10">
                        <Input v-model="gateWay" size="small" :disabled="isConnection"></Input>
                        </Col>
                    </row>
                    <row type="flex" justify="start" align="middle" class="spacing">
                        <Col span="4">
                        <h4>Hostname:</h4>
                        </Col>
                        <Col span="10">
                            <Input v-model="hostName" size="small">
                                <span slot="prepend">http://Chameleon-Vision</span>
                                <span slot="append">.local</span>
                            </Input>
                        </Col>
                    </row>
            </div>
            <Divider class="divdiv" orientation="left"></Divider>
            <row type="flex" justify="start" align="middle" style="margin-top:20px">
                 <Button type="primary" size="small" v-on:click="socketSendAll">Save Changes</Button>
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
                this.$socket.sendObj([
                    {'teamValue':this.teamNum},
                    {'connectionType':this.connectionType},
                    {'ip':this.ip},
                    {'gateWay':this.gateWay},
                    {'hostName':this.hostName}]);
            }
        },
        computed: {
            teamNum:{
                get: function(){
                    return this.$store.state.teamValue;
                },
                set: function(value){
                    this.$store.commit('teamValue',value);
                }
            },
            connectionType:{
                get: function(){
                    return this.$store.state.connectionType;
                },
                set: function(value){
                    this.$store.commit('connectionType',value);
                }
            },
            IP:{
                get: function(){
                    return this.$store.state.ip;
                },
                set: function(value){
                    this.$store.commit('ip',value);
                }
            },
            gateWay:{
                get: function(){
                    return this.$store.state.gateWay;
                },
                set: function(value){
                    this.$store.commit('gateWay',value);
                }
            },
            hostName:{
                get: function(){
                    return this.$store.state.hostName;
                },
                set: function(value){
                    this.$store.commit('hostName',value);
                }
            },
            isConnection: function(){
                if(this.connectionType == "DHCP"){
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