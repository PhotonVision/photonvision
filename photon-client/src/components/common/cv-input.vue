<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="12 - (inputCols || 8)">
        <v-tooltip :disabled="tooltip === undefined" right>
          <template v-slot:activator="{ on, attrs }">
            <span style="cursor: text !important;" v-bind="attrs" v-on="on">{{ name }}</span>
          </template>
          <span>{{ tooltip }}</span>
        </v-tooltip>
      </v-col>
      <v-col :cols="inputCols || 8">
        <v-text-field
          v-model="localValue"
          dark
          dense
          color="accent"
          :disabled="disabled"
          :error-messages="errorMessage"
          :rules="rules"
          class="mt-1 pt-2"
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
        props: ['name', 'value', 'disabled', 'errorMessage', 'inputCols', 'rules', 'tooltip'],
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

<style lang="css" scoped>
</style>