<template>
    <div>
        <v-row dense align="center">
            <v-col :cols="3">
                <span>{{name}}</span>
            </v-col>
            <v-col :cols="9">
                <v-text-field @keydown="handleKeyboard" dark v-model="localValue" dense :disabled="disabled"
                              :error-messages="errorMessage"/>
            </v-col>
        </v-row>
    </div>
</template>

<script>
    export default {
        name: 'Input',
        props: ['name', 'value', 'disabled', 'errorMessage'],
        data() {
            return {}
        },
        methods: {
            handleKeyboard(event) {
                if (event.key == "Enter") {
                    this.$emit("Enter");
                }
            }
        },
        computed: {
            localValue: {
                get() {
                    return this.value;
                },
                set(value) {
                    this.$emit('input', value);
                }
            }
        }
    }
</script>

<style lang="" scoped>

</style>