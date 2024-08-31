<script setup lang="ts">
import { LogLevel, type LogMessage } from "@/types/SettingTypes";
import { computed } from "vue";

const props = defineProps<{ source: LogMessage }>();

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
  <div :class="logColorClass">[{{ source.timestamp.toTimeString().split(" ")[0] }}] {{ source.message }}</div>
</template>
