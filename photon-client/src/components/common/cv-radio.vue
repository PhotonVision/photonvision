<template>
  <div>
    <v-radio-group
      v-model="localValue"
      row
      dark
      :mandatory="true"
    >
      <v-radio
        v-for="(name,index) in list"
        :key="index"
        color="#ffd843"
        :label="name"
        :value="index"
        :disabled="disabled"
      />
    </v-radio-group>
  </div>
</template>

<script>
    export default {
        name: 'Radio',
      // eslint-disable-next-line vue/require-prop-types
        props: ['value', 'list', 'disabled'],
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