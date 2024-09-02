<script setup lang="ts">
import { ref } from "vue";

// TODO finish this stuff

const uploadedFiles = ref<File[] | undefined>();
const validateFiles = (files: File[]): boolean | string => {
  // only one SQL file
  // only one rknn file
  // multiple json files
};
</script>

<template>
  <v-dialog max-width="800px">
    <template #activator="{ props }">
      <slot name="activator" v-bind="{ props }" />
    </template>
    <template #default="{ isActive }">
      <v-card class="pa-3">
        <v-card-title>Settings Import</v-card-title>
        <v-card-subtitle>Replace current settings with previously exported settings</v-card-subtitle>

        <v-file-input
          v-model="uploadedFiles"
          accept=".zip,.rknn,.sqllite,.json"
          class="pt-6 pr-3"
          hide-details="auto"
          label="Upload Setting Files"
          multiple
        />

        <v-card-actions class="mt-3">
          <v-btn color="secondary" text="Cancel" variant="elevated" @click="() => (isActive.value = false)" />
        </v-card-actions>
      </v-card>
    </template>
  </v-dialog>
</template>
