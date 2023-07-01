<script setup lang="ts">
import {useStateStore} from "@/stores/StateStore";
const stateStore = useStateStore();
</script>

<template>
  <v-snackbar
      v-model="stateStore.snackbarData.show"
      top
      :color="stateStore.snackbarData.color"
      :timeout="stateStore.snackbarData.timeout"
  >
    <p style="padding: 0; margin: 0; text-align: center">
      {{ stateStore.snackbarData.message }}
    </p>
  </v-snackbar>
</template>
