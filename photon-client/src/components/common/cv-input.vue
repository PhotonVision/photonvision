<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="3">
        <span>{{ name }}</span>
      </v-col>
      <v-col :cols="9">
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
        props: ['name', 'value', 'disabled', 'errorMessage'],
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