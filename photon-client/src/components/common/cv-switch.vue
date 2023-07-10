<script setup lang="ts">
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";
import { computed } from "vue";

const props = withDefaults(defineProps<{
  label?: string,
  tooltip?: string,
  // TODO fully update v-model usage in custom components on Vue3 update
  value: boolean,
  disabled?: boolean,
  labelCols?: number,
}>(), {
  disabled: false,
  labelCols: 2
});

const emit = defineEmits<{
  (e: "input", value: boolean): void
}>();

const localValue = computed({
  get: () => props.value,
  set: v => emit("input", v)
});
</script>

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
