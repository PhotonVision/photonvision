<script setup lang="ts">
const emit = defineEmits<{
  onOk: [];
  onCancel: [];
}>();
</script>

<template>
  <v-dialog max-width="400px">
    <template #activator="{ props }">
      <slot name="activator" v-bind="{ props }" />
    </template>
    <template #default="{ isActive }">
      <v-card class="pa-3">
        <v-card-title>Are you sure?</v-card-title>
        <v-divider />
        <slot name="message" />
        <v-card-actions>
          <v-btn
            color="error"
            text="Proceed"
            variant="elevated"
            @click="
              () => {
                emit('onOk');
                isActive.value = false;
              }
            "
          />
          <v-btn
            color="accent"
            text="Cancel"
            variant="elevated"
            @click="
              () => {
                emit('onCancel');
                isActive.value = false;
              }
            "
          />
        </v-card-actions>
      </v-card>
    </template>
  </v-dialog>
</template>
