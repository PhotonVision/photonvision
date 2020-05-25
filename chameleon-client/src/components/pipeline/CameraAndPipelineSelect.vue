<template>
    <div>
        <v-row align="center">
            <v-col :cols="3" class="">
                <div style="padding-left:30px">
                    <CVselect v-if="isCameraNameEdit === false" name="Camera" v-model="currentCameraIndex"
                              :list="$store.getters.cameraList"
                              @input="handleInput('currentCamera',currentCameraIndex)"/>
                    <CVinput v-else name="Camera" v-model="newCameraName" @Enter="saveCameraNameChange"
                             :errorMessage="checkCameraName"/>
                </div>
            </v-col>
            <v-col :cols="1">
                <CVicon color="#c5c5c5" v-if="isCameraNameEdit === false" :hover="true" text="edit"
                        @click="toCameraNameChange" tooltip="Edit camera name"/>
                <div v-else>
                    <CVicon color="#c5c5c5" style="display: inline-block;" :hover="true" text="save"
                            @click="saveCameraNameChange" tooltip="Save Camera Name"/>
                    <CVicon color="error" style="display: inline-block;" :hover="true" text="close"
                            @click="discardCameraNameChange" tooltip="Discard Changes"/>
                </div>
            </v-col>
            <v-col :cols="3" class="">
                <CVselect name="Pipeline"
                          :list="['Driver Mode'].concat($store.getters.pipelineList)"
                          v-model="currentPipelineIndex"
                          @input="handleInput('currentPipeline',currentPipelineIndex - 1)"/>
            </v-col>
            <v-col :cols="1" class="" md="3" v-if="currentPipelineIndex !== 0">
                <v-menu offset-y dark auto>
                    <template v-slot:activator="{ on }">
                        <v-icon color="white" v-on="on">menu</v-icon>
                    </template>
                    <v-list dense>
                        <v-list-item @click="toPipelineNameChange">
                            <v-list-item-title>
                                <CVicon color="#c5c5c5" :right="true" text="edit" tooltip="Edit pipeline name"/>
                            </v-list-item-title>
                        </v-list-item>
                        <v-list-item @click="toCreatePipeline">
                            <v-list-item-title>
                                <CVicon color="#c5c5c5" :right="true" text="add" tooltip="Add new pipeline"/>
                            </v-list-item-title>
                        </v-list-item>
                        <v-list-item @click="deleteCurrentPipeline">
                            <v-list-item-title>
                                <CVicon color="red darken-2" :right="true" text="delete"
                                        tooltip="Delete pipeline"/>
                            </v-list-item-title>
                        </v-list-item>
                        <v-list-item @click="openDuplicateDialog">
                            <v-list-item-title>
                                <CVicon color="#c5c5c5" :right="true" text="mdi-content-copy"
                                        tooltip="Duplicate pipeline"/>
                            </v-list-item-title>
                        </v-list-item>
                    </v-list>
                </v-menu>
            </v-col>

            <v-btn style="position: absolute; top:5px;right: 0;" tile color="#4baf62"
                   @click="handleInput('command','save')">
                <v-icon>save</v-icon>
                Save
            </v-btn>
        </v-row>
        <!--pipeline duplicate dialog-->
        <v-dialog dark v-model="duplicateDialog" width="500" height="357">
            <v-card dark>
                <v-card-title class="headline" primary-title>Duplicate Pipeline</v-card-title>
                <v-card-text>
                    <CVselect name="Pipeline" :list="$store.getters.pipelineList" v-model="pipelineDuplicate.pipeline"/>
                    <v-checkbox v-if="$store.getters.cameraList.length > 1" dark :label="'To another camera'"
                                v-model="anotherCamera"/>
                    <CVselect v-if="anotherCamera === true" name="Camera" v-model="pipelineDuplicate.camera"
                              :list="$store.getters.cameraList"/>
                </v-card-text>
                <v-divider>
                </v-divider>
                <v-card-actions>
                    <v-spacer/>
                    <v-btn color="#4baf62" @click="duplicatePipeline">Duplicate</v-btn>
                    <v-btn color="error" @click="closeDuplicateDialog">Cancel</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <!--pipeline naming dialog-->
        <v-dialog dark v-model="namingDialog" width="500" height="357">
            <v-card dark>
                <v-card-title class="headline" primary-title>Pipeline Name</v-card-title>
                <v-card-text>
                    <CVinput name="Pipeline" :error-message="checkPipelineName" v-model="newPipelineName"
                             @Enter="savePipelineNameChange"/>
                </v-card-text>
                <v-divider>
                </v-divider>
                <v-card-actions>
                    <v-spacer/>
                    <v-btn color="#4baf62" @click="savePipelineNameChange" :disabled="checkPipelineName !==''">Save
                    </v-btn>
                    <v-btn color="error" @click="discardPipelineNameChange">Cancel</v-btn>
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
                anotherCamera: false,
                pipelineDuplicate: {
                    pipeline: undefined,
                    camera: -1
                },
            }
        },
        methods: {
            toCameraNameChange() {
                this.newCameraName = this.$store.getters.cameraList[this.currentCameraIndex];
                this.isCameraNameEdit = true;
            },
            saveCameraNameChange() {
                if (this.checkCameraName === "") {
                    this.handleInput("changeCameraName", this.newCameraName);
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
                this.pipelineDuplicate = {
                    pipeline: this.currentPipelineIndex - 1,
                    camera: -1
                };
                this.duplicateDialog = true;
            },
            deleteCurrentPipeline() {
                if (this.$store.getters.pipelineList.length > 1) {
                    this.handleInput('command', 'deleteCurrentPipeline');
                } else {
                    this.snackbar = true;
                }
            },
            savePipelineNameChange() {
                if (this.checkPipelineName === "") {
                    if (this.isPipelineNameEdit) {
                        this.handleInput("changePipelineName", this.newPipelineName);
                    } else {
                        this.handleInput("addNewPipeline", this.newPipelineName);
                    }
                    this.discardPipelineNameChange();
                }
            },
            duplicatePipeline() {
                if (!this.anotherCamera) {
                    this.pipelineDuplicate.camera = -1
                }
                // this.handleInput("duplicatePipeline", this.pipelineDuplicate);
                this.axios.post("http://" + this.$address + "/api/vision/duplicate", this.pipelineDuplicate);
                this.closeDuplicateDialog();
            },
            closeDuplicateDialog() {
                this.duplicateDialog = false;
                this.pipelineDuplicate = {
                    pipeline: undefined,
                    camera: -1
                }
            },
            discardPipelineNameChange() {
                this.namingDialog = false;
                this.isPipelineNameEdit = false;
                this.newPipelineName = "";
            },
        },
        computed: {
            checkCameraName() {
                if (this.newCameraName !== this.$store.getters.cameraList[this.currentCameraIndex]) {
                    if (this.re.test(this.newCameraName)) {
                        for (let cam in this.cameraList) {
                            if (this.cameraList.hasOwnProperty(cam)) {
                                if (this.newCameraName === this.cameraList[cam]) {
                                    return "Camera by that name already Exists"
                                }
                            }
                        }
                    } else {
                        return "Camera name can only contain letters, numbers and spaces"
                    }
                }
                return ""
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
                        return "Pipeline name can only contain letters, numbers, and spaces"
                    }
                }
                return ""
            },
            currentCameraIndex: {
                get() {
                    return this.$store.getters.currentCameraIndex;
                },
                set(value) {
                    this.$store.commit('currentPipelineIndex', value - 1);
                }
            },
            currentPipelineIndex: {
                get() {
                    return this.$store.getters.currentPipelineIndex + 1;
                },
                set(value) {
                    this.$store.commit('currentPipelineIndex', value - 1);
                }
            }
        }

    }
</script>

<style scoped>

</style>