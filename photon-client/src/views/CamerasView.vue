<template>
  <div>
    <v-row
      no-gutters
      class="pa-3"
    >
      <v-col
        cols="12"
        md="7"
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
              v-if="cameraSettings.isFovConfigurable"
              v-model="cameraSettings.fov"
              tooltip="Field of view (in degrees) of the camera measured across the diagonal of the frame"
              name="Diagonal FOV"
            />
            <br>
            <CVnumberinput
              v-model="cameraSettings.tiltDegrees"
              name="Camera pitch"
              tooltip="How many degrees above the horizontal the physical camera is tilted"
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
            <CVselect
              v-model="calibrationVideoMode"
              name="Resolution"
              :list="stringResolutionList"
              :disabled="isCalibrating"
            />
            <br>
            <v-row align-self="center">
              <v-col cols="5">
                <CVnumberinput
                  v-model="squareSize"
                  name="Pattern Spacing (in)"
                  tooltip="Spacing between pattern features in inches"
                  label-cols="unset"
                  :disabled="isCalibrating"
                />
              </v-col>
              <v-row cols="7">
                <v-col>
                  <CVnumberinput
                    v-model="boardWidth"
                    name="Board width"
                    tooltip="Width of the board in dots or corners. With the standard chessboard, this is usually 7."
                    label-cols="7"
                    :disabled="isCalibrating"
                  />
                </v-col>
                <v-col>
                  <CVnumberinput
                    v-model="boardHeight"
                    name="Board height"
                    tooltip="Height of the board in dots or corners. With the standard chessboard, this is usually 7."
                    label-cols="7"
                    :disabled="isCalibrating"
                  />
                </v-col>
              </v-row>
              <v-col cols="4">
                <CVselect
                  v-model="boardType"
                  name="Board Type"
                  align-self="center"
                  select-cols="7"
                  :list="['Chessboard', 'Dot Grid']"
                />
              </v-col>
            </v-row>
            <v-row>
              <v-col>
                <v-btn
                  small
                  color="secondary"
                  :disabled="checkValidConfig"
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
                <span>Snapshots: {{ snapshotAmount }} of at least {{ minSnapshots }}</span>
              </v-col>
            </v-row>
            <div v-if="isCalibrating">
              <CVslider
                v-model="$store.getters.currentPipelineSettings.cameraExposure"
                name="Exposure"
                :min="0"
                :max="100"
                @input="e => handlePipelineUpdate('cameraExposure', e)"
              />
              <CVslider
                v-model="this.$store.getters.currentPipelineSettings.cameraBrightness"
                name="Brightness"
                :min="0"
                :max="100"
                @input="e => handlePipelineUpdate('cameraBrightness', e)"
              />
              <CVslider
                v-if="$store.getters.currentPipelineSettings.cameraGain !== -1"
                v-model="$store.getters.currentPipelineSettings.cameraGain"
                name="Gain"
                :min="0"
                :max="100"
                @input="e => handlePipelineUpdate('cameraGain', e)"
              />
              <!--                <CVselect-->
              <!--                  v-model="$store.getters.currentPipelineSettings.cameraVideoModeIndex"-->
              <!--                  name="FPS"-->
              <!--                  :list="stringFpsList"-->
              <!--                  @input="changeFps"-->
              <!--                />-->
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
            snack: false,
        }
    },
    computed: {
        checkValidConfig() {
            return false;
        },
        checkCancellation() {
            if (this.isCalibrating) {
                return false
            } else if (this.checkValidConfig) {
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

        // Makes sure there's only one entry per resolution
        filteredResolutionList: {
            get() {
                let list = this.$store.getters.videoFormatList;
                let filtered = [];
                list.forEach((it, i) => {
                    if (!filtered.some(e => e.width === it.width && e.height === it.height)) {
                        it['index'] = i;
                        filtered.push(it)
                    }
                })
                return filtered
            }
        },

        stringResolutionList: {
            get() {
                return this.filteredResolutionList.map(res => `${res['width']} X ${res['height']}`)
            }
        },

        cameraSettings: {
            get() {
                return this.$store.getters.currentCameraSettings;
            },
            set(value) {
                this.$store.commit('cameraSettings', value);
            }
        },

        calibrationVideoMode: {
            get() { return this.calibrationData.videoModeIndex },
            set(value) { this.$store.commit('mutateCalibrationState', {['videoModeIndex']: value}) }
        },
        boardType: {
            get() {
                return this.calibrationData.boardType
            },
            set(value) {
                this.$store.commit('mutateCalibrationState', {['boardType']: value})
            }
        },
        snapshotAmount: {
            get() {
                return this.calibrationData.count
            }
        },
        minSnapshots: {
            get() {
                return this.calibrationData.minCount
            }
        },
        hasEnough: {
            get() {
                return this.calibrationData.hasEnough
            }
        },
        boardWidth: {
            get() {
                return this.calibrationData.boardWidth
            },
            set(value) {
                this.$store.commit('mutateCalibrationState', {['boardWidth']: value})
            }
        },
        boardHeight: {
            get() {
                return this.calibrationData.boardHeight
            },
            set(value) {
                this.$store.commit('mutateCalibrationState', {['boardHeight']: value})
            }
        },
        calibrationData: {
            get() {
                return this.$store.state.calibrationData
            }
        },
        isCalibrating: {
            get() {
                return this.$store.getters.currentPipelineIndex === -2;
            }
        }
    },
    methods: {
        downloadBoard() {
            this.axios.get("http://" + this.$address + require('../assets/chessboard.png'), {responseType: 'blob'}).then((response) => {
                require('downloadjs')(response.data, "Calibration Board", "image/png")
            })
        },
        sendCameraSettings() {
            const self = this;
            this.axios.post("http://" + this.$address + "/api/settings/camera", {
                "settings": this.cameraSettings,
                "index": this.$store.state.currentCameraIndex
            }).then(
                function (response) {
                    if (response.status === 200) {
                        self.$store.state.saveBar = true;
                    }
                }
            )
        },

        sendCalibrationMode() {
            let data = {
                ['cameraIndex']: this.$store.state.currentCameraIndex
            };

            if (this.isCalibrating === true) {
                data['takeCalibrationSnapshot'] = true
            } else {
                const calData = this.calibrationData
                calData.isCalibrating = true
                data['startPnpCalibration'] = calData
                this.calibrationModeButton.text = "Take Snapshot";
            }

            this.$socket.send(this.$msgPack.encode(data));
        },
        sendCalibrationFinish() {
            const self = this;
            let connection_string = "/api/settings/endCalibration";
            self.axios.post("http://" + this.$address + connection_string, this.$store.getters.currentCameraIndex).then((response) => {
                    if (response.status === 200) {
                        self.snackbar = {
                            color: "success",
                            text: "calibration successful. \n" +
                                "accuracy: " + response.data['accuracy'].toFixed(5)
                        };
                        self.snack = true;
                    }
                    self.isCalibrating = false;
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