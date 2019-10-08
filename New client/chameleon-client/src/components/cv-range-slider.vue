<template>
    <div>
        <v-row align="center">
            <v-col :cols="2">
                <span>{{name}}</span>
            </v-col>
            <v-col :cols="10">
                <v-range-slider v-model="localValue" :max="max" :min="min" hide-details class="align-center" dark color="#4baf62" :step="step">
                    <template v-slot:prepend> 
                        <v-text-field v-model="localValue[0]" class="mt-0 pt-0" hide-details single-line type="number" style="width: 50px" :step="step"></v-text-field>
                    </template>

                    <template v-slot:append>
                    <v-text-field v-model="localValue[1]" class="mt-0 pt-0" hide-details single-line type="number" style="width: 50px" :step="step"></v-text-field>
                    </template>
                </v-range-slider>
            </v-col>
        </v-row>
    </div>
</template>

<script>
    export default {
        name: 'RangeSlider',
        props:['name','min','max','value','step'],
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