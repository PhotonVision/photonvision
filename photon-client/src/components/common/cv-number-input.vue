<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="labelCols || 2">
        <v-tooltip :disabled="tooltip === undefined" right>
          <template v-slot:activator="{ on, attrs }">
            <span style="cursor: text !important;" v-bind="attrs" v-on="on">{{ name }}</span>
          </template>
          <span>{{ tooltip }}</span>
        </v-tooltip>
      </v-col>
      <v-col>
        <v-text-field
          v-model="localValue"
          dark
          class="mt-0 pt-0"
          hide-details
          single-line
          color="accent"
          type="number"
          style="width: 70px"
          :step="step"
          :rules="rules"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script>
    export default {
        name: 'NumberInput',
      // eslint-disable-next-line vue/require-prop-types
        props: ['name', 'value', 'step', 'labelCols', 'rules', 'tooltip'],
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