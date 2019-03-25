<template>
    <Layout id="main-layout">
        <Content id="main-content">
            <row type="flex" justify="start" align="middle" :gutter="5" >
                <Col span="12">
                    <router-view></router-view>
                </Col>
            </row>
        </Content>
    </Layout>
</template>

<script>
export default {
    name: 'Settings',
    data() {
        return {

        }
    },
    methods: {}
}
</script>

<style scoped>

</style>