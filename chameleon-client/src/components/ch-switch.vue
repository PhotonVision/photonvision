<template>
    <Row type="flex" justify="start" align="middle" :gutter="1" >
        <Col span="4">
            <h4>{{title.charAt(0).toUpperCase() + title.slice(1)}} :</h4>
        </Col>
        <Col span="4" style="text-align: left">
        <i-switch v-model="value" @on-change="handleInput" style="align-self: flex-start"></i-switch>
       </Col>
    </Row>
</template>

<script>
    export default {
        name: 'ch-switch',
        props:{
            title: String,
            Xkey: String
        },
        methods:{
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
        },
        data() {
            return {
                
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>