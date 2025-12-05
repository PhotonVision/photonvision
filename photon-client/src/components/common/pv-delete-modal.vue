<script setup lang="ts">
import { ref } from "vue";
import { useTheme } from "vuetify";
import pvInput from "./pv-input.vue";

const theme = useTheme();

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
  <v-dialog v-model="value" :width="props.width" dark>
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
            <v-btn
              color="buttonActive"
              style="float: right"
              width="100%"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="onBackup"
            >
              <v-icon start class="open-icon" size="large"> mdi-export </v-icon>
              <span class="open-label">Backup Data</span>
            </v-btn>
          </v-col>
          <v-col v-if="description" :cols="onBackup ? '6' : '12'">
            <v-btn
              color="error"
              width="100%"
              :disabled="
                expectedConfirmationText
                  ? confirmationText.toLowerCase() !== expectedConfirmationText.toLowerCase()
                  : false
              "
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="
                onConfirm();
                confirmationText = '';
                value = false;
              "
            >
              <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
              <span class="open-label">
                {{ deleteText ?? title }}
              </span>
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>
