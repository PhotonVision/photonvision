<template>
    <div>
        <div>
            <v-row align="center">
                <v-col :cols="3" class="colsClass">
                    <div style="padding-left:30px">
                        <CVselect v-if="isCameraNameEdit === false" name="Camera" v-model="currentCameraIndex"
                                  :list="cameraList" @input="handleInput('currentCamera',currentCameraIndex)"/>
                        <CVinput v-else name="Camera" v-model="newCameraName" @Enter="saveCameraNameChange"
                                 :errorMessage="checkCameraName"/>
                    </div>
                </v-col>
                <v-col :cols="1">
                    <CVicon color="#c5c5c5" v-if="isCameraNameEdit === false" hover text="edit"
                            @click="toCameraNameChange" tooltip="Edit camera name"/>
                    <div v-else>
                        <CVicon color="#c5c5c5" style="display: inline-block;" hover text="save"
                                @click="saveCameraNameChange" tooltip="Save Camera Name"/>
                        <CVicon color="error" style="display: inline-block;" hover text="close"
                                @click="discardCameraNameChange" tooltip="Discard Changes"/>
                    </div>
                </v-col>
                <v-col :cols="3" class="colsClass">
                    <CVselect v-if="isPipelineEdit === false" name="Pipeline" :list="pipelineList"
                              v-model="currentPipelineIndex"
                              @input="handleInput('currentPipeline',currentPipelineIndex)"/>
                    <CVinput v-else name="Pipeline" v-model="newPipelineName" @Enter="savePipelineNameChange"/>
                </v-col>
                <v-col :cols="1" class="colsClass" md="3">
                    <v-menu v-if="isPipelineEdit === false" offset-y dark auto>
                        <template v-slot:activator="{ on }">
                            <v-icon color="white" v-on="on">menu</v-icon>
                        </template>
                        <v-list dense>
                            <v-list-item @click="toPipelineNameChange">
                                <v-list-item-title>
                                    <CVicon color="#c5c5c5" :right="true" text="edit" tooltip="Edit pipeline name"/>
                                </v-list-item-title>
                            </v-list-item>
                            <v-list-item @click="handleInput('command','addNewPipeline')">
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
                    <div v-else>
                        <CVicon color="#c5c5c5" style="display: inline-block;" hover text="save"
                                @click="savePipelineNameChange" tooltip="Save Pipeline Name"/>
                        <CVicon color="error" style="display: inline-block;" hover text="close"
                                @click="discardPipelineNameChange" tooltip="Discard Changes"/>
                    </div>
                </v-col>

                <v-btn style="position: absolute; top:5px;right: 0;" tile color="#4baf62"
                       @click="handleInput('command','save')">
                    <v-icon>save</v-icon>
                    Save
                </v-btn>

            </v-row>
        </div>
        <v-row>
            <!-- vision tabs -->
            <v-col cols="6" class="colsClass">
                <v-tabs fixed-tabs background-color="#212121" dark height="48" slider-color="#4baf62"
                        v-model="selectedTab">
                    <v-tab>Input</v-tab>
                    <v-tab>Threshold</v-tab>
                    <v-tab>Contours</v-tab>
                    <v-tab>Output</v-tab>
                </v-tabs>
                <div style="padding-left:30px">
                    <keep-alive>
                        <!-- vision component -->
                        <component v-model="pipeline" :is="selectedComponent" @update="$emit('save')"/>
                    </keep-alive>
                </div>
            </v-col>
            <v-col cols="6" class="colsClass">
                <div>
                    <!-- camera image tabs -->
                    <v-tabs background-color="#212121" dark height="48" slider-color="#4baf62" centered
                            style="padding-bottom:10px" v-model="isBinaryNumber"
                            @change="handleInput('isBinary',pipeline.isBinary)">
                        <v-tab>Normal</v-tab>
                        <v-tab>Threshold</v-tab>
                    </v-tabs>
                    <!-- camera image stream -->
                    <div class="videoClass">
                        <img v-if="cameraList.length > 0" :src="streamAddress">
                        <span v-else>No Cameras Are connected</span>
                        <h5 id="Point">{{point}}</h5>
                    </div>
                </div>
            </v-col>
        </v-row>
        <!-- pipeline duplicate dialog -->
        <v-dialog dark v-model="duplicateDialog" width="500" height="357">
            <v-card dark>
                <v-card-title class="headline" primary-title>Duplicate Pipeline</v-card-title>
                <v-card-text>
                    <CVselect name="Pipeline" :list="pipelineList" v-model="pipelineDuplicate.pipeline"/>
                    <v-checkbox v-if="cameraList.length > 1" dark :label="'To another camera'" v-model="anotherCamera"/>
                    <CVselect v-if="anotherCamera === true" name="Camera" v-model="pipelineDuplicate.camera"
                              :list="cameraList"/>
                </v-card-text>
                <v-divider>
                </v-divider>
                <v-card-actions>
                    <v-spacer/>
                    <v-btn color="#4baf62" text @click="duplicatePipeline">Duplicate</v-btn>
                    <v-btn color="error" text @click="closeDuplicateDialog">Cancels</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <!-- snack bar -->
        <v-snackbar :timeout="3000" v-model="snackbar" top color="error">
            <span style="color:#000">Can not remove the only pipeline!</span>
            <v-btn color="black" text @click="snackbar = false">Close</v-btn>
        </v-snackbar>
    </div>
</template>

<script>
    import InputTab from './CameraViewes/InputTab'
    import ThresholdTab from './CameraViewes/ThresholdTab'
    import ContoursTab from './CameraViewes/ContoursTab'
    import OutputTab from './CameraViewes/OutputTab'
    import CVselect from '../components/cv-select'
    import CVicon from '../components/cv-icon'
    import CVinput from '../components/cv-input'

    export default {
        name: 'CameraTab',
        components: {
            InputTab,
            ThresholdTab,
            ContoursTab,
            OutputTab,
            CVselect,
            CVicon,
            CVinput
        },
        methods: {
            toCameraNameChange() {
                this.newCameraName = this.cameraList[this.currentCameraIndex];
                this.isCameraNameEdit = true;
            },
            saveCameraNameChange() {
                if (this.cameraNameError === "") {
                    this.handleInput("changeCameraName", this.newCameraName);
                    this.discardCameraNameChange();
                }
            },
            discardCameraNameChange() {
                this.isCameraNameEdit = false;
                this.newCameraName = "";
            },
            toPipelineNameChange() {
                this.newPipelineName = this.pipelineList[this.currentPipelineIndex];
                this.isPipelineEdit = true;
            },
            savePipelineNameChange() {
                this.handleInput("changePipelineName", this.newPipelineName);
                this.discardPipelineNameChange();
            },
            discardPipelineNameChange() {
                this.isPipelineEdit = false;
                this.newPipelineName = "";
            },
            duplicatePipeline() {
                if (!this.anotherCamera) {
                    this.pipelineDuplicate.camera = -1
                }
                this.handleInput("duplicatePipeline", this.pipelineDuplicate);
                this.closeDuplicateDialog();
            },
            openDuplicateDialog() {
                this.pipelineDuplicate = {
                    pipeline: this.currentPipelineIndex,
                    camera: -1
                };
                this.duplicateDialog = true;
            },
            closeDuplicateDialog() {
                this.duplicateDialog = false;
                this.pipelineDuplicate = {
                    pipeline: undefined,
                    camera: -1
                }
            },
            deleteCurrentPipeline() {
                if (this.pipelineList.length > 1) {
                    this.handleInput('command', 'deleteCurrentPipeline');
                } else {
                    this.snackbar = true;
                }
            }
        },
        data() {
            return {
                selectedTab: 1,
                // camera edit variables
                isCameraNameEdit: false,
                newCameraName: "",
                cameraNameError: "",
                // pipeline edit variables
                isPipelineEdit: false,
                newPipelineName: "",
                duplicateDialog: false,
                anotherCamera: false,
                pipelineDuplicate: {
                    pipeline: undefined,
                    camera: -1
                },
                snackbar: false,

            }
        },
        computed: {
            checkCameraName() {
                if (this.newCameraName !== this.cameraList[this.currentCameraIndex]) {
                    for (let cam in this.cameraList) {
                        if (this.newCameraName === this.cameraList[cam]) {
                            return "Camera by that name already Exists"
                        }
                    }
                }
                return ""
            },
            isBinaryNumber: {
                get() {
                    return this.pipeline.isBinary ? 1 : 0
                },
                set(value) {
                    this.pipeline.isBinary = !!value;
                }
            },
            selectedComponent: {
                get() {
                    switch (this.selectedTab) {
                        case 0:
                            return "InputTab";
                        case 1:
                            return "ThresholdTab";
                        case 2:
                            return "ContoursTab";
                        case 3:
                            return "OutputTab";
                    }
                    return "";
                }
            },
            point: {
                get: function () {
                    let p = this.$store.state.point.calculated;
                    let fps = this.$store.state.point.fps;
                    if (p !== undefined) {
                        return ("Pitch: " + parseFloat(p['pitch']).toFixed(2) + " Yaw: " + parseFloat(p['yaw']).toFixed(2) + " FPS: " + fps.toFixed(2))
                    } else {
                        return undefined;
                    }
                }
            },
            currentCameraIndex: {
                get() {
                    return this.$store.state.currentCameraIndex;
                },
                set(value) {
                    this.$store.commit('currentCameraIndex', value);
                }
            },
            currentPipelineIndex: {
                get() {
                    return this.$store.state.currentPipelineIndex;
                },
                set(value) {
                    this.$store.commit('currentPipelineIndex', value);
                }
            },
            cameraList: {
                get() {
                    return this.$store.state.cameraList;
                }
            },
            pipelineList: {
                get() {
                    return this.$store.state.pipelineList;
                }
            },
            pipeline: {
                get() {
                    return this.$store.state.pipeline;
                }
            },
            streamAddress: {
                get: function () {
                    return "http://" + location.hostname + ":" + this.$store.state.port + "/stream.mjpg";
                }
            },
        }
    }
</script>

<style scoped>
    .colsClass {
        padding: 0 !important;

    }

    .videoClass {
        text-align: center;
    }

    .videoClass img {
        height: auto !important;
        width: 70%;
        vertical-align: middle;
    }

    #Point {
        padding-top: 5px;
        text-align: center;
        color: #f4f4f4;
    }
</style>