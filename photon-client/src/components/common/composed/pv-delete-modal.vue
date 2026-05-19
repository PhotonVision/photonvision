<script setup lang="ts">
import { ref } from "vue";
import IconExport from "~icons/mdi/export";
import IconTrashCanOutline from "~icons/mdi/trash-can-outline";

const value = defineModel<boolean | undefined>({ required: true });

const props = withDefaults(
  defineProps<{
    expectedConfirmationText?: string;
    onBackup?: () => void;
    onConfirm: () => void;
    title: string;
    description?: string;
    deleteText?: string;
    width?: number;
  }>(),
  {
    width: 700
  }
);

const confirmationText = ref("");
</script>

<template>
  <pv-dialog v-model="value" :width="props.width">
    <pv-card class="flex flex-col gap-4">
      <div class="flex justify-center text-lg font-semibold">
        {{ title }}
      </div>
      <div class="pt-0 pb-2.5">
        <span> {{ description }} </span>
      </div>
      <div v-if="expectedConfirmationText" class="pt-0 pb-0">
        <pv-input
          v-model="confirmationText"
          :label="'Type &quot;' + expectedConfirmationText + '&quot;:'"
          :label-cols="6"
          :input-cols="6"
        />
      </div>
      <div class="pt-2.5">
        <div class="flex flex-wrap items-center text-white -mx-3">
          <div v-if="onBackup" class="w-full sm:w-1/2 px-3">
            <pv-button variant="primary" :icon="IconExport" block @click="onBackup">
              <span class="open-label">Backup Data</span>
            </pv-button>
          </div>
          <div v-if="description" :class="['px-3', onBackup ? 'w-full sm:w-1/2' : 'w-full']">
            <pv-button
              variant="danger"
              :icon="IconTrashCanOutline"
              block
              :disabled="
                expectedConfirmationText
                  ? confirmationText.toLowerCase() !== expectedConfirmationText.toLowerCase()
                  : false
              "
              @click="
                onConfirm();
                confirmationText = '';
                value = false;
              "
            >
              <span class="open-label">
                {{ deleteText ?? title }}
              </span>
            </pv-button>
          </div>
        </div>
      </div>
    </pv-card>
  </pv-dialog>
</template>
