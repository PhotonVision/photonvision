<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
</script>

<template>
  <v-snackbar
    v-model="useStateStore().snackbarData.show"
    top
    :color="useStateStore().snackbarData.color"
    :timeout="useStateStore().snackbarData.timeout"
  >
    <p style="padding: 0; margin: 0; text-align: center">
      {{ useStateStore().snackbarData.message }}
    </p>
  </v-snackbar>
</template>
