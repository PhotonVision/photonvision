<script setup lang="ts">
import {computed, defineEmits, defineProps} from "vue";
import TooltippedLabel from "@/components/common/cv-tooltipped-label.vue";

const props = withDefaults(defineProps<{
  label?: string,
  tooltip?: string,
  selectCols?: number,
  value: number,
  disabled?: boolean,
  items: string[]
}>(), {
  selectCols: 9,
  disabled: false
});

const emit = defineEmits(["input"]);

const localValue = computed({
  get: () => props.value,
  set: v => emit("input", v)
});

// Computed in case items changes
const indexList = computed(() => props.items.map((v: string, i: number) => ({name: v, index: i})));
</script>

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
        />
      </v-col>
    </v-row>
  </div>
</template>
