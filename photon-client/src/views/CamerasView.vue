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
              :list="$store.getters.cameraList"
              :select-cols="$vuetify.breakpoint.mdAndUp ? 10 : 7"
              @input="handleInput('currentCamera',currentCameraIndex)"
            />
            <CVnumberinput
              v-model="cameraSettings.fov"
              :tooltip="cameraSettings.isFovConfigurable ? 'Field of view (in degrees) of the camera measured across the diagonal of the frame, in a video mode which covers the whole sensor area.' : 'This setting is managed by a vendor'"
              name="Maximum diagonal FOV"
              :disabled="!cameraSettings.isFovConfigurable"
              :label-cols="$vuetify.breakpoint.mdAndUp ? undefined : 7"
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
                <v-form
                  ref="form"
                  v-model="settingsValid"
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
                    v-model="streamingFrameDivisor"
                    name="Decimation"
                    tooltip="Resolution to which camera frames are downscaled for detection. Calibration still uses full-res"
                    :list="calibrationDivisors"
                    select-cols="7"
                    @rollback="e => rollback('streamingFrameDivisor', e)"
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
                    tooltip="Spacing between pattern features in inches"
                    :disabled="isCalibrating"
                    :rules="[v => (v > 0) || 'Size must be positive']"
                    :label-cols="$vuetify.breakpoint.mdAndUp ? 5 : 7"
                  />
                  <CVnumberinput
                    v-model="boardWidth"
                    name="Board width"
                    tooltip="Width of the board in dots or chessboard squares"
                    :disabled="isCalibrating"
                    :rules="[v => (v >= 4) || 'Width must be at least 4']"
                    :label-cols="$vuetify.breakpoint.mdAndUp ? 5 : 7"
                  />
                  <CVnumberinput
                    v-model="boardHeight"
                    name="Board height"
                    tooltip="Height of the board in dots or chessboard squares"
                    :disabled="isCalibrating"
                    :rules="[v => (v >= 4) || 'Height must be at least 4']"
                    :label-cols="$vuetify.breakpoint.mdAndUp ? 5 : 7"
                  />
                </v-form>
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
                        <th class="text-center">
                          <tooltipped-label
                            tooltip="Estimated Horizontal FOV, in degrees"
                            text="Horizontal FOV"
                          />
                        </th>
                        <th class="text-center">
                          <tooltipped-label
                            tooltip="Estimated Vertical FOV, in degrees"
                            text="Vertical FOV"
                          />
                        </th>
                        <th class="text-center">
                          <tooltipped-label
                            tooltip="Estimated Diagonal FOV, in degrees"
                            text="Diagonal FOV"
                          />
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="(value, index) in filteredResolutionList"
                        :key="index"
                      >
                        <td> {{ value.width }} X {{ value.height }}</td>
                        <td>
                          {{ isCalibrated(value) ? value.mean.toFixed(2) + "px" : "—" }}
                        </td>
                        <td> {{ isCalibrated(value) ? value.standardDeviation.toFixed(2) + "px" : "—" }} </td>
                        <td> {{ isCalibrated(value) ? value.horizontalFOV.toFixed(2) + "°" : "—" }} </td>
                        <td> {{ isCalibrated(value) ? value.verticalFOV.toFixed(2) + "°" : "—" }} </td>
                        <td> {{ isCalibrated(value) ? value.diagonalFOV.toFixed(2) + "°" : "—" }} </td>
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
                  :disabled="$store.getters.currentPipelineSettings.cameraAutoExposure"
                  name="Exposure"
                  :min="0"
                  :max="100"
                  slider-cols="8"
                  step="0.1"
                  tooltip="Directly controls how much light is allowed to fall onto the sensor, which affects apparent brightness"
                  @input="e => handlePipelineUpdate('cameraExposure', e)"
                />
                <CVslider
                  v-model="$store.getters.currentPipelineSettings.cameraBrightness"
                  name="Brightness"
                  :min="0"
                  :max="100"
                  slider-cols="8"
                  @input="e => handlePipelineUpdate('cameraBrightness', e)"
                />
                <CVswitch
                  v-model="$store.getters.currentPipelineSettings.cameraAutoExposure"
                  class="pt-2"
                  name="Auto Exposure"
                  tooltip="Enables or Disables camera automatic adjustment for current lighting conditions"
                  @input="e => handlePipelineUpdate('cameraAutoExposure', e)"
                />
                <CVslider
                  v-if="cameraGain >= 0"
                  v-model="cameraGain"
                  name="Camera Gain"
                  min="0"
                  max="100"
                  tooltip="Controls camera gain, similar to brightness"
                  :slider-cols="largeBox"
                  @input="handlePipelineData('cameraGain')"
                  @rollback="e => rollback('cameraGain', e)"
                />
                <CVslider
                  v-if="$store.getters.currentPipelineSettings.cameraRedGain !== -1"
                  v-model="$store.getters.currentPipelineSettings.cameraRedGain"
                  name="Red AWB Gain"
                  min="0"
                  max="100"
                  tooltip="Controls red automatic white balance gain, which affects how the camera captures colors in different conditions"
                  :slider-cols="8"
                  @input="e => handlePipelineData('cameraRedGain', e)"
                />
                <CVslider
                  v-if="$store.getters.currentPipelineSettings.cameraBlueGain !== -1"
                  v-model="$store.getters.currentPipelineSettings.cameraBlueGain"
                  name="Blue AWB Gain"
                  min="0"
                  max="100"
                  tooltip="Controls blue automatic white balance gain, which affects how the camera captures colors in different conditions"
                  :slider-cols="8"
                  @input="e => handlePipelineData('cameraBlueGain', e)"
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
                  {{ hasEnough ? "Finish Calibration" : "Cancel Calibration" }}
                </v-btn>
              </v-col>
              <v-col>
                <v-btn
                  color="accent"
                  small
                  outlined
                  style="width: 100%;"
                  :disabled="!settingsValid"
                  @click="downloadBoard"
                >
                  <v-icon left>
                    mdi-download
                  </v-icon>
                  Download Target
                </v-btn>
              </v-col>
              <v-col>
                <v-btn
                  color="secondary"
                  small
                  style="width: 100%;"
                  @click="$refs.importCalibrationFromCalibdb.click()"
                >
                  <v-icon left>
                    mdi-upload
                  </v-icon>
                  Import From CalibDB
                </v-btn>
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
        <template>
          <CVimage
            :id="cameras-cal"
            :idx=1
            :disconnected="!$store.state.backendConnected"
            scale="100"
            style="border-radius: 5px;"
          />
          <v-dialog
            v-model="snack"
            width="500px"
            :persistent="true"
          >
            <v-card
              color="primary"
              dark
            >
              <v-card-title> Camera Calibration </v-card-title>
              <div
                class="ml-3"
              >
                <v-col align="center">
                  <template v-if="calibrationInProgress && !calibrationFailed">
                    <v-progress-circular
                      indeterminate
                      :size="70"
                      :width="8"
                      color="accent"
                    />
                    <v-card-text>Camera is being calibrated. This process make take several minutes...</v-card-text>
                  </template>
                  <template v-else-if="!calibrationFailed">
                    <v-icon
                      color="green"
                      size="70"
                    >
                      mdi-check-bold
                    </v-icon>
                    <v-card-text>Camera has been successfully calibrated at {{ stringResolutionList[selectedFilteredResIndex] }}!</v-card-text>
                  </template>
                  <template v-else>
                    <v-icon
                      color="red"
                      size="70"
                    >
                      mdi-close
                    </v-icon>
                    <v-card-text>Camera calibration failed! Make sure that the photos are taken such that the rainbow grid circles align with the corners of the chessboard, and try again. More information is available in the program logs.</v-card-text>
                  </template>
                </v-col>
              </div>
              <v-card-actions>
                <v-spacer />
                <v-btn
                  v-if="!calibrationInProgress || calibrationFailed"
                  color="white"
                  text
                  @click="closeDialog"
                >
                  OK
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>
        </template>
      </v-col>
    </v-row>

    <!-- Special hidden upload input that gets 'clicked' when the user imports settings -->
    <input
      ref="importCalibrationFromCalibdb"
      type="file"
      accept=".json"
      style="display: none;"
      @change="readImportedCalibration"
    />

    <v-snackbar v-model="uploadSnack" top :color="uploadSnackData.color" timeout="-1">
      <span>{{ uploadSnackData.text }}</span>
    </v-snackbar>

  </div>
</template>

<script>
import CVselect from '../components/common/cv-select';
import CVnumberinput from '../components/common/cv-number-input';
import CVslider from '../components/common/cv-slider';
import CVswitch from '../components/common/cv-switch';
import CVimage from "../components/common/cv-image";
import TooltippedLabel from "../components/common/cv-tooltipped-label";
import jsPDF from "jspdf";
import "../jsPDFFonts/Prompt-Regular-normal.js";

export default {
    name: 'Cameras',
    components: {
        TooltippedLabel,
        CVselect,
        CVnumberinput,
        CVslider,
        CVswitch,
        CVimage
    },
    data() {
        return {
            snack: false,
            calibrationInProgress: false,
            calibrationFailed: false,
            filteredVideomodeIndex: 0,
            settingsValid: true,
            unfilteredStreamDivisors: [1, 2, 4],
            uploadSnackData: {
              color: "success",
              text: "",
            },
            uploadSnack: false,
        }
    },
    computed: {
        disallowCalibration() {
            return !(this.calibrationData.boardType === 0 || this.calibrationData.boardType === 1) || !this.settingsValid;
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

        cameraGain: {
          get() {
              return parseInt(this.$store.getters.currentPipelineSettings.cameraGain)
          },
          set(val) {
              this.$store.commit("mutatePipeline", {"cameraGain": parseInt(val)});
          }
        },

        calibrationDivisors: {
          get() {
            return this.unfilteredStreamDivisors.filter(item => {
              var res = this.stringResolutionList[this.selectedFilteredResIndex].split(" X ").map(it => parseInt(it));
              console.log(res);
              console.log(item);
              // Realistically, we need more than 320x240, but lower than this is
              // basically unusable. For now, don't allow decimations that take us
              // below that
              const ret = ((res[0] / item) >= 300 && (res[1] / item) >= 220) || (item === 1);
              console.log(ret);
              return ret;
            })
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
                        if (calib != null) {
                            it['standardDeviation'] = calib.standardDeviation;
                            it['mean'] = calib.perViewErrors.reduce((a, b) => a + b) / calib.perViewErrors.length;
                            it['horizontalFOV'] = 2 * Math.atan2(it.width/2,calib.intrinsics[0]) * (180/Math.PI);
                            it['verticalFOV'] = 2 * Math.atan2(it.height/2,calib.intrinsics[4]) * (180/Math.PI);
                            it['diagonalFOV'] = 2 * Math.atan2(Math.sqrt(it.width**2 + (it.height/(calib.intrinsics[4]/calib.intrinsics[0]))**2)/2,calib.intrinsics[0]) * (180/Math.PI);
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

        streamingFrameDivisor: {
            get() {
                return this.$store.getters.currentPipelineSettings.streamingFrameDivisor;
            },
            set(val) {
                this.$store.commit("mutatePipeline", {"streamingFrameDivisor": val});
                this.handlePipelineUpdate("streamingFrameDivisor", val);
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
                console.log(`Setting filtered index to ${i}`);
                this.filteredVideomodeIndex = i;
                this.$store.commit('mutateCalibrationState', {['videoModeIndex']: this.filteredResolutionList[i].index});
            }
        },
    },
    methods: {

    readImportedCalibration(event) {
      // let formData = new FormData();
      // formData.append("zipData", event.target.files[0]);
      const filename = event.target.files[0].name;

      event.target.files[0].text().then(fileText => {
        const data = {
          "cameraIndex": this.$store.getters.currentCameraIndex,
          "payload": fileText,
          "filename": filename,
        };

        this.axios
          .post("http://" + this.$address + "/api/calibration/import", data, {
            headers: { "Content-Type": "text/plain" },
          })
          .then(() => {
            this.uploadSnackData = {
              color: "success",
              text:
                "Calibration imported successfully! PhotonVision will restart in the background...",
            };
            this.uploadSnack = true;
          })
          .catch((err) => {
            if (err.response) {
              this.uploadSnackData = {
                color: "error",
                text:
                  "Error while uploading calibration file! Could not process provided file.",
              };
            } else if (err.request) {
              this.uploadSnackData = {
                color: "error",
                text:
                  "Error while uploading calibration file! No respond to upload attempt.",
              };
            } else {
              this.uploadSnackData = {
                color: "error",
                text: "Error while uploading calibration file!",
              };
            }
            this.uploadSnack = true;
          });

        })

    },

        closeDialog() {
            this.snack = false;
            this.calibrationInProgress = false;
            this.calibrationFailed = false;
        },
        getCalibrationCoeffs(resolution) {
            const calList = this.$store.getters.calibrationList;
            let ret = null;
            calList.forEach(cal => {
                if (cal.width === resolution.width && cal.height === resolution.height) {
                    ret = cal
                }
            });
            return ret;
        },
        downloadBoard() {
            // Generates a .pdf of a board for calibration and downloads it

            //Murica paper.
            var doc = new jsPDF({unit: 'in', format:'letter'});
            var paper_x = 8.5;
            var paper_y = 11.0;

            //Load in custom fonts
            console.log(doc.getFontList());
            doc.setFont('Prompt-Regular');
            doc.setFontSize(12);

            // Common Parameters
            var num_x = this.boardWidth;
            var num_y = this.boardHeight;
            var patternSize = this.squareSizeIn;
            var isCheckerboard = (this.boardType==0);

            var x_coord = 0.0;
            var y_coord = 0.0;
            var x_idx = 0;
            var y_idx = 0;
            var start_x = 0;
            var start_y = 0;

            var annotation = num_x + " x " + num_y + " | " + patternSize + "in "

            if(isCheckerboard){
              ///////////////////////////////////////////
              // Checkerboard Pattern

              start_x = paper_x/2.0 - (num_x * patternSize)/2.0;
              start_y = paper_y/2.0 - (num_y * patternSize)/2.0;

              for(y_idx = 0; y_idx < num_y; y_idx++){
                for(x_idx = 0; x_idx < num_x; x_idx++){

                  x_coord = start_x + x_idx * patternSize;
                  y_coord = start_y + y_idx * patternSize;
                  if((x_idx + y_idx) % 2 == 0){
                    doc.rect(x_coord, y_coord, patternSize, patternSize, "F");
                  }
                }
              }

            } else {
              ///////////////////////////////////////////
              // Assymetric Dot-Grid Pattern
              // see https://github.com/opencv/opencv/blob/b450dd7a87bc69997a8417d94bdfb87427a9fe62/modules/calib3d/src/circlesgrid.cpp#L437
              // as well as FindBoardCornersPipe.java's Dotboard implementation

              start_x = paper_x/2.0 - ((2*(num_x-1) + (num_y-1) % 2) * patternSize)/2.0;
              start_y = paper_y/2.0 - (num_y-1 * patternSize)/2.0;

              // Dot Grid Pattern
              for(y_idx = 0; y_idx < num_y; y_idx++){
                for(x_idx = 0; x_idx < num_x; x_idx++){
                  x_coord = start_x + (2*x_idx + y_idx % 2) * patternSize;
                  y_coord = start_y + y_idx * patternSize;
                  doc.circle(x_coord, y_coord, patternSize/4.0, "F");
                }
              }
            }

            ///////////////////////////////////////////
            // Draw a fixed size inch ruler pattern to
            // help users debug their printers
            var lineStartX = 1.0;
            var lineEndX = paper_x - lineStartX;
            var lineY = paper_y - 1.0;
            doc.setFont('Prompt-Regular');
            doc.setLineWidth(0.01);
            doc.line(lineStartX, lineY, lineEndX, lineY);
            var segIdx = 0;
            for(var tickX = lineStartX; tickX <= lineEndX; tickX += 1.0){
              doc.line(tickX, lineY, tickX, lineY + 0.25);
              doc.text(String(segIdx) + (segIdx == 0 ? " in" : ""), tickX + 0.1, lineY + 0.25);
              segIdx++;
            }


            ///////////////////////////////////////////
            // Annotate what was drawn + branding
            var img = new Image();
            img.src = require('@/assets/logoMono.png');
            doc.addImage(img, 'PNG', 1.0, 0.75, 1.4, 0.5 );
            doc.setFont('Prompt-Regular');
            doc.text(annotation, paper_x-1.0, 1.0, {maxWidth:(paper_x - 2.0)/2, align:"right"});

            doc.save("calibrationTarget.pdf");

        },
        sendCameraSettings() {
            this.axios.post("http://" + this.$address + "/api/settings/camera", {
                "settings": this.cameraSettings,
                "index": this.$store.state.currentCameraIndex
            }).then(response => {
                    if (response.status === 200) {
                        this.$store.state.saveBar = true;
                    }
                }
            )
        },

        isCalibrated(resolution) {
            return this.$store.getters.currentCameraSettings.calibrations
                .some(e => e.width === resolution.width && e.height === resolution.height);
        },

        sendCalibrationMode() {
            let data = {
                ['cameraIndex']: this.$store.state.currentCameraIndex
            };

            if (this.isCalibrating === true) {
                data['takeCalibrationSnapshot'] = true
            } else {
                // This store prevents an edge case of a user not selecting a different resolution, which causes the set logic to not be called
                this.$store.commit('mutateCalibrationState', {['videoModeIndex']: this.filteredResolutionList[this.selectedFilteredResIndex].index});
                const calData = this.calibrationData;
                calData.isCalibrating = true;
                data['startPnpCalibration'] = calData;
                console.log("starting calibration with index " + calData.videoModeIndex);
            }
            this.$store.commit('currentPipelineIndex', -2);
            this.$store.state.websocket.ws.send(this.$msgPack.encode(data));
        },
        sendCalibrationFinish() {
            console.log("finishing calibration for index " + this.$store.getters.currentCameraIndex);

            this.snack = true;
            this.calibrationInProgress = true;

            this.axios.post("http://" + this.$address + "/api/settings/endCalibration", this.$store.getters.currentCameraIndex)
                .then((response) => {
                        if (response.status === 200) {
                            this.calibrationInProgress = false;
                        } else {
                            this.calibrationFailed = true;
                        }
                    }
                ).catch(() => {
                    this.calibrationFailed = true;
            });
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

.v-data-table th, td {
    font-size: 1rem !important;
}
</style>
