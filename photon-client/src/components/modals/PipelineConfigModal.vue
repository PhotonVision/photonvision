<script setup lang="ts">
import PvTooltippedIcon from "@/components/common/pv-tooltipped-icon.vue";
import ConfirmationModal from "@/components/modals/ConfirmationModal.vue";
import CreateNewPipelineModal from "@/components/modals/CreateNewPipelineModal.vue";
import { computed, onUpdated, ref } from "vue";
import { useCameraSettingsStore } from "@/stores/settings/CameraSettingsStore";
import { ValidationRule } from "@/types/Components";
import { nameChangeRegex } from "@/lib/PhotonUtils";

const modalOpen = ref<boolean>();
// TODO: proto more fields, date created or something
const pipelineTableHeaders = [
  { title: "Name", align: "start", key: "name" },
  { title: "Type", align: "start", key: "type" },
  { title: "Actions", align: "end", key: "actions", sortable: false }
];
// TODO fetch all pipeline data from backend
const pipelineTableData = computed(() => [
  { name: "Test", type: "Reflective", idx: 0 },
  { name: "Test2", type: "Reflective", idx: 1 }
]);

const nameChangeIndex = ref<number>(-1);
const nameChangeBuffer = ref<string>();
const pipelineNameRule: ValidationRule = (name: string) => {
  if (!nameChangeRegex.test(name)) {
    return "A pipeline name can only contain letters, numbers, spaces, underscores, hyphens, parenthesis, and periods";
  }
  if (useCameraSettingsStore().pipelineNames.some((pipelineName) => pipelineName === name)) {
    return "This pipeline name has already been used";
  }

  return true;
};

// TODO implement in backend
const renamePipelineByIndex = (newName: string, idx: number) => {
  nameChangeBuffer.value = undefined;
  nameChangeIndex.value = -1;
};
// TODO implement in backend
const duplicatePipelineByIndex = (idx: number) => {};
// TODO implement in backend
const resetPipelineByIndex = (idx: number) => {};
// TODO implement in backend
const deletePipelineByIndex = (idx: number) => {};

onUpdated(() => {
  if (!modalOpen.value) {
    nameChangeBuffer.value = undefined;
    nameChangeIndex.value = -1;
  }
});
</script>

<template>
  <v-dialog v-model="modalOpen" max-width="700px">
    <template #activator="{ props }">
      <slot v-bind="{ props }" />
    </template>
    <template #default="{ isActive: parentActive }">
      <v-card class="pa-3">
        <v-card-title>Edit Pipeline Configs for {{ useCameraSettingsStore().currentCameraName }}</v-card-title>
        <v-divider />
        <v-data-table :headers="pipelineTableHeaders" hide-default-footer :items="pipelineTableData">
          <template #top>
            <v-toolbar flat>
              <v-toolbar-title>Pipelines</v-toolbar-title>
              <v-divider class="mx-4" inset vertical />
              <v-spacer />
              <CreateNewPipelineModal v-slot="{ props }">
                <v-btn class="mb-2" v-bind="props" text="Create New Pipeline" />
              </CreateNewPipelineModal>
            </v-toolbar>
          </template>
          <template #item.name="{ item, value }">
            <v-row v-if="item.idx === nameChangeIndex" no-gutters>
              <v-col>
                <v-text-field
                  v-model="nameChangeBuffer"
                  :placeholder="value"
                  :rules="[pipelineNameRule]"
                  variant="underlined"
                />
              </v-col>
              <v-col align-self="center" class="pl-4" cols="2">
                <pv-tooltipped-icon
                  clickable
                  color="green"
                  :disabled="pipelineNameRule(nameChangeBuffer) != true"
                  icon-name="mdi-check"
                  tooltip="Rename Pipeline"
                  @click="renamePipelineByIndex(nameChangeBuffer || '', item.idx)"
                />
                <pv-tooltipped-icon
                  clickable
                  color="red"
                  icon-name="mdi-cancel"
                  tooltip="Cancel Rename"
                  @click="
                    () => {
                      nameChangeBuffer = undefined;
                      nameChangeIndex = -1;
                    }
                  "
                />
              </v-col>
            </v-row>
            <div v-else>{{ value }}</div>
          </template>
          <template #item.actions="{ item }">
            <div class="pt-1 pb-1">
              <pv-tooltipped-icon
                clickable
                icon-name="mdi-pencil"
                size="small"
                tooltip="Rename Pipeline"
                @click="nameChangeIndex = item.idx"
              />
              <pv-tooltipped-icon
                clickable
                icon-name="mdi-content-duplicate"
                size="small"
                tooltip="Duplicate Pipeline"
                @click="duplicatePipelineByIndex(item.idx)"
              />
              <ConfirmationModal @on-ok="resetPipelineByIndex(item.idx)">
                <template #activator="{ props }">
                  <pv-tooltipped-icon
                    clickable
                    color="warning"
                    icon-name="mdi-close-circle-outline"
                    size="small"
                    tooltip="Reset Pipeline Settings"
                    :vuetify-props="props"
                  />
                </template>
                <template #message>
                  <v-card-text
                    >By resetting a pipeline's setting you are restoring default settings for pipeline type
                    <b>{{ item.type }}</b
                    >. There is no way to undo this.</v-card-text
                  >
                </template>
              </ConfirmationModal>
              <ConfirmationModal @on-ok="deletePipelineByIndex(item.idx)">
                <template #activator="{ props }">
                  <pv-tooltipped-icon
                    clickable
                    color="error"
                    icon-name="mdi-delete"
                    size="small"
                    tooltip="Delete Pipeline"
                    :vuetify-props="props"
                  />
                </template>
                <template #message>
                  <v-card-text
                    >Are you sure you want to delete the pipeline <b class="text-error">{{ item.name }}</b> this
                    <b>cannot</b> be undone. Backup settings beforehand to be safe.</v-card-text
                  >
                </template>
              </ConfirmationModal>
            </div>
          </template>
        </v-data-table>
        <v-card-actions class="mt-2">
          <v-btn color="secondary" text="Close" variant="elevated" @click="parentActive.value = false" />
        </v-card-actions>
      </v-card>
    </template>
  </v-dialog>
</template>
