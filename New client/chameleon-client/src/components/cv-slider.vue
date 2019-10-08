<template>
    <div>
        <v-row align="center">
            <v-col :cols="2">
                <span>{{name}}</span>
            </v-col>
            <v-col :cols="10">
            <v-slider v-model="localValue" dark class="align-center" :max="max" :min="min" hide-details color="#4baf62" :step="step">
            <template v-slot:append>
              <v-text-field dark v-model="localValue" class="mt-0 pt-0" hide-details single-line type="number" style="width: 50px" :step="step"></v-text-field>
            </template>
          </v-slider>
            </v-col>
        </v-row>
    </div>
</template>

<script>
    export default {
        name: 'Slider',
        props:['min','max','name','value','step'],
        data() {
            return {
                
            }
        },
        methods:{
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