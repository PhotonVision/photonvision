<template>
    <div id="SystemTab">
        <chInputNumber title="Enter Team Number" v-model="TeamVal" v-on:input="onChange(TeamVal)"></chInputNumber>
    </div>
</template>

<script>
    import chInputNumber from './ch-inputNumber.vue'
    export default {
        name: 'SystemTab',
        data() {
            return {
                TeamVal
            }
        },
        components:{
            chInputNumber
        },
        methods: {
            onChange(i) {
                console.log(i);
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>