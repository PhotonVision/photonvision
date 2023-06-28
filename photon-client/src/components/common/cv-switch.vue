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
      <v-col :cols="12 - labelCols">
        <v-switch
          v-model="localValue"
          dark
          :disabled="disabled"
          color="#ffd843"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script lang="ts">
  import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";
  import {computed} from "vue";

  export default {
    emits: ["input"],
    components: {TooltippedLabel},
    props: {
      value: {type: Boolean, required: true},
      label: {type: String, required: false},
      tooltip: {type: String, required: false},
      labelCols: {type: Number, required: false, default: 2},
      disabled: {type: Boolean, required: false}
    },
    setup(props: {value: boolean}, {emit}) {
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
