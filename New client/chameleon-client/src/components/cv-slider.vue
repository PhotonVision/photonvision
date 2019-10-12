<template>
    <div>
        <v-row align="center">
            <v-col :cols="2">
                <span>{{name}}</span>
            </v-col>
            <v-col :cols="10">
                <v-slider :value="localValue" @input="handleInput" dark class="align-center" :max="max" :min="min" hide-details color="#4baf62" :step="step">
            <template v-slot:append>
              <v-text-field dark :max="max" :min="min" :value="localValue" @input="handleChange"  @focus="isFocused = true" @blur="isFocused = false" class="mt-0 pt-0" hide-details single-line type="number" style="width: 50px" :step="step"></v-text-field>
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
                isFocused:false
            }
        },
        methods:{
            handleChange(val){
                if(this.isFocused){
                    this.localValue = parseFloat(val);
                }
            },
            handleInput(val){
                if(!this.isFocused){
                this.localValue = val;
                }
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