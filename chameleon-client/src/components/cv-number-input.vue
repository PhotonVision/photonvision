<template>
    <div>
        <v-row dense align="center">
            <v-col :cols="2">
                <span>{{name}}</span>
            </v-col>
            <v-col>
                <v-text-field dark v-model="localValue" class="mt-0 pt-0" hide-details single-line type="number"
                              style="width: 70px" :step="step"/>
            </v-col>
        </v-row>
    </div>
</template>

<script>
    export default {
        name: 'NumberInput',
        props: ['name', 'value', 'step'],
        data() {
            return {}
        },
        computed: {
            localValue: {
                get() {
                    return this.value;
                },
                set(value) {
                    this.$emit('input', parseFloat(value));
                }
            }
        }
    }
</script>

<style lang="" scoped>

</style>