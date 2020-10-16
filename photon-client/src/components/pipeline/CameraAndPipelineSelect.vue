<template>
  <div>
    <v-row align="center">
      <v-col
        cols="10"
        md="5"
        lg="10"
        class="pt-0 pb-0 pl-6"
      >
        <CVselect
          v-if="isCameraNameEdit === false"
          v-model="currentCameraIndex"
          name="Camera"
          :list="$store.getters.cameraList"
          @input="handleInput('currentCamera',currentCameraIndex)"
        />
        <CVinput
          v-else
          v-model="newCameraName"
          name="Camera"
          input-cols="9"
          :error-message="checkCameraName"
          @Enter="saveCameraNameChange"
        />
      </v-col>
      <v-col
        cols="2"
        md="1"
        lg="2"
      >
        <CVicon
          v-if="isCameraNameEdit === false"
          color="#c5c5c5"
          :hover="true"
          text="edit"
          tooltip="Edit camera name"
          @click="changeCameraName"
        />
        <div v-else>
          <CVicon
            color="#c5c5c5"
            style="display: inline-block;"
            :hover="true"
            text="save"
            tooltip="Save Camera Name"
            @click="saveCameraNameChange"
          />
          <CVicon
            color="error"
            style="display: inline-block;"
            :hover="true"
            text="close"
            tooltip="Discard Changes"
            @click="discardCameraNameChange"
          />
        </div>
      </v-col>
      <v-col
        cols="10"
        md="5"
        lg="10"
        class="pt-0 pb-0 pl-6"
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
              menu
            </v-icon>
          </template>
          <v-list dense>
            <v-list-item @click="toPipelineNameChange">
              <v-list-item-title>
                <CVicon
                  color="#c5c5c5"
                  :right="true"
                  text="edit"
                  tooltip="Edit pipeline name"
                />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="toCreatePipeline">
              <v-list-item-title>
                <CVicon
                  color="#c5c5c5"
                  :right="true"
                  text="add"
                  tooltip="Add new pipeline"
                />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="deleteCurrentPipeline">
              <v-list-item-title>
                <CVicon
                  color="red darken-2"
                  :right="true"
                  text="delete"
                  tooltip="Delete pipeline"
                />
              </v-list-item-title>
            </v-list-item>
            <v-list-item @click="openDuplicateDialog">
              <v-list-item-title>
                <CVicon
                  color="#c5c5c5"
                  :right="true"
                  text="mdi-content-copy"
                  tooltip="Duplicate pipeline"
                />
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </v-col>
    </v-row>
    <!--pipeline duplicate dialog-->
    <v-dialog
      v-model="duplicateDialog"
      dark
      width="500"
      height="357"
    >
      <v-card dark>
        <v-card-title
          class="headline"
          primary-title
        >
          Duplicate Pipeline
        </v-card-title>
        <v-card-text>
          <CVselect
            v-model="pipeIndexToDuplicate"
            name="Pipeline"
            :list="$store.getters.pipelineList"
          />
        </v-card-text>
        <v-divider />
        <v-card-actions>
          <v-spacer />
          <v-btn
            color="#ffd843"
            @click="duplicatePipeline"
          >
            Duplicate
          </v-btn>
          <v-btn
            color="error"
            @click="closeDuplicateDialog"
          >
            Cancel
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <!--pipeline naming dialog-->
    <v-dialog
      v-model="namingDialog"
      dark
      width="500"
      height="357"
    >
      <v-card dark>
        <v-card-title
          class="headline"
          primary-title
        >
          Pipeline Name
        </v-card-title>
        <v-card-text>
          <CVinput
            v-model="newPipelineName"
            name="Pipeline"
            :error-message="checkPipelineName"
            @Enter="savePipelineNameChange"
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
                re: RegExp("^[A-Za-z0-9 \\-)(]*[A-Za-z0-9][A-Za-z0-9 \\-)(.]*$"),
                isCameraNameEdit: false,
                newCameraName: "",
                cameraNameError: "",
                isPipelineNameEdit: false,
                namingDialog: false,
                newPipelineName: "",
                duplicateDialog: false,
                pipeIndexToDuplicate: undefined
            }
        },
        computed: {
            checkCameraName() {
                if (this.newCameraName !== this.$store.getters.cameraList[this.currentCameraIndex]) {
                    if (this.re.test(this.newCameraName)) {
                        for (let cam in this.cameraList) {
                            if (this.cameraList.hasOwnProperty(cam)) {
                                if (this.newCameraName === this.cameraList[cam]) {
                                    return "A camera by that name already Exists"
                                }
                            }
                        }
                    } else {
                        return "A camera name can only contain letters, numbers and spaces"
                    }
                }
                return "";
            },
            checkPipelineName() {
                if (this.newPipelineName !== this.$store.getters.pipelineList[this.currentPipelineIndex - 1] || this.isPipelineNameEdit === false) {
                    if (this.re.test(this.newPipelineName)) {
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
            }
        },
        methods: {
            changeCameraName() {
                this.newCameraName = this.$store.getters.cameraList[this.currentCameraIndex];
                this.isCameraNameEdit = true;
            },
            saveCameraNameChange() {
                if (this.checkCameraName === "") {
                    // this.handleInputWithIndex("changeCameraName", this.newCameraName);
                    this.axios.post('http://' + this.$address + '/api/setCameraNickname',
                        {name: this.newCameraName, cameraIndex: this.$store.getters.currentCameraIndex})
                        .then(r => {
                            this.$emit('camera-name-changed')
                        })
                        .catch(e => {
                            console.log("HTTP error while changing camera name " + e);
                            this.$emit('camera-name-changed')
                        })
                    this.discardCameraNameChange();
                }
            },
            discardCameraNameChange() {
                this.isCameraNameEdit = false;
                this.newCameraName = "";
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
            openDuplicateDialog() {
                this.pipeIndexToDuplicate = this.currentPipelineIndex - 1;
                this.duplicateDialog = true;
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
                        this.handleInputWithIndex("addNewPipeline", this.newPipelineName);
                    }
                    this.discardPipelineNameChange();
                }
            },
            duplicatePipeline() {
                // if (!this.anotherCamera) {
                //     this.pipelineDuplicate.camera = -1
                // }
                this.handleInputWithIndex("duplicatePipeline", this.pipeIndexToDuplicate);
                // this.axios.post("http://" + this.$address + "/api/vision/duplicate", this.pipeIndexToDuplicate);

                this.closeDuplicateDialog();
            },
            closeDuplicateDialog() {
                this.duplicateDialog = false;
                this.pipeIndexToDuplicate = undefined;
            },
            discardPipelineNameChange() {
                this.namingDialog = false;
                this.isPipelineNameEdit = false;
                this.newPipelineName = "";
            },
        }

    }
</script>

<style scoped>

</style>