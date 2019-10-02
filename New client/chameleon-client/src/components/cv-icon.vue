<template>
    <div>
        <v-tooltip :right="right" :bottom="!right" nudge-right="10">
            <template v-slot:activator="{ on }">
                <v-icon @click="handleClick"  v-on="on" :color="color">{{text}}</v-icon>
            </template>
            <span>{{tooltip}}</span>
        </v-tooltip>
    </div>
</template>

<script>
    export default {
        name: 'Icon',
        props:['color','tooltip','text','right'],
        data() {
            return {
                
            }
        },
        methods:{
            handleClick(){
                this.$emit('click');
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>