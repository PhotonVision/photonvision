
<template>
            <row type="flex" justify="start" align="middle" :gutter="10" >
                <Col span="3">
                    <h4>{{title}}</h4>
                </Col>
                <col span="2">
                    <InputNumber v-model="val" size="small"></InputNumber>
                </col>
                <Col span="14">
                    <Slider style="margin-left:5px;" v-model="val" @on-input="handleInput"></Slider>
                </col>
        </row>
</template>

<script>
    export default {
        name: 'ch-slider',
        props:['title','value'],
        data() {
            return {
                val: this.value
            }
        },
        methods: {
            handleInput() {
                this.$emit('input',this.val);
            }
        }
    }
</script>

<style >
h4 {
     color: #e6ebf1;
 }
 .ivu-input-number-input{
     background-color: #2c3e50 !important;
     color: #fff !important;
 }
 
</style>

