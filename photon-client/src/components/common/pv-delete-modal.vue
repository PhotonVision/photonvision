<script setup lang="ts">
import { ref } from "vue";
import { useTheme } from "vuetify";
import pvInput from "./pv-input.vue";

const theme = useTheme();

const value = defineModel<boolean | undefined>({ required: true });

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const props = withDefaults(
  defineProps<{
    expected?: string;
    selectCols?: number;
    onBackup?: () => void;
    onDelete: () => void;
    action: string;
    description?: string;
  }>(),
  {
    selectCols: 9
  }
);

const yesDeleteText = ref("");
</script>

<template>
  <v-dialog v-model="value" width="800" dark>
    <v-card color="surface" flat>
      <v-card-title style="display: flex; justify-content: center">
        <span class="open-label">
          <v-icon v-if="expected" end color="error" class="open-icon ma-1" size="large">mdi-alert-outline</v-icon>
          {{ action }}
          <v-icon v-if="expected" end color="error" class="open-icon ma-1" size="large">mdi-alert-outline</v-icon>
        </span>
      </v-card-title>
      <v-card-text class="pt-0 pb-10px">
        <v-row class="align-center text-white">
          <v-col v-if="description" cols="12" md="6">
            <span> {{ description }} </span>
          </v-col>
          <v-col v-if="onBackup" cols="12" md="6">
            <v-btn
              color="buttonActive"
              style="float: right"
              :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
              @click="onBackup"
            >
              <v-icon start class="open-icon" size="large"> mdi-export </v-icon>
              <span class="open-label">Backup Data</span>
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-text v-if="expected" class="pt-0 pb-0">
        <pv-input
          v-model="yesDeleteText"
          :label="'Type &quot;' + expected + '&quot;:'"
          :label-cols="6"
          :input-cols="6"
        />
      </v-card-text>
      <v-card-text class="pt-10px">
        <v-btn
          color="error"
          width="100%"
          :disabled="expected ? yesDeleteText.toLowerCase() !== expected.toLowerCase() : false"
          :variant="theme.global.name.value === 'LightTheme' ? 'elevated' : 'outlined'"
          @click="
            onDelete();
            yesDeleteText = '';
            value = false;
          "
        >
          <v-icon start class="open-icon" size="large"> mdi-trash-can-outline </v-icon>
          <span class="open-label">
            {{ action }}
          </span>
        </v-btn>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>
