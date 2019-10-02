<template>
    <div>
        <v-row align="center">
            <v-col :cols="2">
                <span>{{name}}</span>
            </v-col>
            <v-col>
                 <v-text-field dark v-model="localValue" class="mt-0 pt-0" hide-details single-line :disabled="disabled"></v-text-field>
            </v-col>
        </v-row>
    </div>
</template>

<script>
    export default {
        name: 'Input',
        props:['name','value','disabled'],
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