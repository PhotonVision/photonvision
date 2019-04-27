<template>
    <Layout id="main-layout">
        <Header id="main-header">
            <Row type="flex" justify="start" align="middle" :gutter="10">
                <Col span="12">
                    <chselect title="camera" :list="['1','2','3']" Xkey="camera"></chselect>
                </Col>
                <Col span="12">
                    <chselect title="pipline" :list="['0','1','2','3','4','5','6','7','8','9']" Xkey="pipeline"></chselect>
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
                    <img :src="steamAdress" style="">
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
            steamAdress: {
                get: function(){
                    return this.$store.state.streamAdress;
                }
            },
            isBinary: {
                get: function(){
                    return this.$store.state.isBinaryImage;
                },
                set: function(value){
                    this.$store.commit('isBinaryImage',value)
                }
            }
        },
    }
</script>
<style>
.ivu-tabs-nav .ivu-tabs-tab:hover{
    color: #0cdfc3 !important;
}
</style>