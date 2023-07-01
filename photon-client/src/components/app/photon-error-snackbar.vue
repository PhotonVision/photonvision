<script setup lang="ts">
import {useStateStore} from "@/stores/StateStore";
import {computed} from "vue";

const showSnackbar = computed<boolean>(() => useStateStore().snackbarData.show);
const snackbarMessage = computed<string>(() => useStateStore().snackbarData.message);
const snackbarColor = computed<string>(() => useStateStore().snackbarData.color);
const snackbarTimeout = computed<number>(() => useStateStore().snackbarData.timeout);
</script>

<template>
  <v-snackbar
      v-model="showSnackbar"
      top
      :color="snackbarColor"
      :timeout="snackbarTimeout"
  >
    <span>{{ snackbarMessage }}</span>
  </v-snackbar>
</template>

<style scoped>

</style>