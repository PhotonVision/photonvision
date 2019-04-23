<template>
    <div id="InputNumber">
    <row type="flex" justify="start" align="middle" :gutter="10" >
        <Col span="6">
            <h4>{{title}}</h4>
        </Col>
        <col span="4">
        <InputNumber :min="0" v-model="value" size="small" @on-change="handleInput"></InputNumber>
        </col>
    </row>
    </div>
</template>

<script>
    export default {
        name: 'ch-InputNumber',
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

<style lang="" scoped>
    
</style>