<template>
    <div>
        <div>
            <CVselect name="Camera" :list="$store.getters.cameraList" v-model="currentCameraIndex"
                      @input="handleInput('currentCamera',currentCameraIndex)"/>
            <CVnumberinput name="Diagonal FOV" v-model="cameraSettings.fov"/>
            <br>
            <CVnumberinput name="Camera pitch" v-model="cameraSettings.tilt" :step="0.01"/>
            <br>
            <v-btn style="margin-top:10px" small color="#4baf62" @click="sendCameraSettings">Save Camera Settings
            </v-btn>
        </div>
        <div style="margin-top: 15px">
            <span>3D Calibration</span>
            <v-divider color="white" style="margin-bottom: 10px"/>
            <v-row>
                <v-col>
                    <CVselect name="Resolution" v-model="resolutionIndex" :list="stringResolutionList"/>
                </v-col>
                <v-col>
                    <CVnumberinput name="Square Size (in)" v-model="squareSize"/>
                </v-col>
            </v-row>
            <v-row>
                <v-col>
                    <v-btn small :color="calibrationModeButton.color" @click="sendCalibrationMode"
                           :disabled="checkResolution">
                        {{calibrationModeButton.text}}
                    </v-btn>
                </v-col>
                <v-col>
                    <v-btn small :color="cancellationModeButton.color" @click="sendCalibrationFinish"
                           :disabled="checkCancellation">
                        {{cancellationModeButton.text}}
                    </v-btn>
                </v-col>
                <v-col>
                    <v-btn color="whitesmoke" small @click="downloadBoard">
                        Download Checkerboard
                    </v-btn>
                    <a ref="calibrationFile" style="color: black; text-decoration: none; display: none"
                       :href="require('../../assets/chessboard.png')"
                       download="Calibration Board.png"/>
                </v-col>
            </v-row>
            <v-row v-if="isCalibrating">
                <v-col>
                    <span>Snapshot Amount: {{snapshotAmount}}</span>
                </v-col>
            </v-row>
            <div v-if="isCalibrating">
                <v-checkbox v-model="isAdvanced" label="Advanced Menu" dark/>
                <div v-if="isAdvanced" >
                    <CVslider name="Exposure" v-model="$store.getters.pipeline.exposure" :min="0" :max="100"
                              @input="e=> handleInput('exposure', e)"/>
                    <CVslider name="Brightness" v-model="$store.getters.pipeline.brightness" :min="0" :max="100"
                              @input="e=> handleInput('brightness', e)"/>
                    <CVslider name="Gain" v-if="$store.getters.pipeline.gain !== -1"
                              v-model="$store.getters.pipeline.gain" :min="0" :max="100"
                              @input="e=> handleInput('gain', e)"/>
                    <CVselect name="FPS" v-model="$store.getters.pipeline.videoModeIndex" :list="stringFpsList"
                              @input="changeFps"/>
                </div>
            </div>
        </div>
        <v-snackbar v-model="snack" top :color="snackbar.color">
            <span>{{snackbar.text}}</span>
        </v-snackbar>
    </div>
</template>

<script>
    import CVselect from '../../components/common/cv-select'
    import CVnumberinput from '../../components/common/cv-number-input'
    import CVslider from '../../components/common/cv-slider'

    export default {
        name: 'CameraSettings',
        components: {
            CVselect,
            CVnumberinput,
            CVslider
        },
        data() {
            return {
                isCalibrating: false,
                resolutionIndex: undefined,
                calibrationModeButton: {
                    text: "Start Calibration",
                    color: "green"
                },
                cancellationModeButton: {
                    text: "Cancel Calibration",
                    color: "red"
                },
                snackbar: {
                    color: "success",
                    text: ""
                },
                squareSize: 1.0,
                snapshotAmount: 0,
                hasEnough: false,
                snack: false,
                isAdvanced: false
            }
        },
        methods: {
            downloadBoard() {
                this.axios.get("http://" + this.$address + require('../../assets/chessboard.png'), {responseType: 'blob'}).then((response) => {
                    require('downloadjs')(response.data, "Calibration Board", "image/png")
                })
            },
            changeFps() {
                this.handleInput('videoModeIndex', this.filteredFpsList[this.$store.getters.pipeline['videoModeIndex']]['actualIndex']);
            },
            sendCameraSettings() {
                const self = this;
                this.axios.post("http://" + this.$address + "/api/settings/camera", this.cameraSettings).then(
                    function (response) {
                        if (response.status === 200) {
                            self.$store.state.saveBar = true;
                        }
                    }
                )
            },
            sendCalibrationMode() {
                const self = this;
                let data = {};
                let connection_string = "/api/settings/";
                if (self.isCalibrating === true) {
                    connection_string += "snapshot"
                } else {
                    connection_string += "startCalibration";
                    data['resolution'] = this.filteredResolutionList[this.resolutionIndex].actualIndex;
                    data['squareSize'] = this.squareSize;
                    self.hasEnough = false;
                }
                this.axios.post("http://" + this.$address + connection_string, data).then(
                    function (response) {
                        if (response.status === 200) {
                            if (self.isCalibrating) {
                                self.snapshotAmount = response.data['snapshotCount'];
                                self.hasEnough = response.data['hasEnough'];
                                if (self.hasEnough === true) {
                                    self.cancellationModeButton.text = "Finish Calibration";
                                    self.cancellationModeButton.color = "green";
                                }
                            } else {
                                self.calibrationModeButton.text = "Take Snapshot";
                                self.isCalibrating = true;
                            }
                        }
                    }
                );
            },
            sendCalibrationFinish() {
                const self = this;
                let connection_string = "/api/settings/endCalibration";
                let data = {};
                data['squareSize'] = this.squareSize;
                self.axios.post("http://" + this.$address + connection_string, data).then((response) => {
                        if (response.status === 200) {
                            self.snackbar = {
                                color: "success",
                                text: "calibration successful. \n" +
                                    "accuracy: " + response.data['accuracy'].toFixed(5)
                            };
                            self.snack = true;
                        }
                        self.isCalibrating = false;
                        self.hasEnough = false;
                        self.snapshotAmount = 0;
                        self.calibrationModeButton.text = "Start Calibration";
                        self.cancellationModeButton.text = "Cancel Calibration";
                        self.cancellationModeButton.color = "red";
                    }
                ).catch(() => {
                    self.snackbar = {
                        color: "error",
                        text: "calibration failed"
                    };
                    self.snack = true;
                    self.isCalibrating = false;
                    self.hasEnough = false;
                    self.snapshotAmount = 0;
                    self.calibrationModeButton.text = "Start Calibration";
                    self.cancellationModeButton.text = "Cancel Calibration";
                    self.cancellationModeButton.color = "red";
                });
            }
        },
        computed: {
            checkResolution() {
                return this.resolutionIndex === undefined;
            },
            checkCancellation() {
                if (this.isCalibrating) {
                    return false
                } else if (this.checkResolution) {
                    return true;
                } else {
                    return true
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
            filteredResolutionList: {
                get() {
                    let tmp_list = [];
                    for (let i in this.$store.state.resolutionList) {
                        if (this.$store.state.resolutionList.hasOwnProperty(i)) {
                            let res = JSON.parse(JSON.stringify(this.$store.state.resolutionList[i]));
                            if (!tmp_list.some(e => e.width === res.width && e.height === res.height)) {
                                res['actualIndex'] = parseInt(i);
                                tmp_list.push(res);
                            }
                        }
                    }
                    return tmp_list;
                }
            },
            filteredFpsList() {
                let selectedRes = this.$store.state.resolutionList[this.resolutionIndex];
                let tmpList = [];
                for (let i in this.$store.state.resolutionList) {
                    if (this.$store.state.resolutionList.hasOwnProperty(i)) {
                        let res = JSON.parse(JSON.stringify(this.$store.state.resolutionList[i]));
                        if (!tmpList.some(e => e['fps'] === res['fps'])) {
                            if (res.width === selectedRes.width && res.height === selectedRes.height) {
                                res['actualIndex'] = parseInt(i);
                                tmpList.push(res);
                            }
                        }
                    }
                }
                return tmpList;
            },
            stringFpsList() {
                let tmp = [];
                for (let i of this.filteredFpsList) {
                    tmp.push(i['fps']);
                }
                return tmp;
            },
            stringResolutionList: {
                get() {
                    let tmp = [];
                    for (let i of this.filteredResolutionList) {
                        tmp.push(`${i['width']} X ${i['height']}`)
                    }
                    return tmp
                }
            },
            cameraSettings: {
                get() {
                    return this.$store.getters.cameraSettings;
                },
                set(value) {
                    this.$store.commit('cameraSettings', value);
                }
            }
        }
    }
</script>

<style lang="" scoped>

</style>