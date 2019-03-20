<template>
    <div id="SystemTab">
        <chInputNumber title="Enter Team Number"></chInputNumber>
    </div>
</template>

<script>
    import chInputNumber from './ch-inputNumber.vue'
    export default {
        name: 'SystemTab',
        data() {
            return {
                value
            }
        },
        components:{
            chInputNumber
        }
    }
</script>

<style lang="" scoped>
    
</style>