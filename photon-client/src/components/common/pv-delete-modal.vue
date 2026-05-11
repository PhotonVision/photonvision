<script setup lang="ts">
import PvButton from "@/components/common/pv-button.vue";
import PvDialog from "@/components/common/pv-dialog.vue";
import { ref } from "vue";
import pvInput from "./pv-input.vue";

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
    <v-card color="surface" flat>
      <v-card-title style="display: flex; justify-content: center">
        {{ title }}
      </v-card-title>
      <v-card-text class="pt-0 pb-10px">
        <span> {{ description }} </span>
      </v-card-text>
      <v-card-text v-if="expectedConfirmationText" class="pt-0 pb-0">
        <pv-input
          v-model="confirmationText"
          :label="'Type &quot;' + expectedConfirmationText + '&quot;:'"
          :label-cols="6"
          :input-cols="6"
        />
      </v-card-text>
      <v-card-text class="pt-10px">
        <v-row class="align-center text-white">
          <v-col v-if="onBackup" cols="6">
            <pv-button variant="primary" icon="mdi-export" block @click="onBackup">
              <span class="open-label">Backup Data</span>
            </pv-button>
          </v-col>
          <v-col v-if="description" :cols="onBackup ? '6' : '12'">
            <pv-button
              variant="danger"
              icon="mdi-trash-can-outline"
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
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </pv-dialog>
</template>
