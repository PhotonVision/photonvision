<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="labelCols || 2">
        <tooltipped-label
          :tooltip="tooltip"
          :text="name"
        />
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
          :disabled="disabled"
          :rules="rules"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script>
    import TooltippedLabel from "./cv-tooltipped-label";

    export default {
        name: 'NumberInput',
        components: {
            TooltippedLabel,
        },
      // eslint-disable-next-line vue/require-prop-types
        props: ['name', 'value', 'step', 'labelCols', 'rules', 'tooltip', 'disabled'],
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
