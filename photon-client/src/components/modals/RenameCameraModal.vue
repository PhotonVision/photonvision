<script setup lang="ts">
import PvTextbox from "@/components/common/pv-textbox.vue";
import type { ValidationRule } from "@/types/Components";
import { computed, ref, watch } from "vue";
import { nameChangeRegex } from "@/lib/PhotonUtils";
import { useServerStore } from "@/stores/ServerStore";

const props = defineProps<{cameraIndex: number}>();

const serverStore = useServerStore();

const dialogOpen = ref<boolean>();

const bufferCameraName = ref<string>();
const cameraNameRule: ValidationRule = (name: string) => {
  if (!nameChangeRegex.test(name)) {
    return "A camera name can only contain letters, numbers, spaces, underscores, hyphens, parenthesis, and periods";
  }
  if (serverStore.cameraNames.some((cameraName) => cameraName === name)) {
    return "This camera name has already been used";
  }

  return true;
};

const targetCameraName = computed<string>(() => serverStore.getCameraSettingsFromIndex(props.cameraIndex)!.nickname);

watch(dialogOpen, () => {
  bufferCameraName.value = targetCameraName.value;
});
</script>

<template>
  <v-dialog v-model="dialogOpen" max-width="700px">
    <template #activator="{ props }">
      <slot v-bind="{ props }" />
    </template>
    <template #default="{ isActive }">
      <v-card class="pa-3">
        <v-card-title>Edit Camera Name: {{ targetCameraName }}</v-card-title>
        <v-divider />
        <pv-textbox
          v-model="bufferCameraName"
          class="pl-3 pr-3 pt-3"
          label="New Camera Name"
          :rules="[cameraNameRule]"
        />
        <v-card-actions>
          <v-btn
            color="accent"
            :disabled="cameraNameRule(bufferCameraName) != true"
            text="Save"
            variant="elevated"
            @click="() => {
              serverStore.changeCameraNickname(bufferCameraName as string, cameraIndex)
              isActive.value = false;
            }"
          />
          <v-btn
            color="error"
            text="Close"
            variant="elevated"
            @click="isActive.value = false"
          />
        </v-card-actions>
      </v-card>
    </template>
  </v-dialog>
</template>
