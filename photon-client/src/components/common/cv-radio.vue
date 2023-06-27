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
        <v-radio-group
          v-model="localValue"
          row
          dark
          :mandatory="true"
        >
          <v-radio
            v-for="(radioName,index) in list"
            :key="index"
            color="#ffd843"
            :label="radioName"
            :value="index"
            :disabled="disabled"
          />
        </v-radio-group>
      </v-col>
    </v-row>
  </div>
</template>

<script>
    import TooltippedLabel from "./cv-tooltipped-label";

    export default {
        name: 'Radio',
        components: {
          TooltippedLabel
        },
      // eslint-disable-next-line vue/require-prop-types
        props: ['name', 'value', 'list', 'disabled', 'inputCols', 'tooltip'],
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
        }
    }
</script>

<style lang="" scoped>

</style>
