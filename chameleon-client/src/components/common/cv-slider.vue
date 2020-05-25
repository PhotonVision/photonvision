<template>
    <div>
        <v-row dense align="center">
            <v-col :cols="2">
                <span>{{name}}</span>
            </v-col>
            <v-col :cols="10">
                <v-slider :value="localValue" @start="isClicked = true" @end="isClicked = false" @change="handleclick"
                          @input="handleInput" dark class="align-center" :max="max" :min="min" hide-details
                          color="#4baf62" :step="step">
                    <template v-slot:append>
                        <v-text-field dark :max="max" :min="min" :value="localValue" @input="handleChange"
                                      @focus="isFocused = true" @blur="isFocused = false" class="mt-0 pt-0" hide-details
                                      single-line type="number" style="width: 50px" :step="step"/>
                    </template>
                </v-slider>
            </v-col>
        </v-row>
    </div>
</template>

<script>
    export default {
        name: 'Slider',
        props: ['min', 'max', 'name', 'value', 'step'],
        data() {
            return {
                isFocused: false,
                isClicked: false
            }
        },
        methods: {
            handleChange(val) {
                if (this.isFocused) {
                    this.localValue = parseFloat(val);
                }
            },
            handleInput(val) {
                if (!this.isFocused && this.isClicked) {
                    this.localValue = val;
                }
            },
            handleclick(val) {
                if (!this.isFocused) {
                    this.localValue = val;
                }
            }
        },
        computed: {
            localValue: {
                get() {
                    return this.value;
                },
                set(value) {
                    this.$emit('input', value)
                }
            }
        }
    }
</script>

<style lang="" scoped>

</style>