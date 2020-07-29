<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="textCols || 2">
        <v-tooltip :disabled="tooltip === undefined" right>
          <template v-slot:activator="{ on, attrs }">
            <span style="cursor: text !important;" v-bind="attrs" v-on="on">{{ name }}</span>
          </template>
          <span>{{ tooltip }}</span>
        </v-tooltip>
      </v-col>
      <v-col :cols="12 - (textCols || 2)">
        <v-switch
          v-model="localValue"
          dark
          :disabled="disabled"
          color="#ffd843"
          @change="$emit('rollback', localValue)"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script>
    export default {
        name: 'CVSwitch',
      // eslint-disable-next-line vue/require-prop-types
        props: ['name', 'value', 'disabled', 'textCols', 'tooltip'],
        data() {
            return {}
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