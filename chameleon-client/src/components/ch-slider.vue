
<template>
            <row type="flex" justify="start" align="middle" :gutter="10" >
                <Col span="3">
                    <h4>{{title}}</h4>
                </Col>
                <col span="2">
                    <InputNumber v-model="value" size="small"></InputNumber>
                </col>
                <Col span="14">
                    <Slider v-model="value"></Slider>
                </col>
        </row>
</template>

<script>
    export default {
        name: 'ch-slider',
        props:['title'],
        data() {
            return {
                value :0
            }
        }
    }
</script>

<style scoped>
h4 {
     color: #e6ebf1;
 }
</style>