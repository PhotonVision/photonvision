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
        color="#4baf62"
        :label="name"
        :value="index"
      />
    </v-radio-group>
  </div>
</template>

<script>
    export default {
        name: 'Radio',
        props: ['value', 'list'],
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