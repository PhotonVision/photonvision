<template>
    <div id="cameraTab" class="spacing">
        <chselect title="select camera" :list="cameraList" Xkey="curr_camera"></chselect>

        <Row type="flex" justify="start" align="middle" :gutter="1" class="spacing">
            <Col span="4">
                <h4>Resolution:</h4>
            </Col>
            <Col span="4">
                <i-select v-model="resolution" size="small" >
                    <i-option v-for="(item,index) in resolutionList" :value="index" :key="index">{{item}}</i-option>
                </i-select>
            </Col>
        </Row>

        <Row type="flex" justify="start" align="middle" :gutter="1" class="spacing">
            <Col span="4">
                <h4>Diagonal FOV:</h4>
            </Col>
            <Col span="4">
                <InputNumber :min="0" v-model="FOV" size="small"></InputNumber>
            </Col>
        </Row>
        
        <Button type="primary" size="small" class="buttonClass spacing" v-on:click="socketSendAll">Save settings to current camera</Button>
          
          <h4 class="spacing">Please Restart the computer Manually after saving all cameras</h4>
        
    </div>
</template>

<script>
import chselect from './ch-select.vue'
import chIndexSelect from './ch-IndexSelect.vue'

    export default {
        name: 'cameraTab',
        components: {
            chselect,
            chIndexSelect
        },
        data() {
            return {
            }
        },
        methods: {
            socketSendAll: function(){
                this.$socket.sendObj({'resolution':this.resolution});
                this.$socket.sendObj({'FOV':this.FOV});
                }
        },
        computed: {
            cameraList:{
                get:function(){
                    return this.$store.state.cameraList;
                }
            },
            resolutionList:{
                get:function(){
                    return this.$store.state.resolutionList;
                }
            },
            resolution:{
                get: function(){
                    return this.$store.state.resolution;
                },
                set: function(value){
                    this.$store.commit('resolution',value);
                }
            },
            FOV:{
                get: function(){
                    return this.$store.state.FOV;
                },
                set: function(value){
                    this.$store.commit('FOV',value);
                }
            }
        }
    }
</script>

<style scoped>
.title{
    text-align:left;
    color: aliceblue
}
.spacing{
        margin-top: 10px;
}
.buttonClass{
    display: flex;
    text-align: left;
}
</style>