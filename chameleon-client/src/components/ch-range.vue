
<template>
            <row type="flex" justify="start" align="middle" :gutter="1" >
                <Col>
                    <h4 style="width:100%">{{title}}</h4>
                </Col>
                <col >
                    <InputNumber  style="margin-left: 5px ;width:10%;"  v-model="value[0]" size="small"></InputNumber>
                </col>
                <Col span="14">
                    <Slider range style="margin-left:10px;" v-model="value" @on-input="handleInput"></Slider>
                </col>
                <col >
                    <InputNumber  style="margin-left: 5px ;width:10%;"  v-model="value[1]" size="small"></InputNumber>
                </col>
        </row>
</template>

<script>
    export default {
        name: 'ch-slider',
        props:['title','value'],
        data() {
            return {
                content:this.value
            }
        },
        methods: {
            handleInput() {
                this.$emit('input',{[this.title]:this.value});
            }
        }
    }
</script>

<style >
h4 {
     color: #e6ebf1;
 }
 /* .ivu-input-number-input{
     background-color: #2c3e50 !important;
     color: #fff !important;
 } */
</style>

