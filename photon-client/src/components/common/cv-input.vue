<script setup lang="ts">
import { computed } from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

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
      if (!(props.rules || []).some((v) => v(localValue.value) === false || typeof v(localValue.value) === "string")) {
        emit("onEnter", localValue.value);
      }
      break;
    case "Escape":
      emit("onEscape");
      break;
  }
};
</script>

<template>
  <div>
    <v-row dense align="center">
      <v-col :cols="labelCols || 12 - inputCols">
        <tooltipped-label :tooltip="tooltip" :label="label" />
      </v-col>

      <v-col :cols="inputCols">
        <v-text-field
          v-model="localValue"
          dark
          dense
          color="accent"
          :placeholder="placeholder"
          :disabled="disabled"
          :error-messages="errorMessage"
          :rules="rules"
          class="mt-1 pt-2"
          @keydown="handleKeydown"
        />
      </v-col>
    </v-row>
  </div>
</template>
