<template>
  <div>
    <v-row
      dense
      align="center"
    >
      <v-col :cols="12 - selectCols">
        <tooltipped-label
          :tooltip="tooltip"
          :label="label"
        />
      </v-col>
      <v-col :cols="selectCols">
        <v-select
          v-model="localValue"
          :items="indexList"
          item-text="name"
          item-value="index"
          dark
          color="accent"
          item-color="secondary"
          :disabled="disabled"
          :rules="rules"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script lang="ts">
import {computed} from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

export default {
  emits: ["input"],
  components: {TooltippedLabel},
  props: {
    label: {type: String, required: false},
    tooltip: {type: String, required: false},
    selectCols: {type: Number, required: false, default: 9},
    items: {type: Array, required: true},
    disabled: {type: Boolean, required: false},
    rules: {type: Array, required: false},
    value: {type: Number, required: true}
  },
  setup(props: {value: number, items: string[]}, {emit}) {

    const localValue = computed({
      get: () => props.value,
      set: v => emit("input", v)
    });

    const indexList = computed(() => props.items.map((v: string, i: number) => ({name: v, index: i})));

    return {
      localValue,
      indexList
    };
  }
};
</script>
