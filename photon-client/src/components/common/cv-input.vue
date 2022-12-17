<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="12 - (inputCols || 8)">
        <tooltipped-label
          :tooltip="tooltip"
          :text="name"
        />
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

<script>
    import TooltippedLabel from "./cv-tooltipped-label";

    export default {
        name: 'Input',
        components: {
          TooltippedLabel
        },
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
