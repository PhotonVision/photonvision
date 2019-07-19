
<template>
        <row type="flex" justify="start" align="middle" :gutter="1">
            <Col span="4">
                <h4>{{title.charAt(0).toUpperCase() + title.slice(1)}} :</h4>
            </Col>
            <Col span="4" style="text-align: left">
                <InputNumber style="align-self: flex-start;" v-model="value[0]" size="small" :step="steps" ></InputNumber>
            </Col>
            <Col span="10">
                <Slider range v-model="value" @on-input="handleInput" :step="steps" :max="maximum"></Slider>
            </Col>
            <Col span="4" style="text-align: right">
                <InputNumber style="align-self: flex-end;" v-model="value[1]" size="small"  :step="steps" :max="maximum"></InputNumber>
            </Col>
        </row>
</template>

<script>
    export default {
        name: 'ch-range',
        props:{
            title:String,
            Xkey:String,
            steps:Number,
            maximum:Number
        },
        data() {
            return {
            }
        },
        methods: {
            handleInput() {
                this.$socket.sendObj({[this.Xkey]:this.value});
            }
        },
        computed:{
            value:{
                get:function(){
                    return this.$store.state[this.Xkey];
                },
                set:function(value){
                    this.$store.commit(this.Xkey,value);
                }
            }
        }
    }
</script>

<style>
h4 {
     color: #e6ebf1;
 }
 /* .ivu-input-number-input{
     background-color: #2c3e50 !important;
     color: #fff !important;
 } */
</style>

