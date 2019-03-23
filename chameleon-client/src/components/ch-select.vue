<template>
            <row type="flex" justify="start" align="middle" :gutter="10" >
                <Col span="3">
                    <h4>{{title}}</h4>
             
                </Col>
                <col span="12">
                    <i-select v-model="value" size="small" style="width:400px" v-bind:placeholder="placeholdert" @on-change="handleInput">
                        <i-option v-for="item in list" :value="item" :key="item">{{item}}</i-option>
                    </i-select>
                </col>
        </row>
</template>

<script>
    export default {
        name: 'ch-select',
        props:{
            title:"",
            placeholdert:"",
            list:[]
        },
        data() {
            return {
                content:this.value
            }
        },
        methods: {
            handleInput() {
                this.$emit('input',this.value);
            }
        }
    }
</script>

<style>
h4 {
     color: #e6ebf1;
 }
 .ivu-select-selection{
     background-color: #2c3e50 !important;
 }
 .ivu-select-selected-value{
     color: #fff !important;
 }

</style>