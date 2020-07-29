<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col cols="2">
        <v-tooltip :disabled="tooltip === undefined" right>
          <template v-slot:activator="{ on, attrs }">
            <span style="cursor: text !important;" v-bind="attrs" v-on="on">{{ name }}</span>
          </template>
          <span>{{ tooltip }}</span>
        </v-tooltip>
      </v-col>
      <v-col cols="10">
        <v-range-slider
          :value="localValue"
          :max="max"
          :min="min"
          hide-details
          class="align-center"
          dark
          color="accent"
          :step="step"
          @input="handleInput"
          @mousedown="$emit('rollback', localValue)"
        >
          <template v-slot:prepend>
            <v-text-field
              dark
              color="accent"
              :value="localValue[0]"
              :max="max"
              :min="min"
              class="mt-0 pt-0"
              hide-details
              single-line
              type="number"
              style="width: 50px"
              :step="step"
              @input="handleChange"
              @focus="prependFocused = true"
              @blur="prependFocused = false"
            />
          </template>

          <template v-slot:append>
            <v-text-field
              dark
              color="accent"
              :value="localValue[1]"
              :max="max"
              :min="min"
              class="mt-0 pt-0"
              hide-details
              single-line
              type="number"
              style="width: 50px"
              :step="step"
              @input="handleChange"
              @focus="appendFocused = true"
              @blur="appendFocused = false"
            />
          </template>
        </v-range-slider>
      </v-col>
    </v-row>
  </div>
</template>

<script>
export default {
  name: "RangeSlider",
  // eslint-disable-next-line vue/require-prop-types
  props: ["name", "min", "max", "value", "step", "tooltip"],
  data() {
    return {
      prependFocused: false,
      appendFocused: false,
      currentTempVal: null,
    };
  },
  computed: {
    localValue: {
      get() {
        return Object.values(this.value);
      },
      set(value) {
        this.$emit("input", value);
      },
    },
  },
  methods: {
    delay(ms) {
      return new Promise((resolve) => setTimeout(resolve, ms));
    },

    async handleChange(val) {
      this.currentTempVal = val;

      await this.delay(200).then(() => {
        let i = 0;
        if (this.prependFocused === false && this.appendFocused === true) {
          i = 1;
        }

        // will get empty string if entry is not a number
        if (this.currentTempVal !== val || val === "") return;

        let parsed = parseFloat(val);
        let tmp = this.localValue;
        tmp[i] = Math.max(this.min, Math.min(parsed, this.max));
        this.localValue = tmp;

        this.$emit("rollback", this.localValue);
      });
    },
    handleInput(val) {
      if (!this.prependFocused || !this.appendFocused) {
        this.localValue = val;
      }
    },
  },
};
</script>

<style lang="" scoped>
</style>