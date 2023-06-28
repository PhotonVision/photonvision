<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="12 - inputCols">
        <tooltipped-label
          :tooltip="tooltip"
          :label="label"
        />
      </v-col>
      <v-col :cols="inputCols">
        <v-radio-group
          v-model="localValue"
          row
          dark
          :mandatory="true"
        >
          <v-radio
            v-for="(radioName, index) in list"
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

<script lang="ts">

import {computed, defineComponent} from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

export default defineComponent({
  emits: ["input"],
  components: {TooltippedLabel},
  props: {
    inputCols: {type: Number, required: false, default: 8},
    tooltip: {type: String, required: false},
    label: {type: String, required: false},
    disabled: {type: Boolean, required: false},
    list: {type: Array, required: true},
    value: {type: Number, required: true}
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
});
</script>
