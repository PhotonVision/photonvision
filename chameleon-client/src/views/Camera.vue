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
                    <CVselect name="Pipeline"
                              :list="['Driver Mode'].concat(pipelineList)"
                              v-model="currentPipelineIndex"
                              @input="handleInput('currentPipeline',currentPipelineIndex - 1)"/>
                </v-col>
                <v-col :cols="1" class="colsClass" md="3" v-if="currentPipelineIndex !== 0">
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
        </div>
        <v-row>
            <!-- vision tabs -->
            <v-col cols="6" class="colsClass">
                <v-tabs fixed-tabs background-color="#212121" dark height="48" slider-color="#4baf62"
                        v-model="selectedTab" v-if="currentPipelineIndex !== 0">
                    <v-tab>Input</v-tab>
                    <v-tab>Threshold</v-tab>
                    <v-tab>Contours</v-tab>
                    <v-tab>Output</v-tab>
                    <v-tab>3D</v-tab>
                </v-tabs>
                <div v-else style="height: 48px"></div>
                <div style="padding-left:30px">
                    <keep-alive>
                        <!-- vision component -->
                        <component v-model="pipeline" :is="selectedComponent" ref="component" @update="$emit('save')"/>
                    </keep-alive>
                </div>
            </v-col>
            <v-col cols="6" class="colsClass">
                <div>
                    <!-- camera image tabs -->
                    <v-tabs background-color="#212121" dark height="48" slider-color="#4baf62" centered
                            style="padding-bottom:10px" v-model="isBinaryNumber"
                            @change="handleInput('isBinary',pipeline.isBinary)" v-if="currentPipelineIndex !== 0">
                        <v-tab>Normal</v-tab>
                        <v-tab>Threshold</v-tab>
                    </v-tabs>
                    <div v-else style="height: 58px"></div>
                    <!-- camera image stream -->
                    <div class="videoClass">
                        <v-row align="center">
                            <img id="CameraStream" style="display: block;margin: auto; width: 70%;height: 70%;"
                                 v-if="cameraList.length > 0"
                                 :src="streamAddress" @click="onImageClick"
                                 crossorigin="Anonymous"/>
                            <span style="display: block;margin: auto; width: 70%;height: 70%;" v-else>No Cameras Are connected</span>
                        </v-row>
                        <v-row justify="end">
                            <span style="margin-right: 45px">FPS:{{parseFloat(fps).toFixed(2)}}</span>
                        </v-row>
                        <v-row align="center">
                            <v-simple-table
                                    style="text-align: center;background-color: transparent; display: block;margin: auto"
                                    dense dark>
                                <template v-slot:default>
                                    <thead>
                                    <tr>
                                        <th class="text-center">Target</th>
                                        <th class="text-center">Pitch</th>
                                        <th class="text-center">Yaw</th>
                                        <th class="text-center">Area</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <tr v-for="(value, index) in targets" :key="index">
                                        <td>{{ index}}</td>
                                        <td>{{ parseFloat(value.pitch).toFixed(2) }}</td>
                                        <td>{{ parseFloat(value.yaw).toFixed(2) }}</td>
                                        <td>{{ parseFloat(value.area).toFixed(2) }}</td>
                                    </tr>
                                    </tbody>
                                </template>
                            </v-simple-table>
                        </v-row>
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
    import pnpTab from './CameraViewes/3D'
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
            pnpTab,
            CVselect,
            CVicon,
            CVinput
        },
        methods: {
            onImageClick(event) {
                if (this.selectedTab === 1) {
                    this.$refs.component.onClick(event);
                }
            },
            toCameraNameChange() {
                this.newCameraName = this.cameraList[this.currentCameraIndex];
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
                this.newPipelineName = this.pipelineList[this.currentPipelineIndex - 1];
                this.isPipelineNameEdit = true;
                this.namingDialog = true;
            },
            toCreatePipeline() {
                this.newPipelineName = "New Pipeline";
                this.isPipelineNameEdit = false;
                this.namingDialog = true;
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
            discardPipelineNameChange() {
                this.namingDialog = false;
                this.isPipelineNameEdit = false;
                this.newPipelineName = "";
            },
            duplicatePipeline() {
                if (!this.anotherCamera) {
                    this.pipelineDuplicate.camera = -1
                }
                // this.handleInput("duplicatePipeline", this.pipelineDuplicate);
                this.axios.post("http://" + this.$address + "/api/vision/duplicate", this.pipelineDuplicate);
                this.closeDuplicateDialog();
            },
            openDuplicateDialog() {
                this.pipelineDuplicate = {
                    pipeline: this.currentPipelineIndex - 1,
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
                re: RegExp('^[A-Za-z0-9 \\-)(]*[A-Za-z0-9][A-Za-z0-9 \\-)(.]*$'),
                selectedTab: 0,
                // camera edit variables
                isCameraNameEdit: false,
                newCameraName: "",
                cameraNameError: "",
                // pipeline edit variables
                isPipelineNameEdit: false,
                namingDialog: false,
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
                    if (this.re.test(this.newCameraName)) {
                        for (let cam in this.cameraList) {
                            if (this.newCameraName === this.cameraList[cam]) {
                                return "Camera by that name already Exists"
                            }
                        }
                    } else {
                        return "Camera name can only contain letters, numbers and spaces"
                    }
                }
                return ""
            },
            checkPipelineName() {

                if (this.newPipelineName !== this.pipelineList[this.currentPipelineIndex - 1] || this.isPipelineNameEdit === false) {
                    if (this.re.test(this.newPipelineName)) {
                        for (let pipe in this.pipelineList) {
                            if (this.newPipelineName === this.pipelineList[pipe]) {
                                return "A pipeline with this name already exists"
                            }
                        }

                    } else {
                        return "Pipeline name can only contain letters, numbers, and spaces"
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
                    if (this.currentPipelineIndex === 0) {
                        return "InputTab"
                    }
                    switch (this.selectedTab) {
                        case 0:
                            return "InputTab";
                        case 1:
                            return "ThresholdTab";
                        case 2:
                            return "ContoursTab";
                        case 3:
                            return "OutputTab";
                        case 4:
                            return "pnpTab";
                    }
                    return "";
                }
            },
            targets: {
                get: function () {

                    return this.$store.state.point.targets;
                }
            },
            fps: {
                get() {
                    return this.$store.state.point.fps;
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
                    return this.$store.state.currentPipelineIndex + 1;
                },
                set(value) {
                    this.$store.commit('currentPipelineIndex', value - 1);
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


    .tableClass {
        padding-top: 5px;
        width: 70%;
        text-align: center;
    }

    th {
        width: 80px;
        text-align: center;
    }

</style>