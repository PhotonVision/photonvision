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
        <!-- Camera card -->
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

        <!-- Calibration card -->
        <v-card
          class="pr-6 pb-3"
          color="primary"
          dark
        >
          <v-card-title>Camera Calibration</v-card-title>

          <div class="ml-5">
            <v-row>
              <!-- Calibration input -->
              <v-col
                cols="12"
                md="6"
              >
                <CVselect
                  v-model="selectedFilteredResIndex"
                  name="Resolution"
                  select-cols="7"
                  :list="stringResolutionList"
                  :disabled="isCalibrating"
                  tooltip="Resolution to calibrate at (you will have to calibrate every resolution you use 3D mode on)"
                />
                <CVselect
                  v-model="boardType"
                  name="Board Type"
                  select-cols="7"
                  :list="['Chessboard', 'Dot Grid']"
                  :disabled="isCalibrating"
                  tooltip="Calibration board pattern to use"
                />
                <CVnumberinput
                  v-model="squareSizeIn"
                  name="Pattern Spacing (in)"
                  label-cols="5"
                  tooltip="Spacing between pattern features in inches"
                  :disabled="isCalibrating"
                />
                <CVnumberinput
                  v-model="boardWidth"
                  name="Board width"
                  label-cols="5"
                  tooltip="Width of the board in dots or corners; with the standard chessboard, this is usually 7"
                  :disabled="isCalibrating"
                />
                <CVnumberinput
                  v-model="boardHeight"
                  name="Board height"
                  label-cols="5"
                  tooltip="Height of the board in dots or corners; with the standard chessboard, this is usually 7"
                  :disabled="isCalibrating"
                />
              </v-col>

              <!-- Calibrated table -->
              <v-col
                cols="12"
                md="6"
              >
                <v-row
                  align="start"
                  class="pb-4"
                >
                  <v-simple-table
                    fixed-header
                    height="100%"
                    dense
                  >
                    <thead style="font-size: 1.25rem;">
                      <tr>
                        <th class="text-center">
                          <tooltipped-label text="Resolution" />
                        </th>
                        <th class="text-center">
                          <tooltipped-label
                            tooltip="Average reprojection error of the calibration, in pixels"
                            text="Mean Error"
                          />
                        </th>
                        <th class="text-center">
                          <tooltipped-label
                            tooltip="Standard deviation of the mean error, in pixels"
                            text="Standard Deviation"
                          />
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="(value, index) in filteredResolutionList"
                        :key="index"
                      >
                        <td> {{ value.width }} X {{ value.height }} </td>
                        <td>
                          {{ isCalibrated(value) ? value.mean.toFixed(2) + "px" : "—" }}
                        </td>
                        <td> {{ isCalibrated(value) ? value.standardDeviation.toFixed(2) + "px" : "—" }} </td>
                      </tr>
                    </tbody>
                  </v-simple-table>
                </v-row>
                <v-row justify="center">
                  <v-chip
                    v-show="isCalibrating"
                    label
                    :color="snapshotAmount < 25 ? 'grey' : 'secondary'"
                  >
                    Snapshots: {{ snapshotAmount }} of at least {{ minSnapshots }}
                  </v-chip>
                </v-row>
              </v-col>
            </v-row>

            <v-row v-if="isCalibrating">
              <v-col
                cols="12"
                class="pt-0"
              >
                <CVslider
                  v-model="$store.getters.currentPipelineSettings.cameraExposure"
                  name="Exposure"
                  :min="0"
                  :max="100"
                  slider-cols="8"
                  @input="e => handlePipelineUpdate('cameraExposure', e)"
                />
                <CVslider
                  v-model="this.$store.getters.currentPipelineSettings.cameraBrightness"
                  name="Brightness"
                  :min="0"
                  :max="100"
                  slider-cols="8"
                  @input="e => handlePipelineUpdate('cameraBrightness', e)"
                />
                <CVslider
                  v-if="$store.getters.currentPipelineSettings.cameraGain !== -1"
                  v-model="$store.getters.currentPipelineSettings.cameraGain"
                  name="Gain"
                  :min="0"
                  :max="100"
                  slider-cols="8"
                  @input="e => handlePipelineUpdate('cameraGain', e)"
                />
              </v-col>
            </v-row>

            <v-row>
              <v-col align-self="center">
                <v-btn
                  small
                  color="secondary"
                  style="width: 100%;"
                  :disabled="disallowCalibration"
                  @click="sendCalibrationMode"
                >
                  {{ isCalibrating ? "Take Snapshot" : "Start Calibration" }}
                </v-btn>
              </v-col>
              <v-col align-self="center">
                <v-btn
                  small
                  :color="hasEnough ? 'accent' : 'red'"
                  :class="hasEnough ? 'black--text' : 'white---text'"
                  style="width: 100%;"
                  :disabled="checkCancellation"
                  @click="sendCalibrationFinish"
                >
                  {{ hasEnough ? "End Calibration" : "Cancel Calibration" }}
                </v-btn>
              </v-col>
              <v-col>
                <v-btn
                  color="accent"
                  small
                  outlined
                  style="width: 100%;"
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
                  download="chessboard.png"
                />
              </v-col>
            </v-row>
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
import TooltippedLabel from "../components/common/cv-tooltipped-label";

export default {
    name: 'Cameras',
    components: {
      TooltippedLabel,
        CVselect,
        CVnumberinput,
        CVslider,
        CVimage
    },
    data() {
        return {
            snackbar: {
                color: "success",
                text: ""
            },
            snack: false,
            filteredVideomodeIndex: 0
        }
    },
    computed: {
        disallowCalibration() {
            return !(this.calibrationData.boardType === 0 || this.calibrationData.boardType === 1);
        },
        checkCancellation() {
            if (this.isCalibrating) {
                return false
            } else if (this.disallowCalibration) {
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
                        const calib = this.getCalibrationCoeffs(it);
                        if(calib != null) {
                            it['standardDeviation'] = calib.standardDeviation;
                            it['mean'] = calib.perViewErrors.reduce((a, b) => a + b) / calib.perViewErrors.length;
                        }
                        filtered.push(it);
                    }
                });
                filtered.sort((a, b) => (b.width + b.height) - (a.width + a.height));
                return filtered
            }
        },

        stringResolutionList: {
            get() {
                return this.filteredResolutionList.map(res => `${res['width']} X ${res['height']}`);
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

        boardType: {
            get() {
                return this.calibrationData.boardType
            },
            set(value) {
                this.$store.commit('mutateCalibrationState', {['boardType']: value});
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
                return this.calibrationData.patternWidth
            },
            set(value) {
                this.$store.commit('mutateCalibrationState', {['patternWidth']: value})
            }
        },
        boardHeight: {
            get() {
                return this.calibrationData.patternHeight
            },
            set(value) {
                this.$store.commit('mutateCalibrationState', {['patternHeight']: value})
            }
        },
        squareSizeIn: {
            get() {
                return this.calibrationData.squareSizeIn
            },
            set(value) {
                this.$store.commit('mutateCalibrationState', {['squareSizeIn']: value})
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
        },

        selectedFilteredResIndex: {
            get() {
                return this.filteredVideomodeIndex
            },
            set(i) {
                console.log(`Setting filtered index to ${i}`)
                this.filteredVideomodeIndex = i
                this.$store.commit('mutateCalibrationState', {['videoModeIndex']: this.filteredResolutionList[i].index});
            }
        },
    },
    methods: {

        getCalibrationCoeffs(resolution) {
            const calList = this.$store.getters.calibrationList;
            let ret = null;
            calList.forEach(cal => {
                if(cal.width === resolution.width && cal.height === resolution.height) {
                    ret = cal
                }
            })
            return ret;
        },
        downloadBoard() {
            this.axios.get("http://" + this.$address + require('../assets/chessboard.png'), {responseType: 'blob'}).then((response) => {
                require('downloadjs')(response.data, "Calibration Board", "image/png")
            })
        },
        sendCameraSettings() {
            this.axios.post("http://" + this.$address + "/api/settings/camera", {
                "settings": this.cameraSettings,
                "index": this.$store.state.currentCameraIndex
            }).then(
                function (response) {
                    if (response.status === 200) {
                        this.$store.state.saveBar = true;
                    }
                }
            )
        },

        isCalibrated(resolution) {
            return this.$store.getters.currentCameraSettings.calibrations
                .some(e => e.width === resolution.width && e.height === resolution.height)
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

                console.log("starting calibration with index " + calData.videoModeIndex)
            }

            this.$socket.send(this.$msgPack.encode(data));
        },
        sendCalibrationFinish() {
            console.log("finishing calibration for index " + this.$store.getters.currentCameraIndex)
            this.axios.post("http://" + this.$address + "/api/settings/endCalibration", this.$store.getters.currentCameraIndex)
            //     .then((response) => {
            //         if (response.status === 200) {
            //             this.snackbar = {
            //                 color: "success",
            //                 text: "Calibration successful! \n" +
            //                     "Standard deviation: " + response.data.toFixed(5)
            //             };
            //             this.snack = true;
            //         }
            //         this.
            //         this.snapshotAmount = 0;
            //     }
            // ).catch(() => {
            //     this.snackbar = {
            //         color: "error",
            //         text: "Calibration Failed!"
            //     };
            //     this.snack = true;
            //     this.isCalibrating = false;
            //     this.hasEnough = false;
            //     this.snapshotAmount = 0;
            // });
        }
    }
}
</script>

<style scoped>
    .v-data-table {
        text-align: center;
        background-color: transparent !important;
        width: 100%;
        height: 100%;
        overflow-y: auto;
    }
    .v-data-table th {
        background-color: #006492 !important;
    }

    .v-data-table th,td {
        font-size: 1rem !important;
    }

    /** This is unfortunately the only way to override table background color **/
    .theme--dark.v-data-table tbody tr:hover:not(.v-data-table__expanded__content):not(.v-data-table__empty-wrapper) {
        background: #005281;
    }
</style>