<template>
  <div>
    <v-row
      no-gutters
      class="pa-3"
    >
      <v-col
        cols="12"
        md="7"
        style="max-width: 1400px;"
      >
        <v-card
          class="mb-3 pr-6 pb-3"
          color="primary"
          dark
        >
          <v-card-title>Camera Settings</v-card-title>
          <div class="ml-5">
            <CVselect
              v-model="currentCameraIndex"
              name="Camera"
              select-cols="10"
              :list="$store.getters.cameraList"
              @input="handleInput('currentCamera',currentCameraIndex)"
            />
            <CVnumberinput
              v-model="cameraSettings.fov"
              name="Diagonal FOV"
            />
            <br>
            <CVnumberinput
              v-model="cameraSettings.tilt"
              name="Camera pitch"
              :step="0.01"
            />
            <br>
            <v-btn
              style="margin-top:10px"
              small
              color="secondary"
              @click="sendCameraSettings"
            >
              <v-icon left>
                mdi-content-save
              </v-icon>
              Save Camera Settings
            </v-btn>
          </div>
        </v-card>
        <v-card
          class="pr-6 pb-3"
          color="primary"
          dark
        >
          <v-card-title>Camera Calibration</v-card-title>
          <div class="ml-5">
            <v-row>
              <v-col cols="8">
                <CVselect
                  v-model="resolutionIndex"
                  name="Resolution"
                  :list="stringResolutionList"
                />
              </v-col>
              <v-col
                cols="4"
                align-self="center"
              >
                <CVnumberinput
                  v-model="squareSize"
                  name="Square Size (in)"
                  label-cols="unset"
                />
              </v-col>
            </v-row>
            <v-row>
              <v-col>
                <v-btn
                  small
                  color="secondary"
                  :disabled="checkResolution"
                  @click="sendCalibrationMode"
                >
                  {{ calibrationModeButton.text }}
                </v-btn>
              </v-col>
              <v-col>
                <v-btn
                  small
                  color="red"
                  :disabled="checkCancellation"
                  @click="sendCalibrationFinish"
                >
                  {{ cancellationModeButton.text }}
                </v-btn>
              </v-col>
              <v-col>
                <v-btn
                  color="accent"
                  small
                  outlined
                  @click="downloadBoard"
                >
                  <v-icon left>
                    mdi-download
                  </v-icon>
                  Download Checkerboard
                </v-btn>
                <a
                  ref="calibrationFile"
                  style="color: black; text-decoration: none; display: none"
                  :href="require('../assets/chessboard.png')"
                  download="Calibration Board.png"
                />
              </v-col>
            </v-row>
            <v-row v-if="isCalibrating">
              <v-col>
                <span>Snapshot Amount: {{ snapshotAmount }}</span>
              </v-col>
            </v-row>
            <div v-if="isCalibrating">
              <v-checkbox
                v-model="isAdvanced"
                label="Advanced Menu"
                dark
              />
              <div v-if="isAdvanced">
                <CVslider
                  v-model="$store.getters.pipeline.exposure"
                  name="Exposure"
                  :min="0"
                  :max="100"
                  @input="e=> handleInput('exposure', e)"
                />
                <CVslider
                  v-model="$store.getters.pipeline.brightness"
                  name="Brightness"
                  :min="0"
                  :max="100"
                  @input="e=> handleInput('brightness', e)"
                />
                <CVslider
                  v-if="$store.getters.pipeline.gain !== -1"
                  v-model="$store.getters.pipeline.gain"
                  name="Gain"
                  :min="0"
                  :max="100"
                  @input="e=> handleInput('gain', e)"
                />
                <CVselect
                  v-model="$store.getters.pipeline.videoModeIndex"
                  name="FPS"
                  :list="stringFpsList"
                  @input="changeFps"
                />
              </div>
            </div>
          </div>
        </v-card>
      </v-col>
      <v-col
        class="pl-md-3 pt-3 pt-md-0"
        cols="12"
        md="5"
      >
        <CVimage
          :address="$store.getters.streamAddress[1]"
          :disconnected="!$store.state.backendConnected"
          scale="100"
          style="border-radius: 5px;"
        />
      </v-col>
    </v-row>
    <v-snackbar
      v-model="snack"
      top
      :color="snackbar.color"
    >
      <span>{{ snackbar.text }}</span>
    </v-snackbar>
  </div>
</template>

<script>
    import CVselect from '../components/common/cv-select';
    import CVnumberinput from '../components/common/cv-number-input';
    import CVslider from '../components/common/cv-slider';
    import CVimage from "../components/common/cv-image";

    export default {
        name: 'Cameras',
        components: {
            CVselect,
            CVnumberinput,
            CVslider,
            CVimage
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
                    return this.$store.getters.currentCameraSettings;
                },
                set(value) {
                    this.$store.commit('cameraSettings', value);
                }
            }
        },
        methods: {
            downloadBoard() {
                this.axios.get("http://" + this.$address + require('../assets/chessboard.png'), {responseType: 'blob'}).then((response) => {
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
        }
    }
</script>

<style lang="" scoped>

</style>