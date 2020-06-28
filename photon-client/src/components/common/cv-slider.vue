<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="2">
        <span>{{ name }}</span>
      </v-col>
      <v-col :cols="10">
        <v-slider
          :value="localValue"
          dark
          class="align-center"
          :max="max"
          :min="min"
          hide-details
          color="#ffd843"
          :step="step"
          @start="isClicked = true"
          @end="isClicked = false"
          @change="handleclick"
          @input="handleInput"
          @mousedown="$emit('rollback', localValue)"
        >
          <template v-slot:append>
            <v-text-field
              dark
              :max="max"
              :min="min"
              :value="localValue"
              class="mt-0 pt-0"
              hide-details
              single-line
              type="number"
              style="width: 50px"
              :step="step"
              @input="handleChange"
              @focus="isFocused = true"
              @blur="isFocused = false"
            />
          </template>
        </v-slider>
      </v-col>
    </v-row>
  </div>
</template>

<script>
    export default {
        name: 'Slider',
        props: ['min', 'max', 'name', 'value', 'step'],
        data() {
            return {
                isFocused: false,
                isClicked: false
            }
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
        },
        methods: {
            handleChange(val) {
                if (this.isFocused) {
                    this.localValue = parseFloat(val);
                    this.$emit('rollback', this.localValue)
                }
            },
            handleInput(val) {
                if (!this.isFocused && this.isClicked) {
                    this.localValue = val;
                }
            },
            handleclick(val) {
                if (!this.isFocused) {
                    this.localValue = val;
                }
            }
        }
    }
</script>

<style lang="" scoped>

</style>