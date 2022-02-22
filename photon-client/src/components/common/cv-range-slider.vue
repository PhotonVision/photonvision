<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col cols="2">
        <tooltipped-label
          :tooltip="tooltip"
          :text="name"
        />
      </v-col>
      <v-col cols="10">
        <v-range-slider
          :value="localValue"
          :max="max"
          :min="min"
          :disabled="disabled"
          hide-details
          class="align-center"
          dark
          :color="inverted ? 'rgba(255, 255, 255, 0.2)' : 'accent'"
          :track-color="inverted ? 'accent' : undefined"
          thumb-color="accent"
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
              style="width: 60px"
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
              style="width: 60px"
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
import TooltippedLabel from "./cv-tooltipped-label";

export default {
  name: "RangeSlider",
  components: {
    TooltippedLabel,
  },
  // eslint-disable-next-line vue/require-prop-types
  props: ["name", "min", "max", "value", "step", "tooltip", "disabled", "inverted"],
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
        return Object.values(this.value || [0, 0]);
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
