<template>
  <div>
    <v-container
        class="pa-3"
        fluid
    >
      <v-row
          no-gutters
          align="center"
          justify="center"
      >
        <v-col
            cols="12"
            :class="['pb-3 ', 'pr-lg-3']"
            lg="8"
            align-self="stretch"
        >
          <v-card
              color="primary"
              height="100%"
              style="display: flex; flex-direction: column"
              dark
          >
            <v-card-title
                class="pb-0 mb-0 pl-4 pt-1"
                style="height: 15%; min-height: 50px;"
            >
              Cameras
              <v-chip
                  :class="fpsTooLow ? 'ml-2 mt-1' : 'mt-2'"
                  x-small
                  label
                  :color="fpsTooLow ? 'error' : 'transparent'"
                  :text-color="fpsTooLow ? 'white' : 'grey'"
              >
                <span class="pr-1">Processing @ {{ Math.round($store.state.pipelineResults.fps) }}&nbsp;FPS &ndash;</span>
                <span v-if="fpsTooLow && !$store.getters.currentPipelineSettings.inputShouldShow && $store.getters.pipelineType == 2">HSV thresholds are too broad; narrow them for better performance</span>
                <span v-else-if="fpsTooLow && getters.currentCameraSettings.inputShouldShow">stop viewing the raw stream for better performance</span>
                <span v-else>{{ Math.min(Math.round($store.state.pipelineResults.latency), 9999) }} ms latency</span>
              </v-chip>
              <v-switch
                  v-model="driverMode"
                  label="Driver Mode"
                  style="margin-left: auto;"
                  color="accent"
              />
            </v-card-title>
            <v-row
                align="center"
            >
              <v-col
                  v-for="idx in (selectedOutputs instanceof Array ? selectedOutputs : [selectedOutputs])"
                  :key="idx"
                  cols="12"
                  :md="selectedOutputs.length === 1 ? 12 : Math.floor(12 / selectedOutputs.length)"
                  class="pb-0 pt-0"
                  style="height: 100%;"
              >
                <div style="position: relative; width: 100%; height: 100%;">
                  <cv-image
                      :id="idx === 0 ? 'raw-stream' : 'processed-stream'"
                      ref="streams"
                      :idx=idx
                      :disconnected="!$store.state.backendConnected"
                      scale="100"
                      :max-height="$store.getters.isDriverMode ? '40vh' : '300px'"
                      :max-height-md="$store.getters.isDriverMode ? '50vh' : '380px'"
                      :max-height-lg="$store.getters.isDriverMode ? '55vh' : '390px'"
                      :max-height-xl="$store.getters.isDriverMode ? '60vh' : '450px'"
                      :alt="idx === 0 ? 'Raw stream' : 'Processed stream'"
                      :color-picking="$store.state.colorPicking && idx === 0"
                      @click="onImageClick"
                  />
                </div>
              </v-col>
            </v-row>
          </v-card>
        </v-col>
        <v-col
            cols="12"
            class="pb-3"
            lg="4"
            align-self="stretch"
        >
          <v-card
              color="primary"
          >
            <camera-and-pipeline-select />
          </v-card>
          <v-card
              :disabled="$store.getters.isDriverMode || $store.state.colorPicking"
              class="mt-3"
              color="primary"
          >
            <v-row
                align="center"
                class="pl-3 pr-3"
            >
              <v-col lg="12">
                <p style="color: white;">
                  Processing mode:
                </p>
                <v-btn-toggle
                    v-model="processingMode"
                    mandatory
                    dark
                    class="fill"
                >
                  <v-btn
                      color="secondary"
                  >
                    <v-icon>mdi-crop-square</v-icon>
                    <span>2D</span>
                  </v-btn>
                  <v-btn
                      color="secondary"
                      @click="on3DClick"
                  >
                    <v-icon>mdi-cube-outline</v-icon>
                    <span>3D</span>
                  </v-btn>
                </v-btn-toggle>
              </v-col>
              <v-col lg="12">
                <p style="color: white;">
                  Stream display:
                </p>
                <v-btn-toggle
                    v-model="selectedOutputs"
                    :multiple="$vuetify.breakpoint.mdAndUp"
                    mandatory
                    dark
                    class="fill"
                >
                  <v-btn
                      color="secondary"
                      class="fill"
                  >
                    <v-icon>mdi-import</v-icon>
                    <span>Raw</span>
                  </v-btn>
                  <v-btn
                      color="secondary"
                      class="fill"
                  >
                    <v-icon>mdi-export</v-icon>
                    <span>Processed</span>
                  </v-btn>
                </v-btn-toggle>
              </v-col>
            </v-row>
          </v-card>
        </v-col>
      </v-row>
      <v-row no-gutters>
        <v-col
            v-for="(tabs, idx) in tabGroups"
            :key="idx"
            :cols="Math.floor(12 / tabGroups.length)"
            :class="idx !== tabGroups.length - 1 ? 'pr-3' : ''"
            align-self="stretch"
        >
          <v-card
              color="primary"
              height="100%"
              class="pr-4 pl-4"
          >
            <v-tabs
                v-if="!$store.getters.isDriverMode"
                v-model="selectedTabs[idx]"
                grow
                background-color="primary"
                dark
                height="48"
                slider-color="accent"
            >
              <v-tab
                  v-for="(tab, i) in tabs"
                  :key="i"
              >
                {{ tab.name }}
              </v-tab>
            </v-tabs>
            <div class="pl-4 pr-4 pt-2">
              <keep-alive>
                <component
                    :is="(tabs[selectedTabs[idx]] || tabs[0]).component"
                    :ref="(tabs[selectedTabs[idx]] || tabs[0]).name"
                    v-model="$store.getters.pipeline"
                    @update="$emit('save')"
                />
              </keep-alive>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>

    <v-snackbar
        v-model="showNTWarning"
        color="error"
        timeout="-1"
        top
    >
      {{ $store.state.settings.networkSettings.runNTServer ?
        "NetworkTables server enabled! PhotonLib may not work." :
        "NetworkTables not connected! Are you on a network with a robot?" }}
      <template v-slot:action>
        <v-btn
            text
            @click="hideNTWarning = true"
        >
          Hide
        </v-btn>
      </template>
    </v-snackbar>

    <v-dialog
        v-model="dialog"
        width="500"
    >
      <v-card
          color="primary"
          dark
      >
        <v-card-title>
          Current resolution not calibrated
        </v-card-title>

        <v-card-text>
          Because the current resolution {{ this.$store.getters.currentVideoFormat.width }} x {{ this.$store.getters.currentVideoFormat.height }} is not yet calibrated, 3D mode cannot be enabled. Please
          <a
              href="/#/cameras"
              class="white--text"
              @click="$emit('switch-to-cameras')"
          > visit the Cameras tab</a> to calibrate this resolution. For now, SolvePNP will do nothing.
        </v-card-text>

        <v-divider />

        <v-card-actions>
          <v-spacer />
          <v-btn
              color="white"
              text
              @click="closeUncalibratedDialog"
          >
            OK
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import CameraAndPipelineSelect from "../components/pipeline/CameraAndPipelineSelect";
import cvImage from '../components/common/cv-image';
import InputTab from './PipelineViews/InputTab';
import ThresholdTab from './PipelineViews/ThresholdTab';
import ContoursTab from './PipelineViews/ContoursTab';
import OutputTab from './PipelineViews/OutputTab';
import TargetsTab from "./PipelineViews/TargetsTab";
import Map3DTab from './PipelineViews/Map3DTab';
import PnPTab from './PipelineViews/PnPTab';
import AprilTagTab from './PipelineViews/AprilTagTab';
import ArucoTab from './PipelineViews/ArucoTab';

export default {
  name: 'Pipeline',
  components: {
    CameraAndPipelineSelect,
    cvImage,
    InputTab,
    ThresholdTab,
    ContoursTab,
    OutputTab,
    TargetsTab,
    Map3DTab,
    PnPTab,
    AprilTagTab,
    ArucoTab,
  },
  data() {
    return {
      selectedTabsData: [0, 0, 0, 0],
      counterData: 0,
      dialog: false,
      processingModeOverride: false,
      hideNTWarning: false,
    }
  },
  computed: {
    selectedTabs: {
      get() {
        return this.$store.getters.isDriverMode ? [0] : this.selectedTabsData;
      },
      set(value) {
        this.selectedTabsData = value;
      }
    },
    tabGroups: {
      get() {
        let tabs = {
          input: {
            name: "Input",
            component: "InputTab",
          },
          threshold: {
            name: "Threshold",
            component: "ThresholdTab",
          },
          contours: {
            name: "Contours",
            component: "ContoursTab",
          },
          apriltag: {
            name: "AprilTag",
            component: "AprilTagTab",
          },
          aruco: {
            name: "Aruco",
            component: "ArucoTab",
          },
          output: {
            name: "Output",
            component: "OutputTab",
          },
          targets: {
            name: "Targets",
            component: "TargetsTab",
          },
          pnp: {
            name: "PnP",
            component: "PnPTab",
          },
          map3d: {
            name: "3D",
            component: "Map3DTab",
          }
        };

        // If not in 3d, name "3D" is illegal
        const allow3d = this.$store.getters.currentPipelineSettings.solvePNPEnabled;
        // If in apriltag, "Threshold" and "Contours" are illegal -- otherwise "AprilTag" is
        const isAprilTag = (this.$store.getters.currentPipelineSettings.pipelineType - 2) === 2;
        const isAruco = (this.$store.getters.currentPipelineSettings.pipelineType - 2) === 3;

        // 2D array of tab names and component names; each sub-array is a separate tab group
        let ret = [];
        if (this.$vuetify.breakpoint.smAndDown || this.$store.getters.isDriverMode || (this.$vuetify.breakpoint.mdAndDown && !this.$store.state.compactMode)) {
          // One big tab group with all the tabs
          ret[0] = Object.values(tabs);
        } else if (this.$vuetify.breakpoint.mdAndDown || !this.$store.state.compactMode) {
          // Two tab groups, one with "input, threshold, contours, output" and the other with "target info, 3D"
          ret[0] = [tabs.input, tabs.threshold, tabs.contours, tabs.apriltag, tabs.aruco, tabs.output];
          ret[1] = [tabs.targets, tabs.pnp, tabs.map3d];
        } else if (this.$vuetify.breakpoint.lgAndDown) {
          // Three tab groups, one with "input", one with "threshold, contours, output", and the other with "target info, 3D"
          ret[0] = [tabs.input];
          ret[1] = [tabs.threshold, tabs.contours, tabs.apriltag,tabs.aruco, tabs.output];
          ret[2] = [tabs.targets, tabs.pnp, tabs.map3d];
        } else if (this.$vuetify.breakpoint.xl) {
          // Three tab groups, one with "input", one with "threshold, contours", and the other with "output, target info, 3D"
          ret[0] = [tabs.input];
          ret[1] = [tabs.threshold];
          ret[2] = [tabs.contours, tabs.apriltag, tabs.aruco,tabs.output];
          ret[3] = [tabs.targets, tabs.pnp, tabs.map3d];
        }

        for(let i = 0; i < ret.length; i++) {
          const group = ret[i];

          // All the tabs we allow
          const filteredGroup = group.filter(it =>
              !(!allow3d && it.name === "3D") //Filter out 3D tab any time 3D isn't calibrated
              && !((!allow3d || isAprilTag || isAruco) && it.name === "PnP") //Filter out the PnP config tab if 3D isn't available, or we're doing Apriltags
              && !((isAprilTag || isAruco) && (it.name === "Threshold")) //Filter out threshold tab if we're doing apriltags
              && !((isAprilTag || isAruco)&& (it.name === "Contours")) //Filter out contours if we're doing Apriltag
              && !(!isAprilTag && it.name === "AprilTag") //Filter out apriltag unless we actually are doing Apriltags
              && !(!isAruco && it.name === "Aruco")
          );
          ret[i] = filteredGroup;
        }

        // One last filter to remove empty lists
        return ret.filter(it => it !== undefined && it.length > 0);
      }
    },
    processingMode: {
      get() {
        return (this.$store.getters.currentPipelineSettings.solvePNPEnabled || this.processingModeOverride) ? 1 : 0;
      },
      set(value) {
        if (this.$store.getters.isCalibrated) {
          this.$store.getters.currentPipelineSettings.solvePNPEnabled = value === 1;
          this.handlePipelineUpdate("solvePNPEnabled", value === 1);
        }
      }
    },
    driverMode: {
      get() {
        return this.$store.getters.isDriverMode;
      },
      set(value) {
        this.$store.getters.currentCameraSettings.currentPipelineIndex = value ? -1 : 0;
        this.handleInputWithIndex('currentPipeline', value ? -1 : 0);
      }
    },
    selectedOutputs: {
      // All this logic exists to deal with the reality that the output select buttons sometimes need an array and sometimes need a number (depending on whether or not they're exclusive)
      get() {
        // We switch the selector to single-select only on sm-and-down size devices, so we have to return a Number instead of an Array in that state
        let ret = [];
        if (this.$store.state.colorPicking) {
          ret = [0]; // We want the input stream only while color picking
        } else if (this.$store.getters.isDriverMode) {
          ret = [1]; // We want only the output stream in driver mode
        } else {
          if (this.$store.getters.currentPipelineSettings.inputShouldShow) ret = ret.concat([0]);
          if (this.$store.getters.currentPipelineSettings.outputShouldShow) ret = ret.concat([1]);
          if (!ret.length) ret = [0];
        }

        if (this.$vuetify.breakpoint.mdAndUp) {
          return ret;
        } else {
          return ret[0] || 0;
        }
      },
      set(value) {
        let valToCommit = [0];
        if (value instanceof Array) {
          // Value is already an array, we don't need to do anything
          valToCommit = value;
        } else if (value) {
          // Value is assumed to be a number, so we wrap it into an array
          valToCommit = [value];
        }

        this.$store.commit("mutatePipeline", {"inputShouldShow": valToCommit.includes(0)});
        this.$store.commit("mutatePipeline", {"outputShouldShow": valToCommit.includes(1)});
        this.handlePipelineUpdate("inputShouldShow", valToCommit.includes(0));
      }
    },
    fpsTooLow: {
      get() {
        // For now we only show the FPS is too low warning when GPU acceleration is enabled, because we don't really trust the presented video modes otherwise
        const currFPS = this.$store.state.pipelineResults.fps;
        const targetFPS = this.$store.getters.currentVideoFormat.fps;
        const driverMode = this.$store.getters.isDriverMode;
        const gpuAccel = this.$store.state.settings.general.gpuAcceleration === true;
        const isReflective = this.$store.getters.pipelineType === 2;

        return (currFPS - targetFPS) < -5 && this.$store.state.pipelineResults.fps !== 0 && !driverMode && gpuAccel && isReflective;
      }
    },
    latency: {
      get() {
        return this.$store.getters.currentPipelineResults.latency;
      }
    },
    isCalibrated: {
      get() {
        const resolution = this.$store.getters.videoFormatList[this.$store.getters.currentPipelineSettings.cameraVideoModeIndex];
        return this.$store.getters.currentCameraSettings.calibrations
            .some(e => e.width === resolution.width && e.height === resolution.height)
      }
    },
    isRobotConnected: {
      get() {
        // return this.$store.state.ntConnectionInfo.connected && this.$store.state.backendConnected;
        return true;
      }
    },
    showNTWarning: {
      get() {
        return (!this.$store.state.ntConnectionInfo.connected || this.$store.state.settings.networkSettings.runNTServer) && this.$store.state.settings.networkSettings.teamNumber > 0 && this.$store.state.backendConnected && !this.hideNTWarning;
      }
    },
  },
  created() {
    this.$store.state.connectedCallbacks.push(this.reloadStreams)
  },
  methods: {
    reloadStreams() {
      // Reload the streams as we technically close and reopen them
      this.$refs.streams.forEach(it => it.reload())
    },
    onImageClick(event) {
      // Get a reference to the threshold tab (if it is shown) and call its "onClick" method
      let ref = this.$refs["Threshold"];
      if (ref && ref[0])
        ref[0].onClick(event)
    },
    on3DClick() {
      if (!this.$store.getters.isCalibrated) {
        this.dialog = true;
        this.processingModeOverride = true;
      }
    },
    closeUncalibratedDialog() {
      this.dialog = false;
      this.processingModeOverride = false;
      // this.$store.getters.currentPipelineSettings.solvePNPEnabled = false;
      this.handlePipelineUpdate("solvePNPEnabled", false);
    }
  }
}
</script>

<style scoped>
.v-btn-toggle.fill {
  width: 100%;
  height: 100%;
}

.v-btn-toggle.fill > .v-btn {
  width: 50%;
  height: 100%;
}

th {
  width: 80px;
  text-align: center;
}
</style>
