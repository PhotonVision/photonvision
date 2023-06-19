<template>
  <div>
    <v-row
      align="center"
      style="padding: 12px 12px 12px 24px"
    >
      <v-col
        cols="10"
        md="5"
        lg="10"
        no-gutters
        class="pa-0"
      >
        <CVselect
          v-model="currentCameraIndex"
          name="Camera"
          :list="$store.getters.cameraList"
          @input="handleInput('currentCamera', currentCameraIndex)"
        />
      </v-col>
      <v-col
        cols="10"
        md="5"
        lg="10"
        no-gutters
        class="pa-0"
      >
        <CVselect
          v-model="currentPipelineIndex"
          name="Pipeline"
          tooltip="Each pipeline runs on a camera output and stores a unique set of processing settings"
          :disabled="$store.getters.isDriverMode"
          :list="($store.getters.isDriverMode ? ['Driver Mode'] : []).concat($store.getters.pipelineList)"
          @input="handleInputWithIndex('currentPipeline', currentPipelineIndex)"
        />
      </v-col>
      <v-col
        cols="2"
        md="1"
        lg="2"
        class="pl-5"
      >
        <v-menu
          v-if="!$store.getters.isDriverMode"
          offset-y
          auto
        >
          <template v-slot:activator="{ on }">
            <v-icon
              color="#c5c5c5"
              v-on="on"
            >
              mdi-menu
            </v-icon>
          </template>
          <v-list
            dark
            dense
            color="primary"
          >
            <v-list-item @click="toPipelineNameChange">
              <v-list-item-title>
                <CVicon
                  color="#c5c5c5"
                  :right="true"
                  text="mdi-pencil"
                  tooltip="Edit pipeline name"
                />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="toCreatePipeline">
              <v-list-item-title>
                <CVicon
                  color="#c5c5c5"
                  :right="true"
                  text="mdi-plus"
                  tooltip="Add new pipeline"
                />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="deleteCurrentPipeline">
              <v-list-item-title>
                <CVicon
                  color="red darken-2"
                  :right="true"
                  text="mdi-delete"
                  tooltip="Delete pipeline"
                />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="duplicatePipeline">
              <v-list-item-title>
                <CVicon
                  color="#c5c5c5"
                  :right="true"
                  text="mdi-content-copy"
                  tooltip="Duplicate pipeline"
                />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="() => showPipeImportDialog = true">
              <v-list-item-title>
                <CVicon
                  color="#c5c5c5"
                  :right="true"
                  text="mdi-import"
                  tooltip="Import Pipeline Settings from File"
                />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="exportPipelineToJson">
              <v-list-item-title>
                <CVicon
                  color="#c5c5c5"
                  :right="true"
                  text="mdi-export"
                  tooltip="Export Pipeline Settings to File"
                />
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </v-col>
      <v-col
        v-if="_currentPipelineType >= 0"
        cols="10"
        md="11"
        lg="10"
        no-gutters
        class="pa-0"
      >
        <CVselect
          v-model="_currentPipelineType"
          name="Type"
          tooltip="Changes the pipeline type, which changes the type of processing that will happen on input frames"
          :list="['Reflective Tape', 'Colored Shape', 'AprilTag']"
          @input="e => showTypeDialog(e)"
        />
      </v-col>
    </v-row>

    <!--pipeline naming dialog-->
    <v-dialog
      v-model="namingDialog"
      dark
      persistent
      width="500"
      height="357"
    >
      <v-card
        dark
        color="primary"
      >
        <v-card-title
          class="headline"
          primary-title
        >
          {{ isPipelineNameEdit ? "Edit Pipeline Name" : "Create Pipeline" }}
        </v-card-title>
        <v-card-text>
          <CVinput
            v-model="newPipelineName"
            name="Name"
            :error-message="checkPipelineName"
          />
        </v-card-text>
        <v-divider />
        <v-card-actions>
          <v-spacer />
          <v-btn
            color="#ffd843"
            :disabled="checkPipelineName !==''"
            @click="savePipelineNameChange"
          >
            Save
          </v-btn>
          <v-btn
            color="error"
            @click="discardPipelineNameChange"
          >
            Cancel
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog
      v-model="showPipeTypeDialog"
      width="600"
    >
      <v-card
        color="primary"
        dark
      >
        <v-card-title>Change Pipeline Type</v-card-title>
        <v-card-text>
          Changing the type of this pipeline will erase the current pipeline's settings and replace it with a new {{ ['Reflective', 'Shape'][proposedPipelineType] }} pipeline. <b class="red--text format_bold">You will lose all settings for the pipeline
            "{{ ($store.getters.isDriverMode ? ['Driver Mode'] : []).concat($store.getters.pipelineList)[currentPipelineIndex] }}."</b> Are you sure you want to do this?
          <v-row
            class="mt-6"
            style="display: flex; align-items: center; justify-content: center"
            align="center"
          >
            <v-btn
              class="mr-3"
              color="red"
              width="250"
              @click="e => changePipeType(true)"
            >
              Yes, replace this pipeline
            </v-btn>
            <v-btn
              class="ml-10"
              color="secondary"
              width="250"
              @click="e => changePipeType(false)"
            >
              No, take me back
            </v-btn>
          </v-row>
        </v-card-text>
      </v-card>
    </v-dialog>
    <v-dialog
      v-model="showPipeImportDialog"
      width="600"
    >
      <v-card
        color="primary"
        dark
      >
        <v-card-title>Import Pipeline Settings</v-card-title>
        <v-card-text>
          Import a previously exported PhotonVision pipeline to this device.
          <v-row>
            <v-col cols="10">
              <v-file-input
                color="secondary"
                accept=".json"
                :error-messages="checkImportedPipelineFile"
                @change="handlePipelineImportFile"
              >
                Select File
              </v-file-input>
            </v-col>
            <v-col>
              <v-icon
                v-if="pipelineImportData === undefined"
                color="red"
                size="70"
              >
                mdi-close
              </v-icon>
              <v-icon
                v-else
                color="green"
                size="70"
              >
                mdi-check-bold
              </v-icon>
            </v-col>
          </v-row>
          <div
            v-if="pipelineImportData !== undefined && pipelineImportData !== null"
          >
            <hr>
            <v-row class="ma-6">
              <CVinput
                v-model="importedPipelineName"
                name="Pipeline Name"
                :label-cols="4"
                :input-cols="8"
                :error-message="_checkPipelineName(importedPipelineName)"
              />
            </v-row>
            <v-row
              class="mt-6"
              style="display: flex; align-items: center; justify-content: center"
              align="center"
            >
              <v-btn
                class="mr-3"
                color="red"
                width="250"
                :disabled="_checkPipelineName(importedPipelineName) !== ''"
                @click="createPipelineFromImportedFile"
              >
                Import Pipeline
              </v-btn>
              <v-btn
                class="ml-10"
                color="secondary"
                width="250"
                @click="cancelPipelineImport"
              >
                No, take me back
              </v-btn>
            </v-row>
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import CVicon from '../common/cv-icon'
import CVselect from '../common/cv-select'
import CVinput from '../common/cv-input'

export default {
    name: "CameraAndPipelineSelect",
    components: {
        CVicon,
        CVselect,
        CVinput
    },
    data: () => {
        return {
            isPipelineNameEdit: false,
            namingDialog: false,
            newPipelineName: "",
            duplicateDialog: false,
            showPipeTypeDialog: false,
            proposedPipelineType : 0,
            pipeIndexToDuplicate: undefined,

            showPipeImportDialog: false,
            pipelineImportData: undefined,
            importedPipelineName: ""
        }
    },
    computed: {
        checkPipelineName() {
            if (this.newPipelineName !== this.$store.getters.pipelineList[this.currentPipelineIndex - 1] || !this.isPipelineNameEdit) {
                const pipelineNameChangeRegex = RegExp("^[A-Za-z0-9_ \\-)(]*[A-Za-z0-9][A-Za-z0-9_ \\-)(.]*$")
                if (pipelineNameChangeRegex.test(this.newPipelineName)) {
                    for (let pipe in this.$store.getters.pipelineList) {
                        if (this.$store.getters.pipelineList.hasOwnProperty(pipe)) {
                            if (this.newPipelineName === this.$store.getters.pipelineList[pipe]) {
                                return "A pipeline with this name already exists"
                            }
                        }
                    }
                } else {
                    return "A pipeline name can only contain letters, numbers, and spaces"
                }
            }
            return ""
        },
        currentCameraIndex: {
            get() {
                return this.$store.getters.currentCameraIndex;
            },
            set(value) {
                this.$store.commit('currentCameraIndex', value);
            }
        },
        currentPipelineIndex: {
            get() {
                return this.$store.getters.currentPipelineIndex + (this.$store.getters.isDriverMode ? 1 : 0);
            },
            set(value) {
                this.$store.commit('currentPipelineIndex', value - (this.$store.getters.isDriverMode ? 1 : 0));
            }
        },
        _currentPipelineType: {
            get() {
                return this.$store.getters.currentPipelineSettings.pipelineType - 2;
            },
            set(value) {
                value; // nop, since we have the dialog for this
            }
        },
        checkImportedPipelineFile() {
          if(this.pipelineImportData === undefined) {
            return "No File Uploaded"
          } else if(this.pipelineImportData === null) {
            return "Corrupt or Malformed JSON File"
          } else {
            return ""
          }
        }
    },
    methods: {
        showTypeDialog(idx) {
            // Only show the dialog if it's a new type
            this.showPipeTypeDialog = idx !== this._currentPipelineType;
            this.proposedPipelineType = idx;
        },
        changePipeType(actuallyChange) {
            const newIdx = actuallyChange ? this.proposedPipelineType : this._currentPipelineType
            this.handleInputWithIndex('pipelineType', newIdx);
            this.showPipeTypeDialog = false;
        },
        toPipelineNameChange() {
            this.newPipelineName = this.$store.getters.pipelineList[this.currentPipelineIndex - 1];
            this.isPipelineNameEdit = true;
            this.namingDialog = true;
        },
        toCreatePipeline() {
            this.newPipelineName = "New Pipeline";
            this.isPipelineNameEdit = false;
            this.namingDialog = true;
        },
        deleteCurrentPipeline() {
            if (this.$store.getters.pipelineList.length > 1) {
                this.handleInputWithIndex('deleteCurrentPipeline', {});
            } else {
                this.snackbar = true;
            }
        },
        savePipelineNameChange() {
            if (this.checkPipelineName === "") {
                if (this.isPipelineNameEdit) {
                    this.handleInputWithIndex("changePipelineName", this.newPipelineName);
                } else {
                    this.handleInputWithIndex("addNewPipeline", [this.newPipelineName, this._currentPipelineType]); // 0 for reflective, 1 for colored shape
                }
                this.discardPipelineNameChange();
            }
        },
        handlePipelineImportFile(event) {
          // TODO, parse required info. null if malformed, else data
          event;
          this.pipelineImportData = {}
        },
        _checkPipelineName(name) {
            const pipelineNameChangeRegex = RegExp("^[A-Za-z0-9_ \\-)(]*[A-Za-z0-9][A-Za-z0-9_ \\-)(.]*$")
            if (pipelineNameChangeRegex.test(name)) {
              for (let pipe in this.$store.getters.pipelineList) {
                if (this.$store.getters.pipelineList.hasOwnProperty(pipe)) {
                  if (name === this.$store.getters.pipelineList[pipe]) {
                    return "A pipeline with this name already exists"
                  }
                }
              }
            } else {
              return "A pipeline name can only contain letters, numbers, and spaces"
            }
          return ""
        },
        duplicatePipeline() {
            this.handleInputWithIndex("duplicatePipeline", this.currentPipelineIndex);
        },
        discardPipelineNameChange() {
            this.namingDialog = false;
            this.isPipelineNameEdit = false;
            this.newPipelineName = "";
        },
        exportPipelineToJson() {
          // TODO determine what data needs to be exported, and export it to JSON
        },
        createPipelineFromImportedFile() {
          // this.pipelineImportData
          // this.importedPipelineName
          // TODO, create a new pipeline using the data, rename it using the name, then select it or whatever order the backend wants
        },
        cancelPipelineImport() {
          this.showPipeImportDialog = false;
          this.pipelineImportData = undefined;
          this.importedPipelineName = ""
        }
    }

}
</script>

<style scoped>

</style>
