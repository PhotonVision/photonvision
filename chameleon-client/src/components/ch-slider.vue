
<template>
        <row type="flex" justify="start" align="middle" :gutter="1">
            <Col span="4">
                <h4>{{title.charAt(0).toUpperCase() + title.slice(1)}} :</h4>
            </Col>
            <Col span="4" style="text-align: left">
                <InputNumber style="align-self: flex-start;" v-model="value" size="small"></InputNumber>
            </Col>
            <Col span="14">
                <Slider v-model="value" @on-input="handleInput"></Slider>
            </Col>
        </row>
</template>

<script>
    export default {
        name: 'ch-slider',
        props:{
            title:String,
            Xkey:String
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
                get: function(){
                    return this.$store.state[this.Xkey];
                },
                set: function(value){
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

