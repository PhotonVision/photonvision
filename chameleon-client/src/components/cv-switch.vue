<template>
    <div>
        <v-row dense align="center">
            <v-col :cols="2">
                <span>{{name}}</span>
            </v-col>
            <v-col>
                <v-switch v-model="localValue" color="#4baf62"></v-switch>
            </v-col>
        </v-row>
    </div>
</template>

<script>
    export default {
        name: 'CVSwitch',
        props:['name','value'],
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
                    this.$emit('input',value)
                }
            }
        }
    }
</script>

<style lang="" scoped>
    
</style>