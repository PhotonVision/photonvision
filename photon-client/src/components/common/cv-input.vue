<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="labelCols || (12 - inputCols)">
        <tooltipped-label
          :tooltip="tooltip"
          :label="label"
        />
      </v-col>

      <v-col :cols="inputCols">
        <v-text-field
          v-model="localValue"
          dark
          dense
          color="accent"
          :disabled="disabled"
          :error-messages="errorMessage"
          :rules="rules"
          class="mt-1 pt-2"
          @keydown="e => e.key === 'Enter' && $emit('onEnter')"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script lang="ts">
import {computed} from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";
  export default {
    components: {TooltippedLabel},
    emits: ["input", "onEnter"],
    props: {
      label: {type: String, required: false},
      tooltip: {type: String, required: false, default: undefined},
      value: {type: String, required: true},
      disabled: {type: Boolean, required: false, default: false},
      errorMessage: {type: String, required: false, default: undefined},
      labelCols: {type: Number, required: false},
      inputCols: {type: Number, required: false, default: 8},
      rules: {type: Array, required: false}
    },
    setup(props: {value: never}, { emit }) {
      const localValue = computed({
        get: () => props.value,
        set: v => emit("input", v)
      });

      return {
        localValue
      };
    }
  };
</script>
