<template>
    <div>
        <v-row align="center">
            <v-col :cols="2">
                <span>{{name}}</span>
            </v-col>
            <v-col :cols="10">
                <v-range-slider :value="localValue" @input="handleInput" :max="max" :min="min" hide-details class="align-center" dark color="#4baf62" :step="step">
                    <template v-slot:prepend> 
                        <v-text-field :value="localValue[0]" :max="max" :min="min" @input="handleChange" @focus="prependFocused = true" @blur="prependFocused = false" class="mt-0 pt-0" hide-details single-line type="number" style="width: 50px" :step="step"></v-text-field>
                    </template>

                    <template v-slot:append>
                        <v-text-field :value="localValue[1]" :max="max" :min="min" @input="handleChange" @focus="appendFocused = true" @blur="appendFocused = false" class="mt-0 pt-0" hide-details single-line type="number" style="width: 50px" :step="step"></v-text-field>
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
                prependFocused:false,
                appendFocused:false
                
            }
        },
        methods:{
            handleChange(val){
                let i = 0;
                if(this.prependFocused === false && this.appendFocused === true){
                    i = 1;
                }
                if(this.prependFocused || this.appendFocused){
                    this.$set(this.localValue,i,val);
                }
            },
            handleInput(val){
                if(!this.prependFocused || !this.appendFocused){
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