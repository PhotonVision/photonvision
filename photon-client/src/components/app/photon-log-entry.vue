<script setup lang="ts">
import { LogLevel } from "@/types/SettingTypes";
import { computed } from "vue";

const props = defineProps<{ source: any }>();

const logColorClass = computed<string>(() => {
  switch (props.source.level) {
    case LogLevel.ERROR:
      return "red--text";
    case LogLevel.WARN:
      return "yellow--text";
    case LogLevel.INFO:
      return "light-blue--text";
    case LogLevel.DEBUG:
      return "white--text";
  }
  return "";
});
</script>

<template>
  <div :class="logColorClass">
    {{ source.message }}
  </div>
</template>
