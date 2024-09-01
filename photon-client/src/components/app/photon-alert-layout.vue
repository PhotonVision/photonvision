<script setup lang="ts">
import { useStateStore } from "@/stores/StateStore";
import { ref, watch } from "vue";

const showSnackbar = ref<boolean>(false);
const currentSnackbarMessage = ref<string>();
const currentSnackbarColor = ref<string | undefined>();

// TODO, all this crap
watch(
  useStateStore().userMessages,
  (newValue) => {
    console.log("fired");
    const message = newValue.at(0);

    if (!message) {
      showSnackbar.value = false;
      currentSnackbarMessage.value = undefined;
      currentSnackbarColor.value = undefined;

      return;
    }

    showSnackbar.value = true;
    currentSnackbarMessage.value = message.message;
    currentSnackbarColor.value = message.color;

    if (!message.timeoutMs) return;

    setTimeout(() => (useStateStore().userMessages = newValue.slice(1)), message.timeoutMs);
  },
  { deep: true }
);
</script>

<template>
  <div>
    <v-snackbar
      v-model="showSnackbar"
      :color="currentSnackbarColor"
      location="top"
      multi-line
      :text="currentSnackbarMessage"
    />
    <slot />
  </div>
</template>
