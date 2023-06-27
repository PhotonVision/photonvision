<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="textCols || 2">
        <tooltipped-label
          :tooltip="tooltip"
          :text="name"
        />
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
import TooltippedLabel from "./cv-tooltipped-label";

export default {
    name: 'CVSwitch',
    components: {
        TooltippedLabel,
    },
  // eslint-disable-next-line vue/require-prop-types
    props: ['name', 'value', 'disabled', 'textCols', 'tooltip'],
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
