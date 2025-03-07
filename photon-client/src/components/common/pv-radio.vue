<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    // TODO fully update v-model usage in custom components on Vue3 update
    value: number;
    disabled?: boolean;
    inputCols?: number;
    list: string[];
  }>(),
  {
    disabled: false,
    inputCols: 8
  }
);

const emit = defineEmits<{
  (e: "input", value: number): void;
}>();

const localValue = computed({
  get: () => props.value,
  set: (v) => emit("input", v)
});
</script>

<template>
  <div>
    <v-row dense align="center">
      <v-col :cols="12 - inputCols">
        <tooltipped-label :tooltip="tooltip" :label="label" />
      </v-col>
      <v-col :cols="inputCols">
        <v-radio-group v-model="localValue" row dark :mandatory="true">
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
