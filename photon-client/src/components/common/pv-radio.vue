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
  <div class="d-flex">
    <v-col :cols="12 - inputCols" class="d-flex align-center pl-0">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>
    <v-col :cols="inputCols" class="d-flex align-center pr-0">
      <v-radio-group v-model="localValue" row dark :mandatory="true" hide-details="auto">
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
  </div>
</template>
<style scoped>
.v-input--radio-group {
  padding-top: 0;
  margin-top: 0;
}
</style>
