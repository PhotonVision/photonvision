<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { useTheme } from "vuetify";

const theme = useTheme();
</script>

<template>
  <v-snackbar
    v-model="useStateStore().snackbarData.show"
    location="top"
    :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'tonal'"
    :color="useStateStore().snackbarData.color"
    :timeout="useStateStore().snackbarData.timeout"
  >
    <p style="padding: 0; margin: 0; text-align: center">
      {{ useStateStore().snackbarData.message }}
    </p>
  </v-snackbar>
</template>
