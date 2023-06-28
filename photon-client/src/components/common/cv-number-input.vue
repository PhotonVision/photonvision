<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="labelCols">
        <tooltipped-label
          :tooltip="tooltip"
          :label="label"
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


<script lang="ts">
  import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";
  import { computed } from "vue";

  export default {
    emits: ["input"],
    components: {TooltippedLabel},
    props: {
      label: {type: String, required: false},
      tooltip: {type: String, required: false},
      value: {type: Number, required: true},
      labelCols: {type: Number, required: false, default: 2},
      disabled: {type: Boolean, required: false},
      rules: {type: Array, required: false},
      step: {type: Number, required: false, default: 1}
    },
    setup(props: {value: number}, {emit}) {
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
