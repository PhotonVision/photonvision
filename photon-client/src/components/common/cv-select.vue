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
        <v-select
          v-model="localValue"
          :items="indexList"
          item-text="name"
          item-value="index"
          dark
          color="#fcf6de"
          item-color="blue"
          :disabled="disabled"
          @change="$emit('rollback', localValue)"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script>
    export default {
        name: 'Select',
        props: ['list', 'name', 'value', 'disabled'],
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
            },
            indexList() {
                let list = [];
                for (let i = 0; i < this.list.length; i++) {
                    list.push({
                        name: this.list[i],
                        index: i
                    });
                }
                return list;
            }
        }
    }
</script>

<style>
</style>