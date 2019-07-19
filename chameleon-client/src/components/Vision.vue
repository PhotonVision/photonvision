<template>
    <Layout id="main-layout">
        <Header id="main-header">
            <Row type="flex" justify="start" align="middle" :gutter="10">
                <Col span="12">
                    <chselect title="camera" :list="cameraList" Xkey="curr_camera"></chselect>
                </Col>
                <Col span="12">
                    <chselect title="pipline" :list="pipelineList" Xkey="curr_pipeline"></chselect>
                </Col>
            </Row>
        </Header>
        <Content id="main-content">
            <row type="flex" justify="start" align="top" :gutter="5" >
                <Col span="12">
                    <router-view></router-view>
                    </Col>
                    <Col span="12">
                    <Tabs :animated="false"  v-model="isBinary" @on-click="handleImage">
                    <TabPane label="Normal"></TabPane>    
                    <TabPane label="Threshold"></TabPane>
                    </Tabs>
                    <img class="imageSize" :src="steamAdress" style="">
                    </Col>
                </Col>
            </row>
        </Content>
    </Layout>
</template>

<script>
    import Vue from "vue"
    import chselect from './ch-select.vue'

    export default {
        name: 'Vision',
        components: {
            chselect
        },
        data() {
            return {
            }
        },
        methods: {
            handleImage() {
                this.$socket.sendObj({"is_binary":this.isBinary});
            }
        },
        computed: {
            cameraList:{
                get:function(){
                    return this.$store.state.cameraList;
                }
            },
            pipelineList:{
                get: function(){
                    return this.$store.state.pipelineList;
                }
            },
            steamAdress: {
                get: function(){
                    return this.$store.state.streamAdress;
                }
            },
            isBinary: {
                get: function(){
                    return this.$store.state.is_binary;
                },
                set: function(value){
                    this.$store.commit('is_binary',value)
                }
            }
        },
    }
</script>
<style>
.ivu-tabs-nav .ivu-tabs-tab:hover{
    color: #0cdfc3 !important;
}
.imageSize{
    width: 75%;
    height: 75%;
}
</style>