<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
    // TODO fully update v-model usage in custom components on Vue3 update
    value: string;
    disabled?: boolean;
    errorMessage?: string;
    placeholder?: string;
    labelCols?: number;
    inputCols?: number;
    rules?: ((v: string) => boolean | string)[];
  }>(),
  {
    disabled: false,
    inputCols: 8
  }
);

const emit = defineEmits<{
  (e: "input", value: string): void;
  (e: "onEnter", value: string): void;
  (e: "onEscape"): void;
}>();

const localValue = computed({
  get: () => props.value,
  set: (v) => emit("input", v)
});

const handleKeydown = ({ key }) => {
  switch (key) {
    case "Enter":
      // Explicitly check that all rule props return true
      if (!props.rules?.every((rule) => rule(localValue.value) === true)) return;

      emit("onEnter", localValue.value);
      break;
    case "Escape":
      emit("onEscape");
      break;
  }
};
</script>

<template>
  <div class="d-flex">
    <v-col :cols="labelCols || 12 - inputCols" class="d-flex align-center pl-0">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>

    <v-col :cols="inputCols" class="d-flex align-center pr-0">
      <v-text-field
        v-model="localValue"
        dark
        dense
        color="accent"
        :placeholder="placeholder"
        :disabled="disabled"
        :error-messages="errorMessage"
        :rules="rules"
        hide-details="auto"
        class="light-error"
        @keydown="handleKeydown"
      />
    </v-col>
  </div>
</template>
<style scoped>
.v-text-field {
  margin-top: 0px;
}
</style>
<style>
.light-error .error--text {
  color: red !important;
  caret-color: red !important;
}
</style>
