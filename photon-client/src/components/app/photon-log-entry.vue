<script setup lang="ts">
import { LogLevel, type LogMessage } from "@/types/SettingTypes";
import { computed } from "vue";

const props = defineProps<{ source: LogMessage }>();

const logColorClass = computed<string>(() => {
  switch (props.source.level) {
    case LogLevel.ERROR:
      return "text-red";
    case LogLevel.WARN:
      return "text-yellow";
    case LogLevel.INFO:
      return "text-light-blue";
    case LogLevel.DEBUG:
      return "text-pv-on-surface";
  }
  return "";
});
</script>

<template>
  <div class="whitespace-pre-wrap" :class="logColorClass">[{{ source.timestamp.toTimeString().split(" ")[0] }}] {{ source.message }}</div>
</template>
