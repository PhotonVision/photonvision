<template>
    <div>
        <v-radio-group row v-model="localValue" dark :mandatory="true">
            <v-radio color="#4baf62" v-for="(name,index) in list" :label="name" v-bind:key="index" :value="index"></v-radio>
        </v-radio-group>
    </div>
</template>

<script>
    export default {
        name: 'Radio',
        props:['value','list'],
        data() {
            return {
                
            }
        },
        computed:{
            localValue:{
                get(){
                    return this.value;
                },
                set(value){
                    this.$emit('input',value);
                }
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>