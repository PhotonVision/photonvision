<script setup lang="ts">
import PvTooltippedIcon from "@/components/common/pv-tooltipped-icon.vue";
import ConfirmationModal from "@/components/modals/ConfirmationModal.vue";
import { computed, reactive, ref } from "vue";
import { ValidationRule } from "@/types/Components";
import { nameChangeRegex, pipelineTypeToString } from "@/lib/PhotonUtils";
import { useServerStore } from "@/stores/ServerStore";
import { PipelineType, UserPipelineType } from "@/types/PipelineTypes";

const modalProps = defineProps<{cameraIndex: number}>();

const serverStore = useServerStore();

const modalOpen = ref<boolean>();

interface PipelineItem {
  name: string | undefined,
  nameBuffer?: string,
  type: UserPipelineType | undefined,
  typeBuffer?: UserPipelineType,
  idx?: number,
  fixedName: boolean,
  fixedType: boolean,
  onConfirm?: (item: PipelineItem) => void,
  onCancel?: (item: PipelineItem) => void
}

const tempPipelines = reactive<PipelineItem[]>([]);
const pipelineTableData = computed<PipelineItem[]>(() => {
  const currentPipelineData = serverStore.getCameraSettingsFromIndex(modalProps.cameraIndex)?.pipelineSettings?.map<PipelineItem>((settings) => ({
    name: settings.pipelineNickname,
    type: settings.pipelineType as UserPipelineType,
    idx: settings.pipelineIndex,
    fixedName: true,
    fixedType: true
  })) || [];

  for (let i = 0; i < tempPipelines.length; i++) {
    currentPipelineData.push(tempPipelines.at(i) as PipelineItem);
  }

  return currentPipelineData;
});

const validNewPipelineTypes = computed(() => {
  const pipelineTypes = [
    { name: "Reflective", value: PipelineType.Reflective },
    { name: "Colored Shape", value: PipelineType.ColoredShape },
    { name: "AprilTag", value: PipelineType.AprilTag },
    { name: "Aruco", value: PipelineType.Aruco }
  ];
  if (serverStore.instanceConfig?.rknnSupported) {
    pipelineTypes.push({ name: "Object Detection", value: PipelineType.ObjectDetection });
  }
  return pipelineTypes;
});

const pipelineNameRule: ValidationRule = (name: string) => {
  if (!nameChangeRegex.test(name)) {
    return "A pipeline name can only contain letters, numbers, spaces, underscores, hyphens, parenthesis, and periods";
  }
  if (serverStore.getCameraSettingsFromIndex(modalProps.cameraIndex)?.pipelineSettings.some((pipelineSettings) => pipelineSettings.pipelineNickname === name)) {
    return "This pipeline name has already been used";
  }

  return true;
};

const pipelineCreate = () => {
  tempPipelines.push({
    fixedName: false,
    fixedType: false,
    nameBuffer: "New Pipeline",
    name: undefined,
    type: undefined,
    onConfirm: (confirmItem: PipelineItem) => {
      serverStore.createNewPipeline(confirmItem.nameBuffer as string, confirmItem.typeBuffer as UserPipelineType);
      tempPipelines.splice(tempPipelines.indexOf(confirmItem), 1);
    },
    onCancel: (cancelItem: PipelineItem) => {
      tempPipelines.splice(tempPipelines.indexOf(cancelItem), 1);
    }
  });
};
const pipelineNameEdit = (item: PipelineItem) => {
  item.fixedName = false;
  item.nameBuffer = item.name;
  item.onConfirm = (confirmItem: PipelineItem) => {
    // TODO does this just rerender everything?
    serverStore.changePipelineNickname(confirmItem.idx as number, confirmItem.nameBuffer as string, modalProps.cameraIndex);

    // is any of this even needed?
    confirmItem.name = confirmItem.nameBuffer as string;

    confirmItem.nameBuffer = undefined;
    confirmItem.fixedName = true;
    confirmItem.onConfirm = undefined;
    confirmItem.onCancel = undefined;
  };
  item.onCancel = (cancelItem: PipelineItem) => {
    cancelItem.nameBuffer = undefined;
    cancelItem.fixedName = true;
    cancelItem.onConfirm = undefined;
    cancelItem.onCancel = undefined;
  };
};
const pipelineDuplicate = (item: PipelineItem) => {
  tempPipelines.push({
    fixedName: false,
    name: undefined,
    nameBuffer: `Duplicated Pipeline ${item.idx}`,
    fixedType: true,
    idx: item.idx,
    type: item.type,
    onConfirm: (confirmItem: PipelineItem) => {
      serverStore.duplicatePipeline(confirmItem.idx as number, confirmItem.nameBuffer as string, false, modalProps.cameraIndex);
      tempPipelines.splice(tempPipelines.indexOf(confirmItem), 1);
    },
    onCancel: (cancelItem: PipelineItem) => {
      tempPipelines.splice(tempPipelines.indexOf(cancelItem), 1);
    }
  });
};
const pipelineReset = (item: PipelineItem) => {
  item.fixedType = false;
  item.typeBuffer = item.type;
  item.onConfirm = (confirmItem: PipelineItem) => {
    const changedTypes = confirmItem.typeBuffer !== confirmItem.type;

    if (changedTypes) {
      serverStore.resetPipeline(confirmItem.idx as number, item.typeBuffer as UserPipelineType, modalProps.cameraIndex);
      confirmItem.type = confirmItem.typeBuffer;
    } else {
      serverStore.resetPipeline(confirmItem.idx as number, undefined, modalProps.cameraIndex);
    }

    confirmItem.fixedType = true;
    confirmItem.typeBuffer = undefined;
    confirmItem.onConfirm = undefined;
    confirmItem.onCancel = undefined;
  };
  item.onCancel = (cancelItem: PipelineItem) => {
    cancelItem.fixedType = true;
    cancelItem.typeBuffer = undefined;
    cancelItem.onConfirm = undefined;
    cancelItem.onCancel = undefined;
  };
};
const pipelineDelete = (item: PipelineItem) => {
  serverStore.deletePipeline(item.idx as number, modalProps.cameraIndex);
};
</script>

<template>
  <v-dialog v-model="modalOpen" max-width="900px">
    <template #activator="{ props }">
      <slot v-bind="{ props }" />
    </template>
    <template #default="{ isActive: parentActive }">
      <v-card class="pa-3">
        <v-card-title>Edit Pipeline Configs for {{ serverStore.getCameraSettingsFromIndex(cameraIndex) }}</v-card-title>
        <v-divider />
        <v-data-table
          :headers="[
            { title: 'Index', align: 'start', key: 'idx', minWidth: '50px', maxWidth: '75px' },
            { title: 'Name', key: 'name', minWidth: '220px' },
            { title: 'Type', key: 'type', minWidth: '220px' },
            { title: 'Actions', align: 'end', key: 'actions', minWidth: '60px', maxWidth: '80px', sortable: false }
          ]"
          hide-default-footer
          :items="pipelineTableData"
        >
          <template #top>
            <v-toolbar flat>
              <v-toolbar-title>Pipelines</v-toolbar-title>
              <v-divider class="mx-4" inset vertical />
              <v-spacer />
              <v-btn
                icon="mdi-plus"
                variant="plain"
                @click="() => pipelineCreate()"
              />
              <!--              <v-btn-->
              <!--                icon="mdi-import"-->
              <!--                variant="plain"-->
              <!--              />-->
            </v-toolbar>
          </template>

          <template #item="{ item }">
            <tr :style="(item.idx && serverStore.getCameraSettingsFromIndex(cameraIndex)?.activePipelineIndex === item.idx) ? {'background': 'rgba(var(--v-border-color), var(--v-hover-opacity))'} : {}">
              <td>{{ item.idx }}</td>
              <td>
                <div v-if="item.fixedName">{{ item.name }}</div>
                <div v-else>
                  <v-text-field
                    v-model="item.nameBuffer"
                    :rules="[(v) => v === undefined ? 'Name must be given' : true, pipelineNameRule]"
                    variant="underlined"
                  />
                </div>
              </td>
              <td>
                <div v-if="item.fixedType">{{ pipelineTypeToString(item.type as UserPipelineType) }}</div>
                <div v-else>
                  <v-select
                    v-model="item.typeBuffer"
                    base-color="accent"
                    item-title="name"
                    item-value="value"
                    :items="validNewPipelineTypes"
                    :rules="[(v) => v === undefined ? 'Type must be defined' : true]"
                    variant="underlined"
                  />
                </div>
              </td>
              <td>
                <div v-if="item.onConfirm || item.onCancel">
                  <v-row no-gutters>
                    <v-col>
                      <pv-tooltipped-icon
                        clickable
                        color="green"
                        :disabled="(!item.fixedName && (pipelineNameRule(item.nameBuffer as string) !== true || item.nameBuffer === item.name)) || (!item.fixedType && item.typeBuffer === undefined)"
                        icon-name="mdi-check"
                        size="small"
                        tooltip="Confirm"
                        @click="() => item.onConfirm && item.onConfirm(item)"
                      />
                    </v-col>
                    <v-col>
                      <pv-tooltipped-icon
                        clickable
                        color="red"
                        icon-name="mdi-cancel"
                        size="small"
                        tooltip="Cancel"
                        @click="() => item.onCancel && item.onCancel(item)"
                      />
                    </v-col>
                  </v-row>
                </div>
                <div v-else>
                  <v-row no-gutters>
                    <v-col>
                      <pv-tooltipped-icon
                        clickable
                        icon-name="mdi-pencil"
                        size="small"
                        tooltip="Rename Pipeline"
                        @click="() => pipelineNameEdit(item)"
                      />
                    </v-col>
                    <v-col>
                      <pv-tooltipped-icon
                        clickable
                        icon-name="mdi-content-duplicate"
                        size="small"
                        tooltip="Duplicate Pipeline"
                        @click="() => pipelineDuplicate(item)"
                      />
                    </v-col>
                    <v-col>
                      <ConfirmationModal @on-ok="() => pipelineReset(item)">
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
                          <v-card-text>
                            By resetting a pipeline's setting you are restoring default settings for pipeline type
                            <b>{{ pipelineTypeToString(item.type as UserPipelineType) }}</b>. There is no way to undo this.
                          </v-card-text>
                        </template>
                      </ConfirmationModal>
                    </v-col>
                    <v-col>
                      <ConfirmationModal @on-ok="() => pipelineDelete(item)">
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
                          <v-card-text>
                            Are you sure you want to delete the pipeline <b class="text-error">{{ item.name }}</b> this
                            <b>cannot</b> be undone. Backup settings beforehand to be safe.
                          </v-card-text>
                        </template>
                      </ConfirmationModal>
                    </v-col>
                  </v-row>
                </div>
              </td>
            </tr>
          </template>
          <template #no-data>
            This camera doesn't have any pipelines. Add one by clicking the plus in the toolbar.
          </template>
        </v-data-table>

        <v-divider class="mt-4" />

        <v-card-actions class="mt-2">
          <v-spacer />
          <v-btn color="white" text="Close" @click="parentActive.value = false" />
        </v-card-actions>
      </v-card>
    </template>
  </v-dialog>
</template>
