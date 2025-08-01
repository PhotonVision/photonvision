<script setup lang="ts">
import TooltippedLabel from "@/components/common/pv-tooltipped-label.vue";

const value = defineModel<string>({ required: true });

const props = withDefaults(
  defineProps<{
    label?: string;
    tooltip?: string;
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
  (e: "onEnter", value: string): void;
  (e: "onEscape"): void;
}>();

const handleKeydown = ({ key }) => {
  switch (key) {
    case "Enter":
      // Explicitly check that all rule props return true
      if (!props.rules?.every((rule) => rule(value.value) === true)) return;

      emit("onEnter", value.value);
      break;
    case "Escape":
      emit("onEscape");
      break;
  }
};

// TODO: fix error text theming
</script>
<template>
  <div class="d-flex">
    <v-col :cols="labelCols || 12 - inputCols" class="d-flex align-center pl-0 pt-10px pb-10px">
      <tooltipped-label :tooltip="tooltip" :label="label" />
    </v-col>

    <v-col :cols="inputCols" class="d-flex align-center pr-0 pt-10px pb-10px">
      <v-text-field
        v-model="value"
        density="compact"
        color="primary"
        :placeholder="placeholder"
        :disabled="disabled"
        :error-messages="errorMessage"
        :rules="rules"
        hide-details="auto"
        variant="underlined"
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
