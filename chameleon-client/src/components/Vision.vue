<template>
    <Layout id="main-layout">
        <Header id="main-header">
            <Row type="flex" justify="start" align="middle" :gutter="10">
                <Col span="12">
                    <chselect title="camera" :parentData="$store.getters.camera" @input="onChange('camera',$event)" :list="[1,2,3]"></chselect>
                </Col>
                <Col span="12">
                    <chselect title="pipline" :list="[0,1,2,3,4,5,6,7,8,9]" :parentData="$store.getters.pipline" @input="onChange('pipline',$event)"></chselect>
                </Col>
            </Row>
        </Header>
        <Content id="main-content">
            <row type="flex" justify="start" align="middle" :gutter="5" >
                <Col span="12">
                    <router-view></router-view>
                </Col>
                    <img src="../assets/logo.png">
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
        methods: {}
    }
</script>

<style scoped>

</style>