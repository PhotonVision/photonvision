<template>
    <div id="SystemTab">
        <chInputNumber title="Enter Team Number" Xkey="teamValue"></chInputNumber>
        <Divider class="divdiv" orientation="left">Networking</Divider>
        <div>
            <RadioGroup v-model="lan" style="display: flex;">
                <Radio label="DHCP"></Radio>
                <Radio label="Static"></Radio>
            </RadioGroup>
<!-- 
                    ip 
                    subnetmask
                    gateway 

                    hostname -->
        </div>
    </div>
</template>

<script>
    import chInputNumber from './ch-inputNumber.vue'
    export default {
        name: 'SystemTab',
        data() {
            return {
                lan:0
            }
        },
        components:{
            chInputNumber
        },
        methods: {
        }
    }
</script>

<style>

    .ivu-divider-inner-text{
        color: aliceblue
    }
    .ivu-radio-group {
        display: flex;
        text-align: left;
        color: aliceblue;
    }
</style>