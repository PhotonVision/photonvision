<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
</script>

<template>
  <v-snackbar
    v-model="useStateStore().snackbarData.show"
    location="top"
    variant="elevated"
    :color="useStateStore().snackbarData.color"
    :timeout="useStateStore().snackbarData.timeout"
  >
    <p style="padding: 0; margin: 0; text-align: center">
      {{ useStateStore().snackbarData.message }}
    </p>
    <v-progress-linear
      v-if="useStateStore().snackbarData.progressBar != -1"
      v-model="useStateStore().snackbarData.progressBar"
      height="15"
      :color="useStateStore().snackbarData.progressBarColor"
    >
      <template #default="{ value }">
        <strong> {{ Math.ceil(value) }}% </strong>
      </template>
    </v-progress-linear>
  </v-snackbar>
</template>
