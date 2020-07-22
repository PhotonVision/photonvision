<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="12 - (inputCols || 8)">
        <span>{{ name }}</span>
      </v-col>
      <v-col :cols="inputCols || 8">
        <v-text-field
          v-model="localValue"
          dark
          dense
          :disabled="disabled"
          :error-messages="errorMessage"
          @keydown="handleKeyboard"
        />
      </v-col>
    </v-row>
  </div>
</template>
s
<script>
    export default {
        name: 'Input',
      // eslint-disable-next-line vue/require-prop-types
        props: ['name', 'value', 'disabled', 'errorMessage', 'inputCols'],
        data() {
            return {}
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
        },
        methods: {
            handleKeyboard(event) {
                if (event.key === "Enter") {
                    this.$emit("Enter");
                }
            }
        }
    }
</script>

<style lang="" scoped>

</style>