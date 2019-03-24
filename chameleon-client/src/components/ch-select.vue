<template>
    <Row type="flex" justify="start" align="middle" :gutter="1" >
        <Col span="4">
            <h4>{{title.charAt(0).toUpperCase() + title.slice(1)}} :</h4>
        </Col>
        <Col span="4">
            <i-select v-model="value" size="small" v-bind:placeholder="list[0]" @on-change="handleInput">
                <i-option v-for="item in list" :value="item" :key="item">{{item}}</i-option>
            </i-select>
        </Col>
    </Row>
</template>

<script>
    export default {
        name: 'ch-select',
        props:[
            'title',
            'list'
        ],
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

<style>
h4 {
     color: #e6ebf1;
      text-align: left;
 }
 /* .ivu-select-selection{
     background-color: #2c3e50 !important;
 } */
 /* .ivu-select-selected-value{
     color: #fff !important;
 } */

</style>